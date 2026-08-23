package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whip.app.R
import com.whip.app.core.OperationStatus
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope
import com.whip.app.domain.matches
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.unitSymbol
import com.whip.app.domain.periodBounds
import com.whip.app.domain.flexibleProgress
import com.whip.app.domain.hasEnded
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.valueForPeriod
import com.whip.app.core.AppSettings
import com.whip.app.core.HomeSection
import com.whip.app.core.SavedTaskFilter
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.HabitDayProgress
import com.whip.app.core.zoneId
import com.whip.app.core.WhipLaunchActions
import com.whip.app.data.TaskBulkEdit
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.whip.app.widget.WhipWidgetProvider

private enum class AppDestination {
    Home,
    Tasks,
    Habits,
    Goals,
    Gym,
    Settings,
}

@Composable
fun WhipApp(
    modifier: Modifier = Modifier,
    initialAction: String? = null,
    initialEntityId: Long? = null,
    initialOccurrenceEpochDay: Long? = null,
    initialSharedText: String? = null,
    initialDeliveryId: Long = 0L,
    foldInfo: WhipFoldInfo? = null,
    onRequestNotificationPermission: () -> Unit = {},
    taskViewModel: TaskViewModel = viewModel(),
    gymViewModel: GymViewModel = viewModel(),
    habitViewModel: HabitViewModel = viewModel(),
    goalViewModel: GoalViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val state by taskViewModel.uiState.collectAsStateWithLifecycle()
    val gymState by gymViewModel.uiState.collectAsStateWithLifecycle()
    val habitState by habitViewModel.uiState.collectAsStateWithLifecycle()
    val goalState by goalViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val operationStatus by taskViewModel.operationStatus.collectAsStateWithLifecycle()
    val taskDeletionImpact by taskViewModel.taskDeletionImpact.collectAsStateWithLifecycle()
    val pendingTaskUndoMessage by taskViewModel.pendingUndoMessage.collectAsStateWithLifecycle()
    val pendingQuickAddTaskId by taskViewModel.pendingQuickAddTaskId.collectAsStateWithLifecycle()
    val gymOperationStatus by gymViewModel.operationStatus.collectAsStateWithLifecycle()
    val pendingMachineArchiveUndo by gymViewModel.pendingMachineArchiveUndo.collectAsStateWithLifecycle()
    val habitOperationStatus by habitViewModel.operationStatus.collectAsStateWithLifecycle()
    val goalOperationStatus by goalViewModel.operationStatus.collectAsStateWithLifecycle()
    val persistedAreaScope = AreaScope.fromStorageKey(settingsState.settings.activeAreaScope)
    var transientAreaScopeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var transientAreaScopeDelivery by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingAreaBadgeId by rememberSaveable { mutableStateOf<String?>(null) }
    val areaScope = transientAreaScopeKey?.let(AreaScope::fromStorageKey) ?: persistedAreaScope
    val requestedLaunchDeliveryId = initialDeliveryId.takeIf { it != 0L }
        ?: 1L.takeIf { initialAction != null }
        ?: 0L

    LaunchedEffect(settingsState.taxonomyLoaded, settingsState.settings.activeAreaScope, settingsState.areas) {
        if (!settingsState.taxonomyLoaded) return@LaunchedEffect
        val stored = AreaScope.fromStorageKey(settingsState.settings.activeAreaScope)
        // A freshly created Area is committed before Room's Area flow emits it.
        // Give that emission a chance to cancel/restart this effect so valid new
        // scopes are not mistaken for stale IDs.
        if (stored is AreaScope.One && settingsState.areas.none { it.id == stored.areaId }) {
            kotlinx.coroutines.delay(500)
        }
        stored.validFor(settingsState.areas).takeIf { it != stored }?.let(settingsViewModel::setAreaScope)
    }

    LaunchedEffect(requestedLaunchDeliveryId, initialAction, initialEntityId, state, habitState, goalState) {
        if (requestedLaunchDeliveryId == 0L || initialEntityId == null) return@LaunchedEffect
        if (transientAreaScopeDelivery == requestedLaunchDeliveryId) return@LaunchedEffect
        if (state.loading || habitState.loading || goalState.loading) return@LaunchedEffect
        val targetAreaId = when (initialAction) {
            WhipLaunchActions.ACTION_OPEN_TASK ->
                (state.inbox + state.today + state.upcoming + state.planning + state.anytime + state.completed + state.archived)
                    .firstOrNull { it.task.id == initialEntityId }?.task?.areaId
            WhipLaunchActions.ACTION_OPEN_HABIT ->
                (habitState.today + habitState.all).firstOrNull { it.habit.id == initialEntityId }?.habit?.areaId
                    ?: habitState.archived.firstOrNull { it.id == initialEntityId }?.areaId
            WhipLaunchActions.ACTION_OPEN_GOAL ->
                (goalState.active + goalState.completed + goalState.archived)
                    .firstOrNull { it.goal.id == initialEntityId }?.goal?.areaId
            else -> return@LaunchedEffect
        }
        if (!areaScope.matches(targetAreaId)) {
            transientAreaScopeKey = (
                targetAreaId?.let(AreaScope::One)
                    ?: settingsState.areas.firstOrNull { !it.archived }?.id?.let(AreaScope::One)
                    ?: AreaScope.All
                ).storageKey
        }
        transientAreaScopeDelivery = requestedLaunchDeliveryId
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val adaptiveLayout = selectWhipAdaptiveLayout(maxWidth.value.toInt(), maxHeight.value.toInt(), foldInfo)
        CompositionLocalProvider(
            LocalAreaUiContext provides AreaUiContext(
                areas = settingsState.areas,
                onSelectScope = { pendingAreaBadgeId = it },
            ),
        ) {
        WhipScreen(
            state = state.forArea(areaScope),
            unscopedTaskState = state,
            gymState = gymState,
            gymViewModel = gymViewModel,
            habitState = habitState.forArea(areaScope),
            unscopedHabitState = habitState,
            habitViewModel = habitViewModel,
            goalState = goalState.forArea(areaScope),
            unscopedGoalState = goalState,
            goalViewModel = goalViewModel,
            settingsState = settingsState,
            settingsViewModel = settingsViewModel,
            areaScope = areaScope,
            onSelectAreaScope = { selected ->
                transientAreaScopeKey = null
                settingsViewModel.setAreaScope(selected)
            },
            transientAreaScope = transientAreaScopeKey != null,
            onRestoreAreaScope = { transientAreaScopeKey = null },
            pendingAreaBadgeId = pendingAreaBadgeId,
            onAreaBadgeConsumed = { pendingAreaBadgeId = null },
            modifier = Modifier.fillMaxSize(),
            adaptiveLayout = adaptiveLayout,
            foldInfo = foldInfo,
            operationStatus = operationStatus,
            onOperationStatusConsumed = taskViewModel::consumeOperationStatus,
            taskUndoMessage = pendingTaskUndoMessage,
            quickAddedTaskId = pendingQuickAddTaskId,
            onTaskUndo = taskViewModel::undoLastTaskAction,
            onTaskUndoDismissed = taskViewModel::clearPendingUndo,
            gymOperationStatus = gymOperationStatus,
            onGymOperationStatusConsumed = gymViewModel::consumeOperationStatus,
            machineArchiveUndoAvailable = pendingMachineArchiveUndo != null,
            onMachineArchiveUndo = gymViewModel::undoLastMachineArchive,
            onMachineArchiveUndoDismissed = gymViewModel::clearPendingMachineArchiveUndo,
            habitOperationStatus = habitOperationStatus,
            onHabitOperationStatusConsumed = habitViewModel::consumeOperationStatus,
            goalOperationStatus = goalOperationStatus,
            onGoalOperationStatusConsumed = goalViewModel::consumeOperationStatus,
            onSaveTask = taskViewModel::saveTask,
            onQuickAddTask = taskViewModel::quickAddTask,
            onComplete = taskViewModel::complete,
            onSkip = taskViewModel::skip,
            onReschedule = taskViewModel::reschedule,
            onSetStepCompleted = taskViewModel::setStepCompleted,
            onPromoteStep = taskViewModel::promoteStep,
            onArchive = taskViewModel::archive,
            onRestore = taskViewModel::restore,
            onDeleteTaskPermanently = taskViewModel::deletePermanently,
            taskDeletionImpact = taskDeletionImpact,
            onPreviewTaskDeletion = taskViewModel::previewPermanentDeletion,
            onClearTaskDeletionPreview = taskViewModel::clearPermanentDeletionPreview,
            onReopen = taskViewModel::reopen,
            onReopenOccurrence = taskViewModel::reopenOccurrence,
            onResetOccurrence = taskViewModel::resetOccurrence,
            onSetTaskPinned = taskViewModel::setPinned,
            onBulkCompleteTasks = taskViewModel::completeAll,
            onBulkArchiveTasks = taskViewModel::archiveAll,
            onBulkRestoreTasks = taskViewModel::restoreAll,
            onBulkPinTasks = taskViewModel::pinAll,
            onBulkPostponeTasks = taskViewModel::postponeAll,
            onBulkEditTasks = taskViewModel::editAll,
            onReorderTasks = taskViewModel::reorder,
            onPlanMyDay = taskViewModel::planMyDay,
            onSetTaskInbox = taskViewModel::setInbox,
            onDuplicateTask = taskViewModel::duplicate,
            onRequestNotificationPermission = onRequestNotificationPermission,
            initialAction = initialAction,
            initialEntityId = initialEntityId,
            initialOccurrenceEpochDay = initialOccurrenceEpochDay,
            initialSharedText = initialSharedText,
            initialDeliveryId = initialDeliveryId,
        )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhipScreen(
    state: TaskUiState,
    unscopedTaskState: TaskUiState = state,
    modifier: Modifier = Modifier,
    gymState: GymUiState = GymUiState(),
    gymViewModel: GymViewModel? = null,
    habitState: HabitUiState = HabitUiState(),
    unscopedHabitState: HabitUiState = habitState,
    habitViewModel: HabitViewModel? = null,
    goalState: GoalUiState = GoalUiState(),
    unscopedGoalState: GoalUiState = goalState,
    goalViewModel: GoalViewModel? = null,
    settingsState: SettingsUiState = SettingsUiState(),
    settingsViewModel: SettingsViewModel? = null,
    areaScope: AreaScope = AreaScope.fromStorageKey(settingsState.settings.activeAreaScope),
    onSelectAreaScope: (AreaScope) -> Unit = { settingsViewModel?.setAreaScope(it) },
    transientAreaScope: Boolean = false,
    onRestoreAreaScope: () -> Unit = {},
    pendingAreaBadgeId: String? = null,
    onAreaBadgeConsumed: () -> Unit = {},
    operationStatus: OperationStatus = OperationStatus.Idle,
    onOperationStatusConsumed: () -> Unit = {},
    taskUndoMessage: String? = null,
    quickAddedTaskId: Long? = null,
    onTaskUndo: () -> Unit = {},
    onTaskUndoDismissed: () -> Unit = {},
    gymOperationStatus: OperationStatus = OperationStatus.Idle,
    onGymOperationStatusConsumed: () -> Unit = {},
    machineArchiveUndoAvailable: Boolean = false,
    onMachineArchiveUndo: () -> Unit = {},
    onMachineArchiveUndoDismissed: () -> Unit = {},
    habitOperationStatus: OperationStatus = OperationStatus.Idle,
    onHabitOperationStatusConsumed: () -> Unit = {},
    goalOperationStatus: OperationStatus = OperationStatus.Idle,
    onGoalOperationStatusConsumed: () -> Unit = {},
    onSaveTask: (Long?, TaskDraft, LocalDate?) -> Unit,
    onQuickAddTask: (String, LocalDate?, Boolean, String?) -> Unit = { _, _, _, _ -> },
    onComplete: (ScheduledTask) -> Unit,
    onSkip: (ScheduledTask) -> Unit,
    onReschedule: (ScheduledTask, LocalDate) -> Unit,
    onSetStepCompleted: (ScheduledTask, Long, Boolean) -> Unit = { _, _, _ -> },
    onPromoteStep: (ScheduledTask, Long) -> Unit = { _, _ -> },
    onArchive: (Long) -> Unit,
    onRestore: (Long) -> Unit = {},
    onDeleteTaskPermanently: (Long) -> Unit = {},
    taskDeletionImpact: com.whip.app.data.TaskDeletionImpact? = null,
    onPreviewTaskDeletion: (Long) -> Unit = {},
    onClearTaskDeletionPreview: () -> Unit = {},
    onReopen: (Long) -> Unit,
    onReopenOccurrence: (ScheduledTask) -> Unit = {},
    onResetOccurrence: (Long, LocalDate) -> Unit = { _, _ -> },
    onSetTaskPinned: (Long, Boolean) -> Unit = { _, _ -> },
    onBulkCompleteTasks: (List<ScheduledTask>) -> Unit = {},
    onBulkArchiveTasks: (List<ScheduledTask>) -> Unit = {},
    onBulkRestoreTasks: (List<ScheduledTask>) -> Unit = {},
    onBulkPinTasks: (List<ScheduledTask>, Boolean) -> Unit = { _, _ -> },
    onBulkPostponeTasks: (List<ScheduledTask>, LocalDate) -> Unit = { _, _ -> },
    onBulkEditTasks: (List<ScheduledTask>, TaskBulkEdit) -> Unit = { _, _ -> },
    onReorderTasks: (List<ScheduledTask>) -> Unit = {},
    onPlanMyDay: (List<ScheduledTask>, Int) -> Unit = { _, _ -> },
    onSetTaskInbox: (Long, Boolean) -> Unit = { _, _ -> },
    onDuplicateTask: (Long) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    initialAction: String? = null,
    initialEntityId: Long? = null,
    initialOccurrenceEpochDay: Long? = null,
    initialSharedText: String? = null,
    initialDeliveryId: Long = 0L,
    adaptiveLayout: WhipAdaptiveLayout = WhipAdaptiveLayout.Compact,
    foldInfo: WhipFoldInfo? = null,
) {
    var appDestination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var settingsCallerDestination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var taskDestination by rememberSaveable { mutableStateOf(TaskDestination.Today) }
    var taskPlanningViewRequest by rememberSaveable { mutableStateOf<TaskPlanningView?>(null) }
    var taskEditorOpen by rememberSaveable { mutableStateOf(false) }
    var taskEditorTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskEditorFromEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskEditorSavePending by rememberSaveable { mutableStateOf(false) }
    var taskEditorSaveStarted by rememberSaveable { mutableStateOf(false) }
    var taskEditorSaveAndNew by rememberSaveable { mutableStateOf(false) }
    var taskEditorCapture by rememberSaveable { mutableStateOf("") }
    var taskEditorInitialScheduleEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskEditorSessionId by rememberSaveable { mutableStateOf(0L) }
    var actionItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var completedItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var rescheduleItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCompleteItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var globalAddExpanded by rememberSaveable { mutableStateOf(false) }
    var secondaryAppActionsExpanded by rememberSaveable { mutableStateOf(false) }
    var globalAddPending by rememberSaveable { mutableStateOf(false) }
    var gymAddExpanded by rememberSaveable { mutableStateOf(false) }
    var createHabitRequested by rememberSaveable { mutableStateOf(false) }
    var createGoalRequested by rememberSaveable { mutableStateOf(false) }
    var createExerciseRequested by rememberSaveable { mutableStateOf(false) }
    var startWorkoutRequested by rememberSaveable { mutableStateOf(false) }
    var recordGoalIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchEntryContext by rememberSaveable { mutableStateOf(WhipSearchEntryContext.AllWhip) }
    var openHabitIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var editHabitIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var openGoalIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var editGoalIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var openGymSearchDomain by rememberSaveable { mutableStateOf<SearchDomain?>(null) }
    var openGymSearchId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reviewOpen by rememberSaveable { mutableStateOf(false) }
    var areaManagerOpen by rememberSaveable { mutableStateOf(false) }
    var areaMoveNotice by rememberSaveable { mutableStateOf<String?>(null) }
    var areaMoveRestoreScope by rememberSaveable { mutableStateOf<String?>(null) }
    var homeHabitValueItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var contentPaneExpanded by rememberSaveable { mutableStateOf(false) }
    var consumedLaunchDeliveryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val shortcutFocusRequester = remember { FocusRequester() }
    val searchInvokerFocusRequester = remember { FocusRequester() }
    var searchPreviouslyOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val allScheduledTasks = state.inbox + state.today + state.upcoming + state.planning + state.anytime + state.completed + state.archived
    val scheduledTaskByKey = allScheduledTasks.associateBy(ScheduledTask::stableKey)
    val actionItem = actionItemKey?.let(scheduledTaskByKey::get)
    val completedItem = completedItemKey?.let(scheduledTaskByKey::get)
    val rescheduleItem = rescheduleItemKey?.let(scheduledTaskByKey::get)
    val pendingCompleteItem = pendingCompleteItemKey?.let(scheduledTaskByKey::get)
    val deleteItem = deleteItemKey?.let(scheduledTaskByKey::get)
    val openGymSearchRequested = openGymSearchDomain?.let { domain ->
        openGymSearchId?.let { id -> WhipSearchResult(domain, id, "", "") }
    }
    fun openSettings() {
        if (appDestination != AppDestination.Settings) settingsCallerDestination = appDestination
        appDestination = AppDestination.Settings
    }
    fun closeSettings() {
        appDestination = settingsCallerDestination.takeUnless { it == AppDestination.Settings } ?: AppDestination.Home
    }
    BackHandler(enabled = appDestination != AppDestination.Home && !areaManagerOpen) {
        if (appDestination == AppDestination.Settings) closeSettings() else appDestination = AppDestination.Home
    }
    val homeHabitValueItem = homeHabitValueItemId?.let { id ->
        (habitState.today + habitState.all).firstOrNull { it.habit.id == id }
    }
    LaunchedEffect(appDestination, globalAddPending) {
        if (appDestination == AppDestination.Home && globalAddPending) {
            globalAddPending = false
            globalAddExpanded = true
        }
    }
    LaunchedEffect(shortcutFocusRequester) {
        shortcutFocusRequester.requestFocus()
    }
    LaunchedEffect(searchOpen) {
        if (searchPreviouslyOpen && !searchOpen) {
            val restored = runCatching { searchInvokerFocusRequester.requestFocus() }.getOrDefault(false)
            if (!restored) shortcutFocusRequester.requestFocus()
        }
        searchPreviouslyOpen = searchOpen
    }
    val editorTask = taskEditorTaskId?.let { id -> allScheduledTasks.firstOrNull { it.task.id == id }?.task }
    val editorRequest = if (taskEditorOpen && (taskEditorTaskId == null || editorTask != null)) {
        TaskEditorRequest(
            editorTask,
            taskEditorFromEpochDay?.let(LocalDate::ofEpochDay),
            initialCapture = taskEditorCapture,
            initialScheduleDate = taskEditorInitialScheduleEpochDay?.let(LocalDate::ofEpochDay),
            sessionId = taskEditorSessionId,
        )
    } else {
        null
    }
    fun openTaskEditor(item: ScheduledTask? = null, capture: String = "", scheduleDate: LocalDate? = null) {
        taskEditorOpen = true
        taskEditorTaskId = item?.task?.id
        taskEditorFromEpochDay = item?.originalDate
            ?.takeIf { item.task.scheduleKind == ScheduleKind.Recurring }
            ?.toEpochDay()
        taskEditorCapture = capture
        taskEditorInitialScheduleEpochDay = scheduleDate?.toEpochDay()
        taskEditorSessionId++
    }
    fun closeTaskEditor() {
        taskEditorOpen = false
        taskEditorTaskId = null
        taskEditorFromEpochDay = null
        taskEditorSavePending = false
        taskEditorSaveStarted = false
        taskEditorSaveAndNew = false
        taskEditorCapture = ""
        taskEditorInitialScheduleEpochDay = null
    }

    LaunchedEffect(operationStatus, taskEditorSavePending) {
        if (!taskEditorSavePending) return@LaunchedEffect
        when (operationStatus) {
            is OperationStatus.Running -> taskEditorSaveStarted = true
            is OperationStatus.Succeeded -> {
                if (!taskEditorSaveStarted) return@LaunchedEffect
                if (taskEditorSaveAndNew) {
                    taskEditorSavePending = false
                    taskEditorSaveStarted = false
                    taskEditorSaveAndNew = false
                    taskEditorCapture = ""
                    taskEditorSessionId++
                } else closeTaskEditor()
            }
            is OperationStatus.Failed -> {
                if (!taskEditorSaveStarted) return@LaunchedEffect
                taskEditorSavePending = false
                taskEditorSaveStarted = false
            }
            else -> Unit
        }
    }

    val launchDeliveryId = initialDeliveryId.takeIf { it != 0L }
        ?: 1L.takeIf { initialAction != null }
        ?: 0L
    LaunchedEffect(launchDeliveryId, state.loading, settingsState.settings.activeAreaScope) {
        if (launchDeliveryId == 0L || consumedLaunchDeliveryId == launchDeliveryId) return@LaunchedEffect
        when (initialAction) {
            WhipWidgetProvider.ACTION_ADD_TASK -> {
                openTaskEditor()
                consumedLaunchDeliveryId = launchDeliveryId
            }
            WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK -> {
                appDestination = AppDestination.Tasks
                openTaskEditor(capture = initialSharedText.orEmpty())
                consumedLaunchDeliveryId = launchDeliveryId
            }
            WhipWidgetProvider.ACTION_ADD_HABIT -> {
                appDestination = AppDestination.Habits
                createHabitRequested = true
                consumedLaunchDeliveryId = launchDeliveryId
            }
            WhipLaunchActions.ACTION_OPEN_TASK -> {
                val id = initialEntityId ?: return@LaunchedEffect
                val found = (state.inbox + state.today + state.upcoming + state.planning + state.anytime + state.completed + state.archived)
                    .firstOrNull {
                        it.task.id == id && (
                            initialOccurrenceEpochDay == null ||
                                it.originalDate?.toEpochDay() == initialOccurrenceEpochDay
                            )
                    } ?: return@LaunchedEffect
                appDestination = AppDestination.Tasks
                if (found in state.completed) completedItemKey = found.stableKey else actionItemKey = found.stableKey
                consumedLaunchDeliveryId = launchDeliveryId
            }
            WhipLaunchActions.ACTION_OPEN_HABIT -> {
                openHabitIdRequested = initialEntityId ?: return@LaunchedEffect
                appDestination = AppDestination.Habits
                consumedLaunchDeliveryId = launchDeliveryId
            }
            WhipLaunchActions.ACTION_OPEN_GOAL -> {
                openGoalIdRequested = initialEntityId ?: return@LaunchedEffect
                appDestination = AppDestination.Goals
                consumedLaunchDeliveryId = launchDeliveryId
            }
            WhipLaunchActions.ACTION_OPEN_GYM -> {
                appDestination = AppDestination.Gym
                consumedLaunchDeliveryId = launchDeliveryId
            }
        }
    }

    LaunchedEffect(operationStatus) {
        val message = when (operationStatus) {
            OperationStatus.Idle, is OperationStatus.Running -> null
            is OperationStatus.Succeeded -> operationStatus.message
            is OperationStatus.Failed -> operationStatus.message
        }
        if (message != null) {
            val quickAddedId = quickAddedTaskId
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = when {
                    operationStatus !is OperationStatus.Succeeded || taskUndoMessage == null -> null
                    quickAddedId != null -> "Edit"
                    else -> "Undo"
                },
                withDismissAction = taskUndoMessage != null,
            )
            onOperationStatusConsumed()
            if (taskUndoMessage != null) {
                when {
                    quickAddedId != null && result == SnackbarResult.ActionPerformed -> {
                        val item = allScheduledTasks.firstOrNull { it.task.id == quickAddedId }
                        if (item != null) openTaskEditor(item) else {
                            taskEditorOpen = true
                            taskEditorTaskId = quickAddedId
                            taskEditorFromEpochDay = null
                            taskEditorCapture = ""
                            taskEditorInitialScheduleEpochDay = null
                            taskEditorSessionId++
                        }
                        onTaskUndoDismissed()
                    }
                    quickAddedId == null && result == SnackbarResult.ActionPerformed -> onTaskUndo()
                    else -> onTaskUndoDismissed()
                }
            }
        }
    }

    LaunchedEffect(gymOperationStatus) {
        val message = when (gymOperationStatus) {
            OperationStatus.Idle, is OperationStatus.Running -> null
            is OperationStatus.Succeeded -> gymOperationStatus.message
            is OperationStatus.Failed -> gymOperationStatus.message
        }
        if (message != null) {
            if (message.startsWith("Timer ", ignoreCase = true)) {
                snackbarHostState.currentSnackbarData
                    ?.takeIf { it.visuals.message.startsWith("Timer ", ignoreCase = true) }
                    ?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo".takeIf {
                    gymOperationStatus is OperationStatus.Succeeded && machineArchiveUndoAvailable
                },
                withDismissAction = machineArchiveUndoAvailable,
                duration = SnackbarDuration.Short,
            )
            onGymOperationStatusConsumed()
            if (machineArchiveUndoAvailable) {
                if (result == SnackbarResult.ActionPerformed) onMachineArchiveUndo()
                else onMachineArchiveUndoDismissed()
            }
        }
    }
    LaunchedEffect(habitOperationStatus) {
        val message = when (habitOperationStatus) {
            OperationStatus.Idle, is OperationStatus.Running -> null
            is OperationStatus.Succeeded -> habitOperationStatus.message
            is OperationStatus.Failed -> habitOperationStatus.message
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onHabitOperationStatusConsumed()
        }
    }
    LaunchedEffect(goalOperationStatus) {
        val message = when (goalOperationStatus) {
            OperationStatus.Idle, is OperationStatus.Running -> null
            is OperationStatus.Succeeded -> goalOperationStatus.message
            is OperationStatus.Failed -> goalOperationStatus.message
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onGoalOperationStatusConsumed()
        }
    }

    LaunchedEffect(areaMoveNotice) {
        val message = areaMoveNotice ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(message, actionLabel = "Restore view", withDismissAction = true)
        if (result == SnackbarResult.ActionPerformed) {
            onSelectAreaScope(AreaScope.fromStorageKey(areaMoveRestoreScope))
        }
        areaMoveNotice = null
        areaMoveRestoreScope = null
    }

    LaunchedEffect(pendingAreaBadgeId) {
        val id = pendingAreaBadgeId ?: return@LaunchedEffect
        val area = settingsState.areas.firstOrNull { it.id == id && !it.archived }
        onAreaBadgeConsumed()
        if (area != null) {
            onSelectAreaScope(AreaScope.One(id))
            val result = snackbarHostState.showSnackbar("Showing ${area.name}", actionLabel = "Show all", withDismissAction = true)
            if (result == SnackbarResult.ActionPerformed) onSelectAreaScope(AreaScope.All)
        }
    }

    fun keepSavedItemVisible(areaId: String?) {
        val target = areaId?.let(AreaScope::One)
            ?: settingsState.areas.firstOrNull { !it.archived }?.id?.let(AreaScope::One)
            ?: AreaScope.All
        if (!areaScope.matches(areaId)) {
            areaMoveRestoreScope = areaScope.storageKey
            onSelectAreaScope(target)
            val label = areaId?.let { id -> settingsState.areas.firstOrNull { it.id == id }?.name }
                ?: settingsState.areas.firstOrNull { !it.archived }?.name
                ?: "Main"
            areaMoveNotice = "Switched to $label to keep the saved item visible"
        }
    }

    fun requestCompletion(item: ScheduledTask) {
        if (item.subtasks.any { !it.completed }) {
            pendingCompleteItemKey = item.stableKey
        } else {
            onComplete(item)
        }
    }

    var gymDestination by rememberSaveable { mutableStateOf(GymDestination.Workout) }
    var requestedWorkoutExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }

    val adaptiveSummary = AdaptiveSummary(
        date = state.currentDate,
        dueTasks = state.today.size,
        dueHabits = habitState.today.count { it.successful != true },
        activeGoals = goalState.active.size,
        activeWorkout = gymState.activeSession != null,
        taskContext = listOfNotNull(actionItem?.task?.title) + state.today.take(6).map { it.task.title },
        habitContext = habitState.today.take(6).map { item ->
            val target = item.habit.targetMax ?: item.habit.targetMin
            "${item.habit.name} · ${if (item.successful == true) "done" else target?.let {
                "${formatHabitValue(item.value, item.habit.precision)}/${formatHabitValue(it, item.habit.precision)}"
            } ?: "log"}"
        },
        goalContext = goalState.active.take(6).map { item -> "${item.goal.name} · ${((item.progress ?: 0.0) * 100).toInt()}%" },
        gymContextTitle = when (gymDestination) {
            GymDestination.Workout -> "Workout Context"
            GymDestination.History -> "Recent Workouts"
            GymDestination.Progress -> "Current Records"
            GymDestination.Routines -> "Routines"
            GymDestination.Exercises -> "Exercise Library"
            GymDestination.Machines -> "Machine Profiles"
            GymDestination.Categories -> "Exercise Categories"
            GymDestination.Tools -> "Workout Tools"
            GymDestination.Library -> "Gym Library"
        },
        gymContext = when (gymDestination) {
            GymDestination.Workout -> buildList {
                gymState.activeSession?.let { add(it.name.ifBlank { "Current Workout" }) }
                addAll(gymState.activeWorkoutExercises.take(5).map { it.exercise.name })
                if (gymState.activeSession == null) add("No Active Workout")
            }
            GymDestination.History -> gymState.history.sortedByDescending { it.localDate }.take(6)
                .map { it.name.ifBlank { "Workout · ${it.localDate}" } }
                .ifEmpty { listOf("Completed workouts will appear here") }
            GymDestination.Progress -> gymState.personalRecords.filter { it.current }.take(6).map { record ->
                val exerciseName = gymState.exercises.firstOrNull { it.id == record.exerciseId }?.name ?: "Exercise"
                "$exerciseName · ${record.type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2") }"
            }.ifEmpty {
                listOf(
                    quantityLabel(gymState.history.size, "completed workout"),
                    "Records appear after eligible completed sets",
                )
            }
            GymDestination.Routines -> gymState.routines.take(6).map { it.name }
                .ifEmpty { listOf("Create a routine to reuse a training plan") }
            GymDestination.Exercises -> gymState.exercises
                .sortedWith(compareByDescending<com.whip.app.domain.Exercise> { it.favorite }.thenBy { it.name })
                .take(6).map { it.name }
                .ifEmpty { listOf("Create your first reusable exercise") }
            GymDestination.Machines -> gymState.machines.take(6).map { it.displayName }
                .ifEmpty { listOf("Machine profiles preserve equipment setup") }
            GymDestination.Categories -> gymState.categories.take(6).map { it.name }
                .ifEmpty { listOf("Categories are optional exercise filters") }
            GymDestination.Tools -> listOf("1RM Calculator", "Plate Calculator")
            GymDestination.Library -> listOf(
                "${quantityLabel(gymState.exercises.size, "Exercise")} · ${quantityLabel(gymState.machines.size, "Machine")}",
                "${quantityLabel(gymState.routines.size, "Routine")} · ${quantityLabel(gymState.history.size, "Completed Workout")}",
            )
        },
    )
    val supportsPaneExpansion = adaptiveLayout in setOf(
        WhipAdaptiveLayout.ExpandedDashboard,
        WhipAdaptiveLayout.BookFold,
        WhipAdaptiveLayout.TabletopFold,
    )
    val contentPaneIsExpanded = supportsPaneExpansion && (
        contentPaneExpanded || appDestination == AppDestination.Settings
    )
    val useSecondaryAppActionsMenu =
        adaptiveLayout == WhipAdaptiveLayout.BookFold && !contentPaneIsExpanded
    LaunchedEffect(useSecondaryAppActionsMenu) {
        if (!useSecondaryAppActionsMenu) secondaryAppActionsExpanded = false
    }
    val layoutDirection = LocalLayoutDirection.current
    // Match AdaptiveNavigationFrame's normalized support-pane geometry exactly. A raw
    // folding-feature bound can be narrower than our 260 dp usable support pane, so
    // centering dialogs from the raw bound can otherwise place them across the hinge.
    val (dialogSupportExtent, dialogContentWidth) = with(LocalDensity.current) {
        val totalWidth = LocalWindowInfo.current.containerSize.width.toDp()
        foldInfo
            ?.takeIf {
                adaptiveLayout == WhipAdaptiveLayout.BookFold &&
                    it.orientation == WhipFoldOrientation.Vertical &&
                    !contentPaneIsExpanded
            }
            ?.let { fold ->
                val rawPaneWidth = fold.leftPx.toDp()
                val largestPaneWidth = (totalWidth - 300.dp).coerceAtLeast(260.dp)
                val paneWidth = rawPaneWidth.coerceIn(260.dp, largestPaneWidth)
                val hingeWidth = fold.widthPx.toDp().coerceAtLeast(1.dp)
                val supportExtent = paneWidth + hingeWidth
                supportExtent to (totalWidth - supportExtent).coerceAtLeast(1.dp)
            }
            ?: (0.dp to totalWidth)
    }
    val dialogPaneOffset = (dialogSupportExtent / 2).let { offset ->
        if (layoutDirection == LayoutDirection.Ltr) offset else -offset
    }
    val dialogPaneWidth = minOf(dialogContentWidth * 0.94f, 720.dp)
    val paneDialogModifier = Modifier
        .absoluteOffset(x = dialogPaneOffset)
        .width(dialogPaneWidth)
    val runningMessage = listOf(operationStatus, gymOperationStatus, habitOperationStatus, goalOperationStatus)
        .filterIsInstance<OperationStatus.Running>()
        .firstOrNull()
        ?.message
    var gymRoutineEditorOpen by rememberSaveable { mutableStateOf(false) }
    val addDescription = when (appDestination) {
        AppDestination.Home -> "Add task, habit, goal, exercise, or workout"
        AppDestination.Tasks -> "Add task"
        AppDestination.Habits -> "Add habit"
        AppDestination.Goals -> "Add goal"
        AppDestination.Gym -> "Add exercise or workout"
        AppDestination.Settings -> "Add"
    }
    val triggerAdd: () -> Unit = {
        when (appDestination) {
            AppDestination.Home -> globalAddExpanded = true
            AppDestination.Tasks -> openTaskEditor(
                scheduleDate = state.currentDate.takeIf { taskDestination == TaskDestination.Today },
            )
            AppDestination.Habits -> createHabitRequested = true
            AppDestination.Goals -> createGoalRequested = true
            AppDestination.Gym -> gymAddExpanded = true
            AppDestination.Settings -> Unit
        }
    }
    AdaptiveNavigationFrame(
        modifier = modifier
            .fillMaxSize()
            .semantics { if (areaManagerOpen || reviewOpen) hideFromAccessibility() }
            .focusRequester(shortcutFocusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.K -> {
                        searchEntryContext = WhipSearchEntryContext.AllWhip
                        searchOpen = true
                    }
                    Key.N -> when (appDestination) {
                        AppDestination.Home -> globalAddExpanded = true
                        AppDestination.Tasks -> openTaskEditor(
                            scheduleDate = state.currentDate.takeIf { taskDestination == TaskDestination.Today },
                        )
                        AppDestination.Habits -> createHabitRequested = true
                        AppDestination.Goals -> createGoalRequested = true
                        AppDestination.Gym -> gymAddExpanded = true
                        AppDestination.Settings -> {
                            appDestination = AppDestination.Home
                            globalAddPending = true
                        }
                    }
                    Key.One -> appDestination = AppDestination.Home
                    Key.Two -> appDestination = AppDestination.Tasks
                    Key.Three -> appDestination = AppDestination.Habits
                    Key.Four -> appDestination = AppDestination.Goals
                    Key.Five -> appDestination = AppDestination.Gym
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .focusTarget()
            .semantics { testTagsAsResourceId = true },
        layout = adaptiveLayout,
        foldInfo = foldInfo,
        selected = appDestination,
        summary = adaptiveSummary,
        contentExpanded = contentPaneIsExpanded,
        navigationEnabled = !gymRoutineEditorOpen,
        onGymContextSelected = { index ->
            appDestination = AppDestination.Gym
            if (gymDestination == GymDestination.Workout) {
                gymDestination = GymDestination.Workout
                requestedWorkoutExerciseId = if (gymState.activeSession != null && index > 0) {
                    gymState.activeWorkoutExercises.getOrNull(index - 1)?.workoutExercise?.id
                } else null
            }
            contentPaneExpanded = true
        },
        onSelect = {
            if (!gymRoutineEditorOpen) {
                if (it == AppDestination.Settings) openSettings() else appDestination = it
            }
        },
    ) { scaffoldModifier ->
      Scaffold(
        modifier = scaffoldModifier.fillMaxSize(),
        topBar = {
            if (!gymRoutineEditorOpen) TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (adaptiveLayout == WhipAdaptiveLayout.Compact) {
                            WhipBrandMark(modifier = Modifier.size(38.dp))
                        }
                        if (
                            appDestination == AppDestination.Gym &&
                            adaptiveLayout == WhipAdaptiveLayout.Compact
                        ) {
                            Column(Modifier.semantics { heading() }) {
                                Text(
                                    "Whip",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    appDestination.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (
                            adaptiveLayout != WhipAdaptiveLayout.Compact &&
                            appDestination == AppDestination.Gym
                        ) {
                            Text(
                                appDestination.label,
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (appDestination in setOf(AppDestination.Home, AppDestination.Tasks, AppDestination.Habits, AppDestination.Goals)) {
                            AreaScopeMenu(
                                scope = areaScope,
                                areas = settingsState.areas,
                                usage = settingsState.areaUsage,
                                onSelect = onSelectAreaScope,
                                onManage = { areaManagerOpen = true },
                                onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                                dialogModifier = paneDialogModifier,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    if (
                        adaptiveLayout != WhipAdaptiveLayout.Compact &&
                        appDestination != AppDestination.Settings &&
                        !(appDestination == AppDestination.Gym && gymDestination in libraryGymDestinations)
                    ) {
                        Box {
                            IconButton(
                                onClick = triggerAdd,
                                modifier = Modifier.size(52.dp).semantics { contentDescription = addDescription },
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                                    if (appDestination == AppDestination.Home || appDestination == AppDestination.Gym) {
                                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            DropdownMenu(expanded = globalAddExpanded && appDestination == AppDestination.Home, onDismissRequest = { globalAddExpanded = false }) {
                                DropdownMenuItem(text = { Text("Task") }, onClick = { openTaskEditor(scheduleDate = state.currentDate); globalAddExpanded = false })
                                DropdownMenuItem(text = { Text("Habit") }, onClick = { appDestination = AppDestination.Habits; createHabitRequested = true; globalAddExpanded = false })
                                DropdownMenuItem(text = { Text("Goal") }, onClick = { appDestination = AppDestination.Goals; createGoalRequested = true; globalAddExpanded = false })
                                DropdownMenuItem(text = { Text("Exercise") }, onClick = { appDestination = AppDestination.Gym; createExerciseRequested = true; globalAddExpanded = false })
                                DropdownMenuItem(text = { Text("Workout") }, onClick = { appDestination = AppDestination.Gym; startWorkoutRequested = true; globalAddExpanded = false })
                            }
                            DropdownMenu(expanded = gymAddExpanded && appDestination == AppDestination.Gym, onDismissRequest = { gymAddExpanded = false }) {
                                DropdownMenuItem(text = { Text("Exercise") }, onClick = { createExerciseRequested = true; gymAddExpanded = false })
                                DropdownMenuItem(text = { Text("Workout") }, onClick = { startWorkoutRequested = true; gymAddExpanded = false })
                            }
                        }
                    }
                    if (supportsPaneExpansion && !useSecondaryAppActionsMenu && appDestination != AppDestination.Settings) {
                        IconButton(
                            onClick = { contentPaneExpanded = !contentPaneExpanded },
                            modifier = Modifier
                                .size(52.dp)
                                .semantics {
                                    contentDescription = if (contentPaneIsExpanded) {
                                        "Restore split view"
                                    } else {
                                        "Expand content pane"
                                    }
                                },
                        ) {
                            Icon(
                                if (contentPaneIsExpanded) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    if (useSecondaryAppActionsMenu && appDestination in setOf(AppDestination.Home, AppDestination.Gym)) {
                        IconButton(
                            onClick = {
                                searchEntryContext = if (appDestination == AppDestination.Gym) gymDestination.searchEntryContext() else WhipSearchEntryContext.AllWhip
                                searchOpen = true
                            },
                            modifier = Modifier.focusRequester(searchInvokerFocusRequester).size(52.dp).semantics {
                                contentDescription = if (appDestination == AppDestination.Gym) "Search Gym" else "Search All Whip Data"
                            },
                        ) { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(28.dp)) }
                    }
                    if (useSecondaryAppActionsMenu) {
                        Box {
                            IconButton(
                                onClick = { secondaryAppActionsExpanded = true },
                                modifier = Modifier.size(52.dp).semantics { contentDescription = "More app actions" },
                            ) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                            DropdownMenu(
                                expanded = secondaryAppActionsExpanded,
                                onDismissRequest = { secondaryAppActionsExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Expand Content") },
                                    leadingIcon = { Icon(Icons.Outlined.Fullscreen, contentDescription = null) },
                                    onClick = {
                                        secondaryAppActionsExpanded = false
                                        contentPaneExpanded = true
                                    },
                                )
                                if (appDestination != AppDestination.Settings) {
                                    DropdownMenuItem(
                                        text = { Text("Open Settings") },
                                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                        onClick = {
                                            secondaryAppActionsExpanded = false
                                            openSettings()
                                        },
                                    )
                                }
                            }
                        }
                    } else if (appDestination == AppDestination.Home || appDestination == AppDestination.Gym) {
                        IconButton(
                            onClick = {
                                searchEntryContext = if (appDestination == AppDestination.Gym) {
                                    gymDestination.searchEntryContext()
                                } else WhipSearchEntryContext.AllWhip
                                searchOpen = true
                            },
                            modifier = Modifier.focusRequester(searchInvokerFocusRequester).size(52.dp).semantics {
                                contentDescription = if (appDestination == AppDestination.Gym) "Search Gym" else "Search All Whip Data"
                            },
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                    }
                    if (!useSecondaryAppActionsMenu || appDestination == AppDestination.Settings) {
                        IconButton(onClick = { if (appDestination == AppDestination.Settings) closeSettings() else openSettings() }, modifier = Modifier.size(52.dp).semantics { contentDescription = if (appDestination == AppDestination.Settings) "Close Settings" else "Open Settings" }) {
                            Icon(
                                if (appDestination == AppDestination.Settings) Icons.Outlined.Close else Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!gymRoutineEditorOpen && (adaptiveLayout == WhipAdaptiveLayout.Compact || contentPaneIsExpanded)) {
                NavigationBar {
                    AppDestination.entries.filter { it != AppDestination.Settings }.forEach { destination ->
                        WhipNavigationBarItem(
                            modifier = Modifier.semantics { contentDescription = "${destination.label} tab" },
                            selected = appDestination == destination,
                            onClick = { appDestination = destination },
                            icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(28.dp)) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(
                    bottom = if (gymState.activeSession != null && appDestination == AppDestination.Gym) 104.dp else 12.dp,
                ),
            ) { data ->
                if (quickAddedTaskId != null && data.visuals.actionLabel == "Edit") {
                    Snackbar(
                        action = {
                            WhipTextButton(onClick = data::performAction) { Text("Edit") }
                        },
                        dismissAction = {
                            WhipTextButton(
                                onClick = {
                                    onTaskUndo()
                                    data.dismiss()
                                },
                            ) { Text("Undo") }
                        },
                    ) { Text(data.visuals.message) }
                } else {
                    Snackbar(data)
                }
            }
        },
        floatingActionButton = {
            if (
                !gymRoutineEditorOpen && adaptiveLayout == WhipAdaptiveLayout.Compact &&
                appDestination != AppDestination.Settings &&
                !(appDestination == AppDestination.Gym && gymDestination in libraryGymDestinations)
            ) {
                Box {
                    FloatingActionButton(
                        onClick = triggerAdd,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.semantics { contentDescription = addDescription },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(30.dp))
                            if (appDestination == AppDestination.Home || appDestination == AppDestination.Gym) {
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    DropdownMenu(expanded = globalAddExpanded && appDestination == AppDestination.Home, onDismissRequest = { globalAddExpanded = false }) {
                        DropdownMenuItem(text = { Text("Task") }, onClick = { openTaskEditor(scheduleDate = state.currentDate); globalAddExpanded = false })
                        DropdownMenuItem(text = { Text("Habit") }, onClick = { appDestination = AppDestination.Habits; createHabitRequested = true; globalAddExpanded = false })
                        DropdownMenuItem(text = { Text("Goal") }, onClick = { appDestination = AppDestination.Goals; createGoalRequested = true; globalAddExpanded = false })
                        DropdownMenuItem(text = { Text("Exercise") }, onClick = { appDestination = AppDestination.Gym; createExerciseRequested = true; globalAddExpanded = false })
                        DropdownMenuItem(text = { Text("Workout") }, onClick = { appDestination = AppDestination.Gym; startWorkoutRequested = true; globalAddExpanded = false })
                    }
                    DropdownMenu(expanded = gymAddExpanded && appDestination == AppDestination.Gym, onDismissRequest = { gymAddExpanded = false }) {
                        DropdownMenuItem(text = { Text("Exercise") }, onClick = { createExerciseRequested = true; gymAddExpanded = false })
                        DropdownMenuItem(text = { Text("Workout") }, onClick = { startWorkoutRequested = true; gymAddExpanded = false })
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.fillMaxHeight().widthIn(max = 1000.dp)) {
                when (appDestination) {
            AppDestination.Home -> {
                HomeContent(
                    state = state,
                    habitState = habitState,
                    gymState = gymState,
                    goalState = goalState,
                    appSettings = settingsState.settings,
                    innerPadding = innerPadding,
                    onQuickHabit = { item ->
                        habitViewModel?.let { vm ->
                            quickHabitAction(item, vm) { appDestination = AppDestination.Habits }
                        }
                    },
                    onHabitValue = { item, value -> habitViewModel?.addValue(item, value) },
                    onSetHabitValue = { homeHabitValueItemId = it.habit.id },
                    onDecrementHabit = { habitViewModel?.decrementValue(it) },
                    onUndoHabit = { item ->
                        habitState.logs.asSequence()
                            .filter { it.habitId == item.habit.id && it.localDate in item.habit.periodBounds(item.date) }
                            .maxByOrNull(com.whip.app.domain.HabitLog::timestamp)
                            ?.let { habitViewModel?.undoLog(it.id, item.habit.id) }
                    },
                    canUndoHabit = { item ->
                        habitState.logs.any { it.habitId == item.habit.id && it.localDate in item.habit.periodBounds(item.date) }
                    },
                    onChecklist = { habitId, itemId, date, completed ->
                        habitViewModel?.toggleChecklist(habitId, itemId, date, completed)
                    },
                    onOpenHabits = { appDestination = AppDestination.Habits },
                    onOpenHabit = { item ->
                        openHabitIdRequested = item.habit.id
                        appDestination = AppDestination.Habits
                    },
                    onEditHabit = { item ->
                        editHabitIdRequested = item.habit.id
                        appDestination = AppDestination.Habits
                    },
                    onOpenTasks = { appDestination = AppDestination.Tasks; taskDestination = TaskDestination.Today },
                    onCompleteTask = ::requestCompletion,
                    onOpenTask = { actionItemKey = it.stableKey },
                    onEditTask = ::openTaskEditor,
                    onOpenGym = { appDestination = AppDestination.Gym },
                    onStartRoutine = { routineId, dayId -> gymViewModel?.startRoutine(routineId, dayId) },
                    onOpenGoals = { appDestination = AppDestination.Goals },
                    onOpenGoal = { projection ->
                        openGoalIdRequested = projection.goal.id
                        appDestination = AppDestination.Goals
                    },
                    onEditGoal = { projection ->
                        editGoalIdRequested = projection.goal.id
                        appDestination = AppDestination.Goals
                    },
                    onRecordGoal = { projection ->
                        recordGoalIdRequested = projection.goal.id
                        appDestination = AppDestination.Goals
                    },
                    onToggleMilestone = { milestoneId, completed ->
                        goalViewModel?.toggleMilestone(milestoneId, completed)
                    },
                    onSelectHomeTaskFilter = { name ->
                        settingsViewModel?.selectHomeTaskFilter(name)
                        settingsState.settings.savedTaskFilters.firstOrNull { it.name == name }?.let { filter ->
                            filter.areaId?.let { onSelectAreaScope(AreaScope.One(it)) }
                        }
                    },
                    onOpenReview = { reviewOpen = true },
                    showFullHeader = adaptiveLayout == WhipAdaptiveLayout.Compact || contentPaneIsExpanded,
                    areaScopeLabel = when (areaScope) {
                        AreaScope.All -> null
                        AreaScope.Unassigned -> "Main"
                        is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name
                    },
                    onShowAllAreas = { onSelectAreaScope(AreaScope.All) },
                )
            }
            AppDestination.Tasks -> {
                TaskAreaContent(
                    state = state,
                    destination = taskDestination,
                    innerPadding = innerPadding,
                    onDestinationChange = { taskDestination = it },
                    onCompleteTask = ::requestCompletion,
                    onOpenTask = { actionItemKey = it.stableKey },
                    onEditTask = ::openTaskEditor,
                    onOpenCompleted = { completedItemKey = it.stableKey },
                    appSettings = settingsState.settings,
                    habitState = habitState,
                    onSaveFilter = { settingsViewModel?.saveTaskFilter(it) },
                    onDeleteFilter = { settingsViewModel?.deleteTaskFilter(it) },
                    onBulkComplete = onBulkCompleteTasks,
                    onBulkArchive = onBulkArchiveTasks,
                    onBulkRestore = onBulkRestoreTasks,
                    onBulkPin = onBulkPinTasks,
                    onBulkPostpone = onBulkPostponeTasks,
                    onBulkEdit = onBulkEditTasks,
                    onSetHabitPlanningOverlay = { enabled -> settingsViewModel?.update { it.copy(showHabitsInTaskPlanning = enabled) } },
                    onOpenPlanningHabit = { habitId -> openHabitIdRequested = habitId; appDestination = AppDestination.Habits },
                    onReorder = onReorderTasks,
                    onPlanMyDay = onPlanMyDay,
                    onBulkTriage = { items -> items.map { it.task.id }.distinct().forEach { id -> onSetTaskInbox(id, false) } },
                    onStopFocus = { settingsViewModel?.stopFocusTimer() },
                    onQuickCapture = onQuickAddTask,
                    onOpenSearch = {
                        searchEntryContext = WhipSearchEntryContext.Tasks
                        searchOpen = true
                    },
                    searchActionModifier = Modifier.focusRequester(searchInvokerFocusRequester),
                    operationStatus = operationStatus,
                    areas = settingsState.areas,
                    areaScope = areaScope,
                    onSelectAreaScope = onSelectAreaScope,
                    planningViewRequest = taskPlanningViewRequest,
                    onPlanningViewRequestConsumed = { taskPlanningViewRequest = null },
                    dialogModifier = paneDialogModifier,
                )
            }
            AppDestination.Habits -> {
                if (habitViewModel != null) {
                    HabitAreaContent(
                        state = habitState,
                        innerPadding = innerPadding,
                        viewModel = habitViewModel,
                        goalState = goalState,
                        createRequested = createHabitRequested,
                        onCreateRequestConsumed = { createHabitRequested = false },
                        openHabitIdRequest = openHabitIdRequested,
                        onOpenHabitRequestConsumed = { openHabitIdRequested = null },
                        editHabitIdRequest = editHabitIdRequested,
                        onEditHabitRequestConsumed = { editHabitIdRequested = null },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        lowPressureMode = settingsState.settings.lowPressureMode,
                        operationStatus = habitOperationStatus,
                        dialogModifier = paneDialogModifier,
                        onOpenTask = { taskId ->
                            appDestination = AppDestination.Tasks
                            allScheduledTasks.firstOrNull { it.task.id == taskId }
                                ?.let { actionItemKey = it.stableKey }
                        },
                        areas = settingsState.areas,
                        defaultAreaId = (areaScope as? AreaScope.One)?.areaId
                            ?: settingsState.areas.firstOrNull { !it.archived }?.id,
                        onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                        onCreateCustomUnit = { name, symbol, dimension, factor, result ->
                            settingsViewModel?.createCustomUnit(name, symbol, dimension, factor, result)
                                ?: result(Result.failure(IllegalStateException("Settings are unavailable")))
                        },
                        areaScopeLabel = when (areaScope) {
                            AreaScope.All -> null
                            AreaScope.Unassigned -> "Main"
                            is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name
                        },
                        onAreaChanged = ::keepSavedItemVisible,
                        onOpenSearch = {
                            searchEntryContext = WhipSearchEntryContext.Habits
                            searchOpen = true
                        },
                        searchActionModifier = Modifier.focusRequester(searchInvokerFocusRequester),
                    )
                } else RoadmapEmptyArea("Habits", "Habits are loading.", innerPadding)
            }
            AppDestination.Gym -> {
                if (gymViewModel != null) {
                    GymAreaContent(
                        state = gymState,
                        innerPadding = innerPadding,
                        viewModel = gymViewModel,
                        createExerciseRequested = createExerciseRequested,
                        startWorkoutRequested = startWorkoutRequested,
                        onExternalRequestConsumed = { createExerciseRequested = false; startWorkoutRequested = false },
                        openSearchRequest = openGymSearchRequested,
                        onOpenSearchRequestConsumed = { openGymSearchDomain = null; openGymSearchId = null },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenBackupSettings = ::openSettings,
                        dialogModifier = paneDialogModifier,
                        onRoutineEditorStateChange = { gymRoutineEditorOpen = it },
                        operationStatus = gymOperationStatus,
                        initialDestination = gymDestination,
                        onDestinationChange = { gymDestination = it },
                        requestedWorkoutExerciseId = requestedWorkoutExerciseId,
                        onRequestedWorkoutExerciseConsumed = { requestedWorkoutExerciseId = null },
                    )
                } else {
                    RoadmapEmptyArea("Gym", "Gym is loading.", innerPadding)
                }
            }
            AppDestination.Goals -> {
                if (goalViewModel != null) GoalAreaContent(
                    state = goalState,
                    innerPadding = innerPadding,
                    viewModel = goalViewModel,
                    createRequested = createGoalRequested,
                    recordGoalIdRequest = recordGoalIdRequested,
                    onExternalRequestConsumed = { createGoalRequested = false; recordGoalIdRequested = null },
                    openGoalIdRequest = openGoalIdRequested,
                    onOpenGoalRequestConsumed = { openGoalIdRequested = null },
                    editGoalIdRequest = editGoalIdRequested,
                    onEditGoalRequestConsumed = { editGoalIdRequested = null },
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    operationStatus = goalOperationStatus,
                    dialogModifier = paneDialogModifier,
                    areas = settingsState.areas,
                    defaultAreaId = (areaScope as? AreaScope.One)?.areaId
                        ?: settingsState.areas.firstOrNull { !it.archived }?.id,
                    onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                    onCreateCustomUnit = { name, symbol, dimension, factor, result ->
                        settingsViewModel?.createCustomUnit(name, symbol, dimension, factor, result)
                            ?: result(Result.failure(IllegalStateException("Settings are unavailable")))
                    },
                    areaScopeLabel = when (areaScope) {
                        AreaScope.All -> null
                        AreaScope.Unassigned -> "Main"
                        is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name
                    },
                    onAreaChanged = ::keepSavedItemVisible,
                    onOpenSearch = {
                        searchEntryContext = WhipSearchEntryContext.Goals
                        searchOpen = true
                    },
                    searchActionModifier = Modifier.focusRequester(searchInvokerFocusRequester),
                )
                else RoadmapEmptyArea("Goals", "Goals are loading.", innerPadding)
            }
            AppDestination.Settings -> {
                if (settingsViewModel != null) SettingsContent(
                    settingsState,
                    innerPadding,
                    settingsViewModel,
                    onEditAreas = { areaManagerOpen = true },
                )
                else RoadmapEmptyArea("Settings", "Settings are loading.", innerPadding)
            }
                }
            }
            if (transientAreaScope) {
                val temporaryLabel = when (areaScope) {
                    AreaScope.All -> "All Areas"
                    AreaScope.Unassigned -> "Main"
                    is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name ?: "this area"
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(innerPadding).padding(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Temporarily showing $temporaryLabel", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        WhipTextButton(onClick = onRestoreAreaScope) { Text("Restore") }
                    }
                }
            }
            runningMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(innerPadding)
                        .padding(8.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = message
                        },
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(Modifier.width(56.dp))
                        Text(message)
                    }
                }
            }
        }
      }
    }

    if (areaManagerOpen && settingsViewModel != null) {
        AreaManagementDialog(
            state = settingsState,
            viewModel = settingsViewModel,
            paneMaxWidth = dialogPaneWidth,
            onDismiss = { areaManagerOpen = false },
        )
    }

    if (searchOpen) {
        UnifiedSearchDialog(
            unscopedTaskState,
            unscopedHabitState,
            unscopedGoalState,
            gymState,
            { searchOpen = false },
            dialogModifier = paneDialogModifier,
            areaScope = areaScope,
            areaScopeLabel = when (val scope = areaScope) {
                AreaScope.All -> null
                AreaScope.Unassigned -> "Main"
                is AreaScope.One -> settingsState.areas.firstOrNull { it.id == scope.areaId }?.name ?: "All Areas"
            },
            onSearchAllAreas = { onSelectAreaScope(AreaScope.All) },
            initialScope = searchEntryContext.defaultSearchScope(),
        ) { result ->
            if (result.domain in setOf(SearchDomain.Task, SearchDomain.Habit, SearchDomain.Goal) && !areaScope.matches(result.areaId)) {
                onSelectAreaScope(
                    result.areaId?.let(AreaScope::One)
                        ?: settingsState.areas.firstOrNull { !it.archived }?.id?.let(AreaScope::One)
                        ?: AreaScope.All,
                )
            }
            when (result.domain) {
                SearchDomain.Task -> {
                    appDestination = AppDestination.Tasks
                    (unscopedTaskState.inbox + unscopedTaskState.today + unscopedTaskState.upcoming + unscopedTaskState.planning + unscopedTaskState.anytime + unscopedTaskState.completed + unscopedTaskState.archived)
                        .firstOrNull { it.task.id == result.id }
                        ?.let { found ->
                            if (found in unscopedTaskState.completed) completedItemKey = found.stableKey else {
                                if (found in unscopedTaskState.planning) taskDestination = TaskDestination.Upcoming
                                actionItemKey = found.stableKey
                            }
                        }
                }
                SearchDomain.Habit -> {
                    openHabitIdRequested = result.id
                    appDestination = AppDestination.Habits
                }
                SearchDomain.Goal -> {
                    openGoalIdRequested = result.id
                    appDestination = AppDestination.Goals
                }
                SearchDomain.Exercise, SearchDomain.Machine, SearchDomain.Workout, SearchDomain.Routine -> {
                    openGymSearchDomain = result.domain
                    openGymSearchId = result.id
                    appDestination = AppDestination.Gym
                }
            }
            searchOpen = false
        }
    }

    if (reviewOpen) {
        ReviewDialog(
            state,
            habitState,
            goalState,
            gymState,
            settingsState.settings.reviewPeriod,
            settingsState.settings.zoneId(),
            { period -> settingsViewModel?.update { it.copy(reviewPeriod = period) } },
            { reviewOpen = false },
            sections = settingsState.settings.reviewSections,
            savedFilters = settingsState.settings.savedReviewFilters,
            selectedFilterName = settingsState.settings.selectedReviewFilterName,
            onSectionsChange = { settingsViewModel?.setReviewSections(it) },
            onSaveFilter = { settingsViewModel?.saveReviewFilter(it) },
            onSelectFilter = { settingsViewModel?.selectReviewFilter(it) },
            onDeleteFilter = { settingsViewModel?.deleteReviewFilter(it) },
            dialogModifier = paneDialogModifier,
            onDrillDown = { section ->
                reviewOpen = false
                appDestination = when (section) {
                    HomeSection.Tasks -> {
                        taskDestination = TaskDestination.Completed
                        AppDestination.Tasks
                    }
                    HomeSection.Habits -> AppDestination.Habits
                    HomeSection.Goals -> AppDestination.Goals
                    HomeSection.Gym -> AppDestination.Gym
                }
            },
            productivityAreaLabel = when (areaScope) {
                AreaScope.All -> null
                AreaScope.Unassigned -> "Main"
                is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name ?: "Selected Area"
            },
        )
    }
    homeHabitValueItem?.let { item ->
        HabitValueDialog(
            item = item,
            onDismiss = { homeHabitValueItemId = null },
            onLog = { value, note ->
                habitViewModel?.setPeriodValue(item, value, note)
                homeHabitValueItemId = null
            },
        )
    }

    editorRequest?.let { request ->
        TaskEditorDialog(
            request = request,
            onDismiss = ::closeTaskEditor,
            onSave = { taskId, draft, fromOccurrence ->
                taskEditorSavePending = true
                taskEditorSaveStarted = false
                taskEditorSaveAndNew = false
                onSaveTask(taskId, draft, fromOccurrence)
                keepSavedItemVisible(draft.areaId)
            },
            onSaveAndNew = { taskId, draft, fromOccurrence ->
                taskEditorSavePending = true
                taskEditorSaveStarted = false
                taskEditorSaveAndNew = true
                onSaveTask(taskId, draft, fromOccurrence)
                keepSavedItemVisible(draft.areaId)
            },
            onRequestNotificationPermission = onRequestNotificationPermission,
            defaultRepeatStepPolicy = settingsState.settings.defaultTaskStepPolicy,
            firstDayOfWeek = settingsState.settings.firstDayOfWeek,
            today = state.currentDate,
            naturalLanguageCapture = settingsState.settings.naturalLanguageTaskCapture,
            powerMode = settingsState.settings.powerMode,
            areas = settingsState.areas,
            defaultAreaId = (areaScope as? AreaScope.One)?.areaId
                ?: settingsState.areas.firstOrNull { !it.archived }?.id,
            onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
            knownTags = (state.inbox + state.today + state.upcoming + state.planning + state.anytime + state.completed + state.archived)
                .flatMap { it.task.tags }.distinct().sorted(),
            paneOffsetX = dialogPaneOffset,
            paneMaxWidth = dialogPaneWidth,
            saving = taskEditorSavePending,
        )
    }

    actionItem?.let { item ->
        TaskActionsDialog(
            item = item,
            onDismiss = { actionItemKey = null },
            onComplete = {
                requestCompletion(item)
                actionItemKey = null
            },
            onEdit = {
                openTaskEditor(item)
                actionItemKey = null
            },
            onReschedule = {
                rescheduleItemKey = item.stableKey
                actionItemKey = null
            },
            onSkip = {
                onSkip(item)
                actionItemKey = null
            },
            onArchive = {
                if (item.task.archived) onRestore(item.task.id) else onArchive(item.task.id)
                actionItemKey = null
            },
            onDeletePermanently = {
                deleteItemKey = item.stableKey
                onPreviewTaskDeletion(item.task.id)
                actionItemKey = null
            },
            onPin = {
                onSetTaskPinned(item.task.id, !item.task.pinned)
                actionItemKey = null
            },
            onDuplicate = {
                onDuplicateTask(item.task.id)
                actionItemKey = null
            },
            onToggleInbox = {
                onSetTaskInbox(item.task.id, !item.task.inbox)
                actionItemKey = null
            },
            onStartFocus = { minutes ->
                settingsViewModel?.startFocusTimer(item.task.id, minutes)
                onRequestNotificationPermission()
                actionItemKey = null
            },
            onToggleSubtask = { stepId, checked ->
                onSetStepCompleted(item, stepId, checked)
                actionItemKey = null
            },
            onPromoteSubtask = { stepId ->
                onPromoteStep(item, stepId)
                actionItemKey = null
            },
            occurrenceHistory = state.occurrences.filter { it.taskId == item.task.id },
            onReopenOccurrence = { occurrence ->
                onReopenOccurrence(
                    ScheduledTask(
                        task = item.task,
                        originalDate = occurrence.originalDate,
                        scheduledDate = occurrence.scheduledDate,
                        completedAtMillis = occurrence.completedAtMillis,
                    ),
                )
                actionItemKey = null
            },
            onResetOccurrence = { occurrence ->
                onResetOccurrence(item.task.id, occurrence.originalDate)
                actionItemKey = null
            },
            dialogModifier = paneDialogModifier,
        )
    }

    pendingCompleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingCompleteItemKey = null },
            title = { Text("Complete With Unfinished Subtasks?") },
            text = {
                Text(
                    "${item.totalSubtasks - item.completedSubtasks} subtasks remain. " +
                        "The task will be completed, but the saved history keeps their progress.",
                )
            },
            confirmButton = {
                WhipTextButton(
                    onClick = {
                        onComplete(item)
                        pendingCompleteItemKey = null
                    },
                ) { Text("Complete Anyway") }
            },
            dismissButton = {
                WhipTextButton(onClick = { pendingCompleteItemKey = null }) { Text("Keep Working") }
            },
        )
    }

    completedItem?.let { item ->
        CompletedTaskDialog(
            item = item,
            onDismiss = { completedItemKey = null },
            onEdit = {
                openTaskEditor(item)
                completedItemKey = null
            },
            onReopen = {
                if (item.task.scheduleKind == ScheduleKind.Recurring) {
                    onReopenOccurrence(item)
                } else {
                    onReopen(item.task.id)
                }
                completedItemKey = null
            },
            onDeletePermanently = {
                deleteItemKey = item.stableKey
                onPreviewTaskDeletion(item.task.id)
                completedItemKey = null
            },
            occurrenceHistory = state.occurrences.filter { it.taskId == item.task.id },
            onReopenOccurrence = { occurrence ->
                onReopenOccurrence(
                    ScheduledTask(
                        task = item.task,
                        originalDate = occurrence.originalDate,
                        scheduledDate = occurrence.scheduledDate,
                        completedAtMillis = occurrence.completedAtMillis,
                    ),
                )
                completedItemKey = null
            },
            onResetOccurrence = { occurrence ->
                onResetOccurrence(item.task.id, occurrence.originalDate)
                completedItemKey = null
            },
            dialogModifier = paneDialogModifier,
        )
    }

    deleteItem?.let { item ->
        PermanentTaskDeleteDialog(
            item = item,
            impact = taskDeletionImpact,
            onDismiss = { deleteItemKey = null; onClearTaskDeletionPreview() },
            onConfirm = {
                onDeleteTaskPermanently(item.task.id)
                deleteItemKey = null
            },
        )
    }

    rescheduleItem?.let { item ->
        WhipDatePickerDialog(
            initialDate = item.scheduledDate ?: state.currentDate,
            onDismiss = { rescheduleItemKey = null },
            onDateSelected = { newDate ->
                onReschedule(item, newDate)
                rescheduleItemKey = null
            },
        )
    }
    if (!settingsState.settings.setupCompleted && settingsViewModel != null) {
        FirstRunSetupDialog(
            onComplete = { sections, power, pounds, lowPressure, notifications ->
                settingsViewModel.completeSetup(sections, power, pounds, lowPressure)
                if (notifications) onRequestNotificationPermission()
            },
            onUseDefaults = {
                settingsViewModel.completeSetup(
                    DEFAULT_FIRST_RUN_HOME_SECTIONS,
                    powerMode = false,
                    usePounds = false,
                    lowPressureMode = false,
                )
            },
        )
    }
}

@Composable
internal fun AreaScopeMenu(
    scope: AreaScope,
    areas: List<Area>,
    usage: Map<String, AreaUsageCounts> = emptyMap(),
    onSelect: (AreaScope) -> Unit,
    onManage: () -> Unit = {},
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    dialogModifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val activeAreas = areas.filterNot(Area::archived)
    val selectedArea = (scope as? AreaScope.One)?.let { selected -> areas.firstOrNull { it.id == selected.areaId } }
    val label = when (scope) {
        AreaScope.All -> activeAreas.singleOrNull()?.name ?: "All Areas"
        AreaScope.Unassigned -> activeAreas.firstOrNull()?.name ?: "Main"
        is AreaScope.One -> selectedArea?.name ?: "All Areas"
    }
    val displayLabel = if (activeAreas.isEmpty()) "Main" else label
    Box {
        WhipFilterChip(
            selected = scope != AreaScope.All,
            onClick = { expanded = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedArea?.colorArgb?.let { color ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(color)))
                    }
                    Text(displayLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 112.dp))
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier
                .widthIn(min = 48.dp)
                .semantics { contentDescription = "Area scope: $displayLabel" },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (activeAreas.isEmpty()) {
                DropdownMenuItem(
                    text = { Column { Text("Create Main Area"); Text("Every item belongs to an Area.", style = MaterialTheme.typography.bodySmall) } },
                    onClick = {
                        expanded = false
                        onCreateArea("Main", null) { result -> result.onSuccess { onSelect(AreaScope.One(it)) } }
                    },
                )
                if (areas.any(Area::archived)) {
                    DropdownMenuItem(
                        text = { Text("Manage Archived Areas…") },
                        onClick = { expanded = false; onManage() },
                    )
                }
                return@DropdownMenu
            }
            if (activeAreas.size > 1) {
                DropdownMenuItem(
                    text = { Text((if (scope == AreaScope.All) "✓  " else "") + "All Areas") },
                    onClick = { onSelect(AreaScope.All); expanded = false },
                )
            }
            if (activeAreas.size > 8) {
                DropdownMenuItem(
                    text = { OutlinedTextField(query, { query = it.take(40) }, label = { Text("Find area") }, singleLine = true) },
                    onClick = {},
                )
            }
            areas.filter { (!it.archived || it.id == selectedArea?.id) && (query.isBlank() || it.name.contains(query, true)) }.forEach { area ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            area.colorArgb?.let { color -> Box(Modifier.size(10.dp).clip(CircleShape).background(Color(color))) }
                            val selected = (scope as? AreaScope.One)?.areaId == area.id ||
                                (scope == AreaScope.All && activeAreas.size == 1)
                            Text((if (selected) "✓  " else "") + "${area.name} · ${(usage[area.id]?.total ?: 0).itemCount("item")}")
                            if (area.archived) Text("Archived", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick = { onSelect(AreaScope.One(area.id)); expanded = false },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Create Area…") },
                onClick = { expanded = false; creating = true },
            )
            DropdownMenuItem(
                text = { Text("Manage Areas") },
                onClick = { expanded = false; onManage() },
            )
        }
    }
    if (creating) {
        CreateAreaDialog(
            modifier = dialogModifier,
            existingAreas = areas,
            onDismiss = { creating = false },
            onCreate = onCreateArea,
            onSelected = { id, _ -> creating = false; onSelect(AreaScope.One(id)) },
        )
    }
}

private data class AdaptiveSummary(
    val date: LocalDate,
    val dueTasks: Int,
    val dueHabits: Int,
    val activeGoals: Int,
    val activeWorkout: Boolean,
    val taskContext: List<String> = emptyList(),
    val habitContext: List<String> = emptyList(),
    val goalContext: List<String> = emptyList(),
    val gymContextTitle: String = "Workout Context",
    val gymContext: List<String> = emptyList(),
)

@Composable
private fun AdaptiveNavigationFrame(
    modifier: Modifier,
    layout: WhipAdaptiveLayout,
    foldInfo: WhipFoldInfo?,
    selected: AppDestination,
    summary: AdaptiveSummary,
    contentExpanded: Boolean,
    navigationEnabled: Boolean = true,
    onGymContextSelected: (Int) -> Unit = {},
    onSelect: (AppDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val frameModifier = modifier.background(MaterialTheme.colorScheme.background)
    if (contentExpanded) {
        Box(frameModifier.testTag("expanded-content-pane")) {
            content(Modifier.fillMaxSize())
        }
        return
    }
    when (layout) {
        WhipAdaptiveLayout.Compact -> Box(frameModifier) { content(Modifier.fillMaxSize()) }

        WhipAdaptiveLayout.NavigationRail -> Row(frameModifier) {
            WhipNavigationRail(selected, onSelect, navigationEnabled)
            content(Modifier.weight(1f))
        }

        WhipAdaptiveLayout.ExpandedDashboard -> Row(frameModifier) {
            WhipNavigationRail(selected, onSelect, navigationEnabled)
            content(Modifier.weight(1f))
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .testTag("expanded-support-pane"),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                FoldContextPane(
                    summary,
                    selected,
                    onSelect,
                    onGymContextSelected = onGymContextSelected,
                    navigationEnabled = navigationEnabled,
                )
            }
        }

        WhipAdaptiveLayout.BookFold -> BoxWithConstraints(frameModifier) {
            val density = LocalDensity.current
            val rawPaneWidth = with(density) { (foldInfo?.leftPx ?: 0).toDp() }
            val rawHingeWidth = with(density) { (foldInfo?.widthPx ?: 0).toDp() }
            val largestPaneWidth = (maxWidth - 300.dp).coerceAtLeast(260.dp)
            val paneWidth = rawPaneWidth.coerceIn(260.dp, largestPaneWidth)
            val hingeWidth = rawHingeWidth.coerceAtLeast(1.dp)
            Row(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .width(paneWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .testTag("fold-support-pane"),
                ) {
                    WhipNavigationRail(selected, onSelect, navigationEnabled)
                    FoldContextPane(
                        summary,
                        selected,
                        onSelect,
                        Modifier.weight(1f),
                        onGymContextSelected = onGymContextSelected,
                        navigationEnabled = navigationEnabled,
                    )
                }
                Surface(
                    modifier = Modifier
                        .width(hingeWidth)
                        .fillMaxHeight()
                        .semantics { contentDescription = "Device hinge separator" },
                    color = MaterialTheme.colorScheme.outlineVariant,
                ) {}
                content(Modifier.weight(1f))
            }
        }

        WhipAdaptiveLayout.TabletopFold -> BoxWithConstraints(frameModifier) {
            val density = LocalDensity.current
            val rawTopHeight = with(density) { (foldInfo?.topPx ?: 0).toDp() }
            val rawHingeHeight = with(density) { (foldInfo?.heightPx ?: 0).toDp() }
            val largestTopHeight = (maxHeight - 280.dp).coerceAtLeast(180.dp)
            val topHeight = rawTopHeight.coerceIn(180.dp, largestTopHeight)
            val hingeHeight = rawHingeHeight.coerceAtLeast(1.dp)
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .height(topHeight)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .testTag("fold-support-pane"),
                ) {
                    TabletopNavigation(selected, onSelect, navigationEnabled)
                    FoldContextPane(
                        summary,
                        selected,
                        onSelect,
                        Modifier.weight(1f),
                        horizontal = true,
                        onGymContextSelected = onGymContextSelected,
                        navigationEnabled = navigationEnabled,
                    )
                }
                Surface(
                    modifier = Modifier
                        .height(hingeHeight)
                        .fillMaxWidth()
                        .semantics { contentDescription = "Device hinge separator" },
                    color = MaterialTheme.colorScheme.outlineVariant,
                ) {}
                content(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.WhipNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val itemColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .weight(1f)
            .heightIn(min = 64.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else itemColor,
        ) {
            Box(
                modifier = Modifier.size(width = 56.dp, height = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }
        Surface(color = Color.Transparent, contentColor = itemColor) {
            label()
        }
    }
}

@Composable
private fun WhipNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val itemColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .width(80.dp)
            .heightIn(min = 72.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else itemColor,
        ) {
            Box(
                modifier = Modifier.size(width = 56.dp, height = 36.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }
        Surface(color = Color.Transparent, contentColor = itemColor) {
            label()
        }
    }
}

@Composable
private fun WhipNavigationRail(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    enabled: Boolean = true,
) {
    val railColor = MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .testTag("adaptive-navigation-rail"),
        color = railColor,
    ) {
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = railColor,
            windowInsets = WindowInsets(0, 0, 0, 0),
            header = {
                WhipBrandMark(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(40.dp),
                    contentDescription = "Whip",
                )
            },
        ) {
            AppDestination.entries.filter { it != AppDestination.Settings }.forEach { destination ->
                WhipNavigationRailItem(
                    modifier = Modifier.semantics { contentDescription = "${destination.label} tab" },
                    selected = destination == selected,
                    enabled = enabled,
                    onClick = { onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(28.dp)) },
                    label = { Text(destination.label) },
                )
            }
        }
    }
}

@Composable
private fun WhipBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(R.drawable.ic_whip_mark),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

@Composable
private fun TabletopNavigation(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    enabled: Boolean = true,
) {
    NavigationBar(
        modifier = Modifier.testTag("adaptive-tabletop-navigation"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        AppDestination.entries.filter { it != AppDestination.Settings }.forEach { destination ->
            WhipNavigationBarItem(
                modifier = Modifier.semantics { contentDescription = "${destination.label} tab" },
                selected = destination == selected,
                enabled = enabled,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(28.dp)) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun FoldContextPane(
    summary: AdaptiveSummary,
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    onGymContextSelected: (Int) -> Unit = {},
    navigationEnabled: Boolean = true,
) {
    val cards = listOf(
        Triple(AppDestination.Tasks, "Tasks Due", summary.dueTasks.toString()),
        Triple(AppDestination.Habits, "Habits Remaining", summary.dueHabits.toString()),
        Triple(AppDestination.Goals, "Active Goals", summary.activeGoals.toString()),
        Triple(AppDestination.Gym, "Workout", if (summary.activeWorkout) "In progress" else "Ready"),
    )
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (selected == AppDestination.Home) "Today" else selected.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                summary.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val contextLines = when (selected) {
                AppDestination.Tasks -> summary.taskContext
                AppDestination.Habits -> summary.habitContext
                AppDestination.Goals -> summary.goalContext
                AppDestination.Gym -> summary.gymContext
                else -> emptyList()
            }
            if (selected == AppDestination.Gym) {
                if (contextLines.isNotEmpty()) {
                    Text(summary.gymContextTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        contextLines.forEachIndexed { index, line ->
                            Card(
                                Modifier.fillMaxWidth().clickable(onClickLabel = "Open $line") { onGymContextSelected(index) },
                            ) { Text(line, Modifier.padding(12.dp), maxLines = 3) }
                        }
                    }
                }
            } else if (selected != AppDestination.Home && contextLines.isNotEmpty()) {
                Text(
                    when (selected) {
                        AppDestination.Tasks -> "Today and selected task"
                        AppDestination.Habits -> "Current habit period"
                        AppDestination.Goals -> "Current goal trend"
                        AppDestination.Gym -> "Live workout"
                        else -> "Context"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                contextLines.forEach { line ->
                    Card(Modifier.fillMaxWidth()) { Text(line, Modifier.padding(12.dp), maxLines = 2) }
                }
            } else if (selected == AppDestination.Home) {
                val todayLines = buildList {
                    addAll(summary.taskContext.map { "Task · $it" })
                    addAll(summary.habitContext.map { "Habit · $it" })
                }
                if (horizontal) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        todayLines.take(8).forEach { line ->
                            Card(Modifier.width(220.dp)) {
                                Text(line, Modifier.padding(12.dp), maxLines = 2)
                            }
                        }
                        Text(
                            if (summary.activeWorkout) "Workout · In Progress" else "Workout · Ready When You Are",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (todayLines.isEmpty()) {
                            Text(
                                "No tasks or habits need attention today.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            todayLines.take(8).forEach { line ->
                                Card(Modifier.fillMaxWidth()) {
                                    Text(line, Modifier.padding(12.dp), maxLines = 2)
                                }
                            }
                        }
                        Text(
                            if (summary.activeWorkout) "Workout · In Progress" else "Workout · Ready When You Are",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (selected == AppDestination.Settings) {
                Text(
                    "Preferences and data controls are open in the content pane.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                cards.firstOrNull { it.first == selected }?.let { (destination, label, value) ->
                    AdaptiveSummaryCard(
                        label = label,
                        value = value,
                        destination = destination,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = navigationEnabled,
                    ) { onSelect(destination) }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveSummaryCard(
    label: String,
    value: String,
    destination: AppDestination,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .widthIn(min = 150.dp)
            .clickable(enabled = enabled, onClickLabel = "Open ${destination.label}", onClick = onClick)
            .semantics {
                contentDescription = if (enabled) "$label: $value. Open ${destination.label}"
                else "$label: $value. Navigation unavailable while editing a routine"
            },
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun HomeContent(
    state: TaskUiState,
    habitState: HabitUiState,
    gymState: GymUiState,
    goalState: GoalUiState,
    appSettings: AppSettings,
    innerPadding: PaddingValues,
    onQuickHabit: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onHabitValue: (com.whip.app.domain.HabitDayProgress, Double) -> Unit,
    onSetHabitValue: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onDecrementHabit: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onUndoHabit: (com.whip.app.domain.HabitDayProgress) -> Unit,
    canUndoHabit: (com.whip.app.domain.HabitDayProgress) -> Boolean,
    onChecklist: (Long, Long, LocalDate, Boolean) -> Unit,
    onOpenHabits: () -> Unit,
    onOpenHabit: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onEditHabit: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onOpenTasks: () -> Unit,
    onCompleteTask: (ScheduledTask) -> Unit,
    onOpenTask: (ScheduledTask) -> Unit,
    onEditTask: (ScheduledTask) -> Unit,
    onOpenGym: () -> Unit,
    onStartRoutine: (Long, Long?) -> Unit,
    onOpenGoals: () -> Unit,
    onOpenGoal: (com.whip.app.domain.GoalProjection) -> Unit,
    onEditGoal: (com.whip.app.domain.GoalProjection) -> Unit,
    onRecordGoal: (com.whip.app.domain.GoalProjection) -> Unit,
    onToggleMilestone: (Long, Boolean) -> Unit,
    onSelectHomeTaskFilter: (String?) -> Unit,
    onOpenReview: () -> Unit,
    showFullHeader: Boolean = true,
    areaScopeLabel: String? = null,
    onShowAllAreas: () -> Unit = {},
) {
    val homeTaskFilter = appSettings.savedTaskFilters.firstOrNull { it.name == appSettings.homeTaskFilterName }
    val homeTasks = state.today.filter { homeTaskFilter == null || it.matches(homeTaskFilter, state.currentDate, appSettings.zoneId()) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        areaScopeLabel?.let { label -> item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Showing $label", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                WhipTextButton(onClick = onShowAllAreas) { Text("Show All Areas") }
            }
        } }
        item {
            TodayHeader(
                state.currentDate,
                habitState.today.count { it.successful == true },
                habitState.today.size,
                onOpenHabits,
                showFullHeader,
            )
        }
        item {
            WhipOutlinedButton(
                onClick = onOpenReview,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Insights, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Review & Trends")
            }
        }
        appSettings.homeSections.filter { it !in appSettings.hiddenHomeSections }.forEach { section ->
            val collapsed = section in appSettings.collapsedHomeSections
            when (section) {
                HomeSection.Tasks -> {
                    item { SectionHeading("Tasks", homeTasks.size, onOpenTasks) }
                    if (!collapsed) {
                        if (appSettings.savedTaskFilters.isNotEmpty()) item {
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                WhipFilterChip(homeTaskFilter == null, { onSelectHomeTaskFilter(null) }, { Text("All Tasks") })
                                appSettings.savedTaskFilters.forEach { filter ->
                                    WhipFilterChip(homeTaskFilter?.name == filter.name, { onSelectHomeTaskFilter(filter.name) }, { Text(filter.name) })
                                }
                            }
                        }
                        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        else if (homeTasks.isEmpty()) item { Text(if (homeTaskFilter == null) areaScopeLabel?.let { "No tasks due today in $it." } ?: "Nothing due today." else "No due tasks match ${homeTaskFilter.name}.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(homeTasks, key = ScheduledTask::stableKey) { task ->
                            TaskRow(
                                item = task,
                                completed = false,
                                onComplete = { onCompleteTask(task) },
                                onOpenActions = { onOpenTask(task) },
                                onEdit = { onEditTask(task) },
                            )
                        }
                    }
                }
                HomeSection.Habits -> {
                    item { SectionHeading("Habits", habitState.today.size, onOpenHabits) }
                    if (!collapsed) {
                        if (habitState.today.isEmpty()) item { HomeStatusCard(areaScopeLabel?.let { "No habits due in $it" } ?: "No habits due", "Create a habit on the Habits screen.", onOpenHabits) }
                        items(habitState.today, key = { "home-habit-${it.habit.id}" }) { habit ->
                            HabitProgressCard(
                                item = habit,
                                onOpen = { onOpenHabit(habit) },
                                onEdit = { onEditHabit(habit) },
                                onQuick = { onQuickHabit(habit) },
                                onQuickValue = { value -> onHabitValue(habit, value) },
                                onSetValue = { onSetHabitValue(habit) },
                                onDecrement = { onDecrementHabit(habit) },
                                onUndo = { onUndoHabit(habit) },
                                canUndo = canUndoHabit(habit),
                                onChecklist = onChecklist,
                                lowPressureMode = appSettings.lowPressureMode,
                            )
                        }
                    }
                }
                HomeSection.Goals -> {
                    item { SectionHeading("Goals", goalState.active.size, onOpenGoals) }
                    if (!collapsed) {
                        if (goalState.active.isEmpty()) item { HomeStatusCard(areaScopeLabel?.let { "No active goals in $it" } ?: "No active goals", "Create a measurable or milestone goal.", onOpenGoals) }
                        items(goalState.active.take(3), key = { "home-goal-${it.goal.id}" }) { projection ->
                            GoalCard(
                                projection = projection,
                                customUnits = goalState.customUnits,
                                onOpen = { onOpenGoal(projection) },
                                onEdit = { onEditGoal(projection) },
                                onRecord = { onRecordGoal(projection) },
                                onToggleMilestone = onToggleMilestone,
                            )
                        }
                    }
                }
                HomeSection.Gym -> {
                    item {
                        SectionHeading(
                            if (areaScopeLabel == null) "Gym" else "Gym · All data",
                            if (gymState.activeSession == null) 0 else 1,
                            onOpenGym,
                        )
                    }
                    if (!collapsed) {
                        item {
                            val active = gymState.activeSession
                            val workoutSummary = gymState.summary
                            HomeStatusCard(
                                when {
                                    gymState.loading -> "Loading gym…"
                                    active != null -> active.name.ifBlank { "Active workout" }
                                    else -> "No active workout"
                                },
                                when {
                                    gymState.loading -> "Preparing workouts and history."
                                    workoutSummary != null -> with(workoutSummary) {
                                        "$completedSetCount sets · $repetitions reps · " +
                                            "${massFromKilograms(volumeKg, appSettings.gymWeightUnitId).toInt()} " +
                                            "${unitSymbol(appSettings.gymWeightUnitId)} volume"
                                    }
                                    else -> "Start and resume workouts from the Gym screen."
                                },
                                onOpenGym,
                            )
                        }
                        if (!gymState.loading && gymState.activeSession == null) {
                            items(gymState.routines.filter { it.pinned }, key = { "pinned-routine-${it.id}" }) { routine ->
                                val days = gymState.routineDays.filter { it.routineId == routine.id }.sortedBy { it.position }
                                if (days.size <= 1) {
                                    HomeStatusCard("★ ${routine.name}", days.firstOrNull()?.name ?: "Start pinned routine") {
                                        onStartRoutine(routine.id, days.firstOrNull()?.id)
                                    }
                                } else {
                                    Card(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("★ ${routine.name}", fontWeight = FontWeight.Bold)
                                            Text("Start a Routine Day", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                days.forEach { day ->
                                                    WhipTextButton(onClick = { onStartRoutine(routine.id, day.id) }) { Text(day.name) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskAreaContent(
    state: TaskUiState,
    destination: TaskDestination,
    innerPadding: PaddingValues,
    onDestinationChange: (TaskDestination) -> Unit,
    onCompleteTask: (ScheduledTask) -> Unit,
    onOpenTask: (ScheduledTask) -> Unit,
    onEditTask: (ScheduledTask) -> Unit,
    onOpenCompleted: (ScheduledTask) -> Unit,
    appSettings: AppSettings,
    habitState: HabitUiState,
    onSaveFilter: (SavedTaskFilter) -> Unit,
    onDeleteFilter: (String) -> Unit,
    onBulkComplete: (List<ScheduledTask>) -> Unit,
    onBulkArchive: (List<ScheduledTask>) -> Unit,
    onBulkRestore: (List<ScheduledTask>) -> Unit,
    onBulkPin: (List<ScheduledTask>, Boolean) -> Unit,
    onBulkPostpone: (List<ScheduledTask>, LocalDate) -> Unit,
    onBulkEdit: (List<ScheduledTask>, TaskBulkEdit) -> Unit,
    onSetHabitPlanningOverlay: (Boolean) -> Unit,
    onOpenPlanningHabit: (Long) -> Unit,
    onReorder: (List<ScheduledTask>) -> Unit,
    onPlanMyDay: (List<ScheduledTask>, Int) -> Unit,
    onBulkTriage: (List<ScheduledTask>) -> Unit,
    onStopFocus: () -> Unit,
    onQuickCapture: (String, LocalDate?, Boolean, String?) -> Unit,
    onOpenSearch: () -> Unit,
    searchActionModifier: Modifier = Modifier,
    operationStatus: OperationStatus = OperationStatus.Idle,
    areas: List<Area> = emptyList(),
    areaScope: AreaScope = AreaScope.All,
    onSelectAreaScope: (AreaScope) -> Unit = {},
    planningViewRequest: TaskPlanningView? = null,
    onPlanningViewRequestConsumed: () -> Unit = {},
    dialogModifier: Modifier = Modifier,
) {
    var planningView by rememberSaveable { mutableStateOf(TaskPlanningView.List) }
    var historySection by rememberSaveable {
        mutableStateOf(destination.toWorkspaceRoute().historySection)
    }
    var focusClockMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var textQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf("Smart") }
    var groupMode by rememberSaveable { mutableStateOf("None") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var priorities by rememberSaveable { mutableStateOf(emptySet<TaskPriority>()) }
    var pinnedOnly by rememberSaveable { mutableStateOf(false) }
    var selectedTags by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var requireAllTags by rememberSaveable { mutableStateOf(true) }
    var dateMode by rememberSaveable { mutableStateOf("Any") }
    var deadlineOnly by rememberSaveable { mutableStateOf(false) }
    var efforts by rememberSaveable { mutableStateOf(emptySet<TaskEffort>()) }
    var maximumDuration by rememberSaveable { mutableStateOf("") }
    var saveFilterOpen by rememberSaveable { mutableStateOf(false) }
    var filterName by rememberSaveable { mutableStateOf("") }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectionActionsOpen by rememberSaveable { mutableStateOf(false) }
    var selectedKeys by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var archivePreviewKeys by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var bulkEditOpen by rememberSaveable { mutableStateOf(false) }
    var bulkDatePickerOpen by rememberSaveable { mutableStateOf(false) }
    var calendarMonth by rememberSaveable(state.currentDate) { mutableStateOf(YearMonth.from(state.currentDate)) }
    var selectedDate by rememberSaveable(state.currentDate) { mutableStateOf(state.currentDate) }
    var dayCapacityText by rememberSaveable { mutableStateOf("240") }
    var showDayPlanner by rememberSaveable { mutableStateOf(false) }
    var dayPlanCandidateKeys by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var taskToolsExpanded by rememberSaveable { mutableStateOf(false) }
    var quickCapture by rememberSaveable { mutableStateOf("") }
    var quickCaptureSubmitting by rememberSaveable { mutableStateOf(false) }
    var quickCaptureSaveStarted by rememberSaveable { mutableStateOf(false) }
    var submittedQuickCapture by rememberSaveable { mutableStateOf("") }
    val workspaceDestination = destination.toWorkspaceRoute().destination
    LaunchedEffect(destination) {
        destination.toWorkspaceRoute().takeIf { it.destination == TaskWorkspaceDestination.History }
            ?.let { historySection = it.historySection }
        planningView = workspaceDestination.normalizePlanningView(planningView)
        selectionMode = false
        selectionActionsOpen = false
        selectedKeys = emptySet()
    }
    LaunchedEffect(planningViewRequest, workspaceDestination) {
        planningViewRequest?.let {
            planningView = workspaceDestination.normalizePlanningView(it)
            onPlanningViewRequestConsumed()
        }
    }
    LaunchedEffect(operationStatus, quickCaptureSubmitting) {
        if (!quickCaptureSubmitting) return@LaunchedEffect
        when (operationStatus) {
            is OperationStatus.Running -> quickCaptureSaveStarted = true
            is OperationStatus.Succeeded -> {
                if (!quickCaptureSaveStarted) return@LaunchedEffect
                if (quickCapture == submittedQuickCapture) quickCapture = ""
                quickCaptureSubmitting = false
                quickCaptureSaveStarted = false
                submittedQuickCapture = ""
            }
            is OperationStatus.Failed -> {
                if (!quickCaptureSaveStarted) return@LaunchedEffect
                quickCaptureSubmitting = false
                quickCaptureSaveStarted = false
                submittedQuickCapture = ""
            }
            else -> Unit
        }
    }
    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedKeys = emptySet()
        selectionActionsOpen = false
    }
    LaunchedEffect(areaScope) {
        if (areaScope != AreaScope.All && groupMode == "Area") groupMode = "None"
    }
    LaunchedEffect(appSettings.focusTimerDeadlineMillis) {
        while ((appSettings.focusTimerDeadlineMillis ?: 0L) > System.currentTimeMillis()) {
            focusClockMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
        focusClockMillis = System.currentTimeMillis()
    }
    val allTasks = state.inbox + state.today + state.upcoming + state.planning + state.anytime + state.completed + state.archived
    val availableAreas = areas.filterNot(Area::archived)
    val availableTags = allTasks.flatMap { it.task.tags }.distinct().sorted()
    val currentFilter = SavedTaskFilter(
        name = "Current",
        priorities = priorities,
        areaId = (areaScope as? AreaScope.One)?.areaId,
        pinnedOnly = pinnedOnly,
        tags = selectedTags,
        requireAllTags = requireAllTags,
        dateMode = dateMode,
        deadlineOnly = deadlineOnly,
        inboxOnly = destination == TaskDestination.Inbox,
        efforts = efforts,
        maximumDurationMinutes = maximumDuration.toIntOrNull(),
        textQuery = textQuery.trim(),
        destination = destination.name,
        planningView = planningView.name,
        sortMode = sortMode,
        groupMode = groupMode,
    )
    // `upcoming` is the authoritative 30-day, preference-aware collection. The
    // year-long `planning` collection exists for search and future planning, but
    // using it here made Agenda and Calendar silently ignore the user's recurring
    // occurrence preference and contradicted the "next 30 days" heading.
    val sourceTasks = state.tasksFor(destination)
    val filtered = sourceTasks.filter { it.matches(currentFilter, state.currentDate, appSettings.zoneId()) }.let { matches ->
        when (sortMode) {
            "Title" -> matches.sortedBy { it.task.title.lowercase() }
            "Date" -> matches.sortedWith(
                compareBy<ScheduledTask, LocalDate?>(nullsLast()) { it.scheduledDate }
                    .thenBy { it.task.title.lowercase() },
            )
            "Priority" -> matches.sortedByDescending { it.task.priority.ordinal }
            "Manual" -> matches.sortedWith(compareBy<ScheduledTask> { it.task.manualPosition }.thenBy { it.task.createdAtMillis })
            else -> matches
        }
    }
    val visibleTasks = filtered.forPlanningView(planningView, selectedDate, appSettings.zoneId())
    val selectedItems = visibleTasks.filter { it.stableKey in selectedKeys }
    val hiddenSelectedCount = (selectedKeys - visibleTasks.mapTo(mutableSetOf(), ScheduledTask::stableKey)).size
    val habitPlanningDates = when (planningView) {
        TaskPlanningView.List -> emptyList()
        TaskPlanningView.Agenda -> (0L..30L).map(state.currentDate::plusDays)
        TaskPlanningView.Calendar -> (1..calendarMonth.lengthOfMonth()).map(calendarMonth::atDay)
    }
    val plannedHabitsByDate = if (appSettings.showHabitsInTaskPlanning) {
        habitPlanningDates.associateWith { date -> habitState.plannedOn(date) }.filterValues { it.isNotEmpty() }
    } else emptyMap()
    val activeFilters = buildList {
        if (priorities.isNotEmpty()) add(
            WhipActiveFilter("priority", "Priority: ${priorities.joinToString { it.name }}") {
                priorities = emptySet()
            },
        )
        if (selectedTags.isNotEmpty()) add(
            WhipActiveFilter(
                "tags",
                "Tags: ${selectedTags.joinToString()}",
            ) {
                selectedTags = emptySet()
            },
        )
        if (pinnedOnly) add(WhipActiveFilter("pinned", "Pinned") { pinnedOnly = false })
        if (dateMode != "Any") add(WhipActiveFilter("date", dateMode.taskDateModeLabel()) { dateMode = "Any" })
        if (deadlineOnly) add(WhipActiveFilter("deadline", "Has Deadline") { deadlineOnly = false })
        if (efforts.isNotEmpty()) add(
            WhipActiveFilter("effort", "Effort: ${efforts.joinToString { it.label }}") { efforts = emptySet() },
        )
        if (maximumDuration.isNotBlank()) add(
            WhipActiveFilter("duration", "Up to $maximumDuration min") { maximumDuration = "" },
        )
        if (textQuery.isNotBlank()) add(
            WhipActiveFilter("saved-query", "Query: $textQuery") { textQuery = "" },
        )
    }
    val activeFilterCount = activeFilters.count { it.key != "saved-query" }

    fun applyFilter(filter: SavedTaskFilter) {
        val normalized = filter.normalizedForWorkspace()
        priorities = normalized.priorities
        normalized.areaId?.let {
            onSelectAreaScope(AreaScope.One(it))
        }
        pinnedOnly = normalized.pinnedOnly
        selectedTags = normalized.tags
        requireAllTags = normalized.requireAllTags
        dateMode = normalized.dateMode
        deadlineOnly = normalized.deadlineOnly
        efforts = normalized.efforts
        maximumDuration = normalized.maximumDurationMinutes?.toString().orEmpty()
        textQuery = normalized.textQuery
        sortMode = normalized.sortMode
        groupMode = normalized.groupMode
        runCatching { TaskDestination.valueOf(normalized.destination) }.getOrNull()?.let(onDestinationChange)
        planningView = runCatching { TaskPlanningView.valueOf(normalized.planningView) }
            .getOrDefault(TaskPlanningView.List)
        showFilters = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        DestinationTabBar(
            selected = workspaceDestination,
            destinations = TaskWorkspaceDestination.entries,
            onSelect = { selected ->
                textQuery = ""
                val nextRoute = TaskWorkspaceRoute(selected, historySection)
                onDestinationChange(nextRoute.dataDestination())
            },
            label = TaskWorkspaceDestination::label,
            testTagPrefix = "task-destination",
        )
        if (workspaceDestination == TaskWorkspaceDestination.History) {
            SegmentedChoiceBar(
                selected = historySection,
                choices = TaskHistorySection.entries,
                onSelect = { selected ->
                    historySection = selected
                    onDestinationChange(TaskWorkspaceRoute(TaskWorkspaceDestination.History, selected).dataDestination())
                },
                label = TaskHistorySection::label,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WhipPageHeader(
                title = destination.label,
                supportingText = taskDestinationSupportingText(destination, visibleTasks.size),
            ) {
                WhipPageIconAction(
                    icon = Icons.Outlined.Search,
                    label = "Search Tasks & Steps",
                    onClick = onOpenSearch,
                    modifier = searchActionModifier,
                )
                Box {
                    IconButton(
                        onClick = { taskToolsExpanded = true },
                        modifier = Modifier.size(48.dp).semantics { contentDescription = "More task list actions" },
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = taskToolsExpanded,
                        onDismissRequest = { taskToolsExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select Tasks") },
                            onClick = {
                                selectionMode = true
                                selectedKeys = emptySet()
                                taskToolsExpanded = false
                            },
                        )
                    }
                }
            }
            if (selectionMode) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${selectedItems.size} selected", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            WhipTextButton(onClick = { selectionMode = false; selectedKeys = emptySet() }) { Text("Done") }
                        }
                        if (hiddenSelectedCount > 0) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "$hiddenSelectedCount selected item${if (hiddenSelectedCount == 1) " is" else "s are"} hidden by the current view or filters.",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                WhipTextButton(onClick = {
                                    val visibleKeys = visibleTasks.mapTo(mutableSetOf(), ScheduledTask::stableKey)
                                    selectedKeys = selectedKeys.intersect(visibleKeys)
                                }) { Text("Clear Hidden") }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (activeFilterCount > 0) {
                                WhipTextButton(onClick = { showFilters = true }) { Text("Filter & Sort · $activeFilterCount") }
                            }
                            WhipTextButton(onClick = {
                                selectedKeys = if (selectedItems.size == visibleTasks.size) emptySet() else visibleTasks.mapTo(linkedSetOf(), ScheduledTask::stableKey)
                            }) { Text(if (selectedItems.size == visibleTasks.size && visibleTasks.isNotEmpty()) "Clear All" else "Select All") }
                            if (destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)) {
                                WhipButton(
                                    enabled = selectedItems.isNotEmpty(),
                                    onClick = { onBulkComplete(selectedItems); selectedKeys = emptySet() },
                                ) { Text("Complete") }
                                WhipOutlinedButton(
                                    enabled = selectedItems.isNotEmpty(),
                                    onClick = { archivePreviewKeys = selectedItems.mapTo(linkedSetOf(), ScheduledTask::stableKey) },
                                ) { Text("Archive") }
                                WhipOutlinedButton(
                                    enabled = selectedItems.isNotEmpty(),
                                    onClick = { bulkEditOpen = true },
                                ) { Text("Edit") }
                                Box {
                                    WhipOutlinedButton(
                                        enabled = selectedItems.isNotEmpty(),
                                        onClick = { selectionActionsOpen = true },
                                    ) {
                                        Icon(Icons.Outlined.MoreVert, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("More")
                                    }
                                    DropdownMenu(
                                        expanded = selectionActionsOpen,
                                        onDismissRequest = { selectionActionsOpen = false },
                                    ) {
                                        DropdownMenuItem(text = { Text("Pin") }, onClick = {
                                            onBulkPin(selectedItems, true); selectedKeys = emptySet(); selectionActionsOpen = false
                                        })
                                        DropdownMenuItem(text = { Text("Unpin") }, onClick = {
                                            onBulkPin(selectedItems, false); selectedKeys = emptySet(); selectionActionsOpen = false
                                        })
                                        DropdownMenuItem(text = { Text("Move to Tomorrow") }, onClick = {
                                            onBulkPostpone(selectedItems, state.currentDate.plusDays(1)); selectedKeys = emptySet(); selectionActionsOpen = false
                                        })
                                        DropdownMenuItem(text = { Text("Move to Next Week") }, onClick = {
                                            onBulkPostpone(selectedItems, state.currentDate.plusWeeks(1)); selectedKeys = emptySet(); selectionActionsOpen = false
                                        })
                                        DropdownMenuItem(text = { Text("Choose Date") }, onClick = {
                                            bulkDatePickerOpen = true; selectionActionsOpen = false
                                        })
                                        if (destination == TaskDestination.Inbox) DropdownMenuItem(text = { Text("Mark Triaged") }, onClick = {
                                            onBulkTriage(selectedItems); selectedKeys = emptySet(); selectionActionsOpen = false
                                        })
                                    }
                                }
                            } else if (destination == TaskDestination.Archived) {
                                WhipButton(
                                    enabled = selectedItems.isNotEmpty(),
                                    onClick = { onBulkRestore(selectedItems); selectedKeys = emptySet() },
                                ) { Text("Restore") }
                            }
                        }
                    }
                }
            } else {
                WhipViewAndFilterRow(
                    selectedView = planningView,
                    views = workspaceDestination.allowedPlanningViews(),
                    viewLabel = TaskPlanningView::name,
                    onSelectView = { planningView = workspaceDestination.normalizePlanningView(it) },
                    filterCount = activeFilterCount,
                    onOpenFilters = { showFilters = true },
                )
                WhipActiveFilterRow(
                    filters = activeFilters,
                    onClearAll = {
                        priorities = emptySet()
                        selectedTags = emptySet()
                        pinnedOnly = false
                        dateMode = "Any"
                        deadlineOnly = false
                        efforts = emptySet()
                        maximumDuration = ""
                        textQuery = ""
                    },
                )
                if (sortMode != "Smart" || groupMode != "None") {
                    Text(
                        listOfNotNull(
                            sortMode.takeIf { it != "Smart" }?.let { "Sorted by $it" },
                            groupMode.takeIf { it != "None" }?.let { "Grouped by $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (planningView != TaskPlanningView.List) {
                    WhipFilterChip(
                        selected = appSettings.showHabitsInTaskPlanning,
                        onClick = { onSetHabitPlanningOverlay(!appSettings.showHabitsInTaskPlanning) },
                        label = { Text("Include Habits") },
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        appSettings.focusTimerDeadlineMillis?.takeIf { it > focusClockMillis }?.let { deadline ->
            item {
                val taskName = allTasks.firstOrNull { it.task.id == appSettings.focusTimerTaskId }?.task?.title ?: "Focus session"
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(taskName, fontWeight = FontWeight.Bold)
                            Text(
                                "${formatFocusDuration(((deadline - focusClockMillis).coerceAtLeast(0L) / 1_000L))} remaining · until ${java.time.Instant.ofEpochMilli(deadline).atZone(appSettings.zoneId()).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        WhipTextButton(onClick = onStopFocus) { Text("Stop") }
                    }
                }
            }
        }
        if (destination in setOf(TaskDestination.Today, TaskDestination.Inbox)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = quickCapture,
                        onValueChange = { quickCapture = it },
                        label = { Text(if (destination == TaskDestination.Inbox) "Quick Add to Inbox" else "Quick Add") },
                        trailingIcon = {
                            IconButton(
                                enabled = quickCapture.isNotBlank() && !quickCaptureSubmitting,
                                onClick = {
                                    submittedQuickCapture = quickCapture.trim()
                                    quickCaptureSubmitting = true
                                    quickCaptureSaveStarted = false
                                    onQuickCapture(
                                        submittedQuickCapture,
                                        state.currentDate.takeIf { destination == TaskDestination.Today },
                                        destination == TaskDestination.Inbox,
                                        (areaScope as? AreaScope.One)?.areaId
                                            ?: availableAreas.firstOrNull()?.id,
                                    )
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Add task now")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("task-quick-capture"),
                    )
                    Text(
                        if (quickCaptureSubmitting) "Saving…" else "Saves immediately. Use Edit or Undo in the confirmation.",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (destination in setOf(TaskDestination.Inbox, TaskDestination.Anytime)) {
            item {
                DisclosureRow(
                    title = "Plan My Day",
                    supportingText = "Build a realistic plan from your available time.",
                    expanded = showDayPlanner,
                    onClick = {
                        showDayPlanner = !showDayPlanner
                        if (!showDayPlanner) dayPlanCandidateKeys = null
                    },
                )
                if (showDayPlanner) Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Choose a realistic capacity. Whip prioritizes urgent and high-effort items without overfilling the day; tasks without an estimate count as 30 minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            dayCapacityText,
                            { dayCapacityText = it.filter(Char::isDigit).take(4) },
                            label = { Text("Available minutes") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        WhipButton(
                            enabled = (dayCapacityText.toIntOrNull() ?: 0) > 0 && filtered.isNotEmpty(),
                            onClick = {
                                dayPlanCandidateKeys = selectTasksForCapacity(
                                    filtered,
                                    dayCapacityText.toIntOrNull() ?: 240,
                                ).mapTo(linkedSetOf(), ScheduledTask::stableKey)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Preview Plan") }
                        dayPlanCandidateKeys?.let { selected ->
                            val capacity = dayCapacityText.toIntOrNull() ?: 240
                            val selectedMinutes = filtered.filter { it.stableKey in selected }
                                .sumOf { it.task.durationMinutes ?: 30 }
                            HorizontalDivider()
                            Text(
                                "Proposed: ${selected.size} of ${filtered.size} tasks · $selectedMinutes of $capacity minutes",
                                fontWeight = FontWeight.Bold,
                            )
                            filtered.forEach { candidate ->
                                val checked = candidate.stableKey in selected
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        dayPlanCandidateKeys = if (checked) selected - candidate.stableKey
                                        else selected + candidate.stableKey
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(checked, null)
                                    Column(Modifier.weight(1f)) {
                                        Text(candidate.task.title)
                                        Text(
                                            if (checked) {
                                                "Selected · ${candidate.task.durationMinutes ?: 30} min · ${candidate.task.priority.name.lowercase()} priority"
                                            } else if (selectedMinutes + (candidate.task.durationMinutes ?: 30) > capacity) {
                                                "Skipped · would exceed capacity"
                                            } else {
                                                "Skipped · lower planning rank"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WhipTextButton(onClick = { dayPlanCandidateKeys = null }) { Text("Cancel") }
                                WhipButton(
                                    enabled = selected.isNotEmpty() && selectedMinutes <= capacity,
                                    onClick = {
                                        onPlanMyDay(filtered.filter { it.stableKey in selected }, capacity)
                                        dayPlanCandidateKeys = null
                                    },
                                ) { Text("Apply Plan") }
                            }
                            if (selectedMinutes > capacity) Text(
                                "Remove ${selectedMinutes - capacity} minutes before applying.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        if (planningView == TaskPlanningView.Calendar) {
            item {
                TaskMonthPlanner(
                    month = calendarMonth,
                    selectedDate = selectedDate,
                    tasks = filtered,
                    onPrevious = { calendarMonth = calendarMonth.minusMonths(1) },
                    onNext = { calendarMonth = calendarMonth.plusMonths(1) },
                    onSelect = { selectedDate = it },
                    firstDayOfWeek = appSettings.firstDayOfWeek,
                    zoneId = appSettings.zoneId(),
                    habitCounts = plannedHabitsByDate.mapValues { it.value.size },
                )
            }
            if (appSettings.showHabitsInTaskPlanning) {
                val selectedHabits = plannedHabitsByDate[selectedDate].orEmpty()
                if (selectedHabits.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Habits · ${selectedHabits.size}", fontWeight = FontWeight.Bold)
                        selectedHabits.forEach { habit ->
                            Card(
                                Modifier.fillMaxWidth().clickable(onClickLabel = "Open habit ${habit.habit.name}") {
                                    onOpenPlanningHabit(habit.habit.id)
                                },
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(habit.habit.name, fontWeight = FontWeight.SemiBold)
                                    Text("Habit projection · kept separate from tasks", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (state.loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        } else if (visibleTasks.isEmpty()) {
            item {
                EmptyTasks(destination, areaScope.takeUnless { it == AreaScope.All }?.let {
                    when (it) {
                        AreaScope.All -> null
                        AreaScope.Unassigned -> availableAreas.firstOrNull { !it.archived }?.name ?: "Main"
                        is AreaScope.One -> availableAreas.firstOrNull { area -> area.id == it.areaId }?.name
                    }
                }, constrained = sourceTasks.isNotEmpty())
                if (areaScope != AreaScope.All) WhipTextButton(onClick = { onSelectAreaScope(AreaScope.All) }) { Text("Show All Areas") }
            }
        }
        if (planningView == TaskPlanningView.Agenda) {
            val tasksByDate = visibleTasks.groupBy { it.planningDate(appSettings.zoneId()) }
            (tasksByDate.keys + plannedHabitsByDate.keys)
                .distinct().sortedWith(java.util.Comparator.nullsLast(naturalOrder<LocalDate>()))
                .forEach { date ->
                val tasks = tasksByDate[date].orEmpty()
                item(key = "agenda-${date ?: "anytime"}") {
                    Text(
                        date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) ?: "No date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(tasks, key = ScheduledTask::stableKey) { item ->
                    TaskPlanningRow(
                        item = item,
                        destination = destination,
                        selectionMode = selectionMode,
                        selectedKeys = selectedKeys,
                        onSelectionChange = { selectedKeys = it },
                        onCompleteTask = onCompleteTask,
                        onOpenTask = onOpenTask,
                        onEditTask = onEditTask,
                        onOpenCompleted = onOpenCompleted,
                    )
                }
                items(plannedHabitsByDate[date].orEmpty(), key = { "agenda-habit-${date}-${it.habit.id}" }) { habit ->
                    Card(
                        Modifier.fillMaxWidth().clickable(onClickLabel = "Open habit ${habit.habit.name}") {
                            onOpenPlanningHabit(habit.habit.id)
                        },
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(habit.habit.name, fontWeight = FontWeight.SemiBold)
                                Text("Habit · ${habit.habit.trackingMode.uiLabel()}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(if (habit.successful == true && date == habitState.currentDate) "Done" else "Scheduled")
                        }
                    }
                }
            }
        } else if (planningView == TaskPlanningView.List && groupMode != "None") {
            visibleTasks.groupBy { it.groupingLabel(groupMode, appSettings.zoneId()) }.toSortedMap().forEach { (label, tasks) ->
                item(key = "task-group-$groupMode-$label") { Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(tasks, key = ScheduledTask::stableKey) { item ->
                    TaskPlanningListRow(
                        item = item,
                        destination = destination,
                        selectionMode = selectionMode,
                        selectedKeys = selectedKeys,
                        onSelectionChange = { selectedKeys = it },
                        onCompleteTask = onCompleteTask,
                        onOpenTask = onOpenTask,
                        onEditTask = onEditTask,
                        onOpenCompleted = onOpenCompleted,
                        manualOrder = emptyList(),
                        onReorder = onReorder,
                    )
                }
            }
        } else items(visibleTasks, key = ScheduledTask::stableKey) { item ->
            TaskPlanningListRow(
                item = item,
                destination = destination,
                selectionMode = selectionMode,
                selectedKeys = selectedKeys,
                onSelectionChange = { selectedKeys = it },
                onCompleteTask = onCompleteTask,
                onOpenTask = onOpenTask,
                onEditTask = onEditTask,
                onOpenCompleted = onOpenCompleted,
                manualOrder = visibleTasks.takeIf { sortMode == "Manual" }.orEmpty(),
                onReorder = onReorder,
            )
        }
        }
    }

    if (showFilters) {
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { showFilters = false },
            title = { Text("Filter & Sort Tasks") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SelectionField(
                        label = "Sort by",
                        values = listOf("Smart", "Manual", "Date", "Priority", "Title"),
                        selected = sortMode,
                        valueText = { it },
                        onSelect = { sortMode = it },
                    )
                    val groupOptions = if (areaScope == AreaScope.All) {
                        listOf("None", "Date", "Area", "Priority")
                    } else {
                        listOf("None", "Date", "Priority")
                    }
                    SelectionField(
                        label = "Group by",
                        values = groupOptions,
                        selected = groupMode.takeIf { it in groupOptions } ?: "None",
                        valueText = { it },
                        onSelect = { groupMode = it },
                    )
                    Text("Priority", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.entries.filter { it != TaskPriority.None }.forEach { value ->
                            WhipFilterChip(
                                selected = value in priorities,
                                onClick = { priorities = if (value in priorities) priorities - value else priorities + value },
                                label = { Text(value.name) },
                            )
                        }
                    }
                    if (availableTags.isNotEmpty()) {
                        Text("Tags", fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableTags.forEach { value ->
                                WhipFilterChip(
                                    selected = value in selectedTags,
                                    onClick = { selectedTags = if (value in selectedTags) selectedTags - value else selectedTags + value },
                                    label = { Text("#$value") },
                                )
                            }
                            if (selectedTags.size > 1) WhipFilterChip(
                                selected = requireAllTags,
                                onClick = { requireAllTags = !requireAllTags },
                                label = { Text(if (requireAllTags) "Match All" else "Match Any") },
                            )
                        }
                    }
                    Text("Date and Status", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Any" to "Any date", "Today" to "Today", "Overdue" to "Overdue", "Next7Days" to "Next 7 days", "NoDate" to "No date").forEach { (value, filterLabel) ->
                            WhipFilterChip(dateMode == value, { dateMode = value }, { Text(filterLabel) })
                        }
                        WhipFilterChip(selected = deadlineOnly, onClick = { deadlineOnly = !deadlineOnly }, label = { Text("Has Deadline") })
                        WhipFilterChip(selected = pinnedOnly, onClick = { pinnedOnly = !pinnedOnly }, label = { Text("Pinned Only") })
                    }
                    Text("Effort and Duration", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskEffort.entries.forEach { value ->
                            WhipFilterChip(value in efforts, { efforts = if (value in efforts) efforts - value else efforts + value }, { Text(value.label) })
                        }
                    }
                    OutlinedTextField(
                        maximumDuration,
                        { maximumDuration = it.filter(Char::isDigit).take(4) },
                        label = { Text("Maximum minutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (appSettings.savedTaskFilters.isNotEmpty()) {
                        Text("Saved Filters", fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            appSettings.savedTaskFilters.forEach { filter ->
                                WhipFilterChip(
                                    selected = filter.copy(name = "Current") == currentFilter,
                                    onClick = { applyFilter(filter) },
                                    label = { Text(filter.name) },
                                )
                            }
                        }
                        appSettings.savedTaskFilters.firstOrNull { it.copy(name = "Current") == currentFilter }?.let { selected ->
                            WhipTextButton(onClick = { onDeleteFilter(selected.name) }) { Text("Delete “${selected.name}”") }
                        }
                    }
                    WhipTextButton(onClick = {
                        showFilters = false
                        saveFilterOpen = true
                    }) { Text("Save These Filters") }
                }
            },
            confirmButton = { WhipTextButton(onClick = { showFilters = false }) { Text("Done") } },
            dismissButton = {
                WhipTextButton(onClick = {
                    sortMode = "Smart"; groupMode = "None"
                    priorities = emptySet(); pinnedOnly = false
                    selectedTags = emptySet(); requireAllTags = true; dateMode = "Any"; deadlineOnly = false
                    efforts = emptySet(); maximumDuration = ""
                }) { Text("Clear All") }
            },
        )
    }

    if (saveFilterOpen) {
        AlertDialog(
            onDismissRequest = { saveFilterOpen = false },
            title = { Text("Save Task Filter") },
            text = { OutlinedTextField(filterName, { filterName = it }, label = { Text("Filter name") }, singleLine = true) },
            confirmButton = {
                WhipTextButton(
                    enabled = filterName.isNotBlank(),
                    onClick = {
                        onSaveFilter(currentFilter.copy(name = filterName.trim()))
                        filterName = ""
                        saveFilterOpen = false
                    },
                ) { Text("Save") }
            },
            dismissButton = { WhipTextButton(onClick = { saveFilterOpen = false }) { Text("Cancel") } },
        )
    }
    archivePreviewKeys?.let { keys ->
        val affected = allTasks.filter { it.stableKey in keys }.distinctBy(ScheduledTask::stableKey)
        val series = affected.distinctBy { it.task.id }
        val recurringSeries = series.count { it.task.scheduleKind == ScheduleKind.Recurring }
        AlertDialog(
            onDismissRequest = { archivePreviewKeys = null },
            title = { Text("Archive ${series.size} ${if (series.size == 1) "Task" else "Tasks"}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This hides ${series.size} ${if (series.size == 1) "task" else "tasks"} from active views. " +
                            if (recurringSeries > 0) "$recurringSeries repeating ${if (recurringSeries == 1) "series is" else "series are"} archived in full, not just the selected occurrence." else "No repeating series are included.",
                    )
                    series.take(8).forEach { Text("• ${it.task.title}") }
                    if (series.size > 8) Text("…and ${series.size - 8} more")
                    Text("Completed history is kept. You can restore these from Archived.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                WhipButton(
                    enabled = affected.isNotEmpty(),
                    onClick = {
                        onBulkArchive(affected)
                        selectedKeys = emptySet()
                        archivePreviewKeys = null
                    },
                ) { Text("Archive ${series.size}") }
            },
            dismissButton = { WhipTextButton(onClick = { archivePreviewKeys = null }) { Text("Cancel") } },
        )
    }
    if (bulkDatePickerOpen) {
        WhipDatePickerDialog(
            initialDate = state.currentDate,
            onDismiss = { bulkDatePickerOpen = false },
            onDateSelected = { date ->
                onBulkPostpone(selectedItems, date)
                selectedKeys = emptySet()
                bulkDatePickerOpen = false
            },
        )
    }
    if (bulkEditOpen) {
        TaskBulkEditDialog(
            count = selectedItems.map { it.task.id }.distinct().size,
            knownAreas = availableAreas,
            knownTags = availableTags,
            onDismiss = { bulkEditOpen = false },
            onApply = { edit ->
                onBulkEdit(selectedItems, edit)
                selectedKeys = emptySet()
                bulkEditOpen = false
            },
        )
    }
}

@Composable
private fun TaskBulkEditDialog(
    count: Int,
    knownAreas: List<Area>,
    knownTags: List<String>,
    onDismiss: () -> Unit,
    onApply: (TaskBulkEdit) -> Unit,
) {
    var applyArea by rememberSaveable { mutableStateOf(false) }
    var areaId by rememberSaveable { mutableStateOf<String?>(null) }
    var areaName by rememberSaveable { mutableStateOf("") }
    var applyTags by rememberSaveable { mutableStateOf(false) }
    var tags by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("") }
    var effort by rememberSaveable { mutableStateOf("") }
    var inbox by rememberSaveable { mutableStateOf("") }
    val canApply = applyArea || applyTags || priority.isNotBlank() || effort.isNotBlank() || inbox.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $count ${if (count == 1) "Task" else "Tasks"}") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Only enabled fields change. Tags replace the selected tasks’ complete tag set.", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(applyArea, { applyArea = it })
                    Text("Change Area", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                }
                if (applyArea) {
                    AreaSelectionDropdown(
                        areas = knownAreas,
                        selectedAreaId = areaId,
                        selectedAreaName = areaName,
                        onSelect = { id, name -> areaId = id; areaName = name },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(applyTags, { applyTags = it })
                    OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma-separated") }, enabled = applyTags, modifier = Modifier.weight(1f))
                }
                if (applyTags && knownTags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    knownTags.forEach { value ->
                        val current = tags.split(',').map(String::trim).filter(String::isNotBlank)
                        WhipFilterChip(current.any { it.equals(value, true) }, {
                            tags = if (current.any { it.equals(value, true) }) current.filterNot { it.equals(value, true) }.joinToString(", ")
                            else (current + value).joinToString(", ")
                        }, { Text("#$value") })
                    }
                }
                Text("Priority · ${priority.ifBlank { "Keep existing" }}", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WhipFilterChip(priority.isBlank(), { priority = "" }, { Text("Keep") })
                    TaskPriority.entries.forEach { value -> WhipFilterChip(priority == value.name, { priority = value.name }, { Text(value.name) }) }
                }
                val selectedEffortLabel = effort.takeIf(String::isNotBlank)
                    ?.let { stored -> TaskEffort.entries.firstOrNull { it.name == stored }?.label }
                    ?: "Keep existing"
                Text("Effort · $selectedEffortLabel", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WhipFilterChip(effort.isBlank(), { effort = "" }, { Text("Keep") })
                    TaskEffort.entries.forEach { value -> WhipFilterChip(effort == value.name, { effort = value.name }, { Text(value.label) }) }
                }
                Text("Inbox · ${when (inbox) { "true" -> "Move to Inbox Where Possible"; "false" -> "Mark Triaged"; else -> "Keep Existing" }}", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WhipFilterChip(inbox.isBlank(), { inbox = "" }, { Text("Keep") })
                    WhipFilterChip(inbox == "true", { inbox = "true" }, { Text("Move to Inbox") })
                    WhipFilterChip(inbox == "false", { inbox = "false" }, { Text("Mark Triaged") })
                }
            }
        },
        confirmButton = {
            WhipButton(
                enabled = canApply,
                onClick = {
                    onApply(
                        TaskBulkEdit(
                            updateArea = applyArea,
                            areaId = areaId,
                            areaName = areaName,
                            tags = tags.split(',').map(String::trim).filter(String::isNotBlank).toSet().takeIf { applyTags },
                            priority = priority.takeIf(String::isNotBlank)?.let(TaskPriority::valueOf),
                            effort = effort.takeIf(String::isNotBlank)?.let(TaskEffort::valueOf),
                            inbox = inbox.toBooleanStrictOrNull(),
                        ),
                    )
                },
            ) { Text("Apply Changes") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun ScheduledTask.matches(filter: SavedTaskFilter, today: LocalDate, zoneId: java.time.ZoneId): Boolean =
    (filter.textQuery.isBlank() || buildString {
        append(task.title).append(' ').append(task.notes).append(' ')
        task.steps.forEach { append(it.title).append(' ').append(it.notes).append(' ') }
    }.contains(filter.textQuery.trim(), ignoreCase = true)) &&
        (filter.priorities.isEmpty() || task.priority in filter.priorities) &&
        (filter.areaId == null || task.areaId == filter.areaId) &&
        (!filter.pinnedOnly || task.pinned) &&
        (!filter.deadlineOnly || task.deadline != null) &&
        (!filter.inboxOnly || task.inbox) &&
        (filter.efforts.isEmpty() || task.effort in filter.efforts) &&
        (filter.maximumDurationMinutes == null || (task.durationMinutes ?: Int.MAX_VALUE) <= filter.maximumDurationMinutes) &&
        run {
            val normalized = task.tags.mapTo(mutableSetOf()) { it.lowercase() }
            val required = filter.tags.mapTo(mutableSetOf()) { it.lowercase() }
            required.isEmpty() || if (filter.requireAllTags) normalized.containsAll(required) else normalized.any(required::contains)
        } &&
        when (filter.dateMode) {
            "Today" -> planningDate(zoneId) == today
            "Overdue" -> planningDate(zoneId)?.isBefore(today) == true
            "Next7Days" -> planningDate(zoneId)?.let { it in today..today.plusDays(7) } == true
            "NoDate" -> planningDate(zoneId) == null
            else -> true
        }

private fun ScheduledTask.planningDate(zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()): LocalDate? =
    completedAtMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
        ?: scheduledDate
        ?: task.deadline

private fun ScheduledTask.groupingLabel(mode: String, zoneId: java.time.ZoneId): String = when (mode) {
    "Date" -> planningDate(zoneId)?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "No date"
    "Area" -> task.area.ifBlank { "Main" }
    "Priority" -> "${task.priority.name} priority"
    else -> "Tasks"
}

internal fun HabitUiState.plannedOn(date: LocalDate): List<HabitDayProgress> {
    if (date == currentDate) return today
    return all.mapNotNull { current ->
        val habit = current.habit
        val habitLogs = logs.filter { it.habitId == habit.id }
        val habitPauses = pauses.filter { it.habitId == habit.id }
        if (habit.archived || habit.hasEnded(habitLogs, date, habitPauses, customUnits)) return@mapNotNull null
        val explicitlyPaused = habitPauses.any { pause ->
            !date.isBefore(pause.startDate) && (pause.endDate == null || !date.isAfter(pause.endDate))
        }
        if (explicitlyPaused) return@mapNotNull null
        val flexible = habit.flexibleProgress(habitLogs, date, habitPauses)
        val weekCount = flexible?.completed.takeIf { habit.scheduleType == com.whip.app.domain.HabitScheduleType.FlexibleTimesPerWeek } ?: 0
        val monthCount = flexible?.completed.takeIf { habit.scheduleType == com.whip.app.domain.HabitScheduleType.FlexibleTimesPerMonth } ?: 0
        current.copy(
            date = date,
            scheduled = habit.isScheduledOn(date, weekCount, monthCount),
            value = habit.valueForPeriod(habitLogs, date, customUnits),
            successful = habit.outcomeForPeriod(habitLogs, date, customUnits),
        ).takeIf(HabitDayProgress::scheduled)
    }
}

private fun formatFocusDuration(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
    else "%d:%02d".format(minutes, remainingSeconds)
}

@Composable
private fun TaskPlanningRow(
    item: ScheduledTask,
    destination: TaskDestination,
    selectionMode: Boolean,
    selectedKeys: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onCompleteTask: (ScheduledTask) -> Unit,
    onOpenTask: (ScheduledTask) -> Unit,
    onEditTask: (ScheduledTask) -> Unit,
    onOpenCompleted: (ScheduledTask) -> Unit,
) {
    val completed = destination == TaskDestination.Completed
    TaskRow(
        item = item,
        completed = completed,
        onComplete = if (completed) null else ({ onCompleteTask(item) }),
        onOpenActions = if (completed) ({ onOpenCompleted(item) }) else ({ onOpenTask(item) }),
        onEdit = { onEditTask(item) },
        selectionMode = selectionMode,
        selected = item.stableKey in selectedKeys,
        onSelectionToggle = {
            onSelectionChange(
                if (item.stableKey in selectedKeys) selectedKeys - item.stableKey
                else selectedKeys + item.stableKey,
            )
        },
    )
}

@Composable
private fun TaskPlanningListRow(
    item: ScheduledTask,
    destination: TaskDestination,
    selectionMode: Boolean,
    selectedKeys: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onCompleteTask: (ScheduledTask) -> Unit,
    onOpenTask: (ScheduledTask) -> Unit,
    onEditTask: (ScheduledTask) -> Unit,
    onOpenCompleted: (ScheduledTask) -> Unit,
    manualOrder: List<ScheduledTask>,
    onReorder: (List<ScheduledTask>) -> Unit,
) {
    Column {
        TaskPlanningRow(
            item = item,
            destination = destination,
            selectionMode = selectionMode,
            selectedKeys = selectedKeys,
            onSelectionChange = onSelectionChange,
            onCompleteTask = onCompleteTask,
            onOpenTask = onOpenTask,
            onEditTask = onEditTask,
            onOpenCompleted = onOpenCompleted,
        )
        if (manualOrder.isNotEmpty() && !selectionMode) {
            val unique = manualOrder.distinctBy { it.task.id }
            val index = unique.indexOfFirst { it.task.id == item.task.id }
            if (index >= 0 && manualOrder.indexOfFirst { it.task.id == item.task.id } == manualOrder.indexOf(item)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    WhipTextButton(enabled = index > 0, onClick = {
                        val reordered = unique.toMutableList().also { java.util.Collections.swap(it, index, index - 1) }
                        onReorder(reordered)
                    }) { Text("Earlier") }
                    WhipTextButton(enabled = index < unique.lastIndex, onClick = {
                        val reordered = unique.toMutableList().also { java.util.Collections.swap(it, index, index + 1) }
                        onReorder(reordered)
                    }) { Text("Later") }
                }
            }
        }
    }
}

@Composable
private fun TaskMonthPlanner(
    month: YearMonth,
    selectedDate: LocalDate,
    tasks: List<ScheduledTask>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    firstDayOfWeek: java.time.DayOfWeek,
    zoneId: java.time.ZoneId,
    habitCounts: Map<LocalDate, Int> = emptyMap(),
) {
    val dates = (1..month.lengthOfMonth()).map(month::atDay)
    val orderedDays = (0L..6L).map { firstDayOfWeek.plus(it) }
    val leading = orderedDays.indexOf(dates.first().dayOfWeek).coerceAtLeast(0)
    val cells: List<LocalDate?> = List(leading) { null } + dates
    val counts = tasks.flatMap { item -> listOfNotNull(item.planningDate(zoneId), item.task.deadline).distinct() }.groupingBy { it }.eachCount()
    Card(Modifier.fillMaxWidth().testTag("task-calendar")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                WhipTextButton(onClick = onPrevious) { Text("Previous") }
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontWeight = FontWeight.Bold)
                WhipTextButton(onClick = onNext) { Text("Next") }
            }
            Row(Modifier.fillMaxWidth()) {
                orderedDays.forEach { day ->
                    Text(
                        day.name.take(1),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    (week + List(7 - week.size) { null }).forEach { date ->
                        if (date == null) Spacer(Modifier.weight(1f).height(48.dp))
                        else {
                            val taskCount = counts[date] ?: 0
                            val habitCount = habitCounts[date] ?: 0
                            WhipTextButton(
                                onClick = { onSelect(date) },
                                modifier = Modifier.weight(1f).height(48.dp).semantics {
                                    selected = date == selectedDate
                                    stateDescription = if (date == selectedDate) "Selected" else "Not selected"
                                    contentDescription = buildString {
                                        append(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)))
                                        append(", ${taskCount.itemCount("task")}")
                                        if (habitCount > 0) append(", ${habitCount.itemCount("habit")}")
                                    }
                                },
                            ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = if (date == selectedDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (date == selectedDate) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (taskCount + habitCount > 0) Text(
                                    buildString {
                                        if (taskCount > 0) append("$taskCount T")
                                        if (taskCount > 0 && habitCount > 0) append(" · ")
                                        if (habitCount > 0) append("$habitCount H")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        }
                    }
                }
            }
            Text("Selected: ${selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RoadmapEmptyArea(title: String, message: String, innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeStatusCard(title: String, detail: String, onClick: (() -> Unit)? = null) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClickLabel = "Open $title", onClick = onClick)),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun TodayHeader(
    date: LocalDate,
    completed: Int,
    total: Int,
    onOpenHabits: () -> Unit,
    showFullHeader: Boolean = true,
) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    Column(modifier = Modifier.clickable(onClickLabel = "Open habits", onClick = onOpenHabits).padding(top = 12.dp, bottom = 12.dp)) {
        if (showFullHeader) {
            Text(
                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text("Home", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Habit Progress", style = MaterialTheme.typography.titleSmall)
            Text(
                "$completed of $total",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

private fun taskDestinationSupportingText(destination: TaskDestination, count: Int): String = when (destination) {
    TaskDestination.Inbox -> "Captured and waiting for a decision · ${count.itemCount("task")}"
    TaskDestination.Today -> "What needs your attention now · ${count.itemCount("task")}"
    TaskDestination.Upcoming -> "The next 30 days · ${count.itemCount("task")}"
    TaskDestination.Anytime -> "Unscheduled tasks · ${count.itemCount("task")}"
    TaskDestination.Completed -> "Your latest completed tasks · ${count.itemCount("task")}"
    TaskDestination.Archived -> "Stored safely until you restore them · ${count.itemCount("task")}"
}

private fun Int.itemCount(noun: String): String = "$this $noun${if (this == 1) "" else "s"}"

private fun String.taskDateModeLabel(): String = when (this) {
    "Today" -> "Due Today"
    "Overdue" -> "Overdue"
    "Next7Days" -> "Next 7 Days"
    "NoDate" -> "No Date"
    else -> this
}

@Composable
private fun EmptyTasks(destination: TaskDestination, areaLabel: String? = null, constrained: Boolean = false) {
    val supportingText = if (constrained) {
        "No tasks match the current view or filters. Change the view or remove a filter to see more."
    } else when (destination) {
            TaskDestination.Inbox -> areaLabel?.let { "No Inbox tasks in $it." } ?: "No tasks in Inbox. Quick captures can wait here until you triage them."
            TaskDestination.Today -> areaLabel?.let { "Nothing due today in $it." } ?: "Nothing due today."
            TaskDestination.Upcoming -> areaLabel?.let { "No tasks in $it over the next 30 days." } ?: "No tasks in the next 30 days."
            TaskDestination.Anytime -> areaLabel?.let { "No anytime tasks in $it." } ?: "No anytime tasks yet."
            TaskDestination.Completed -> areaLabel?.let { "No completed tasks in $it." } ?: "Completed tasks will appear here."
            TaskDestination.Archived -> areaLabel?.let { "No archived tasks in $it." } ?: "Archived tasks will appear here and can be restored."
        }
    WhipEmptyState(
        title = if (constrained) "No Matching Tasks" else when (destination) {
            TaskDestination.Today -> "Today Is Clear"
            TaskDestination.Inbox -> "Inbox Is Clear"
            else -> "No ${destination.label} Tasks"
        },
        supportingText = supportingText,
    )
}

internal fun TaskUiState.tasksFor(destination: TaskDestination): List<ScheduledTask> = when (destination) {
    TaskDestination.Inbox -> inbox
    TaskDestination.Today -> today
    TaskDestination.Upcoming -> upcoming
    TaskDestination.Anytime -> anytime
    TaskDestination.Completed -> completed
    TaskDestination.Archived -> archived
}

internal fun List<ScheduledTask>.forPlanningView(
    view: TaskPlanningView,
    selectedDate: LocalDate,
    zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault(),
): List<ScheduledTask> = if (view == TaskPlanningView.Calendar) {
    filter { it.planningDate(zoneId) == selectedDate || it.task.deadline == selectedDate }
} else this

private val TaskDestination.label: String
    get() = when (this) {
        TaskDestination.Inbox -> "Inbox"
        TaskDestination.Today -> "Today"
        TaskDestination.Upcoming -> "Upcoming"
        TaskDestination.Anytime -> "Anytime"
        TaskDestination.Completed -> "Completed"
        TaskDestination.Archived -> "Archived"
    }

private val AppDestination.label: String
    get() = when (this) {
        AppDestination.Home -> "Home"
        AppDestination.Tasks -> "Tasks"
        AppDestination.Habits -> "Habits"
        AppDestination.Gym -> "Gym"
        AppDestination.Goals -> "Goals"
        AppDestination.Settings -> "Settings"
    }

private val AppDestination.icon: ImageVector
    get() = when (this) {
        AppDestination.Home -> Icons.Outlined.Home
        AppDestination.Tasks -> Icons.Outlined.CheckCircle
        AppDestination.Habits -> Icons.Outlined.Autorenew
        AppDestination.Gym -> Icons.Outlined.FitnessCenter
        AppDestination.Goals -> Icons.Outlined.Flag
        AppDestination.Settings -> Icons.Outlined.Settings
    }
