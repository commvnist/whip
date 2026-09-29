package com.whip.app.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.PushPin
import com.whip.app.core.AppSettings
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.calculateHabitTimerElapsedSeconds
import com.whip.app.data.HabitDeletionImpact
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
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.withConfigurationSemantics
import com.whip.app.domain.DEFAULT_HABIT_EMOJI
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.compactNumericSequence
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.plainNumericValue
import com.whip.app.domain.preciseSum
import com.whip.app.domain.preciseAverage
import com.whip.app.domain.parseNumericSequence
import com.whip.app.domain.periodBounds
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.domain.valueInUnit
import com.whip.app.domain.validationErrors
import com.whip.app.domain.dayStateOn
import com.whip.app.domain.successfulPeriodOutcomeDates
import com.whip.app.domain.supportsQuickAddAmounts
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.delay
import com.whip.app.ui.theme.whipColors

private val habitShortDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

enum class HabitDestination(val label: String) {
    Today("Today"),
    All("Habits"),
    Archived("Archived"),
    Insights("Insights"),
}

@Composable
fun HabitAreaContent(
    onBackToSource: (() -> Unit)? = null,
    state: HabitUiState,
    editorState: HabitUiState = state,
    innerPadding: PaddingValues,
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    editorModifier: Modifier = modifier,
    createRequested: Boolean = false,
    onCreateRequestConsumed: () -> Unit = {},
    openHabitIdRequest: Long? = null,
    onOpenHabitRequestConsumed: () -> Unit = {},
    editHabitIdRequest: Long? = null,
    onEditHabitRequestConsumed: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    lowPressureMode: Boolean = false,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    onCreateCustomUnit: CreateCustomUnitAction = UnavailableCreateCustomUnitAction,
    customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveIdentityEmoji: (CustomIdentityEmoji) -> Unit = {},
    onRemoveSavedIdentityEmoji: (String) -> Unit = {},
    areaScopeLabel: String? = null,
    onShowAllAreasForReorder: () -> Unit = {},
    onAreaChanged: (EntitySaveReceipt) -> Unit = {},
    destinationState: MutableState<HabitDestination>? = null,
    showWorkspace: Boolean = true,
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
    mutationRequestNamespace: String = "habit-workspace",
) {
    val localDestinationState = rememberSaveable { mutableStateOf(HabitDestination.All) }
    val activeDestinationState = destinationState ?: localDestinationState
    var destination by activeDestinationState
    var workspaceReordering by rememberSaveable { mutableStateOf(false) }
    var localReorderDismiss by rememberSaveable { mutableStateOf(0) }
    var archiveReturn by rememberSaveable { mutableStateOf(HabitDestination.All) }
    val pages = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val inspectorPages = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    BackHandler(enabled = showWorkspace && destination == HabitDestination.Archived) { if (onBackToSource != null) onBackToSource() else destination = archiveReturn }
    if (state.loading || state.errorMessage != null) {
        if (showWorkspace) Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DestinationTabBar(
                selected = destination.takeUnless { it == HabitDestination.Archived } ?: archiveReturn,
                destinations = listOf(HabitDestination.All, HabitDestination.Today, HabitDestination.Insights),
                onSelect = { destination = it },
                label = HabitDestination::label,
                testTagPrefix = "habit-destination",
                testTagValue = HabitDestination::name,
                barTestTag = "habit-workspace-navigation",
            )
            WhipWorkspaceHeader("Loading habits")
            DomainLoadContent("habits", PaddingValues(), state.errorMessage, viewModel::retryLoading)
        }
        return
    }
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historyHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    fun closeHabitActions() {
        actionsHabitId?.let { inspectorPages.removeState("habit-$it") }
        actionsHabitId = null
        historyHabitId = null
    }
    var numericLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var numericLogAddsAmount by rememberSaveable { mutableStateOf(false) }
    var pauseRequestHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingPauseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var templatesOpen by rememberSaveable { mutableStateOf(false) }
    var reorderAllRequested by rememberSaveable { mutableStateOf(false) }
    var templateDraft by rememberSaveable { mutableStateOf<HabitDraft?>(null) }
    var historicalLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historicalLogForToday by rememberSaveable { mutableStateOf(false) }
    var editingLogHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingLogId by rememberSaveable { mutableStateOf<Long?>(null) }
    var focusedArchivedHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var skipConfirmationHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    val timerReviewPrompt by viewModel.timerReviewPrompt.collectAsStateWithLifecycle()
    val habitDeletionReview by viewModel.habitDeletionReviewState.collectAsStateWithLifecycle()
    val habitDeletionRequestState by viewModel.habitDeletionRequestState.collectAsStateWithLifecycle()
    val progressById = (state.all + state.today + state.archivedProgress).associateBy { it.habit.id }
    val editorProgressById = (editorState.all + editorState.today + editorState.archivedProgress)
        .associateBy { it.habit.id }
    val editing = editingHabitId?.let(editorProgressById::get)
    val actions = actionsHabitId?.let(editorProgressById::get)
    val numericLog = numericLogHabitId?.let(editorProgressById::get)
    val pauseRequest = pauseRequestHabitId?.let(editorProgressById::get)
    val historicalLogHabit = historicalLogHabitId?.let(editorProgressById::get)
    val restoredLogTarget = editingLogId?.let { id -> editorState.logs.firstOrNull { it.id == id } }
    val liveEditingLog = restoredLogTarget?.takeIf { it.habitId == editingLogHabitId }
    LaunchedEffect(editingLogHabitId, editingLogId, restoredLogTarget) {
        if (restoredLogTarget != null && restoredLogTarget.habitId != editingLogHabitId) {
            editingLogHabitId = null
            editingLogId = null
        }
    }
    var editingLogSnapshot by rememberSaveable(editingLogId) { mutableStateOf(liveEditingLog) }
    LaunchedEffect(editingLogId, liveEditingLog) {
        editingLogSnapshot = when {
            editingLogId == null -> null
            liveEditingLog != null -> liveEditingLog
            editingLogSnapshot?.id == editingLogId -> editingLogSnapshot
            else -> null
        }
    }
    val editingLog = editingLogHabitId?.let(editorProgressById::get)?.let { item ->
        (liveEditingLog ?: editingLogSnapshot?.takeIf {
            it.id == editingLogId && it.habitId == editingLogHabitId
        })?.let { item to it }
    }
    val restoredPauseTarget = editingPauseId?.let { id -> editorState.pauses.firstOrNull { it.id == id } }
    val liveEditingPause = restoredPauseTarget?.takeIf { it.habitId == pauseRequestHabitId }
    LaunchedEffect(pauseRequestHabitId, editingPauseId, restoredPauseTarget) {
        if (restoredPauseTarget != null && restoredPauseTarget.habitId != pauseRequestHabitId) {
            pauseRequestHabitId = null
            editingPauseId = null
        }
    }
    var editingPauseSnapshot by rememberSaveable(editingPauseId) { mutableStateOf(liveEditingPause) }
    LaunchedEffect(editingPauseId, liveEditingPause) {
        editingPauseSnapshot = when {
            editingPauseId == null -> null
            liveEditingPause != null -> liveEditingPause
            editingPauseSnapshot?.id == editingPauseId -> editingPauseSnapshot
            else -> null
        }
    }
    val editingPause = liveEditingPause ?: editingPauseSnapshot?.takeIf {
        it.id == editingPauseId && it.habitId == pauseRequestHabitId
    }
    val habitDeletionCoordinator = habitDeletionReview.requestNamespace?.let { requestNamespace ->
        rememberPersistenceRequestCoordinator(
            state = habitDeletionRequestState,
            consume = viewModel::consumeHabitDeletionResult,
            key = requestNamespace,
            requestNamespace = requestNamespace,
            orphanedMessage =
                "The previous permanent deletion was interrupted and its outcome is unknown. " +
                    "Verify the current Habit before retrying.",
            onPersisted = {},
        )
    }
    val editorSaveState by viewModel.editorSaveState.collectAsStateWithLifecycle()
    val editorSaveCoordinator = rememberEntitySaveCoordinator(
        state = editorSaveState,
        consume = viewModel::consumeEditorSaveResult,
        key = editingHabitId ?: if (creating) "creating-habit" else "no-habit-editor",
        onPersisted = { receipt ->
            onAreaChanged(receipt)
            creating = false
            editingHabitId = null
            templateDraft = null
        },
    )
    val authoredMutationSurfaceOpen = actionsHabitId != null ||
        numericLogHabitId != null ||
        historicalLogHabitId != null ||
        editingLogHabitId != null ||
        pauseRequestHabitId != null
    val authoredMutationState by viewModel.authoredMutationState.collectAsStateWithLifecycle()
    val authoredMutationCoordinator = if (authoredMutationSurfaceOpen) {
        rememberPersistenceRequestCoordinator(
            state = authoredMutationState,
            consume = viewModel::consumeAuthoredMutationResult,
            key = mutationRequestNamespace,
            requestNamespace = mutationRequestNamespace,
            onPersisted = {
                numericLogHabitId = null
                historicalLogHabitId = null
                historicalLogForToday = false
                editingLogHabitId = null
                editingLogId = null
                pauseRequestHabitId = null
                editingPauseId = null
            },
        )
    } else null
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
    if (showWorkspace) Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        DestinationTabBar(
            selected = destination.takeUnless { it == HabitDestination.Archived } ?: archiveReturn,
            destinations = listOf(HabitDestination.All, HabitDestination.Today, HabitDestination.Insights),
            onSelect = { destination = it; focusedArchivedHabitId = null },
            label = HabitDestination::label,
            testTagPrefix = "habit-destination",
            testTagValue = HabitDestination::name,
            barTestTag = "habit-workspace-navigation",
        )
        WhipWorkspaceHeader(
            summary = if (workspaceReordering) "Reordering Habits" else when (destination) {
                HabitDestination.Today -> "${state.today.count { !it.isFinishedForToday() }} remaining · Today"
                HabitDestination.All -> "${state.all.size} habits · Schedules & settings"
                HabitDestination.Insights -> "Patterns across ${state.all.size} habits"
                HabitDestination.Archived -> "Archived Habits"
            },
            onBack = { if (onBackToSource != null) onBackToSource() else destination = archiveReturn }.takeIf { destination == HabitDestination.Archived },
        ) {
            if (workspaceReordering) WhipTextButton(onClick = { localReorderDismiss++ }) { Text("Done") }
            else WhipWorkspaceMore("More Habit Actions") { close ->
                WhipMenuItem("Browse Templates", onClick = { close(); templatesOpen = true })
                if (destination != HabitDestination.Archived) WhipMenuItem("Archived Habits", onClick = {
                    close(); archiveReturn = destination; destination = HabitDestination.Archived
                })
                if (destination == HabitDestination.All && (state.all.size > 1 || areaScopeLabel != null)) WhipMenuItem(if (areaScopeLabel == null) "Reorder Habits" else "Show All Areas & Reorder", onClick = {
                    close(); if (areaScopeLabel != null) onShowAllAreasForReorder(); reorderAllRequested = true
                })
            }
        }
        val emptyArea = areaScopeLabel != null && state.all.isEmpty() && editorState.all.isNotEmpty()
        pages.SaveableStateProvider(destination.name) { when (destination) {
            HabitDestination.Today -> HabitList(
                title = "Today",
                progress = state.today,
                customUnits = state.customUnits,
                empty = when {
                    emptyArea -> "Your habits are saved in other Areas. Show all Areas to find them."
                    state.all.isEmpty() -> "Choose a simple template or use + to create a Habit from scratch."
                    else -> "No check-ins are expected today${areaScopeLabel?.let { " in $it" }.orEmpty()}. Your habits and their schedules are still saved."
                },
                emptyTitle = when {
                    emptyArea -> "No Habits in $areaScopeLabel"
                    state.all.isNotEmpty() -> "No Habits Due Today"
                    else -> "No Habits Here"
                },
                emptyActionLabel = when {
                    emptyArea -> "Show All Areas"
                    state.all.isNotEmpty() -> "View All Habits"
                    else -> "Browse Templates"
                },
                onEmptyAction = {
                    when {
                        emptyArea -> onShowAllAreasForReorder()
                        state.all.isNotEmpty() -> destination = HabitDestination.All
                        else -> templatesOpen = true
                    }
                },
                onOpen = { actionsHabitId = it.habit.id },
                onEdit = { editingHabitId = it.habit.id },
                onQuick = { item -> quickHabitAction(item, viewModel) { numericLogAddsAmount = false; numericLogHabitId = item.habit.id } },
                onQuickValue = viewModel::addValue,
                onSetValue = { numericLogAddsAmount = false; numericLogHabitId = it.habit.id },
                onAddAmount = { numericLogAddsAmount = true; numericLogHabitId = it.habit.id },
                onDecrement = viewModel::decrementValue,
                onUndo = { item -> latestPeriodLog(item)?.let { viewModel.undoLog(it.id, item.habit.id) } },
                canUndo = { latestPeriodLog(it) != null },
                onUndoSkip = { item -> viewModel.undoSkip(item.habit.id, item.date) },
                onChecklist = viewModel::toggleChecklist,
                onReorder = null,

                lowPressureMode = lowPressureMode,
                separateCompleted = true,
                onReorderModeChange = { workspaceReordering = it; onReorderModeChange(it) },
                reorderDismissRequest = reorderDismissRequest + localReorderDismiss,
            )
            HabitDestination.All -> HabitList(
                title = "Habits",
                progress = state.all,
                customUnits = state.customUnits,
                empty = if (emptyArea) {
                    "Your habits are saved in other Areas. Show all Areas to find them."
                } else "Choose a simple template or use + to create a Habit from scratch.",
                emptyTitle = if (emptyArea) "No Habits in $areaScopeLabel" else "No Habits Here",
                emptyActionLabel = if (emptyArea) "Show All Areas" else "Browse Templates",
                onEmptyAction = {
                    if (emptyArea) onShowAllAreasForReorder() else templatesOpen = true
                },
                onOpen = { actionsHabitId = it.habit.id },
                onEdit = { editingHabitId = it.habit.id },
                onQuick = { item -> quickHabitAction(item, viewModel) { numericLogAddsAmount = false; numericLogHabitId = item.habit.id } },
                onQuickValue = viewModel::addValue,
                onSetValue = { numericLogAddsAmount = false; numericLogHabitId = it.habit.id },
                onAddAmount = { numericLogAddsAmount = true; numericLogHabitId = it.habit.id },
                onDecrement = viewModel::decrementValue,
                onUndo = { item -> latestPeriodLog(item)?.let { viewModel.undoLog(it.id, item.habit.id) } },
                canUndo = { latestPeriodLog(it) != null },
                onUndoSkip = { item -> viewModel.undoSkip(item.habit.id, item.date) },
                onChecklist = viewModel::toggleChecklist,
                onReorder = if (areaScopeLabel == null) viewModel::reorder else null,
                onShowAllAreasForReorder = onShowAllAreasForReorder.takeIf { areaScopeLabel != null },
                reorderRequested = reorderAllRequested,
                onReorderRequestConsumed = { reorderAllRequested = false },
                lowPressureMode = lowPressureMode,
                onReorderModeChange = { workspaceReordering = it; onReorderModeChange(it) },
                reorderDismissRequest = reorderDismissRequest + localReorderDismiss,
            )
            HabitDestination.Insights -> HabitInsights(state, lowPressureMode, onOpenHistory = {
                historyHabitId = it
                actionsHabitId = it
            })
            HabitDestination.Archived -> ArchivedHabitList(
                habits = state.archived,
                focusedHabitId = focusedArchivedHabitId,
                onOpen = { actionsHabitId = it.id },
                onEdit = { editingHabitId = it.id },
            )
        }
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
            today = editorState.currentDate,
            defaultWeekStart = viewModel.defaultSettings().defaultHabitWeekStart,
            defaults = viewModel.defaultSettings(),
            customUnits = editorState.customUnits,
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
                editingHabitId = null
                templateDraft = null
            },
            onSave = { draft ->
                val requestId = editorSaveCoordinator.begin()
                if (requestId != null) {
                    if (!viewModel.saveHabit(editing?.habit?.id, draft, requestId = requestId)) {
                        editorSaveCoordinator.finishFailure("Another Habit save is already finishing.")
                    }
                }
            },
        )
    }
    actions?.takeIf { editingHabitId == null }?.let { item ->
        val mutationCoordinator = authoredMutationCoordinator ?: return@let
        inspectorPages.SaveableStateProvider("habit-${item.habit.id}") {
        HabitActionsDialog(
            item,
            openHistory = historyHabitId == item.habit.id,
            onChecklist = { habitId, itemId, date, completed ->
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.toggleChecklist(habitId, itemId, date, completed, requestId)) {
                    mutationCoordinator.finishFailure("Another Habit change is already finishing.")
                }
            },
            customUnits = state.customUnits,
            modifier = modifier,
            onDismiss = {
                mutationCoordinator.clear()
                closeHabitActions()
            },
            onEdit = { editingHabitId = item.habit.id },
            onDuplicate = { viewModel.duplicate(item.habit.id); closeHabitActions() },
            onPin = { viewModel.setPinned(item.habit.id, !item.habit.pinned); closeHabitActions() },
            onPause = { viewModel.setPaused(item.habit.id, !item.habit.paused); closeHabitActions() },
            onSchedulePause = {
                pauseRequestHabitId = item.habit.id
                editingPauseId = null
            },
            onQuick = {
                if (item.habit.trackingMode in setOf(HabitTrackingMode.Rating, HabitTrackingMode.LogOnly)) {
                    numericLogAddsAmount = false; numericLogHabitId = item.habit.id
                } else {
                    quickHabitAction(item, viewModel) { numericLogAddsAmount = false; numericLogHabitId = item.habit.id }
                }
            },
            onQuickValue = { amount ->
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.log(item.habit.id, amount, date = item.date, requestId = requestId)) mutationCoordinator.finishFailure("Another Habit change is already finishing.")
            },
            onSetValue = { numericLogAddsAmount = false; numericLogHabitId = item.habit.id },
            onAddAmount = { numericLogAddsAmount = true; numericLogHabitId = item.habit.id },
            onUndoValue = {
                latestPeriodLog(item)?.let { log ->
                    val requestId = mutationCoordinator.begin()
                    if (requestId != null && !viewModel.undoLog(log.id, item.habit.id, requestId)) mutationCoordinator.finishFailure("Another Habit change is already finishing.")
                }
            },
            canUndoValue = latestPeriodLog(item) != null,
            onSkip = { skipConfirmationHabitId = item.habit.id },
            onUndoSkip = {
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.undoSkip(item.habit.id, item.date, requestId)) {
                    mutationCoordinator.finishFailure("Another Habit history change is already finishing.")
                }
            },
            onUndoHistoricalSkip = { date ->
                val requestId = mutationCoordinator.begin()
                if (requestId != null && !viewModel.undoSkip(item.habit.id, date, requestId)) {
                    mutationCoordinator.finishFailure("Another Habit history change is already finishing.")
                }
            },
            logs = state.logs.filter { it.habitId == item.habit.id },
            skips = state.skips.filter { it.habitId == item.habit.id },
            pauses = state.pauses.filter { it.habitId == item.habit.id },
            onAddHistoricalLog = {
                historicalLogForToday = false
                historicalLogHabitId = item.habit.id
            },
            onEnterDurationManually = {
                historicalLogForToday = true
                historicalLogHabitId = item.habit.id
            },
            onEditLog = { log -> editingLogHabitId = item.habit.id; editingLogId = log.id },
            onEditPause = { pause ->
                pauseRequestHabitId = item.habit.id
                editingPauseId = pause.id
            },
            onArchive = { viewModel.setArchived(item.habit.id, !item.habit.archived); closeHabitActions() },
            onDelete = {
                viewModel.preparePermanentDeletion(item.habit.id, item.habit.uuid, item.habit.name)
                closeHabitActions()
            },
            lowPressureMode = lowPressureMode,
            mutationSaving = mutationCoordinator.saving,
            mutationError = mutationCoordinator.errorMessage,
        )
        }
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
    timerReviewPrompt?.let { prompt ->
        HabitTimerReviewDialog(
            prompt = prompt,
            onDismiss = viewModel::dismissTimerReview,
            onStopAndLog = { seconds -> viewModel.resolveTimerReview(seconds, continueTimer = false) },
            onContinue = { seconds -> viewModel.resolveTimerReview(seconds, continueTimer = true) },
            onDiscard = viewModel::discardTimer,
        )
    }
    if (habitDeletionReview.habitId != null && habitDeletionCoordinator != null) {
        HabitPermanentDeleteDialog(
            state = habitDeletionReview,
            saving = habitDeletionCoordinator.saving,
            persistenceError = habitDeletionCoordinator.errorMessage,
            onDismiss = {
                habitDeletionCoordinator.clear()
                viewModel.dismissPermanentDeletionReview()
            },
            onRefresh = {
                habitDeletionCoordinator.clear()
                viewModel.refreshPermanentDeletionReview()
            },
            onConfirm = { impact ->
                val requestId = habitDeletionCoordinator.begin()
                if (requestId != null && !viewModel.deletePermanently(
                        habitId = impact.habitId,
                        expectedRevisionToken = impact.revisionToken,
                        requestId = requestId,
                    )
                ) {
                    habitDeletionCoordinator.finishFailure(
                        "Another Habit deletion is already finishing. Review the current impact before retrying.",
                    )
                }
            },
        )
    }
    if (authoredMutationCoordinator != null) {
        numericLog?.let { item ->
            HabitValueDialog(
                item = item,
                additive = numericLogAddsAmount,
                customUnits = state.customUnits,
                saving = authoredMutationCoordinator.saving,
                persistenceError = authoredMutationCoordinator.errorMessage,
                onDismiss = {
                    authoredMutationCoordinator.clear()
                    numericLogHabitId = null
                },
                onLog = { value, note ->
                    val requestId = authoredMutationCoordinator.begin()
                    if (requestId != null) {
                        val accepted = if (numericLogAddsAmount || item.habit.trackingMode == HabitTrackingMode.LogOnly) {
                            viewModel.log(item.habit.id, value, date = item.date, note = note, requestId = requestId)
                        } else {
                            viewModel.setPeriodValue(item, requireNotNull(value), note, requestId = requestId)
                        }
                        if (!accepted) authoredMutationCoordinator.finishFailure(
                            "Another Habit history change is already finishing.",
                        )
                    }
                },
            )
        }
        historicalLogHabit?.let { item ->
            HabitHistoryLogDialog(
                item = item,
                customUnits = state.customUnits,
                log = null,
                initialDate = if (historicalLogForToday) state.currentDate else state.currentDate.minusDays(1),
                saving = authoredMutationCoordinator.saving,
                persistenceError = authoredMutationCoordinator.errorMessage,
                onDismiss = {
                    authoredMutationCoordinator.clear()
                    historicalLogHabitId = null
                    historicalLogForToday = false
                },
                onSave = { value, status, date, note ->
                    val requestId = authoredMutationCoordinator.begin()
                    if (requestId != null && !viewModel.log(
                            item.habit.id,
                            value,
                            status,
                            date,
                            note,
                            requestId,
                        )
                    ) authoredMutationCoordinator.finishFailure(
                        "Another Habit history change is already finishing.",
                    )
                },
            )
        }
        editingLog?.let { (item, log) ->
            HabitHistoryLogDialog(
                item = item,
                customUnits = state.customUnits,
                log = log,
                initialDate = log.localDate,
                saving = authoredMutationCoordinator.saving,
                persistenceError = authoredMutationCoordinator.errorMessage,
                onDismiss = {
                    authoredMutationCoordinator.clear()
                    editingLogHabitId = null
                    editingLogId = null
                },
                onSave = { value, status, date, note ->
                    val requestId = authoredMutationCoordinator.begin()
                    if (requestId != null && !viewModel.updateLog(
                            log.id,
                            value,
                            status,
                            date,
                            note,
                            item.habit.id,
                            requestId,
                        )
                    ) authoredMutationCoordinator.finishFailure(
                        "Another Habit history change is already finishing.",
                    )
                },
                onDelete = {
                    val requestId = authoredMutationCoordinator.begin()
                    if (requestId != null && !viewModel.undoLog(log.id, item.habit.id, requestId)) {
                        authoredMutationCoordinator.finishFailure(
                            "Another Habit history change is already finishing.",
                        )
                    }
                },
            )
        }
        pauseRequest?.let { item ->
            HabitPauseDialog(
                today = state.currentDate,
                pause = editingPause,
                saving = authoredMutationCoordinator.saving,
                persistenceError = authoredMutationCoordinator.errorMessage,
                onDismiss = {
                    authoredMutationCoordinator.clear()
                    pauseRequestHabitId = null
                    editingPauseId = null
                },
                onSave = { start, end, note ->
                    val requestId = authoredMutationCoordinator.begin()
                    if (requestId != null) {
                        val accepted = editingPause?.let { pause ->
                            viewModel.updatePause(pause.id, item.habit.id, start, end, note, requestId)
                        } ?: viewModel.addPause(item.habit.id, start, end, note, requestId)
                        if (!accepted) authoredMutationCoordinator.finishFailure(
                            "Another Habit schedule change is already finishing.",
                        )
                    }
                },
                onDelete = editingPause?.let { pause ->
                    {
                        val requestId = authoredMutationCoordinator.begin()
                        if (requestId != null && !viewModel.deletePause(pause.id, item.habit.id, requestId)) {
                            authoredMutationCoordinator.finishFailure(
                                "Another Habit schedule change is already finishing.",
                            )
                        }
                    }
                },
            )
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
}

@Composable
internal fun HabitPermanentDeleteDialog(
    state: HabitDeletionReviewUiState,
    saving: Boolean,
    persistenceError: String?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onConfirm: (HabitDeletionImpact) -> Unit,
) {
    val impact = state.impact
    val requiresFreshReview = state.reviewRequired || state.outcomeUncertain || state.targetMissing
    val displayedError = state.errorMessage ?: persistenceError
    val titleName = state.displayName.ifBlank { "Habit" }
    PaneAwareAlertDialog(
        testTag = "habit-permanent-delete-dialog",
        paneTitle = "Permanent Habit Deletion",
        inputBlocked = saving,
        inputBlockedLabel = "Permanently Deleting Habit",
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                when {
                    state.targetMissing -> "Habit Not Found"
                    else -> "Delete $titleName Permanently?"
                },
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).testTag("habit-delete-impact-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersistenceFailureNotice(displayedError, testTag = "habit-delete-problem")
                if (state.preparing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().testTag("habit-delete-preparing"),
                    )
                    Text("Whip is verifying the exact Habit, measurement history, checklist, exceptions, and timer sessions.")
                }
                impact?.let { reviewed ->
                    Text(
                        if (state.preparing || requiresFreshReview) {
                            "The previous review included:"
                        } else {
                            "This cannot be undone. The reviewed deletion includes:"
                        },
                    )
                    Text("• The ${if (reviewed.archived) "archived " else ""}Habit and its measurement definition")
                    Text("• ${quantityLabel(reviewed.measurementEntryCount, "measurement Entry")}")
                    Text("• ${quantityLabel(reviewed.logCount, "check-in")}")
                    Text(
                        "• ${quantityLabel(reviewed.checklistItemCount, "checklist item")} and " +
                            quantityLabel(reviewed.checklistStateCount, "saved checklist state"),
                    )
                    Text(
                        "• ${quantityLabel(reviewed.skipCount, "skipped day")} and " +
                            quantityLabel(reviewed.pauseCount, "scheduled pause"),
                    )
                    Text("• ${quantityLabel(reviewed.timerSessionCount, "timer session")}")
                    if (reviewed.activeTimerSessionCount > 0) {
                        Text(
                            "An active or unresolved timer is included and will not be logged.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        "Export a backup first if you may need this history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!saving && (requiresFreshReview || displayedError != null)) {
                    WhipOutlinedButton(
                        onClick = onRefresh,
                        modifier = Modifier.fillMaxWidth().testTag("habit-delete-refresh"),
                    ) {
                        Text(
                            when {
                                state.outcomeUncertain -> "Verify Current Habit"
                                state.targetMissing -> "Check Again"
                                impact == null -> "Retry Impact Review"
                                requiresFreshReview -> "Review Updated Impact"
                                else -> "Refresh Impact"
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            WhipDestructiveTextButton(
                enabled = impact != null && !state.preparing && !requiresFreshReview && !saving,
                onClick = { impact?.let(onConfirm) },
                modifier = Modifier.testTag("habit-delete-confirm"),
            ) {
                Text(
                    when {
                        saving -> "Deleting…"
                        persistenceError != null -> "Retry Delete"
                        else -> "Delete Permanently"
                    },
                )
            }
        },
        dismissButton = {
            WhipTextButton(
                enabled = !saving,
                onClick = onDismiss,
                modifier = Modifier.testTag("habit-delete-cancel"),
            ) { Text(if (state.targetMissing) "Close" else "Cancel") }
        },
    )
}

internal fun HabitDayProgress.compactCollectionStatus(lowPressureMode: Boolean = false, customUnits: List<UnitDefinition> = emptyList()): String {
    val streakUnit = when (habit.scheduleType) {
        HabitScheduleType.FlexibleTimesPerWeek -> "week"
        HabitScheduleType.FlexibleTimesPerMonth -> "month"
        else -> "day"
    }
    val streakLabel = "$streak $streakUnit streak"
    fun withStreak(label: String) = if (lowPressureMode) label else "$label · $streakLabel"
    return when {
        dayState == HabitDayState.Skipped -> if (lowPressureMode) "Skipped · no check-in expected" else "Skipped · streak protected"
        habit.paused || dayState == HabitDayState.Paused -> "Paused · no check-in expected"
        dayState == HabitDayState.NotScheduled -> "Not scheduled today"
        habit.sourceMeasurementId != null -> "Linked measurement"
        habit.trackingMode == HabitTrackingMode.Checklist -> {
            val completedItems = checklistItems.count { it.second }
            withStreak("$completedItems/${checklistItems.size} items")
        }
        habit.trackingMode == HabitTrackingMode.CheckOff ->
            withStreak(if (isDoneForToday()) "Done" else "Pending")
        habit.trackingMode == HabitTrackingMode.Duration && habit.timerStartedAtMillis != null -> "Timer running"
        habit.trackingMode == HabitTrackingMode.Rating && status != null ->
            "Rating ${formatHabitValue(value, habit.precision)}"
        habit.trackingMode == HabitTrackingMode.LogOnly ->
            if (status != null) "Entry recorded today" else "No entry today"
        habit.comparison != TargetComparison.None -> targetProgressLabel(customUnits)
        value != 0.0 -> "${formatHabitValue(value, habit.precision)} ${habit.unitSymbol(customUnits)}".trim()
        else -> if (habit.comparison == TargetComparison.None) habit.trackingMode.uiLabel() else withStreak(habit.trackingMode.uiLabel())
    }
}

internal fun Habit.streakUnitLabel(count: Int): String {
    val unit = when (scheduleType) {
        HabitScheduleType.FlexibleTimesPerWeek -> "week"
        HabitScheduleType.FlexibleTimesPerMonth -> "month"
        else -> "day"
    }
    return "$count $unit${if (count == 1) "" else "s"}"
}

internal fun Habit.unitSymbol(customUnits: List<UnitDefinition> = emptyList()): String =
    habitUnitSymbol(unitId, customUnits)

internal fun habitUnitSymbol(unitId: String, customUnits: List<UnitDefinition> = emptyList()): String =
    if (unitId in setOf("count", "unitless")) "" else
        (BuiltInUnits.get(unitId) ?: customUnits.firstOrNull { it.id == unitId })?.symbol ?: unitId.unitLabel()

internal fun HabitDayProgress.targetProgressLabel(customUnits: List<UnitDefinition> = emptyList()): String {
    fun amount(number: Double?) = number?.let { formatHabitValue(it, habit.precision) } ?: "—"
    val target = when (habit.comparison) {
        TargetComparison.AtLeast -> "at least ${amount(habit.targetMin)}"
        TargetComparison.AtMost -> "at most ${amount(habit.targetMax)}"
        TargetComparison.Exactly -> "exactly ${amount(habit.targetMin)}"
        TargetComparison.WithinRange -> "${amount(habit.targetMin)}–${amount(habit.targetMax)}"
        TargetComparison.None -> return "${amount(value)} ${habit.unitSymbol(customUnits)}".trim()
    }
    return "${amount(value)} ${habit.unitSymbol(customUnits)} · $target ${habit.unitSymbol(customUnits)} · ${habit.periodTotalDescription()}".replace("  ", " ")
}

@Composable
private fun rememberHabitTimerElapsedSeconds(habit: Habit) = produceState(
    initialValue = habit.currentTimerElapsedSeconds(),
    key1 = habit.timerSessionId,
    key2 = habit.timerStartedAtMillis,
    key3 = Triple(
        habit.timerAccumulatedSeconds,
        habit.timerAnchorElapsedRealtimeMillis,
        habit.timerNeedsReview,
    ),
) {
    while (true) {
        value = habit.currentTimerElapsedSeconds()
        delay(1_000L)
    }
}

private fun Habit.currentTimerElapsedSeconds(): Double = calculateHabitTimerElapsedSeconds(
    accumulatedCanonicalSeconds = timerAccumulatedSeconds,
    anchorWallMillis = timerStartedAtMillis,
    anchorElapsedRealtimeMillis = timerAnchorElapsedRealtimeMillis,
    needsReview = timerNeedsReview,
    nowWallMillis = System.currentTimeMillis(),
    nowElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
)

internal fun formatElapsedDuration(seconds: Double): String {
    val total = seconds.takeIf { it.isFinite() && it >= 0.0 }?.toLong() ?: 0L
    val days = total / 86_400L
    val hours = total % 86_400L / 3_600L
    val minutes = total % 3_600L / 60L
    val remainingSeconds = total % 60L
    return when {
        days > 0L -> "%dd %02d:%02d:%02d".format(Locale.ROOT, days, hours, minutes, remainingSeconds)
        hours > 0L -> "%d:%02d:%02d".format(Locale.ROOT, hours, minutes, remainingSeconds)
        else -> "%d:%02d".format(Locale.ROOT, minutes, remainingSeconds)
    }
}

internal fun formatElapsedDurationSpoken(seconds: Double): String {
    val total = seconds.takeIf { it.isFinite() && it >= 0.0 }?.toLong() ?: 0L
    val days = total / 86_400L
    val hours = total % 86_400L / 3_600L
    val minutes = total % 3_600L / 60L
    val remainingSeconds = total % 60L
    return buildList {
        if (days > 0) add("$days ${if (days == 1L) "day" else "days"}")
        if (hours > 0) add("$hours ${if (hours == 1L) "hour" else "hours"}")
        if (minutes > 0) add("$minutes ${if (minutes == 1L) "minute" else "minutes"}")
        if (remainingSeconds > 0 || isEmpty()) {
            add("$remainingSeconds ${if (remainingSeconds == 1L) "second" else "seconds"}")
        }
    }.joinToString(" ")
}

@Composable
internal fun HabitTimerReviewDialog(
    prompt: HabitTimerReviewPrompt,
    onDismiss: () -> Unit,
    onStopAndLog: (Double) -> Unit,
    onContinue: (Double) -> Unit,
    onDiscard: () -> Unit,
) {
    var minutesText by rememberSaveable(prompt.boundary.sessionId) {
        mutableStateOf(editableNumericValue(prompt.estimatedCanonicalSeconds / 60.0))
    }
    val minutes = minutesText.toWhipDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    val seconds = minutes?.times(60.0)?.takeIf(Double::isFinite)
    var reviewViewport by remember { mutableStateOf(IntSize.Zero) }
    val minutesVisibility = rememberFocusedInputVisibility(reviewViewport)
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Timer") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().onSizeChanged { reviewViewport = it }
                    .verticalScroll(rememberScrollState()).testTag("habit-timer-review-body"),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
            ) {
                Text(prompt.habitName, fontWeight = FontWeight.SemiBold)
                Text(
                    "Estimated elapsed: ${formatElapsedDuration(prompt.estimatedCanonicalSeconds)}",
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it },
                    label = { Text("Elapsed minutes") },
                    supportingText = {
                        Text(if (seconds == null) "Enter zero or a positive number." else formatElapsedDuration(seconds))
                    },
                    isError = minutesText.isNotBlank() && seconds == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().then(minutesVisibility).testTag("habit-timer-review-minutes"),
                )
                Text("After a reboot, clock reset, or restored backup, elapsed time is an estimate. Review it before logging.")
                WhipDestructiveTextButton(
                    onClick = onDiscard,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Discard Timer") }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = seconds != null,
                onClick = { seconds?.let(onStopAndLog) },
                modifier = Modifier.testTag("habit-timer-review-stop"),
            ) { Text("Stop & Log") }
        },
        dismissButton = {
            WhipTextButton(
                enabled = seconds != null,
                onClick = { seconds?.let(onContinue) },
                modifier = Modifier.testTag("habit-timer-review-continue"),
            ) { Text("Continue Timer") }
        },
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
    reorderMode: Boolean = false,
    customUnits: List<UnitDefinition> = emptyList(),
    onAddAmount: (() -> Unit)? = null,
    management: Boolean = false,
) {
    val habit = item.habit
    val weekdayFormatter = rememberWhipWeekdayFormatter()
    if (management && habit.timerSessionId == null) {
        WhipItemCard(Modifier.clickable(enabled = !reorderMode, onClickLabel = "Open habit details for ${habit.name}", onClick = onOpen)
            .testTag("habit-card-${habit.id}")) {
            WhipProductivityItemContent(itemType = "habit", itemName = habit.name, emoji = habit.icon, compact = true) {
                area(habit.areaId, habit.area)
                edit(onEdit.takeUnless { reorderMode })
                summary {
                    text(listOf(habit.area, habit.collectionScheduleLabel { weekdayFormatter.label(it, WhipWeekdayLabelWidth.Short) })
                        .filter(String::isNotBlank).joinToString(" · "))
                    text(item.managementAvailabilityLabel(customUnits))
                }
            }
        }
        return
    }
    val timerElapsedSeconds by rememberHabitTimerElapsedSeconds(habit)
    val skipped = item.dayState == HabitDayState.Skipped
    val unavailableForCheckIn = habit.timerStartedAtMillis == null &&
        (habit.paused || item.dayState in setOf(HabitDayState.Paused, HabitDayState.NotScheduled))
    val disclosure = rememberItemDisclosure(itemKey = habitDisclosureKey(habit.id, item.date))
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
    val compactStatus = if (habit.timerStartedAtMillis != null) {
        if (habit.timerNeedsReview) "Review timer · ${formatElapsedDuration(timerElapsedSeconds)} estimated"
        else "${formatElapsedDuration(timerElapsedSeconds)} elapsed"
    } else item.compactCollectionStatus(lowPressureMode, customUnits)
    val primaryAction: (@Composable () -> Unit)? = when {
        reorderMode -> null
        unavailableForCheckIn -> null
        skipped -> {{ ItemPrimaryTextButton("Undo", onUndoSkip) }}
        habit.sourceMeasurementId != null -> null
        habit.trackingMode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist) -> {{
            WhipCompletionCheckbox(
                checked = item.successful == true,
                onCheckedChange = { onQuick() },
                modifier = Modifier.semantics {
                    contentDescription = if (item.successful == true) {
                        "Mark habit ${habit.name} incomplete"
                    } else "Check off habit ${habit.name}"
                },
            )
        }}
        habit.trackingMode == HabitTrackingMode.Duration -> {{
            val label = when {
                habit.timerStartedAtMillis == null -> "Start"
                habit.timerNeedsReview -> "Review"
                else -> "Stop"
            }
            val actionDescription = when {
                habit.timerStartedAtMillis == null -> "Start timer for ${habit.name}"
                habit.timerNeedsReview -> "Review timer for ${habit.name}; ${formatElapsedDurationSpoken(timerElapsedSeconds)} estimated"
                else -> "Stop and log ${habit.name}; ${formatElapsedDurationSpoken(timerElapsedSeconds)} elapsed"
            }
            ItemPrimaryTextButton(label, onQuick, Modifier.semantics { contentDescription = actionDescription })
        }}
        habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal) -> {{
            ItemPrimaryTextButton("+${editableNumericValue(habit.quickIncrement)}", { onQuickValue(habit.quickIncrement) })
        }}
        else -> {{
            ItemPrimaryTextButton(if (habit.trackingMode == HabitTrackingMode.Rating) "Rate" else "Log", onQuick)
        }}
    }
    WhipItemCard(
        modifier = Modifier
            .then(
                if (reorderMode) Modifier
                else Modifier.clickable(onClickLabel = "Open habit details for ${habit.name}", onClick = onOpen),
            )
            .testTag("habit-card-${habit.id}")
            .then(
                if (reorderMode) Modifier
                else Modifier.semantics { contentDescription = "Open habit details for ${habit.name}" },
            ),
        containerColor = if (item.isDoneForToday()) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        WhipProductivityItemContent(
            itemType = "habit",
            itemName = habit.name,
            emoji = habit.icon,
            identityModifier = Modifier.testTag("habit-icon-${habit.id}"),
            titleModifier = Modifier.testTag("habit-card-title-${habit.id}"),
            primaryActionModifier = Modifier.testTag("habit-primary-action-${habit.id}"),
            editModifier = Modifier.testTag("habit-edit-action-${habit.id}"),
        ) {
            area(habit.areaId, habit.area)
            edit(onEdit.takeUnless { reorderMode })
            details {
                text(
                    text = if (habit.timerStartedAtMillis != null) {
                        if (habit.timerNeedsReview) {
                            "Timer needs review · ${formatElapsedDuration(timerElapsedSeconds)} estimated"
                        } else "Timer running · ${formatElapsedDuration(timerElapsedSeconds)} elapsed"
                    } else if (habit.comparison == TargetComparison.None) {
                        compactStatus
                    } else if (lowPressureMode) {
                        "${habit.trackingMode.uiLabel()} · ${(item.completionRate * 100).toInt()}% / $rateWindow"
                    } else {
                        "${habit.trackingMode.uiLabel()} · ${item.streak} $streakUnit streak · ${(item.completionRate * 100).toInt()}% / $rateWindow"
                    },
                )
            }
            summary {
                text(
                    text = compactStatus,
                    modifier = Modifier.testTag("habit-card-status-${habit.id}"),
                )
            }
            disclosure(expanded = disclosure.expanded, tag = "habit-expand-${habit.id}", onToggle = disclosure.toggle.takeUnless { reorderMode })
            primaryAction(width = if (
                skipped || habit.trackingMode in setOf(HabitTrackingMode.Duration, HabitTrackingMode.Rating, HabitTrackingMode.LogOnly)
            ) 72.dp else 64.dp, content = primaryAction)

            expandedContent {
                if (skipped) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
                        if (stacked) {
                            Column {
                                Text(
                                    if (lowPressureMode) "Skipped Today · No Check-In Expected" else "Skipped Today · Streak Protected",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.whipColors.warning,
                                )
                                WhipTextButton(onClick = onUndoSkip, modifier = Modifier.fillMaxWidth()) { Text("Undo Skip") }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(if (lowPressureMode) "Skipped Today · No Check-In Expected" else "Skipped Today · Streak Protected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.whipColors.warning)
                                WhipTextButton(onClick = onUndoSkip) { Text("Undo Skip") }
                            }
                        }
                    }
                }
                if (unavailableForCheckIn) {
                    Text(
                        if (habit.paused || item.dayState == HabitDayState.Paused) {
                            "Paused · no check-in is expected. Open details to resume or edit pause dates."
                        } else {
                            "Not scheduled today. Open details if you intentionally want to log outside the schedule."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!skipped && !unavailableForCheckIn && habit.sourceMeasurementId == null && habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal)) {
                    HabitNumericActions(item, customUnits, onQuickValue, onSetValue, onAddAmount, onDecrement, onUndo, canUndo)
                }
                if (!skipped && !unavailableForCheckIn && habit.trackingMode == HabitTrackingMode.Checklist) {
                    HabitChecklist(item, onChecklist)
                }
                if (!skipped && !unavailableForCheckIn && item.flexibleScheduleTarget != null && item.flexibleScheduleProgress != null) {
                    val target = item.flexibleScheduleTarget
                    val fraction = (item.flexibleScheduleProgress.toFloat() / target).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "${item.flexibleScheduleProgress} / $target completions this ${if (habit.scheduleType == HabitScheduleType.FlexibleTimesPerWeek) "week" else "month"}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else if (
                    !skipped &&
                    !unavailableForCheckIn &&
                    habit.trackingMode !in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist) &&
                    habit.comparison != TargetComparison.None
                ) {
                    if (habit.comparison == TargetComparison.AtLeast && (habit.targetMin ?: 0.0) > 0.0) {
                        val fraction = (item.value / requireNotNull(habit.targetMin)).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        item.targetProgressLabel(customUnits),
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else if (
                    !skipped &&
                    !unavailableForCheckIn &&
                    item.value != 0.0 &&
                    habit.trackingMode !in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist)
                ) {
                    val formattedValue = "${formatHabitValue(item.value, habit.precision)} ${habit.unitSymbol(customUnits)}".trim()
                    Text(
                        when (habit.trackingMode) {
                            HabitTrackingMode.Rating -> "Rating: $formattedValue"
                            HabitTrackingMode.LogOnly -> "Logged value: $formattedValue"
                            else -> formattedValue
                        },
                    )
                }
                if (habit.sourceMeasurementId != null) {
                    Text(
                        "Activity from a linked measurement.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal fun Habit.collectionScheduleLabel(weekdayLabel: (DayOfWeek) -> String): String = when (scheduleType) {
    HabitScheduleType.Daily -> "Every day"
    HabitScheduleType.SelectedWeekdays -> weekdays.sortedBy { (it.value - weekStart.value + 7) % 7 }.joinToString(", ", transform = weekdayLabel)
    HabitScheduleType.EveryNDays -> "Every $scheduleInterval days"
    HabitScheduleType.FlexibleTimesPerWeek -> "$flexibleTimesPerWeek times per week"
    HabitScheduleType.FlexibleTimesPerMonth -> "$flexibleTimesPerWeek times per month"
}

internal fun HabitDayProgress.managementAvailabilityLabel(customUnits: List<UnitDefinition> = emptyList()): String = when {
    habit.paused || dayState == HabitDayState.Paused -> "Paused · no check-in expected"
    habit.endType == HabitEndType.OnDate && habit.endDate?.isBefore(date) == true -> "Ended · ${habit.endDate.format(habitShortDateFormatter)}"
    habit.sourceMeasurementId != null -> "Linked measurement · ${habit.trackingMode.uiLabel()}"
    !scheduled -> "No check-in expected today · ${habit.trackingMode.uiLabel()}"
    else -> compactCollectionStatus(lowPressureMode = true, customUnits = customUnits)
}

internal fun Habit.newEntryUnavailableReason(): String? = when {
    archived -> "Restore this Habit before adding an entry. Existing history can still be corrected."
    paused -> "Resume this Habit before adding an entry. Existing history can still be corrected."
    sourceMeasurementId != null -> "Entries come from the linked measurement."
    else -> null
}

@Composable
private fun HabitNumericActions(
    item: HabitDayProgress,
    units: List<UnitDefinition>,
    onQuickValue: (Double) -> Unit,
    onSetValue: () -> Unit,
    onAddAmount: (() -> Unit)?,
    onDecrement: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    enabled: Boolean = true,
) {
    val habit = item.habit
    var expanded by rememberSaveable(habit.id) { mutableStateOf(false) }
    val values = remember(habit.quickIncrement, habit.quickActions) {
        (listOf(habit.quickIncrement) + habit.quickActions).filter { it.isFinite() && it > 0.0 }.distinct()
    }
    val unit = habit.unitSymbol(units).takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()
    FlowRow(Modifier.fillMaxWidth().testTag("habit-numeric-actions-${habit.id}"),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (if (expanded) values else values.take(3)).forEach { amount ->
            WhipTextButton(enabled = enabled, onClick = { onQuickValue(amount) }) { Text("+${editableNumericValue(amount)}$unit") }
        }
        if (values.size > 3) DisclosureButton("Quick Values", expanded, { expanded = !expanded })
        onAddAmount?.let { add -> WhipTextButton(enabled = enabled, onClick = add) { Text("Add Amount") } }
        WhipTextButton(enabled = enabled, onClick = onSetValue) { Text("Set Total") }
        WhipTextButton(enabled = enabled && item.value > 0.0, onClick = onDecrement) {
            Text("−${editableNumericValue(minOf(habit.quickIncrement, item.value.coerceAtLeast(0.0)))}$unit")
        }
        WhipTextButton(enabled = enabled && canUndo, onClick = onUndo) { Text("Undo Last Entry") }
    }
}

@Composable
private fun HabitChecklist(
    item: HabitDayProgress,
    onChecklist: (Long, Long, LocalDate, Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val habit = item.habit
    item.checklistItems.forEach { (checklistItem, completed) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("habit-checklist-item-${checklistItem.id}")
                .toggleable(
                    value = completed,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = {
                        onChecklist(habit.id, checklistItem.id, item.date, !completed)
                    },
                )
                .semantics {
                    contentDescription = if (completed) {
                        "Mark checklist item ${checklistItem.name} incomplete"
                    } else "Complete checklist item ${checklistItem.name}"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                checklistItem.name,
                modifier = Modifier.weight(1f).testTag("habit-checklist-text-${checklistItem.id}"),
                color = completionTextColor(completed),
                textDecoration = completionTextDecoration(completed),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(48.dp).testTag("habit-checklist-check-${checklistItem.id}"),
                contentAlignment = Alignment.Center,
            ) {
                WhipCompletionCheckbox(
                    checked = completed,
                    onCheckedChange = null,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
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

fun quickHabitAction(item: HabitDayProgress, vm: HabitViewModel, openNumeric: () -> Unit) {
    if (item.habit.sourceMeasurementId != null) return
    when (item.habit.trackingMode) {
        HabitTrackingMode.CheckOff -> vm.setCheckOff(item.habit.id, item.date, item.successful != true)
        HabitTrackingMode.Count, HabitTrackingMode.Decimal -> vm.log(item.habit.id, item.habit.quickIncrement)
        HabitTrackingMode.Duration -> if (item.habit.timerStartedAtMillis == null) vm.startTimer(item.habit.id) else vm.stopTimer(item.habit.id)
        HabitTrackingMode.Checklist -> vm.setCheckOff(item.habit.id, item.date, item.successful != true)
        HabitTrackingMode.Rating, HabitTrackingMode.LogOnly -> openNumeric()
    }
}

internal data class DailyHabitSections(
    val actionNeeded: List<HabitDayProgress>,
    val finished: List<HabitDayProgress>,
)

internal fun HabitDayProgress.isDoneForToday(): Boolean =
    dayState == HabitDayState.Completed || successful == true

internal fun HabitDayProgress.isFinishedForToday(): Boolean =
    dayState == HabitDayState.Skipped || isDoneForToday()

internal fun List<HabitDayProgress>.dailyHabitSections(): DailyHabitSections {
    val (finished, actionNeeded) = partition(HabitDayProgress::isFinishedForToday)
    return DailyHabitSections(actionNeeded = actionNeeded, finished = finished)
}

internal fun habitDisclosureKey(habitId: Long, date: LocalDate): String =
    "habit:$habitId:${date.toEpochDay()}"

@Composable
internal fun FinishedHabitsDisclosure(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DisclosureRow(
        title = "Finished for Today ($count)",
        supportingText = "Completed or skipped · expand to review or undo.",
        expanded = expanded,
        onClick = onToggle,
        modifier = modifier.testTag("habit-done-disclosure"),
    )
}

@Composable
private fun HabitList(
    title: String,
    progress: List<HabitDayProgress>,
    customUnits: List<UnitDefinition>,
    empty: String,
    emptyTitle: String,
    emptyActionLabel: String,
    onEmptyAction: () -> Unit,
    onOpen: (HabitDayProgress) -> Unit,
    onEdit: (HabitDayProgress) -> Unit,
    onQuick: (HabitDayProgress) -> Unit,
    onQuickValue: (HabitDayProgress, Double) -> Unit,
    onSetValue: (HabitDayProgress) -> Unit,
    onAddAmount: (HabitDayProgress) -> Unit,
    onDecrement: (HabitDayProgress) -> Unit,
    onUndo: (HabitDayProgress) -> Unit,
    canUndo: (HabitDayProgress) -> Boolean,
    onUndoSkip: (HabitDayProgress) -> Unit,
    onChecklist: (Long, Long, LocalDate, Boolean) -> Unit,
    onReorder: ((List<Long>) -> Unit)?,
    onShowAllAreasForReorder: (() -> Unit)? = null,
    reorderRequested: Boolean = false,
    onReorderRequestConsumed: () -> Unit = {},
    lowPressureMode: Boolean,
    separateCompleted: Boolean = false,
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
) {
    var manageOrder by rememberSaveable { mutableStateOf(false) }
    val sections = if (separateCompleted) progress.dailyHabitSections() else DailyHabitSections(progress, emptyList())
    val finishedIds = sections.finished.mapTo(linkedSetOf()) { it.habit.id }
    val dateKey = progress.firstOrNull()?.date?.toEpochDay() ?: Long.MIN_VALUE
    val itemDisclosureState = LocalItemDisclosureState.current
    var finishedExpanded by rememberSaveable(title, dateKey) {
        mutableStateOf(false)
    }
    var knownFinishedIds by remember(title, dateKey) { mutableStateOf(finishedIds) }
    LaunchedEffect(finishedIds, dateKey, itemDisclosureState) {
        (finishedIds - knownFinishedIds).forEach { habitId ->
            itemDisclosureState?.collapse(habitDisclosureKey(habitId, LocalDate.ofEpochDay(dateKey)))
        }
        knownFinishedIds = finishedIds
    }
    LaunchedEffect(reorderRequested, onReorder) {
        if (reorderRequested && onReorder != null) {
            manageOrder = true
            onReorderRequestConsumed()
        }
    }
    BackHandler(enabled = manageOrder) { manageOrder = false }
    DisposableEffect(manageOrder) {
        onReorderModeChange(manageOrder)
        onDispose { if (manageOrder) onReorderModeChange(false) }
    }
    LaunchedEffect(reorderDismissRequest) {
        if (reorderDismissRequest > 0) manageOrder = false
    }
    WhipReorderLazyColumn(
        modifier = Modifier.fillMaxSize().testTag("habit-list-$title"),
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {


        if (progress.isEmpty()) item {
            WhipEmptyState(
                title = emptyTitle,
                supportingText = empty,
                primaryActionLabel = emptyActionLabel,
                onPrimaryAction = onEmptyAction,
            )
        }
        if (separateCompleted && sections.actionNeeded.isEmpty() && sections.finished.isNotEmpty()) item {
            WhipEmptyState(
                title = "All Set for Today",
                supportingText = "Completed and skipped Habits remain below if you need to review or undo one.",
            )
        }
        items(sections.actionNeeded, key = { it.habit.id }) { item ->
            val index = sections.actionNeeded.indexOfFirst { it.habit.id == item.habit.id }
            Column {
                if (
                    manageOrder &&
                    (index == 0 || sections.actionNeeded[index - 1].habit.pinned != item.habit.pinned)
                ) {
                    WhipGroupHeading(
                        if (item.habit.pinned) "Pinned Habits" else "Other Habits",
                        compact = true,
                    )
                }
                val card: @Composable () -> Unit = {
                    HabitProgressCard(
                    item = item,
                    customUnits = customUnits,
                    onOpen = { onOpen(item) },
                    onEdit = { onEdit(item) },
                    onQuick = { onQuick(item) },
                    onQuickValue = { value -> onQuickValue(item, value) },
                    onSetValue = { onSetValue(item) },
                    onAddAmount = { onAddAmount(item) },
                    onDecrement = { onDecrement(item) },
                    onUndo = { onUndo(item) },
                    canUndo = canUndo(item),
                    onUndoSkip = { onUndoSkip(item) },
                    onChecklist = onChecklist,
                    lowPressureMode = lowPressureMode,
                    reorderMode = manageOrder,
                    management = !separateCompleted,
                    )
                }
                if (manageOrder && onReorder != null) {
                    val partition = sections.actionNeeded.filter { it.habit.pinned == item.habit.pinned }
                    val partitionIndex = partition.indexOfFirst { it.habit.id == item.habit.id }
                    val reorderInteraction = rememberWhipReorderInteractionState()
                    Row(
                        modifier = Modifier.whipReorderItem(
                            reorderInteraction,
                            layoutPosition = partitionIndex + 1,
                            layoutScope = "habit-browse-${item.habit.pinned}",
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WhipReorderHandle(
                            label = item.habit.name,
                            canMovePrevious = partitionIndex > 0,
                            canMoveNext = partitionIndex in 0 until partition.lastIndex,
                            position = partitionIndex + 1,
                            total = partition.size,
                            interactionState = reorderInteraction,
                            moveWholeItem = true,
                            layoutScope = "habit-browse-${item.habit.pinned}",
                            reserveWhenUnavailable = true,
                            onMove = { delta ->
                                val moved = moveListItem(partition, partitionIndex, delta)
                                val iterator = moved.iterator()
                                onReorder(sections.actionNeeded.map { current ->
                                    if (current.habit.pinned == item.habit.pinned) iterator.next().habit.id else current.habit.id
                                })
                            },
                        )
                        Box(Modifier.weight(1f)) { card() }
                    }
                } else card()
            }
        }
        if (!manageOrder && sections.finished.isNotEmpty()) {
            item {
                FinishedHabitsDisclosure(
                    count = sections.finished.size,
                    expanded = finishedExpanded,
                    onToggle = { finishedExpanded = !finishedExpanded },
                )
            }
            if (finishedExpanded) items(sections.finished, key = { it.habit.id }) { item ->
                HabitProgressCard(
                    item = item,
                    customUnits = customUnits,
                    onOpen = { onOpen(item) },
                    onEdit = { onEdit(item) },
                    onQuick = { onQuick(item) },
                    onQuickValue = { value -> onQuickValue(item, value) },
                    onSetValue = { onSetValue(item) },
                    onAddAmount = { onAddAmount(item) },
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
internal fun HabitInsights(
    state: HabitUiState,
    lowPressureMode: Boolean,
    onOpenHistory: ((Long) -> Unit)? = null,
    focused: Boolean = false,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("habit-insights-list"),
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {

        if (state.all.isEmpty()) item {
            WhipEmptyState("No Habit Insights Yet", "Create and check in to a habit to build insight over time.")
        }
        items(state.all, key = { it.habit.id }) { item ->
            val habit = item.habit
            val logs = state.logs.filter { it.habitId == habit.id && !it.localDate.isAfter(state.currentDate) }
            val skips = state.skips.filter { it.habitId == habit.id }
            val pauses = state.pauses.filter { it.habitId == habit.id }
            val recordedDates = logs.mapTo(mutableSetOf(), HabitLog::localDate)
            val flexible = habit.scheduleType in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth)
            val showOutcomes = habit.comparison != TargetComparison.None && !flexible && !lowPressureMode
            WhipItemCard {
                if (!focused) WhipProductivityItemContent(
                    itemType = "habit", itemName = habit.name, emoji = habit.icon,
                    identityModifier = Modifier.testTag("habit-insight-icon-${habit.id}"),
                ) {
                    area(habit.areaId, habit.area)
                    details { text(habit.trackingMode.uiLabel()) }
                }
                if (habit.paused) Text(
                    "Paused now. Past dates use your schedule and saved pause dates. Choose Schedule Pause Dates in Habit Options to mark time away in history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("habit-insights-current-pause-context"),
                )
                if (habitHistoryEvents(logs, skips, pauses, state.currentDate).isEmpty()) {
                    Text("No activity yet. Your recorded entries and pauses will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val values = logs.mapNotNull { it.valueInUnit(habit.unitId, state.customUnits) }
                    WhipGroupedInformationCard {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HabitTodayMetric("entries-${habit.id}", logs.size.toString(), "Recorded entries")
                            if (!lowPressureMode && habit.comparison != TargetComparison.None) {
                                HabitTodayMetric("streak-${habit.id}", habit.streakUnitLabel(item.streak), "Current streak")
                            }
                            if (habit.trackingMode == HabitTrackingMode.Rating && values.isNotEmpty()) {
                                HabitTodayMetric("average-${habit.id}", formatHabitValue(values.preciseAverage(), habit.precision), "Average rating")
                            } else if (habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.Duration) && values.isNotEmpty()) {
                                HabitTodayMetric("total-${habit.id}", "${formatHabitValue(values.preciseSum(), habit.precision)} ${habit.unitSymbol(state.customUnits)}".trim(), "All-time logged")
                            }
                        }
                        if (habit.comparison != TargetComparison.None && !lowPressureMode) {
                            val outcomes = habit.successfulPeriodOutcomeDates(logs, state.currentDate.minusDays(29), state.currentDate, pauses, state.customUnits, skips)
                            val scoredStates = (29L downTo 0L).map { state.currentDate.minusDays(it) }
                                .map { habit.dayStateOn(it, state.currentDate, logs, pauses, skips, state.customUnits) }
                            Text(when {
                                flexible -> "${outcomes.size} ${if (habit.scheduleType == HabitScheduleType.FlexibleTimesPerWeek) "weekly" else "monthly"} targets completed in the last 30 days"
                                scoredStates.none { it in setOf(HabitDayState.Completed, HabitDayState.Missed, HabitDayState.BelowTarget) } -> "30-day completion: No scored periods"
                                else -> "30-day completion: ${(item.completionRate * 100).toInt()}%"
                            }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    val weekStarts = (7L downTo 0L).map { state.currentDate.minusWeeks(it).with(TemporalAdjusters.previousOrSame(habit.weekStart)) }
                    val rates = weekStarts.map { start ->
                        val end = minOf(start.plusDays(6), state.currentDate)
                        val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.filter { !it.isBefore(habit.startDate) }.toList()
                        if (showOutcomes) {
                            habit.scheduledCompletionRateForDays(days, state.currentDate, logs, pauses, skips, state.customUnits)
                        } else days.takeIf { it.isNotEmpty() }?.let { it.count(recordedDates::contains).toDouble() / it.size }
                    }
                    WhipGroupHeading(if (showOutcomes) "Eight-Week Consistency" else "Days with Entries", compact = true)
                    HabitRateChart(habit.name, rates, weekStarts,
                        metricLabel = if (showOutcomes) "scheduled completion" else "recorded activity")
                    Text(if (showOutcomes) "Completed scheduled days; pauses and skips are excluded. Gaps have no scored days."
                        else "Share of days with a recorded entry each week; this is activity, not target success.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WhipGroupHeading("Recent Activity", compact = true)
                    HabitActivityGrid(
                        days = (27L downTo 0L).map { state.currentDate.minusDays(it) },
                        firstDayOfWeek = habit.weekStart,
                        recordedDates = recordedDates.takeUnless { showOutcomes },
                        stateForDay = { day -> habit.dayStateOn(day, state.currentDate, logs, pauses, skips, state.customUnits) },
                    )
                }
                onOpenHistory?.let { open ->
                    WhipTextButton(onClick = { open(habit.id) }, modifier = Modifier.testTag("habit-insights-history-${habit.id}")) { Text("View History") }
                }
            }
        }
    }
}

internal fun Habit.scheduledCompletionRateForDays(
    days: List<LocalDate>,
    today: LocalDate,
    logs: List<HabitLog>,
    pauses: List<HabitPause> = emptyList(),
    skips: List<HabitSkip> = emptyList(),
    customUnits: List<UnitDefinition> = emptyList(),
): Double? {
    val outcomes = days.mapNotNull { day ->
        when (dayStateOn(day, today, logs, pauses, skips, customUnits)) {
            HabitDayState.Completed -> true
            HabitDayState.Missed, HabitDayState.BelowTarget -> false
            else -> null
        }
    }
    return outcomes.takeIf { it.isNotEmpty() }?.let { it.count { done -> done }.toDouble() / it.size }
}

@Composable
internal fun HabitActivityGrid(
    days: List<LocalDate>,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    recordedDates: Set<LocalDate>? = null,
    stateForDay: (LocalDate) -> HabitDayState,
) {
    val leading = days.firstOrNull()?.let { orderedWhipWeekdays(firstDayOfWeek).indexOf(it.dayOfWeek) } ?: 0
    val cells: List<LocalDate?> = List(leading) { null } + days
    Column(
        modifier = Modifier.fillMaxWidth().testTag("habit-activity-grid"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (days.isNotEmpty()) Text(
            "${days.first().format(habitShortDateFormatter)} – ${days.last().format(habitShortDateFormatter)}",
            style = MaterialTheme.typography.bodySmall,
        )
        WhipCalendarWeekdayHeader(firstDayOfWeek = firstDayOfWeek)
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (week + List(7 - week.size) { null }).forEach dayCell@ { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f))
                        return@dayCell
                    }
                    val state = stateForDay(day)
                    val stateLabel = if (recordedDates != null) {
                        if (day in recordedDates) "entry recorded" else when (state) {
                            HabitDayState.Paused -> "paused"
                            HabitDayState.Skipped -> "skipped"
                            else -> "no entry"
                        }
                    } else when (state) {
                        HabitDayState.Completed -> "completed"
                        HabitDayState.Skipped -> "skipped"
                        HabitDayState.Missed -> "missed"
                        HabitDayState.BelowTarget -> "below target"
                        HabitDayState.Pending -> "pending"
                        HabitDayState.Paused -> "paused"
                        HabitDayState.NotScheduled -> "not scheduled"
                    }
                    val (containerColor, contentColor) = if (recordedDates != null) {
                        if (day in recordedDates) MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
                    } else when (state) {
                        HabitDayState.Completed -> MaterialTheme.whipColors.success to MaterialTheme.whipColors.onSuccess
                        HabitDayState.Skipped -> MaterialTheme.whipColors.warning to MaterialTheme.whipColors.onWarning
                        HabitDayState.Missed, HabitDayState.BelowTarget ->
                            MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
                        HabitDayState.Pending ->
                            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        HabitDayState.Paused ->
                            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                        HabitDayState.NotScheduled ->
                            MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f).heightIn(min = 48.dp)
                            .testTag("habit-activity-day-${day.toEpochDay()}")
                            .clearAndSetSemantics {
                                contentDescription = "${day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}: $stateLabel"
                            },
                        shape = MaterialTheme.shapes.extraSmall,
                        color = containerColor,
                        contentColor = contentColor,
                    ) {
                        Column(Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
                            Text(
                                if (recordedDates != null) {
                                    if (day in recordedDates) "●" else "·"
                                } else when (state) {
                                    HabitDayState.Completed -> "✓"
                                    HabitDayState.Skipped -> "○"
                                    HabitDayState.Missed, HabitDayState.BelowTarget -> "×"
                                    HabitDayState.Paused -> "Ⅱ"
                                    HabitDayState.Pending, HabitDayState.NotScheduled -> "·"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        Text(
            if (recordedDates != null) "● Entry recorded · No entry; pauses and skips remain in History"
            else "✓ Completed · ○ Skipped · × Missed or below target · Ⅱ Paused · · Pending or not scheduled",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun HabitRateChart(name: String, rates: List<Double?>, weekStarts: List<LocalDate> = emptyList(), metricLabel: String = "consistency") {
    val values = rates.map { it?.coerceIn(0.0, 1.0) }
    val lineColor = MaterialTheme.colorScheme.primary
    val description = values.mapIndexed { index, value ->
        "${weekStarts.getOrNull(index)?.format(habitShortDateFormatter) ?: "week ${index + 1}"}: ${value?.let { "${(it * 100).toInt()} percent" } ?: "no data"}"
    }.joinToString("; ")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("100%", style = MaterialTheme.typography.labelSmall)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .testTag("habit-rate-chart")
            .semantics { contentDescription = "$name eight-week $metricLabel chart; $description" },
    ) {
        if (values.isEmpty()) return@Canvas
        val radius = 4.dp.toPx()
        val step = (size.width - radius * 2) / (values.size - 1).coerceAtLeast(1)
        fun point(index: Int, value: Double) = androidx.compose.ui.geometry.Offset(
            radius + index * step, radius + (1f - value.toFloat()) * (size.height - radius * 2),
        )
        values.zipWithNext().forEachIndexed { index, (start, end) ->
            if (start != null && end != null) {
                drawLine(
                    color = lineColor,
                    start = point(index, start),
                    end = point(index + 1, end),
                    strokeWidth = 4.dp.toPx(),
                )
            }
        }
        values.forEachIndexed { index, value ->
            if (value != null) drawCircle(lineColor, radius, point(index, value))
        }
    }
    Text("0%", style = MaterialTheme.typography.labelSmall)
    if (weekStarts.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(weekStarts.first().format(habitShortDateFormatter), style = MaterialTheme.typography.labelSmall)
        Text(weekStarts.last().format(habitShortDateFormatter), style = MaterialTheme.typography.labelSmall)
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
        contentPadding = WhipPageContentPadding,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {

        if (visible.isEmpty()) item {
            WhipEmptyState(
                title = "No Archived Habits",
                supportingText = "Archived habits will appear here and can be restored or edited.",
            )
        }
        items(visible, key = Habit::id) { habit ->
            WhipItemCard(
                modifier = Modifier
                    .clickable(onClickLabel = "Open habit details for ${habit.name}") { onOpen(habit) }
                    .semantics { contentDescription = "Open habit details for ${habit.name}" },
            ) {
                WhipProductivityItemContent(
                    itemType = "habit",
                    itemName = habit.name,
                    emoji = habit.icon,
                    identityModifier = Modifier.testTag("habit-icon-${habit.id}"),
                    editModifier = Modifier.testTag("habit-edit-action-${habit.id}"),
                ) {
                    area(habit.areaId, habit.area)
                    edit({ onEdit(habit) })
                    details {
                        text(
                            text = "Archived",
                        )
                    }
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
    sourceMeasurementId = sourceMeasurementId,
)

@Composable
internal fun HabitEditorDialog(
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
    onRequestNotificationPermission: () -> Unit = {},
    saving: Boolean = false,
    persistenceError: String? = null,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    onCreateCustomUnit: CreateCustomUnitAction = UnavailableCreateCustomUnitAction,
    customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveIdentityEmoji: (CustomIdentityEmoji) -> Unit = {},
    onRemoveSavedIdentityEmoji: (String) -> Unit = {},
) {
    val weekdayFormatter = rememberWhipWeekdayFormatter()
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
    var targetMin by rememberSaveable(editorKey) { mutableStateOf(initial.targetMin?.let(::plainNumericValue) ?: "1") }
    var targetMax by rememberSaveable(editorKey) { mutableStateOf(initial.targetMax?.let(::plainNumericValue).orEmpty()) }
    var targetPeriod by rememberSaveable(editorKey) { mutableStateOf(initial.targetPeriod) }
    var schedule by rememberSaveable(editorKey) { mutableStateOf(initial.scheduleType) }
    var interval by rememberSaveable(editorKey) { mutableStateOf(initial.scheduleInterval.toString()) }
    var weekdays by rememberSaveable(editorKey) { mutableStateOf(initial.weekdays) }
    var flexible by rememberSaveable(editorKey) { mutableStateOf(initial.flexibleTimesPerWeek?.toString() ?: "3") }
    var rollingDays by rememberSaveable(editorKey) { mutableStateOf(initial.rollingDays?.toString() ?: "7") }
    var quickIncrement by rememberSaveable(editorKey) { mutableStateOf(initial.quickIncrement.let(::plainNumericValue)) }
    var quickActions by rememberSaveable(editorKey) {
        mutableStateOf(
            initial.quickActions.let { values ->
                val compact = compactNumericSequence(values)
                if (compact.increment == initial.quickIncrement) compact.specification
                else values.joinToString(",", transform = ::plainNumericValue)
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
    var endValue by rememberSaveable(editorKey) { mutableStateOf(initial.endValue?.let(::plainNumericValue).orEmpty()) }
    var weekStart by rememberSaveable(editorKey) { mutableStateOf(initial.weekStart) }
    var showEndDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var unitId by rememberSaveable(editorKey) { mutableStateOf(initial.unitId) }
    var unitChangeError by rememberSaveable(editorKey) { mutableStateOf<String?>(null) }
    var dimension by rememberSaveable(editorKey) { mutableStateOf(initial.dimension) }
    var precision by rememberSaveable(editorKey) { mutableStateOf(initial.precision.toString()) }
    var sourceMeasurementId by rememberSaveable(editorKey) { mutableStateOf(initial.sourceMeasurementId) }
    var checklistDrafts by rememberSaveable(editorKey) {
        mutableStateOf<List<HabitChecklistItemDraft>>(
            initial.checklistItems.map { draft ->
                if (draft.uuid != null || draft.id != null) draft
                else draft.copy(uuid = java.util.UUID.randomUUID().toString())
            },
        )
    }
    var autoCompleteFromItems by rememberSaveable(editorKey) {
        mutableStateOf(initial.autoCompleteFromItems)
    }
    val quickAddsEnabled = sourceMeasurementId == null && mode.supportsQuickAddAmounts()
    val quickActionResult = parseNumericSequence(
        specification = quickActions,
        rangeIncrement = quickIncrement.toWhipDoubleOrNull(),
        maximumValues = 24,
    )
    val quickIncrementValid = quickIncrement.toWhipDoubleOrNull()?.let { it.isFinite() && it > 0.0 } == true
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }
    val currentDraft = HabitDraft(
        name = name,
        notes = notes,
        areaId = areaId,
        area = area,
        tags = tags.split(',').map(String::trim).filter(String::isNotBlank),
        icon = icon.ifBlank { DEFAULT_HABIT_EMOJI },
        trackingMode = mode,
        dimension = dimension,
        unitId = unitId,
        precision = precision.toIntOrNull() ?: -1,
        comparison = comparison,
        targetMin = targetMin.toWhipDoubleOrNull(),
        targetMax = targetMax.toWhipDoubleOrNull(),
        targetPeriod = targetPeriod,
        rollingDays = rollingDays.toIntOrNull(),
        scheduleType = schedule,
        scheduleInterval = interval.toIntOrNull() ?: 0,
        weekdays = weekdays,
        flexibleTimesPerWeek = flexible.toIntOrNull(),
        startDate = initial.startDate,
        endType = endType,
        endDate = endDate,
        endValue = endValue.toWhipDoubleOrNull(),
        quickIncrement = if (quickAddsEnabled) quickIncrement.toWhipDoubleOrNull() ?: Double.NaN else 1.0,
        quickActions = if (quickAddsEnabled) quickActionResult.values else emptyList(),
        reminderMinutes = reminders.split(',').mapNotNull { parseClockMinutes(it.trim()) },
        weekdayReminderMinutes = parseWeekdayReminderMap(weekdayReminders),
        weekStart = weekStart,
        checklistItems = checklistDrafts.filter { it.name.isNotBlank() }
            .mapIndexed { index, item -> item.copy(name = item.name.trim(), position = index) },
        autoCompleteFromItems = autoCompleteFromItems,
        sourceMeasurementId = sourceMeasurementId,
    ).withConfigurationSemantics()
    val rawFieldProblems = buildList {
        if (
            comparison in setOf(TargetComparison.AtLeast, TargetComparison.Exactly, TargetComparison.WithinRange) &&
            targetMin.isNotBlank() && targetMin.toWhipDoubleOrNull() == null
        ) add("Target must be a valid number")
        if (
            comparison in setOf(TargetComparison.AtMost, TargetComparison.WithinRange) &&
            targetMax.isNotBlank() && targetMax.toWhipDoubleOrNull() == null
        ) add("Maximum must be a valid number")
        if (schedule == HabitScheduleType.EveryNDays && interval.toIntOrNull() == null) {
            add("Schedule interval must be a positive whole number")
        }
        if (
            sourceMeasurementId == null &&
            mode !in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist, HabitTrackingMode.Rating) &&
            precision.toIntOrNull() == null
        ) add("Decimal places must be between 0 and 6")
        if (
            comparison != TargetComparison.None && targetPeriod == TargetPeriod.RollingDays &&
            rollingDays.isNotBlank() && rollingDays.toIntOrNull() == null
        ) add("Rolling window must be a positive whole number")
        if (
            schedule in setOf(HabitScheduleType.FlexibleTimesPerWeek, HabitScheduleType.FlexibleTimesPerMonth) &&
            flexible.isNotBlank() && flexible.toIntOrNull() == null
        ) add("Flexible schedule count must be a positive whole number")
        if (
            endType in setOf(HabitEndType.AfterStreak, HabitEndType.AfterCompletions, HabitEndType.AfterTotal) &&
            endValue.isNotBlank() && endValue.toWhipDoubleOrNull() == null
        ) add("Ending threshold must be a valid number")
        if (quickAddsEnabled) quickActionResult.error?.let(::add)
        if (habit == null && areas.count { !it.archived } > 1 && areaId == null) add("Choose an Area for this Habit")
    }
    val validationMessages = (rawFieldProblems + currentDraft.validationErrors()).distinct()
    val advancedScheduleProblems = validationMessages.filter { message ->
        message in setOf(
            "Ending threshold must be a valid number",
            "Choose an end date on or after the start date",
            "Enter a positive whole-number ending threshold",
            "Enter a positive ending total",
        )
    }
    val validationRequester = remember { BringIntoViewRequester() }
    val editorListState = rememberLazyListState()
    var showScheduleOptions by rememberSaveable(editorKey) {
        mutableStateOf(
            defaults.powerMode || initial.reminderMinutes.isNotEmpty() ||
                initial.weekdayReminderMinutes.values.any { it.isNotEmpty() } ||
                initial.endType != HabitEndType.Never || initial.weekStart != defaultWeekStart,
        )
    }
    var showAdditionalDetails by rememberSaveable(editorKey) {
        mutableStateOf(
            defaults.powerMode || initial.notes.isNotBlank() || initial.tags.isNotEmpty() ||
                initial.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Duration, HabitTrackingMode.Decimal, HabitTrackingMode.LogOnly) &&
                    initial.precision != (if (initial.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Duration)) 0 else defaults.numberPrecision) ||
                initial.trackingMode.supportsQuickAddAmounts() && initial.quickActions.isNotEmpty(),
        )
    }
    LaunchedEffect(validationRequested, validationMessages) {
        if (validationRequested && validationMessages.isNotEmpty()) validationRequester.bringIntoView()
        if (validationRequested && validationMessages.any { it.contains("Decimal places", ignoreCase = true) }) showAdditionalDetails = true
    }
    LaunchedEffect(validationRequested, advancedScheduleProblems) {
        if (validationRequested && advancedScheduleProblems.isNotEmpty()) showScheduleOptions = true
    }
    LaunchedEffect(persistenceError) {
        if (!persistenceError.isNullOrBlank()) editorListState.scrollToItem(0)
    }
    val editorFingerprint = listOf(
        name, notes, areaId, area, tags, icon, mode, comparison, targetMin, targetMax,
        targetPeriod, schedule, interval, weekdays.sortedBy { it.value }, flexible, rollingDays,
        quickIncrement, quickActions, reminders, weekdayReminders, endType,
        endDate, endValue, weekStart, unitId, dimension, precision, sourceMeasurementId,
        checklistDrafts.map { "${it.id}:${it.uuid}:${it.position}:${it.name}" }, autoCompleteFromItems,
    ).joinToString("\u001f")
    val initialFingerprint by rememberSaveable(editorKey) { mutableStateOf(editorFingerprint) }
    var showDiscardConfirmation by rememberSaveable(editorKey) { mutableStateOf(false) }
    val requestDismiss = { if (editorFingerprint != initialFingerprint) showDiscardConfirmation = true else onDismiss() }
    BackHandler(enabled = !showDiscardConfirmation && !saving, onBack = requestDismiss)
    ProductivityEditorDialog(
        modifier = modifier,
        testTag = "habit-editor-surface",
        primary = true,
        paneTitle = if (habit == null) "Create Habit" else "Edit Habit",
        onDismissRequest = { if (!saving) requestDismiss() },
        title = { Text(if (habit == null) "Create Habit" else "Edit Habit") },
        text = {
            WhipReorderLazyColumn(
                modifier = Modifier.testTag("habit-editor-fields"),
                state = editorListState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Text("* Required field", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (persistenceError != null) item {
                    PersistenceFailureNotice(
                        message = persistenceError,
                        testTag = "habit-persistence-save-problem",
                    )
                }
                if (validationRequested && validationMessages.isNotEmpty()) item {
                    FormValidationSummary(
                        messages = validationMessages,
                        visible = true,
                        modifier = Modifier.bringIntoViewRequester(validationRequester),
                        testTag = "habit-save-problem",
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
                        singleLine = true,
                        isError = validationRequested && name.isBlank(),
                        supportingText = if (validationRequested && name.isBlank()) {{ Text("Habit name is required") }} else {{ Text("${name.length}/100") }},
                        modifier = Modifier.fillMaxWidth().testTag("habit-editor-name"),
                    )
                        },
                        emojiPicker = {
                    WhipEmojiPicker(
                        value = icon,
                        defaultEmoji = DEFAULT_HABIT_EMOJI,
                        onValueChange = { icon = it },
                        modifier = Modifier.fillMaxWidth(),
                        customEmojis = customIdentityEmojis,
                        onSaveEmoji = onSaveIdentityEmoji,
                        onRemoveSavedEmoji = onRemoveSavedIdentityEmoji,
                    )
                        },
                    )
                }
                item { EditorSectionHeader("Tracking") }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HabitTrackingMode.entries.forEach { selected ->
                            WhipFilterChip(
                                selected = mode == selected,
                                enabled = sourceMeasurementId == null && (habit == null || habit.canChangeTrackingModeTo(selected)),
                                onClick = {
                                    val existingUnit = unitId
                                    mode = selected
                                    when (selected) {
                                        HabitTrackingMode.Duration -> { unitId = "second"; dimension = UnitDimension.Duration; precision = "0"; comparison = TargetComparison.AtLeast }
                                        HabitTrackingMode.Rating -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); comparison = TargetComparison.None }
                                        HabitTrackingMode.Decimal -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); comparison = TargetComparison.AtLeast }
                                        HabitTrackingMode.LogOnly -> { unitId = "unitless"; dimension = UnitDimension.Unitless; precision = defaults.numberPrecision.toString(); comparison = TargetComparison.None }
                                        HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist -> { unitId = "count"; dimension = UnitDimension.Count; precision = "0"; comparison = TargetComparison.AtLeast; targetMin = "1"; targetPeriod = TargetPeriod.Occurrence }
                                        HabitTrackingMode.Count -> { unitId = "count"; dimension = UnitDimension.Count; precision = "0"; comparison = TargetComparison.AtLeast }
                                    }
                                    if (habit != null) {
                                        dimension = habit.dimension
                                        unitId = existingUnit
                                    }
                                },
                                label = { Text(selected.uiLabel()) },
                            )
                        }
                    }
                    AvailabilityNotice(
                        label = "Tracking mode",
                        availability = ControlAvailability(
                            enabled = sourceMeasurementId == null,
                            unavailableExplanation = "The linked measurement determines this value.",
                        ),
                    )
                    if (habit != null && sourceMeasurementId == null) Text(
                        "This Habit keeps its ${habit.dimension.uiLabel()} measurement type. Create a new Habit for a different type or timer.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("habit-edit-measurement-contract"),
                    )
                }
                if (mode == HabitTrackingMode.Checklist) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Checklist Items", fontWeight = FontWeight.Bold)
                            WhipReorderLayout(itemSpacing = 6.dp) {
                            checklistDrafts.forEachIndexed { index, draft ->
                                key(draft.uuid ?: "habit-checklist-${draft.id ?: index}") {
                                val reorderInteraction = rememberWhipReorderInteractionState()
                                Row(
                                    modifier = Modifier.whipReorderItem(
                                        reorderInteraction,
                                        layoutPosition = index + 1,
                                        layoutScope = "habit-editor-checklist",
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    WhipReorderHandle(
                                        label = draft.name.ifBlank { "checklist item ${index + 1}" },
                                        canMovePrevious = index > 0,
                                        canMoveNext = index < checklistDrafts.lastIndex,
                                        position = index + 1,
                                        total = checklistDrafts.size,
                                        interactionState = reorderInteraction,
                                        moveWholeItem = true,
                                        layoutScope = "habit-editor-checklist",
                                        onMove = { delta -> checklistDrafts = ArrayList(moveListItem(checklistDrafts, index, delta)) },
                                    )
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
                                        onClick = {
                                            checklistDrafts = ArrayList(checklistDrafts).also { it.removeAt(index) }
                                        },
                                    ) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${draft.name.ifBlank { "item ${index + 1}" }}") }
                                }
                                }
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
                            WhipToggleRow(
                                title = "Complete Habit With Final Item",
                                supportingText = "Automatically complete the parent Habit when every checklist item is checked.",
                                checked = autoCompleteFromItems,
                                onCheckedChange = { autoCompleteFromItems = it },
                                modifier = Modifier.testTag("habit-auto-complete-from-items"),
                            )
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
                if (quickAddsEnabled) {
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
                    if (sourceMeasurementId == null && mode != HabitTrackingMode.Rating) {
                        item {
                            if (habit != null) Text("Measurement type · ${dimension.uiLabel()}")
                            else EnumDropdown("What are you tracking?", UnitDimension.entries, dimension, UnitDimension::uiLabel) { selected ->
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
                                onSelect = { selectedId ->
                                    val units = BuiltInUnits.all + customUnits
                                    val from = units.firstOrNull { it.id == unitId }
                                    val to = units.firstOrNull { it.id == selectedId }
                                    if (selectedId == unitId) {
                                        unitChangeError = null
                                    } else if (from == null || to == null || from.dimension != to.dimension) {
                                        unitId = selectedId
                                        unitChangeError = null
                                    } else if (habit != null && from.toCanonicalOffset != to.toCanonicalOffset) {
                                        // Adding affine readings is unit-dependent; there is no constant
                                        // target conversion that preserves every possible period total.
                                        unitChangeError = "These units use different zero points, so changing them would change this Habit's totals. Create a new Habit to use ${to.symbol.ifBlank { to.name }}. Your current unit and values are unchanged."
                                    } else {
                                        val raw = listOf(
                                            targetMin.takeIf { comparison in setOf(TargetComparison.AtLeast, TargetComparison.Exactly, TargetComparison.WithinRange) }.orEmpty(),
                                            targetMax.takeIf { comparison in setOf(TargetComparison.AtMost, TargetComparison.WithinRange) }.orEmpty(),
                                            quickIncrement.takeIf { quickAddsEnabled }.orEmpty(),
                                            endValue.takeIf { endType == HabitEndType.AfterTotal }.orEmpty())
                                        val converted = convertNumericDraftValues(raw, from, to)
                                        val actions = if (quickAddsEnabled && quickActionResult.error == null) {
                                            convertNumericDraftValues(quickActionResult.values.map(::plainNumericValue), from, to)
                                        } else if (!quickAddsEnabled) emptyList() else null
                                        if (converted == null || actions == null) {
                                            unitChangeError = "Finish or clear the numeric targets, quick actions, and ending total before changing the unit. Values must be finite in the selected unit. Your current unit and draft are unchanged."
                                        } else {
                                            targetMin = converted[0]
                                            targetMax = converted[1]
                                            if (quickAddsEnabled) {
                                                quickIncrement = converted[2]
                                                quickActions = actions.joinToString(";")
                                            }
                                            if (endType == HabitEndType.AfterTotal) endValue = converted[3]
                                            unitId = selectedId
                                            unitChangeError = null
                                        }
                                    }
                                },
                                onCreateUnit = onCreateCustomUnit,
                                dialogModifier = modifier,
                                enabled = habit?.timerSessionId == null,
                                supportingText = if (habit?.timerSessionId != null) "Stop or discard the timer before changing its unit."
                                    else "Changing the unit converts your targets and quick amounts. Saved check-ins keep their original values and units.",
                            )
                            unitChangeError?.let { message ->
                                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("habit-unit-change-error"))
                            }
                        }
                    }
                    item {
                        EnumDropdown("Target rule", TargetComparison.entries, comparison, TargetComparison::displayLabel) { selected ->
                            if (selected == TargetComparison.AtMost && targetMax.isBlank()) targetMax = targetMin
                            if (selected in setOf(TargetComparison.AtLeast, TargetComparison.Exactly) && targetMin.isBlank()) {
                                targetMin = targetMax
                            }
                            comparison = selected
                        }
                    }
                    if (comparison != TargetComparison.None) {
                        item {
                            if (comparison == TargetComparison.WithinRange) {
                                ResponsiveFieldPair(
                                    first = { field -> NumberTextField(targetMin, { targetMin = it }, "Minimum", field) },
                                    second = { field -> NumberTextField(targetMax, { targetMax = it }, "Maximum", field) },
                                )
                            } else {
                                val maximum = comparison == TargetComparison.AtMost
                                NumberTextField(
                                    if (maximum) targetMax else targetMin,
                                    { value -> if (maximum) targetMax = value else targetMin = value },
                                    when (targetPeriod) {
                                        TargetPeriod.Week -> if (maximum) "Maximum per week" else "Target per week"
                                        TargetPeriod.Month -> if (maximum) "Maximum per month" else "Target per month"
                                        TargetPeriod.Occurrence -> if (maximum) "Maximum each time" else "Target each time"
                                        TargetPeriod.RollingDays -> if (maximum) "Maximum per window" else "Target per window"
                                        TargetPeriod.Day -> if (maximum) "Maximum per day" else "Target per day"
                                    },
                                )
                            }
                        }
                        item {
                            EnumChips("Target period", TargetPeriod.entries, targetPeriod, TargetPeriod::displayLabel) { targetPeriod = it }
                            Text(targetPeriod.explanation(schedule), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (targetPeriod == TargetPeriod.RollingDays) item { NumberTextField(rollingDays, { rollingDays = it }, "Rolling-day window") }
                    }
                }
                item { EditorSectionHeader("Schedule") }
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
                                WhipFilterChip(
                                    selected = day in weekdays,
                                    onClick = { weekdays = if (day in weekdays) weekdays - day else weekdays + day },
                                    label = { Text(weekdayFormatter.label(day, WhipWeekdayLabelWidth.Short)) },
                                )
                            }
                        }
                    }
                }
                item {
                    val defaultReminderTimes = reminders.split(',')
                        .mapNotNull { parseClockMinutes(it.trim()) }
                        .distinct()
                        .sorted()
                    val weekdayReminderTimes = parseWeekdayReminderMap(weekdayReminders)
                    val context = LocalContext.current
                    Column(
                        modifier = Modifier.fillMaxWidth().testTag("habit-schedule-summary"),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Reminders", fontWeight = FontWeight.Bold)
                        Text(
                            habitReminderSummary(defaultReminderTimes, weekdayReminderTimes, weekdayFormatter) { minutes ->
                                formatClockMinutes(context, minutes)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (endType != HabitEndType.Never) {
                            Text(
                                habitEndSummary(endType, endDate, currentDraft.endValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (weekStart != defaultWeekStart) {
                            Text(
                                "Week starts ${weekdayFormatter.label(weekStart, WhipWeekdayLabelWidth.Full)} (different from your app default)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DisclosureButton(
                            label = "Reminders & Schedule Options",
                            expanded = showScheduleOptions,
                            onClick = { showScheduleOptions = !showScheduleOptions },
                            modifier = Modifier.fillMaxWidth().testTag("habit-schedule-options"),
                        )
                    }
                }
                if (showScheduleOptions) {
                    item {
                        EditorSectionHeader(
                            "Reminder & Schedule Options",
                            "Set notification times, optional weekday overrides, an ending rule, or a different week boundary.",
                        )
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
                    item { EnumDropdown("First Day of Week", DayOfWeek.entries, weekStart, { weekdayFormatter.label(it, WhipWeekdayLabelWidth.Full) }) { weekStart = it } }
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
                        inheritedFromScope = habit == null && initialDraft?.areaId == null && defaultAreaId != null,
                    )
                        },
                        extras = {
                    DisclosureButton(
                        label = "Additional Details",
                        expanded = showAdditionalDetails,
                        onClick = { showAdditionalDetails = !showAdditionalDetails },
                        modifier = Modifier.fillMaxWidth(),
                    )
                        },
                    )
                }
                if (showAdditionalDetails) {
                    item {
                        EditorSectionHeader("Details")
                    }
                    if (sourceMeasurementId == null && mode !in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist, HabitTrackingMode.Rating)) item {
                        NumberTextField(precision, { precision = it }, "Decimal places (0–6)")
                    }
                    item { OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma-separated") }, modifier = Modifier.fillMaxWidth()) }
                    if (quickAddsEnabled) item {
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
                enabled = !saving,
                onClick = {
                    validationRequested = true
                    if (validationMessages.isEmpty()) onSave(currentDraft)
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
        inputBlocked = saving,
        inputBlockedLabel = "Saving Habit",
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

internal fun Habit.canChangeTrackingModeTo(mode: HabitTrackingMode): Boolean = when {
    sourceMeasurementId != null -> false
    trackingMode == HabitTrackingMode.Duration || mode == HabitTrackingMode.Duration -> mode == trackingMode
    mode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist) -> dimension == UnitDimension.Count
    mode == HabitTrackingMode.Rating -> dimension == UnitDimension.Unitless
    else -> true
}

@Composable
internal fun HabitValueDialog(
    item: HabitDayProgress,
    onDismiss: () -> Unit,
    onLog: (Double?, String) -> Unit,
    saving: Boolean = false,
    persistenceError: String? = null,
    customUnits: List<UnitDefinition> = emptyList(),
    additive: Boolean = false,
) {
    val initialValue by rememberSaveable(item.habit.id, item.date) {
        mutableStateOf(if (additive || item.habit.trackingMode == HabitTrackingMode.LogOnly) "" else plainNumericValue(item.value))
    }
    var value by rememberSaveable(item.habit.id, item.date) {
        mutableStateOf(initialValue)
    }
    var note by rememberSaveable(item.habit.id, item.date) { mutableStateOf("") }
    var confirmDiscard by rememberSaveable(item.habit.id, item.date) { mutableStateOf(false) }
    var validationRequested by rememberSaveable(item.habit.id, item.date) { mutableStateOf(false) }
    fun requestDismiss() {
        if (saving) return
        if (value != initialValue || note.isNotEmpty()) confirmDiscard = true else onDismiss()
    }
    val logOnly = item.habit.trackingMode == HabitTrackingMode.LogOnly
    val setsPeriodTotal = !additive && item.habit.trackingMode in setOf(
        HabitTrackingMode.Count,
        HabitTrackingMode.Decimal,
        HabitTrackingMode.Duration,
    )
    val parsedValue = value.toWhipDoubleOrNull()
    val validValue = if (logOnly) value.isBlank() || parsedValue?.isFinite() == true else parsedValue?.isFinite() == true
    PaneAwareAlertDialog(
        testTag = "habit-value-dialog",
        onDismissRequest = ::requestDismiss,
        title = { Text(if (additive) "Add Amount" else item.habit.todayCheckInTitle()) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersistenceFailureNotice(persistenceError, testTag = "habit-value-save-problem")
                Text("${item.habit.icon} ${item.habit.name}", style = MaterialTheme.typography.titleMedium)
                if (additive) Text("Adds a new entry to this period; your existing entries stay unchanged.", style = MaterialTheme.typography.bodySmall)
                if (setsPeriodTotal) Text(
                    "Current ${item.habit.periodTotalDescription()}: " +
                        "${formatHabitValue(item.value, item.habit.precision)} " +
                        "${item.habit.unitSymbol(customUnits)}. Saving sets this total; it does not add to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumberTextField(
                    value,
                    { value = it },
                    if (setsPeriodTotal) item.habit.periodTotalAmountLabel(customUnits)
                    else if (additive) "Amount to Add (${item.habit.unitSymbol(customUnits)})"
                    else item.habit.historyAmountLabel(optional = logOnly, customUnits = customUnits),
                    modifier = Modifier.testTag("habit-value-input"),
                    enabled = !saving,
                    validationError = "Enter a valid, finite number".takeIf { validationRequested && !validValue },
                )
                if (logOnly) Text(
                    "A note is enough; add a number only when it is useful.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    note,
                    { note = it },
                    label = { Text(if (logOnly) "What happened? (optional)" else "Note (optional)") },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("habit-value-note"),
                )
                if (item.habit.quickActions.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item.habit.quickActions.forEach { quick ->
                            val label = editableNumericValue(quick)
                            WhipTextButton(enabled = !saving, onClick = { value = label }) { Text(label) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !saving,
                onClick = { validationRequested = true; if (validValue) onLog(parsedValue, note) },
                modifier = Modifier.testTag("habit-value-save"),
            ) { Text(if (saving) "Saving…" else if (additive) "Add Amount" else if (logOnly) "Add Entry" else "Save") }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = ::requestDismiss) { Text("Cancel") } },
        inputBlocked = saving,
        inputBlockedLabel = "Saving Habit Check-In",
    )
    if (confirmDiscard && !saving) UnsavedChangesDialog(
        subject = "Habit check-in",
        onKeepEditing = { confirmDiscard = false },
        onDiscard = { confirmDiscard = false; onDismiss() },
    )
}

@Composable
internal fun HabitHistoryLogDialog(
    item: HabitDayProgress,
    log: HabitLog?,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Double?, HabitLogStatus, LocalDate, String) -> Unit,
    onDelete: (() -> Unit)? = null,
    saving: Boolean = false,
    persistenceError: String? = null,
    customUnits: List<UnitDefinition> = emptyList(),
) {
    val editorKey = "habit-log-${log?.id ?: "${item.habit.id}-${initialDate.toEpochDay()}"}"
    val openingValue by rememberSaveable(editorKey) { mutableStateOf(log?.value?.let(::plainNumericValue).orEmpty()) }
    val openingDate by rememberSaveable(editorKey) { mutableStateOf(initialDate) }
    val openingNote by rememberSaveable(editorKey) { mutableStateOf(log?.note.orEmpty()) }
    var value by rememberSaveable(editorKey) { mutableStateOf(openingValue) }
    var date by rememberSaveable(editorKey) { mutableStateOf(openingDate) }
    var note by rememberSaveable(editorKey) { mutableStateOf(openingNote) }
    var showDatePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(editorKey) { mutableStateOf(false) }
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }
    fun requestDismiss() {
        if (saving) return
        if (value != openingValue || date != openingDate || note != openingNote) confirmDiscard = true else onDismiss()
    }
    val mode = item.habit.trackingMode
    val checkBased = mode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist)
    val showsAmount = !checkBased || (log?.value != null && log.value !in setOf(0.0, 1.0))
    val requiresAmount = if (log != null) log.value != null else
        mode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.Duration, HabitTrackingMode.Rating)
    val dateIsValid = !date.isAfter(LocalWhipToday.current)
    val parsedValue = value.toWhipDoubleOrNull()
    val amountIsValid = when {
        requiresAmount -> parsedValue?.isFinite() == true
        !showsAmount -> true
        else -> value.isBlank() || parsedValue?.isFinite() == true
    }
    PaneAwareAlertDialog(
        testTag = "habit-history-dialog",
        onDismissRequest = ::requestDismiss,
        title = { Text(item.habit.historyDialogTitle(editing = log != null)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
            ) {
                PersistenceFailureNotice(persistenceError, testTag = "habit-history-save-problem")
                Text("${item.habit.icon} ${item.habit.name}", style = MaterialTheme.typography.titleMedium)
                WhipOutlinedButton(
                    enabled = !saving,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Date · ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                }
                if (!dateIsValid) Text(
                    "Choose today or an earlier date. Future check-ins cannot be recorded.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("habit-history-date-error"),
                )
                if (log != null) Text(
                    "Recorded outcome: ${log.status.activityLabel()}. Editing the note or date keeps this outcome.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (showsAmount) {
                    NumberTextField(
                        value,
                        { value = it },
                        item.habit.historyAmountLabel(
                            unitId = log?.enteredUnitId ?: item.habit.unitId,
                            optional = !requiresAmount,
                            customUnits = customUnits,
                        ),
                        modifier = Modifier.testTag("habit-history-value"),
                        enabled = !saving,
                        validationError = "Enter a valid, finite number".takeIf { validationRequested && !amountIsValid },
                    )
                    if (!requiresAmount) Text(
                        "A note is enough; add a number only when it is useful.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    note,
                    { note = it },
                    label = { Text(if (mode == HabitTrackingMode.LogOnly) "What happened? (optional)" else "Note (optional)") },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("habit-history-note"),
                )
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = dateIsValid && !saving,
                onClick = {
                    validationRequested = true
                    if (!amountIsValid) return@WhipTextButton
                    val effectiveValue = if (showsAmount) parsedValue else if (log != null) log.value else 1.0
                    val effectiveStatus = log?.status ?: when (mode) {
                        HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist -> HabitLogStatus.Success
                        else -> HabitLogStatus.Recorded
                    }
                    onSave(effectiveValue, effectiveStatus, date, note)
                },
            ) { Text(if (saving) "Saving…" else if (log == null) "Record" else "Save Changes") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) WhipDestructiveTextButton(enabled = !saving, onClick = { confirmDelete = true }) {
                    Text("Delete")
                }
                WhipTextButton(enabled = !saving, onClick = ::requestDismiss) { Text("Cancel") }
            }
        },
        inputBlocked = saving,
        inputBlockedLabel = if (onDelete != null && confirmDelete) "Updating Habit History" else "Saving Habit History",
    )
    if (showDatePicker) {
        WhipDatePickerDialog(date, { showDatePicker = false }, { selected -> date = selected; showDatePicker = false })
    }
    if (confirmDelete && onDelete != null) {
        PaneAwareAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Check-In?") },
            text = { Text("This removes the check-in from ${item.habit.name}'s history and recalculates its progress and streak.") },
            confirmButton = {
                WhipDestructiveTextButton(
                    onClick = { confirmDelete = false; onDelete() },
                    modifier = Modifier.testTag("habit-history-confirm-delete"),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { WhipTextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    if (confirmDiscard && !saving) UnsavedChangesDialog(
        subject = "Habit history update",
        onKeepEditing = { confirmDiscard = false },
        onDiscard = { confirmDiscard = false; onDismiss() },
    )
}

@Composable
internal fun HabitActionsDialog(
    item: HabitDayProgress,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPin: () -> Unit,
    onPause: () -> Unit,
    onSchedulePause: () -> Unit,
    onQuick: () -> Unit,
    onSkip: () -> Unit,
    onUndoSkip: () -> Unit,
    onUndoHistoricalSkip: (LocalDate) -> Unit,
    logs: List<HabitLog>,
    skips: List<HabitSkip>,
    pauses: List<HabitPause>,
    onAddHistoricalLog: () -> Unit,
    onEnterDurationManually: () -> Unit = onAddHistoricalLog,
    onEditLog: (HabitLog) -> Unit,
    onEditPause: (HabitPause) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    lowPressureMode: Boolean = false,
    mutationSaving: Boolean = false,
    mutationError: String? = null,
    openHistory: Boolean = false,
    customUnits: List<UnitDefinition> = emptyList(),
    onChecklist: (Long, Long, LocalDate, Boolean) -> Unit = { _, _, _, _ -> },
    onQuickValue: (Double) -> Unit = {},
    onSetValue: () -> Unit = {},
    onAddAmount: () -> Unit = {},
    onUndoValue: () -> Unit = {},
    canUndoValue: Boolean = false,
) {
    val activeZoneId = LocalWhipZone.current
    val historyLocale = LocalConfiguration.current.locales[0]
    var historyQuery by rememberSaveable(item.habit.id) { mutableStateOf("") }
    val historyIndex = remember(logs, skips, pauses, item.date, item.habit, customUnits, historyLocale) {
        habitHistoryEvents(logs, skips, pauses, item.date).map {
            it to it.habitHistorySearchText(item.habit, item.date, customUnits, historyLocale)
        }
    }
    val normalizedHistoryQuery = historyQuery.trim().lowercase(Locale.ROOT)
    val matchingEvents = remember(historyIndex, normalizedHistoryQuery) {
        historyIndex.filter { normalizedHistoryQuery in it.second }.map { it.first }
    }
    var visibleLogs by rememberSaveable(item.habit.id, normalizedHistoryQuery) { mutableIntStateOf(8) }
    val timerElapsedSeconds by rememberHabitTimerElapsedSeconds(item.habit)
    var section by rememberSaveable(item.habit.id, openHistory) {
        mutableStateOf(if (item.habit.archived || openHistory) HabitDetailSection.History else HabitDetailSection.Today)
    }
    val skipAvailable = item.dayState == HabitDayState.Pending &&
        item.habit.sourceMeasurementId == null &&
        item.habit.scheduleType !in setOf(
            HabitScheduleType.FlexibleTimesPerWeek,
            HabitScheduleType.FlexibleTimesPerMonth,
        )
    val newEntryUnavailable = item.habit.newEntryUnavailableReason()
    val primaryAction = when {
        item.habit.timerStartedAtMillis != null -> EntityInspectorPrimaryAction(
            "timer",
            item.inspectorPrimaryActionLabel(customUnits),
            onQuick,
        )
        item.habit.archived -> EntityInspectorPrimaryAction("restore", "Restore", onArchive)
        item.habit.sourceMeasurementId != null -> null
        item.habit.paused -> EntityInspectorPrimaryAction("resume", "Resume Habit", onPause)
        item.dayState == HabitDayState.Paused -> null
        item.dayState == HabitDayState.Skipped -> EntityInspectorPrimaryAction("undo-skip", "Undo Today's Skip", onUndoSkip)
        item.dayState == HabitDayState.NotScheduled -> EntityInspectorPrimaryAction(
            "check-in-outside-schedule",
            item.inspectorOutsideScheduleActionLabel(),
            onQuick,
        )
        else -> EntityInspectorPrimaryAction("check-in", item.inspectorPrimaryActionLabel(customUnits), onQuick)
    }
    EntityInspector(
        entityType = "Habit",
        stateKey = "habit:${item.habit.id}",
        title = item.habit.name,
        emoji = item.habit.icon,
        context = item.habit.area.ifBlank {
            item.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        },
        status = item.inspectorStatus(lowPressureMode),
        statusTone = item.inspectorStatusTone(),
        sections = HabitDetailSection.entries.map { it.inspectorSection },
        selectedSectionId = section.id,
        onSelectSection = { id -> section = HabitDetailSection.entries.first { it.id == id } },
        onDismiss = onDismiss,
        onEdit = onEdit,
        editLabel = "Edit Habit",
        modifier = modifier,
        connectedSurfaceTag = "habit-detail-surface",
        connectedSectionTagPrefix = "habit-detail-section",
        primaryAction = primaryAction,
        inputBlocked = mutationSaving,
        inputBlockedLabel = "Updating Habit",
        content = {
            if (section == HabitDetailSection.Insights) HabitInsights(
                state = HabitUiState(all = listOf(item), logs = logs, skips = skips, pauses = pauses, currentDate = item.date, customUnits = customUnits, loading = false),
                lowPressureMode = lowPressureMode, focused = true,
                onOpenHistory = { section = HabitDetailSection.History },
            ) else LazyColumn(
                modifier = Modifier.testTag("habit-detail-content"),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.screenExpanded),
            ) {
                if (!mutationError.isNullOrBlank()) item {
                    PersistenceFailureNotice(mutationError, testTag = "habit-actions-save-problem")
                }
                when (section) {
                    HabitDetailSection.Today -> {
                        item { WhipGroupedInformationCard(
                            Modifier.testTag("habit-today-overview"),
                        ) {
                            Text(
                                "Today's check-in",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                item.inspectorTodaySummary(
                                    timerElapsedSeconds = timerElapsedSeconds,
                                    activeZoneId = activeZoneId,
                                    lowPressureMode = lowPressureMode,
                                    customUnits = customUnits,
                                ),
                                modifier = Modifier.testTag("habit-today-state"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    item.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                                    modifier = Modifier.testTag("habit-today-date"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (item.habit.trackingMode == HabitTrackingMode.Duration && item.habit.sourceMeasurementId == null) {
                                EntityInspectorAction(
                                    id = "enter-duration-manually",
                                    label = "Enter Duration Manually",
                                    onClick = onEnterDurationManually,
                                    enabled = newEntryUnavailable == null,
                                    supportingText = newEntryUnavailable ?: "Record a duration when you forgot to start the timer.",
                                )
                            }
                        }
                        }
                        if (item.habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal) &&
                            !item.habit.archived && !item.habit.paused && item.habit.sourceMeasurementId == null &&
                            item.dayState !in setOf(HabitDayState.Paused, HabitDayState.Skipped, HabitDayState.NotScheduled)) item {
                            EntityInspectorInformationGroup("Record or Adjust") {
                                HabitNumericActions(item, customUnits, onQuickValue, onSetValue, onAddAmount,
                                    onDecrement = { onQuickValue(-minOf(item.habit.quickIncrement, item.value.coerceAtLeast(0.0))) },
                                    onUndo = onUndoValue, canUndo = canUndoValue, enabled = !mutationSaving)
                            }
                        }
                        if (item.habit.trackingMode == HabitTrackingMode.Checklist && item.checklistItems.isNotEmpty()) item {
                            EntityInspectorInformationGroup(title = "Today's Checklist") {
                                val editable = item.date == LocalWhipToday.current && !mutationSaving && !item.habit.archived && !item.habit.paused &&
                                    item.habit.sourceMeasurementId == null &&
                                    item.dayState !in setOf(HabitDayState.Paused, HabitDayState.Skipped, HabitDayState.NotScheduled)
                                Text(
                                    if (editable) {
                                        if (item.habit.autoCompleteFromItems) "The final item completes today's check-in."
                                        else "Check items independently, then complete the Habit when you are ready."
                                    } else "Checklist items are read-only while this Habit is unavailable for today's check-in.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                HabitChecklist(item, onChecklist, enabled = editable)
                            }
                        }
                        if (skipAvailable) item {
                            WhipActionList {
                                WhipActionRow(
                                    title = "Skip Today",
                                    onClick = onSkip,
                                    modifier = Modifier.testTag("entity-inspector-action-skip-today"),
                                    supportingText = if (lowPressureMode) {
                                        "No check-in will be expected today."
                                    } else {
                                        "No check-in will be expected; your streak stays protected."
                                    },
                                    icon = Icons.Outlined.PauseCircleOutline,
                                    navigates = false,
                                )
                            }
                        }
                    }
                    HabitDetailSection.History -> {
                        item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.habit.sourceMeasurementId == null) {
                                EntityInspectorAction(
                                    id = "add-past-entry", label = item.habit.pastCheckInActionLabel(),
                                    onClick = onAddHistoricalLog,
                                    enabled = newEntryUnavailable == null,
                                    supportingText = newEntryUnavailable ?: "Choose an earlier date and record what happened.",
                                )
                            } else Text("This history comes from a linked measurement.")
                            WhipSectionHeading("Habit History", compact = true)
                            if (historyIndex.isNotEmpty()) {
                                WhipSearchField(
                                    label = "Search Habit History", query = historyQuery,
                                    onQueryChange = { historyQuery = it }, hint = "Value, date, note, skip, or pause",
                                    modifier = Modifier.fillMaxWidth().testTag("habit-history-search"),
                                )
                                Text("${matchingEvents.size} of ${quantityLabel(historyIndex.size, "event")}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.testTag("habit-history-result-count"))
                            } else Text("Nothing recorded yet.")
                        } }
                        if (matchingEvents.isEmpty() && historyQuery.isNotBlank()) item {
                            WhipEmptyState("No Matching Events", "Try a value, date, or words from a note.",
                                primaryActionLabel = "Clear Search", onPrimaryAction = { historyQuery = "" })
                        }
                        items(matchingEvents.take(visibleLogs), key = { event ->
                            when (event) {
                                is HabitHistoryEvent.Log -> "log-${event.value.uuid}"
                                is HabitHistoryEvent.Skip -> "skip-${event.value.localDate}"
                                is HabitHistoryEvent.Pause -> "pause-${event.value.id}"
                            }
                        }) { event ->
                            when (event) {
                                is HabitHistoryEvent.Log -> EntityInspectorAction(
                                    id = "log-${event.value.id}",
                                    label = event.value.activityTitle(item.habit, customUnits),
                                    supportingText = event.value.activitySupportingText(item.date),
                                    enabled = event.value.isUserEditable(),
                                    onClick = { if (event.value.isUserEditable()) onEditLog(event.value) },
                                )
                                is HabitHistoryEvent.Skip -> EntityInspectorAction(
                                    id = "skip-${event.value.localDate.toEpochDay()}",
                                    label = "Skipped · ${event.value.localDate.relativeActivityDate(item.date)}",
                                    supportingText = "Undo this skip and return the day to its normal schedule.",
                                    onClick = { onUndoHistoricalSkip(event.value.localDate) },
                                )
                                is HabitHistoryEvent.Pause -> EntityInspectorAction(
                                    id = "pause-history-${event.value.id}",
                                    label = "Paused · ${event.value.displayDateRange()}",
                                    supportingText = event.value.note.ifBlank {
                                        "Excluded from check-ins, misses, and reminders · tap to edit"
                                    },
                                    onClick = { onEditPause(event.value) },
                                )
                            }
                        }
                        if (visibleLogs < matchingEvents.size) item {
                            EntityInspectorAction(
                                id = "show-more-history", label = "Show More History",
                                supportingText = "${matchingEvents.size - visibleLogs} earlier events",
                                onClick = { visibleLogs = (visibleLogs + 25).coerceAtMost(matchingEvents.size) },
                            )
                        }
                    }
                    HabitDetailSection.Insights -> Unit
                    HabitDetailSection.More -> item {
                        if (!item.habit.archived) {
                            EntityInspectorGroup("Whip Home") {
                                WhipActionList {
                                    WhipActionRow(
                                        title = if (item.habit.pinned) "Unpin from Whip Home" else "Pin to Whip Home",
                                        onClick = onPin,
                                        modifier = Modifier.testTag("entity-inspector-action-pin"),
                                        supportingText = if (item.habit.pinned) {
                                            "The Habit keeps its schedule and remains available in Habits."
                                        } else {
                                            "When this Habit is due, it stays first in Whip Home's Habits section."
                                        },
                                        icon = Icons.Outlined.PushPin,
                                        navigates = false,
                                    )
                                }
                            }
                            EntityInspectorGroup("Schedule and availability") {
                                WhipActionList {
                                    if (!item.habit.paused) {
                                        WhipActionRow(
                                            title = "Pause Indefinitely",
                                            onClick = onPause,
                                            modifier = Modifier.testTag("entity-inspector-action-pause"),
                                            supportingText = "Stops check-ins until you resume. Use Schedule Pause Dates to mark a period as neutral in history.",
                                            icon = Icons.Outlined.PauseCircleOutline,
                                            navigates = false,
                                        )
                                        WhipActionDivider()
                                    }
                                    WhipActionRow(
                                        title = "Schedule Pause Dates",
                                        onClick = onSchedulePause,
                                        modifier = Modifier.testTag("entity-inspector-action-schedule-pause"),
                                        supportingText = "Choose a start and end date without changing the Habit.",
                                        icon = Icons.Outlined.CalendarMonth,
                                    )
                                    pauses.sortedByDescending(HabitPause::startDate).forEach { pause ->
                                        WhipActionDivider()
                                        WhipActionRow(
                                            title = pause.displayDateRange(),
                                            onClick = { onEditPause(pause) },
                                            modifier = Modifier.testTag("entity-inspector-action-pause-${pause.id}"),
                                            supportingText = pause.note.ifBlank { "Scheduled pause · tap to edit or delete" },
                                            icon = Icons.Outlined.CalendarMonth,
                                        )
                                    }
                                }
                            }
                            EntityInspectorGroup("Manage") {
                                WhipActionList {
                                    WhipActionRow(
                                        title = "Duplicate Habit",
                                        onClick = onDuplicate,
                                        modifier = Modifier.testTag("entity-inspector-action-duplicate"),
                                        icon = Icons.Outlined.ContentCopy,
                                        navigates = false,
                                    )
                                    WhipActionDivider()
                                    WhipActionRow(
                                        title = "Archive Habit",
                                        onClick = onArchive,
                                        modifier = Modifier.testTag("entity-inspector-action-archive"),
                                        supportingText = "Hides it from active Habit views without deleting its history.",
                                        icon = Icons.Outlined.Archive,
                                        navigates = false,
                                    )
                                }
                            }
                        }
                        EntityInspectorDangerZone {
                            Box(Modifier.testTag("entity-inspector-delete")) {
                                WhipActionRow(
                                    title = "Delete Habit Permanently",
                                    onClick = onDelete,
                                    modifier = Modifier.testTag("entity-inspector-action-delete"),
                                    supportingText = "Removes this Habit, its check-ins, checklist state, and streak history.",
                                    icon = Icons.Outlined.DeleteForever,
                                    navigates = false,
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

@Composable
private fun HabitTodayMetric(
    id: String,
    value: String,
    label: String,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 104.dp)
            .testTag("habit-today-metric-$id"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun HabitDayProgress.inspectorTodaySummary(
    timerElapsedSeconds: Double,
    activeZoneId: ZoneId,
    lowPressureMode: Boolean,
    customUnits: List<UnitDefinition> = emptyList(),
): String = when {
    habit.timerStartedAtMillis != null -> if (habit.timerNeedsReview) {
        "Timer needs review · ${formatElapsedDuration(timerElapsedSeconds)} estimated."
    } else {
        "Timer started ${Instant.ofEpochMilli(requireNotNull(habit.timerStartedAtMillis)).atZone(activeZoneId).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))} · ${formatElapsedDuration(timerElapsedSeconds)} elapsed."
    }
    habit.archived -> "This habit is archived. Use Restore below to resume check-ins."
    dayState == HabitDayState.Skipped -> if (lowPressureMode) {
        "Skipped today. No check-in is expected."
    } else {
        "Skipped today. Your streak remains protected."
    }
    dayState == HabitDayState.Paused -> "Paused today. No check-in is expected."
    dayState == HabitDayState.NotScheduled -> "Not scheduled today."
    habit.trackingMode == HabitTrackingMode.CheckOff -> if (dayState == HabitDayState.Completed) {
        "Completed today."
    } else {
        "Ready for today's check-in."
    }
    habit.trackingMode == HabitTrackingMode.Checklist -> {
        val completedItems = checklistItems.count { it.second }
        if (checklistItems.isEmpty()) "Ready for today's checklist."
        else "$completedItems of ${checklistItems.size} items complete."
    }
    habit.trackingMode == HabitTrackingMode.Rating -> if (status == null) "Not rated yet today."
        else "Rating: ${formatHabitValue(value, habit.precision)}."
    habit.trackingMode == HabitTrackingMode.LogOnly -> if (status == null) "No entries yet today." else "Entry recorded today."
    habit.comparison != TargetComparison.None -> targetProgressLabel(customUnits)
    else -> "${formatHabitValue(value, habit.precision)} ${habit.unitSymbol(customUnits)} · ${habit.periodTotalDescription()}".trim()
}

internal sealed interface HabitHistoryEvent {
    val effectiveDate: LocalDate
    val tieBreaker: Long

    data class Log(val value: HabitLog) : HabitHistoryEvent {
        override val effectiveDate: LocalDate = value.localDate
        override val tieBreaker: Long = value.timestamp.toEpochMilli()
    }

    data class Skip(val value: HabitSkip) : HabitHistoryEvent {
        override val effectiveDate: LocalDate = value.localDate
        override val tieBreaker: Long = value.skippedAtMillis
    }

    data class Pause(val value: HabitPause) : HabitHistoryEvent {
        override val effectiveDate: LocalDate = value.startDate
        override val tieBreaker: Long = value.id
    }
}

internal fun HabitHistoryEvent.habitHistorySearchText(
    habit: Habit, today: LocalDate, customUnits: List<UnitDefinition> = emptyList(),
    locale: Locale = Locale.getDefault(),
): String = when (this) {
    is HabitHistoryEvent.Log -> "$effectiveDate ${value.activityTitle(habit, customUnits)} ${value.activitySupportingText(today, locale)}"
    is HabitHistoryEvent.Skip -> "$effectiveDate Skipped ${value.localDate.relativeActivityDate(today, locale)}"
    is HabitHistoryEvent.Pause -> "$effectiveDate ${value.endDate} Paused ${value.displayDateRange(locale)} ${value.note}"
}.lowercase(Locale.ROOT)

internal fun habitHistoryEvents(
    logs: List<HabitLog>,
    skips: List<HabitSkip>,
    pauses: List<HabitPause>,
    throughDate: LocalDate,
): List<HabitHistoryEvent> {
    val events = logs.map(HabitHistoryEvent::Log) +
        skips.map(HabitHistoryEvent::Skip) +
        pauses.filter { !it.startDate.isAfter(throughDate) }.map(HabitHistoryEvent::Pause)
    return events
        .filter { !it.effectiveDate.isAfter(throughDate) }
        .sortedWith(
            compareByDescending<HabitHistoryEvent>(HabitHistoryEvent::effectiveDate)
                .thenByDescending(HabitHistoryEvent::tieBreaker),
        )
}

private enum class HabitDetailSection(val id: String, val label: String) {
    Today("today", "Today"),
    History("history", "History"),
    Insights("insights", "Insights"),
    More("options", "Options"),
    ;

    val inspectorSection: EntityInspectorSection
        get() = EntityInspectorSection(id = id, label = label)
}

internal fun HabitDayProgress.inspectorStatus(lowPressureMode: Boolean): String = when {
    habit.timerStartedAtMillis != null && habit.timerNeedsReview -> "Timer needs review"
    habit.timerStartedAtMillis != null -> "Timer running"
    habit.archived -> "Archived"
    habit.paused || dayState == HabitDayState.Paused -> "Paused"
    dayState == HabitDayState.Skipped -> "Skipped today"
    habit.comparison == TargetComparison.None && status != null -> "Recorded today"
    dayState == HabitDayState.Completed -> "Complete today"
    dayState == HabitDayState.BelowTarget -> if (lowPressureMode) "Checked in today" else "In progress today"
    dayState == HabitDayState.Missed -> if (lowPressureMode) "Ready for a fresh check-in" else "Missed"
    dayState == HabitDayState.NotScheduled -> "Not scheduled today"
    else -> "Ready today"
}

internal fun HabitDayProgress.inspectorStatusTone(): WhipStatusTone = when {
    habit.timerStartedAtMillis != null && habit.timerNeedsReview -> WhipStatusTone.Warning
    habit.timerStartedAtMillis != null -> WhipStatusTone.Info
    habit.archived -> WhipStatusTone.Neutral
    habit.paused || dayState == HabitDayState.Paused -> WhipStatusTone.Warning
    dayState == HabitDayState.Skipped -> WhipStatusTone.Warning
    dayState == HabitDayState.Completed -> WhipStatusTone.Success
    dayState == HabitDayState.BelowTarget -> WhipStatusTone.Info
    dayState == HabitDayState.Missed -> WhipStatusTone.Destructive
    dayState == HabitDayState.NotScheduled -> WhipStatusTone.Neutral
    else -> WhipStatusTone.Info
}

internal fun HabitDayProgress.inspectorPrimaryActionLabel(customUnits: List<UnitDefinition> = emptyList()): String = when (habit.trackingMode) {
    HabitTrackingMode.CheckOff -> if (successful == true) "Undo Check-In" else "Check In"
    HabitTrackingMode.Count, HabitTrackingMode.Decimal -> {
        val value = formatHabitValue(habit.quickIncrement, habit.precision)
        "Add $value ${habit.unitSymbol(customUnits)}".trim()
    }
    HabitTrackingMode.Duration -> when {
        habit.timerStartedAtMillis == null -> "Start Timer"
        habit.timerNeedsReview -> "Review Timer"
        else -> "Stop & Log"
    }
    HabitTrackingMode.Checklist -> if (successful == true) "Undo Today's Completion" else "Mark Today Complete"
    HabitTrackingMode.Rating -> "Rate Today"
    HabitTrackingMode.LogOnly -> "Add Entry"
}

private fun HabitDayProgress.inspectorOutsideScheduleActionLabel(): String = when (habit.trackingMode) {
    HabitTrackingMode.CheckOff -> "Check In Outside Schedule"
    HabitTrackingMode.Count, HabitTrackingMode.Decimal -> "Log Today Outside Schedule"
    HabitTrackingMode.Duration -> "Start Timer Outside Schedule"
    HabitTrackingMode.Checklist -> "Mark Today Complete Outside Schedule"
    HabitTrackingMode.Rating -> "Rate Outside Schedule"
    HabitTrackingMode.LogOnly -> "Add Entry Outside Schedule"
}

internal fun Habit.todayCheckInTitle(): String = when (trackingMode) {
    HabitTrackingMode.Rating -> "Rate Today"
    HabitTrackingMode.LogOnly -> "Add an Entry"
    HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.Duration ->
        "Set ${periodTotalTitle()}"
    else -> "Log Today's Progress"
}

private fun Habit.periodTotalTitle(): String = when (targetPeriod) {
    TargetPeriod.Occurrence, TargetPeriod.Day -> "Today's Total"
    TargetPeriod.Week -> "This Week's Total"
    TargetPeriod.Month -> "This Month's Total"
    TargetPeriod.RollingDays -> "Current ${rollingDays?.coerceAtLeast(1) ?: 1}-Day Total"
}

internal fun Habit.periodTotalDescription(): String = when (targetPeriod) {
    TargetPeriod.Occurrence, TargetPeriod.Day -> "today's total"
    TargetPeriod.Week -> "this week's total"
    TargetPeriod.Month -> "this month's total"
    TargetPeriod.RollingDays -> "current ${rollingDays?.coerceAtLeast(1) ?: 1}-day total"
}

internal fun Habit.periodTotalAmountLabel(customUnits: List<UnitDefinition> = emptyList()): String {
    val unit = unitSymbol(customUnits)
    return buildString {
        append(periodTotalTitle())
        if (unit.isNotBlank()) append(" ($unit)")
    }
}

internal fun Habit.pastCheckInActionLabel(): String = when (trackingMode) {
    HabitTrackingMode.CheckOff -> "Check In for an Earlier Day"
    HabitTrackingMode.Checklist -> "Complete an Earlier Day"
    HabitTrackingMode.Rating -> "Rate an Earlier Day"
    HabitTrackingMode.LogOnly -> "Add an Earlier Entry"
    else -> "Log an Earlier Day"
}

internal fun Habit.historyDialogTitle(editing: Boolean): String = if (editing) {
    when (trackingMode) {
        HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist -> "Edit Check-In"
        HabitTrackingMode.Rating -> "Edit Rating"
        HabitTrackingMode.LogOnly -> "Edit Entry"
        else -> "Edit Logged Progress"
    }
} else {
    when (trackingMode) {
        HabitTrackingMode.CheckOff -> "Record Past Check-In"
        HabitTrackingMode.Checklist -> "Record Past Completion"
        HabitTrackingMode.Rating -> "Rate an Earlier Day"
        HabitTrackingMode.LogOnly -> "Add an Earlier Entry"
        else -> "Log an Earlier Day"
    }
}

internal fun Habit.historyAmountLabel(unitId: String = this.unitId, optional: Boolean = false, customUnits: List<UnitDefinition> = emptyList()): String {
    val base = when (trackingMode) {
        HabitTrackingMode.Duration -> "Duration"
        HabitTrackingMode.Rating -> "Rating"
        HabitTrackingMode.LogOnly -> "Number"
        else -> "Amount"
    }
    val unit = habitUnitSymbol(unitId, customUnits)
    return buildString {
        append(base)
        if (unit.isNotBlank()) append(" ($unit)")
        if (optional) append(" · optional")
    }
}

internal fun HabitLog.isUserEditable(): Boolean = id > 0L && sourceType == MeasurementSourceType.Manual

internal fun HabitLog.activityTitle(habit: Habit, customUnits: List<UnitDefinition> = emptyList()): String {
    if (status == HabitLogStatus.Recorded && value == null) return if (note.isNotBlank()) "Added a note" else "Added an entry"
    if (status == HabitLogStatus.Failed && value == null) return "Below target"
    val amount = value?.let { raw ->
        "${formatHabitValue(raw, habit.precision)} ${habitUnitSymbol(enteredUnitId ?: habit.unitId, customUnits)}".trim()
    }
    if (habit.trackingMode in setOf(HabitTrackingMode.CheckOff, HabitTrackingMode.Checklist) &&
        value != null && value !in setOf(0.0, 1.0)
    ) return "Logged $amount"
    return when (habit.trackingMode) {
        HabitTrackingMode.CheckOff -> if (status == HabitLogStatus.Failed) "Not completed" else "Checked in"
        HabitTrackingMode.Checklist -> if (status == HabitLogStatus.Failed) "Not completed" else "Marked complete"
        HabitTrackingMode.Rating -> amount?.let { "Rated $it" } ?: status.activityLabel()
        HabitTrackingMode.LogOnly -> amount?.let { "Logged $it" }
            ?: if (note.isNotBlank()) "Added a note" else "Added an entry"
        HabitTrackingMode.Count, HabitTrackingMode.Decimal, HabitTrackingMode.Duration ->
            amount?.let { "Logged $it" } ?: status.activityLabel()
    }
}

internal fun HabitLog.activitySupportingText(today: LocalDate, locale: Locale = Locale.getDefault()): String = buildList {
    add(localDate.relativeActivityDate(today, locale))
    if (status == HabitLogStatus.Failed && value != null) add("Below target")
    note.takeIf(String::isNotBlank)?.let(::add)
    sourceType.activityAttribution()?.let(::add)
}.joinToString(" · ")

internal fun LocalDate.relativeActivityDate(today: LocalDate, locale: Locale = Locale.getDefault()): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}

@Composable
internal fun HabitPauseDialog(
    today: LocalDate,
    pause: HabitPause? = null,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?, String) -> Unit,
    onDelete: (() -> Unit)? = null,
    saving: Boolean = false,
    persistenceError: String? = null,
) {
    val editorKey = "habit-pause-${pause?.id ?: "new"}"
    val openingStart by rememberSaveable(editorKey) { mutableStateOf(pause?.startDate ?: today) }
    val openingEnd by rememberSaveable(editorKey) { mutableStateOf(pause?.endDate ?: today.takeIf { pause == null }) }
    val openingNote by rememberSaveable(editorKey) { mutableStateOf(pause?.note.orEmpty()) }
    var start by rememberSaveable(editorKey) { mutableStateOf(openingStart) }
    var end by rememberSaveable(editorKey) { mutableStateOf(openingEnd) }
    var note by rememberSaveable(editorKey) { mutableStateOf(openingNote) }
    var pickingStart by rememberSaveable(editorKey) { mutableStateOf(false) }
    var pickingEnd by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(editorKey) { mutableStateOf(false) }
    fun requestDismiss() {
        if (saving) return
        if (start != openingStart || end != openingEnd || note != openingNote) confirmDiscard = true else onDismiss()
    }
    val validRange = end == null || !requireNotNull(end).isBefore(start)
    PaneAwareAlertDialog(
        testTag = "habit-pause-dialog",
        onDismissRequest = ::requestDismiss,
        title = { Text(if (pause == null) "Schedule Habit Pause" else "Edit Scheduled Pause") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersistenceFailureNotice(persistenceError, testTag = "habit-pause-save-problem")
                Text(
                    "Scheduled pauses exclude these dates from check-ins, streak misses, and reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WhipOutlinedButton(
                    enabled = !saving,
                    onClick = { pickingStart = true },
                    modifier = Modifier.fillMaxWidth().testTag("habit-pause-start"),
                ) {
                    Text("Start · ${start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                }
                end?.let { selectedEnd ->
                    WhipOutlinedButton(
                        enabled = !saving,
                        onClick = { pickingEnd = true },
                        modifier = Modifier.fillMaxWidth().testTag("habit-pause-end"),
                    ) {
                        Text("End · ${selectedEnd.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                    }
                    WhipTextButton(
                        enabled = !saving,
                        onClick = { end = null },
                        modifier = Modifier.testTag("habit-pause-no-end"),
                    ) { Text("No End Date") }
                } ?: run {
                    Text(
                        "No end date · this pause continues until you edit or delete it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WhipOutlinedButton(
                        enabled = !saving,
                        onClick = {
                            end = start
                            pickingEnd = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("habit-pause-set-end"),
                    ) { Text("Set End Date") }
                }
                if (!validRange) Text(
                    "End date cannot be before the start date.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!start.isAfter(today)) Text(
                    "This range includes today or earlier. Saving can recalculate streak and consistency; completed check-ins and skipped days stay recorded.",
                    color = MaterialTheme.whipColors.warning,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("habit-pause-history-impact"),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    enabled = !saving,
                    label = { Text("Reason or note (optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("habit-pause-note"),
                )
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = validRange && !saving,
                onClick = { onSave(start, end, note) },
                modifier = Modifier.testTag("habit-pause-save"),
            ) { Text(if (saving) "Saving…" else if (pause == null) "Schedule Pause" else "Save Changes") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) WhipDestructiveTextButton(
                    enabled = !saving,
                    onClick = { confirmDelete = true },
                    modifier = Modifier.testTag("habit-pause-delete"),
                ) { Text("Delete") }
                WhipTextButton(enabled = !saving, onClick = ::requestDismiss) { Text("Cancel") }
            }
        },
        inputBlocked = saving,
        inputBlockedLabel = if (pause == null) "Scheduling Habit Pause" else "Saving Scheduled Pause",
    )
    if (pickingStart && !saving) WhipDatePickerDialog(
        start,
        { pickingStart = false },
        { selected ->
            start = selected
            if (end?.isBefore(selected) == true) end = selected
            pickingStart = false
        },
    )
    if (pickingEnd && !saving) WhipDatePickerDialog(
        end ?: start,
        { pickingEnd = false },
        { selected ->
            end = selected
            pickingEnd = false
        },
    )
    if (confirmDelete && onDelete != null && !saving) PaneAwareAlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete Scheduled Pause?") },
        text = {
            Text(
                "These dates will return to the Habit schedule. Completed check-ins and skipped days stay recorded, but unlogged past dates may become missed and streak or consistency may recalculate.",
            )
        },
        confirmButton = {
            WhipDestructiveTextButton(
                onClick = {
                    confirmDelete = false
                    onDelete()
                },
                modifier = Modifier.testTag("habit-pause-confirm-delete"),
            ) { Text("Delete") }
        },
        dismissButton = { WhipTextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
    if (confirmDiscard && !saving) UnsavedChangesDialog(
        subject = "scheduled Habit pause",
        onKeepEditing = { confirmDiscard = false },
        onDiscard = { confirmDiscard = false; onDismiss() },
    )
}

private fun HabitPause.displayDateRange(locale: Locale = Locale.getDefault()): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return endDate?.let { "${startDate.format(formatter)} – ${it.format(formatter)}" }
        ?: "From ${startDate.format(formatter)} · no end date"
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
    validationError: String? = null,
) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, enabled = enabled,
        isError = validationError != null,
        supportingText = validationError?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth().semantics { validationError?.let { error(it) } })
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

private fun habitReminderSummary(
    defaultTimes: List<Int>,
    weekdayTimes: Map<DayOfWeek, List<Int>>,
    weekdayFormatter: WhipWeekdayFormatter,
    formatTime: (Int) -> String,
): String {
    val configuredWeekdays = weekdayTimes.entries
        .filter { (_, times) -> times.isNotEmpty() }
        .sortedBy { (day, _) -> day.value }
        .map { (day, _) -> weekdayFormatter.label(day, WhipWeekdayLabelWidth.Short) }
    if (defaultTimes.isEmpty() && configuredWeekdays.isEmpty()) return "Off — no reminders configured"

    return buildList {
        if (defaultTimes.isNotEmpty()) {
            val visibleTimes = defaultTimes.take(3).joinToString(transform = formatTime)
            val remainingCount = defaultTimes.size - 3
            add(
                if (remainingCount > 0) "Default: $visibleTimes +$remainingCount more"
                else "Default: $visibleTimes",
            )
        }
        if (configuredWeekdays.isNotEmpty()) {
            add("Weekday overrides: ${configuredWeekdays.joinToString()}")
        }
    }.joinToString(" · ")
}

private fun habitEndSummary(
    endType: HabitEndType,
    endDate: LocalDate?,
    endValue: Double?,
): String = when (endType) {
    HabitEndType.Never -> "No end condition"
    HabitEndType.OnDate -> endDate?.let {
        "Ends ${it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}"
    } ?: "Ends on a date — choose the date"
    HabitEndType.AfterStreak -> "Ends after ${endValue?.let(::editableNumericValue) ?: "—"} consecutive completions"
    HabitEndType.AfterCompletions -> "Ends after ${endValue?.let(::editableNumericValue) ?: "—"} completions"
    HabitEndType.AfterTotal -> "Ends after a total of ${endValue?.let(::editableNumericValue) ?: "—"}"
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
