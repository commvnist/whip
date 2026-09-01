package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.PersistenceRequestState
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskEditBoundary
import com.whip.app.domain.TaskQuickCaptureParser
import com.whip.app.domain.toEditBoundary
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope
import com.whip.app.domain.matches
import com.whip.app.domain.scopeForSavedArea
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.unitSymbol
import com.whip.app.domain.periodBounds
import com.whip.app.domain.flexibleProgress
import com.whip.app.domain.hasEnded
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.dayStateOn
import com.whip.app.domain.valueForPeriod
import com.whip.app.core.AppSettings
import com.whip.app.core.HomeSection
import com.whip.app.core.ReviewSection
import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.visibleHomeSections
import com.whip.app.core.openingAreaScope
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.TriggerAction
import com.whip.app.domain.HabitDayProgress
import com.whip.app.core.zoneId
import com.whip.app.core.supportedTrackedRecordTypes
import com.whip.app.core.WhipLaunchActions
import com.whip.app.core.WhipCalendarContext
import com.whip.app.data.TaskBulkEdit
import com.whip.app.data.TaskDeletionBatchImpact
import com.whip.app.data.TaskDeletionImpact
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.whip.app.widget.WhipWidgetProvider

internal enum class AppDestination {
    Home,
    Tasks,
    Habits,
    Goals,
    Gym,
    Tracks,
    Settings,
}

private data class TaskSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val withDismissAction: Boolean,
    override val duration: SnackbarDuration,
    val undoToken: Long?,
    val quickAdd: Boolean,
) : SnackbarVisuals

private data class TransientFeedbackRequest(
    val source: String,
    val priority: Int,
    val recoverable: Boolean,
    val show: suspend () -> Unit,
)

internal fun transientFeedbackSurvivesDestinationChange(recoverable: Boolean): Boolean = recoverable

internal fun SavedTaskFilter.restoredAreaScope(): AreaScope =
    areaId?.let(AreaScope::One) ?: AreaScope.All

internal fun AreaScope.creationDefaultAreaId(areas: List<Area>): String? = when (this) {
    is AreaScope.One -> areaId
    AreaScope.All -> areas.filterNot(Area::archived).singleOrNull()?.id
    AreaScope.Unassigned -> null
}

internal fun AreaScope.requiresExplicitCreationArea(areas: List<Area>): Boolean =
    this == AreaScope.All && areas.count { !it.archived } > 1

internal fun shouldShowHomeGettingStarted(hasAnyUserData: Boolean): Boolean = !hasAnyUserData

internal sealed interface LaunchTargetResolution {
    data object NotApplicable : LaunchTargetResolution
    data object Pending : LaunchTargetResolution
    data class LoadFailed(val destination: AppDestination) : LaunchTargetResolution
    data class Available(val areaId: String?, val unavailableDetail: String? = null) : LaunchTargetResolution
    data class Unavailable(val destination: AppDestination, val message: String) : LaunchTargetResolution
}

internal fun resolveLaunchTarget(
    action: String?,
    entityId: Long?,
    occurrenceEpochDay: Long?,
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    trackState: TrackUiState,
): LaunchTargetResolution {
    val destination = when (action) {
        WhipLaunchActions.ACTION_OPEN_TASK -> AppDestination.Tasks
        WhipLaunchActions.ACTION_OPEN_HABIT -> AppDestination.Habits
        WhipLaunchActions.ACTION_OPEN_GOAL -> AppDestination.Goals
        WhipLaunchActions.ACTION_OPEN_TRACK -> AppDestination.Tracks
        else -> return LaunchTargetResolution.NotApplicable
    }
    val label = destination.label.removeSuffix("s")
    val id = entityId ?: return LaunchTargetResolution.Unavailable(destination, "This $label is no longer available.")
    return when (destination) {
        AppDestination.Tasks -> when {
            taskState.loading -> LaunchTargetResolution.Pending
            taskState.errorMessage != null -> LaunchTargetResolution.LoadFailed(destination)
            else -> {
                val item = (taskState.inbox + taskState.today + taskState.upcoming + taskState.planning + taskState.completed + taskState.archived)
                    .firstOrNull { scheduled ->
                        scheduled.task.id == id && (
                            occurrenceEpochDay == null || scheduled.originalDate?.toEpochDay() == occurrenceEpochDay
                            )
                    }
                item?.let { LaunchTargetResolution.Available(it.task.areaId) }
                    ?: taskState.taskEntities.firstOrNull { it.id == id }?.let { task ->
                        LaunchTargetResolution.Available(
                            task.areaId,
                            "This Task occurrence is no longer available. Showing Tasks instead.",
                        )
                    }
                    ?: LaunchTargetResolution.Unavailable(destination, "This Task is no longer available.")
            }
        }
        AppDestination.Habits -> when {
            habitState.loading -> LaunchTargetResolution.Pending
            habitState.errorMessage != null -> LaunchTargetResolution.LoadFailed(destination)
            else -> ((habitState.today + habitState.all).firstOrNull { it.habit.id == id }?.habit?.areaId
                ?: habitState.archived.firstOrNull { it.id == id }?.areaId)
                .let { areaId ->
                    val exists = (habitState.today + habitState.all).any { it.habit.id == id } || habitState.archived.any { it.id == id }
                    if (exists) LaunchTargetResolution.Available(areaId)
                    else LaunchTargetResolution.Unavailable(destination, "This Habit is no longer available.")
                }
        }
        AppDestination.Goals -> when {
            goalState.loading -> LaunchTargetResolution.Pending
            goalState.errorMessage != null -> LaunchTargetResolution.LoadFailed(destination)
            else -> (goalState.active + goalState.completed + goalState.archived)
                .firstOrNull { it.goal.id == id }
                ?.let { LaunchTargetResolution.Available(it.goal.areaId) }
                ?: LaunchTargetResolution.Unavailable(destination, "This Goal is no longer available.")
        }
        AppDestination.Tracks -> when {
            trackState.loading -> LaunchTargetResolution.Pending
            trackState.errorMessage != null -> LaunchTargetResolution.LoadFailed(destination)
            else -> trackState.track(id)?.let { projection ->
                LaunchTargetResolution.Available(projection.track.areaId)
            } ?: LaunchTargetResolution.Unavailable(destination, "This Track is no longer available.")
        }
        AppDestination.Home, AppDestination.Gym, AppDestination.Settings -> LaunchTargetResolution.NotApplicable
    }
}

internal fun homeEmptyStateEligible(
    visibleSections: Collection<HomeSection>,
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    trackState: TrackUiState,
    gymState: GymUiState,
): Boolean = visibleSections.all { section ->
    when (section) {
        HomeSection.Tasks -> !taskState.loading && taskState.errorMessage == null
        HomeSection.Habits -> !habitState.loading && habitState.errorMessage == null
        HomeSection.Goals -> !goalState.loading && goalState.errorMessage == null
        HomeSection.Tracks -> !trackState.loading && trackState.errorMessage == null
        HomeSection.Gym -> !gymState.loading && gymState.errorMessage == null
    }
}

internal fun homeHasAnyUserData(
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    trackState: TrackUiState,
    gymState: GymUiState,
): Boolean =
    (taskState.inbox + taskState.today + taskState.upcoming + taskState.planning +
        taskState.completed + taskState.archived).isNotEmpty() ||
        habitState.all.isNotEmpty() || habitState.today.isNotEmpty() || habitState.archived.isNotEmpty() ||
        habitState.logs.isNotEmpty() ||
        (goalState.active + goalState.completed + goalState.archived).isNotEmpty() ||
        trackState.projections.isNotEmpty() ||
        gymState.activeSession != null || gymState.allSessions.isNotEmpty() ||
        gymState.exercises.isNotEmpty() || gymState.archivedExercises.isNotEmpty() ||
        gymState.machines.isNotEmpty() || gymState.archivedMachines.isNotEmpty() ||
        gymState.routines.isNotEmpty() || gymState.archivedRoutines.isNotEmpty()

data class DomainRetryActions(
    val tasks: () -> Unit = {},
    val habits: () -> Unit = {},
    val goals: () -> Unit = {},
    val tracks: () -> Unit = {},
    val gym: () -> Unit = {},
)

/**
 * Keeps platform-entry delivery policy out of the already broad app shell.
 * Besides keeping the effect testable, this prevents Compose/Jacoco from
 * folding another complete navigation state machine into WhipScreen's method.
 */
private sealed interface LaunchDeliveryCommand {
    data class LoadFailed(val destination: AppDestination) : LaunchDeliveryCommand
    data class Unavailable(val destination: AppDestination, val message: String) : LaunchDeliveryCommand
    data object OpenTaskAgenda : LaunchDeliveryCommand
    data object OpenHabitTracking : LaunchDeliveryCommand
    data class AddTask(val date: LocalDate?) : LaunchDeliveryCommand
    data class CaptureSharedTask(val text: String) : LaunchDeliveryCommand
    data object AddHabit : LaunchDeliveryCommand
    data class OpenTask(
        val item: ScheduledTask,
        val completed: Boolean,
        val destination: TaskDestination?,
    ) : LaunchDeliveryCommand
    data class OpenTaskFallback(val message: String) : LaunchDeliveryCommand
    data class OpenHabit(val id: Long) : LaunchDeliveryCommand
    data class OpenGoal(val id: Long) : LaunchDeliveryCommand
    data object OpenGym : LaunchDeliveryCommand
    data class OpenTrack(val id: Long) : LaunchDeliveryCommand
}

@Composable
private fun LaunchDeliveryEffect(
    launchDeliveryId: Long,
    consumedLaunchDeliveryId: Long?,
    areaSelectionReady: Boolean,
    initialAction: String?,
    initialEntityId: Long?,
    initialOccurrenceEpochDay: Long?,
    initialSharedText: String?,
    setupCompleted: Boolean,
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    trackState: TrackUiState,
    onConsume: (Long) -> Unit,
    onCommand: (LaunchDeliveryCommand) -> Unit,
) {
    LaunchedEffect(
        launchDeliveryId,
        initialAction,
        initialEntityId,
        initialOccurrenceEpochDay,
        initialSharedText,
        setupCompleted,
        areaSelectionReady,
        taskState,
        habitState,
        goalState,
        trackState,
    ) {
        if (
            launchDeliveryId == 0L ||
            consumedLaunchDeliveryId == launchDeliveryId ||
            !setupCompleted ||
            !areaSelectionReady
        ) {
            return@LaunchedEffect
        }
        val targetResolution = resolveLaunchTarget(
            action = initialAction,
            entityId = initialEntityId,
            occurrenceEpochDay = initialOccurrenceEpochDay,
            taskState = taskState,
            habitState = habitState,
            goalState = goalState,
            trackState = trackState,
        )
        when (targetResolution) {
            LaunchTargetResolution.Pending -> return@LaunchedEffect
            is LaunchTargetResolution.LoadFailed -> {
                onCommand(LaunchDeliveryCommand.LoadFailed(targetResolution.destination))
                return@LaunchedEffect
            }
            is LaunchTargetResolution.Unavailable -> {
                onConsume(launchDeliveryId)
                onCommand(
                    LaunchDeliveryCommand.Unavailable(
                        targetResolution.destination,
                        targetResolution.message,
                    ),
                )
                return@LaunchedEffect
            }
            LaunchTargetResolution.NotApplicable,
            is LaunchTargetResolution.Available -> Unit
        }
        when (initialAction) {
            WhipWidgetProvider.ACTION_OPEN_TASK_AGENDA -> onCommand(LaunchDeliveryCommand.OpenTaskAgenda)
            WhipWidgetProvider.ACTION_OPEN_HABIT_TRACKING -> onCommand(LaunchDeliveryCommand.OpenHabitTracking)
            WhipWidgetProvider.ACTION_ADD_TASK -> onCommand(
                LaunchDeliveryCommand.AddTask(initialOccurrenceEpochDay?.let(LocalDate::ofEpochDay)),
            )
            WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK -> onCommand(
                LaunchDeliveryCommand.CaptureSharedTask(initialSharedText.orEmpty()),
            )
            WhipWidgetProvider.ACTION_ADD_HABIT -> onCommand(LaunchDeliveryCommand.AddHabit)
            WhipLaunchActions.ACTION_OPEN_TASK -> {
                val id = initialEntityId ?: return@LaunchedEffect
                val allTasks = taskState.inbox + taskState.today + taskState.upcoming +
                    taskState.planning + taskState.completed + taskState.archived
                val found = allTasks.firstOrNull { item ->
                    item.task.id == id && (
                        initialOccurrenceEpochDay == null ||
                            item.originalDate?.toEpochDay() == initialOccurrenceEpochDay
                        )
                }
                if (found == null) {
                    val detail = (targetResolution as? LaunchTargetResolution.Available)?.unavailableDetail
                    if (detail != null) onCommand(LaunchDeliveryCommand.OpenTaskFallback(detail))
                    onConsume(launchDeliveryId)
                    return@LaunchedEffect
                }
                val destination = when (found) {
                    in taskState.inbox -> TaskDestination.Inbox
                    in taskState.planning -> TaskDestination.Upcoming
                    else -> null
                }
                onCommand(
                    LaunchDeliveryCommand.OpenTask(
                        found,
                        found in taskState.completed,
                        destination,
                    ),
                )
            }
            WhipLaunchActions.ACTION_OPEN_HABIT -> initialEntityId?.let {
                onCommand(LaunchDeliveryCommand.OpenHabit(it))
            }
            WhipLaunchActions.ACTION_OPEN_GOAL -> initialEntityId?.let {
                onCommand(LaunchDeliveryCommand.OpenGoal(it))
            }
            WhipLaunchActions.ACTION_OPEN_GYM -> onCommand(LaunchDeliveryCommand.OpenGym)
            WhipLaunchActions.ACTION_OPEN_TRACK -> initialEntityId?.let { trackId ->
                onCommand(LaunchDeliveryCommand.OpenTrack(trackId))
            }
        }
        onConsume(launchDeliveryId)
    }
}

/** Keeps every pinned item visible while retaining the compact default summary. */
internal fun <T> pinnedHomeSummary(
    items: List<T>,
    limit: Int,
    isPinned: (T) -> Boolean,
): List<T> {
    val pinned = items.filter(isPinned)
    val remainingSlots = (limit - pinned.size).coerceAtLeast(0)
    return pinned + items.filterNot(isPinned).take(remainingSlots)
}

internal fun gymHomeItemCount(hasActiveSession: Boolean, pinnedRoutineCount: Int): Int =
    if (hasActiveSession) 1 else pinnedRoutineCount

@Composable
internal fun UserDataGenerationBoundary(
    generation: Long,
    content: @Composable () -> Unit,
) {
    key(generation) { content() }
}

internal fun globalAddAvailable(
    appDestination: AppDestination,
    gymDestination: GymDestination,
    gymRoutineEditorOpen: Boolean,
    taskSelectionMode: Boolean,
    selectedTrackArchived: Boolean = false,
): Boolean =
    !gymRoutineEditorOpen &&
        appDestination != AppDestination.Settings &&
        !(appDestination == AppDestination.Gym && gymDestination == GymDestination.Tools) &&
        !(appDestination == AppDestination.Tasks && taskSelectionMode) &&
        !(appDestination == AppDestination.Tracks && selectedTrackArchived)

private val primaryAppDestinations = listOf(
    AppDestination.Tasks,
    AppDestination.Habits,
    AppDestination.Goals,
    AppDestination.Tracks,
    AppDestination.Gym,
)

/** Prevents Home and cross-domain surfaces from rendering mixed logical-day projections. */
private data class CalendarCoherentUiState(
    val calendar: WhipCalendarContext,
    val tasks: TaskUiState,
    val habits: HabitUiState,
    val goals: GoalUiState,
    val tracks: TrackUiState,
)

internal fun calendarProjectionsAreCoherent(logicalDate: LocalDate, projectionDates: List<LocalDate>): Boolean =
    projectionDates.all { it == logicalDate }

@Composable
fun WhipApp(
    modifier: Modifier = Modifier,
    initialAction: String? = null,
    initialEntityId: Long? = null,
    initialOccurrenceEpochDay: Long? = null,
    initialSharedText: String? = null,
    initialAreaScopeStorageKey: String? = null,
    initialDeliveryId: Long = 0L,
    foldInfo: WhipFoldInfo? = null,
    onRequestNotificationPermission: () -> Unit = {},
    taskViewModel: TaskViewModel = viewModel(),
    gymViewModel: GymViewModel = viewModel(),
    habitViewModel: HabitViewModel = viewModel(),
    goalViewModel: GoalViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    trackViewModel: TrackViewModel = viewModel(),
) {
    val app = LocalContext.current.applicationContext as WhipApplication
    val observedCalendarContext by app.calendarContext.collectAsStateWithLifecycle()
    val userDataGeneration by app.userDataGeneration.collectAsStateWithLifecycle()
    val observedTaskState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val gymState by gymViewModel.uiState.collectAsStateWithLifecycle()
    val observedHabitState by habitViewModel.uiState.collectAsStateWithLifecycle()
    val observedGoalState by goalViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val observedTrackState by trackViewModel.uiState.collectAsStateWithLifecycle()
    var coherentCalendarState by remember {
        mutableStateOf(
            CalendarCoherentUiState(
                observedCalendarContext,
                observedTaskState,
                observedHabitState,
                observedGoalState,
                observedTrackState,
            ),
        )
    }
    val observedDates = listOf(
        observedTaskState.currentDate,
        observedHabitState.currentDate,
        observedGoalState.currentDate,
        observedTrackState.currentDate,
    )
    if (calendarProjectionsAreCoherent(observedCalendarContext.logicalDate, observedDates)) {
        SideEffect {
            coherentCalendarState = CalendarCoherentUiState(
                observedCalendarContext,
                observedTaskState,
                observedHabitState,
                observedGoalState,
                observedTrackState,
            )
        }
    }
    val calendarContext = coherentCalendarState.calendar
    val state = coherentCalendarState.tasks
    val habitState = coherentCalendarState.habits
    val goalState = coherentCalendarState.goals
    val trackState = coherentCalendarState.tracks
    val taskOperationFeedback by taskViewModel.operationFeedback.collectAsStateWithLifecycle()
    val taskEditorSaveState by taskViewModel.editorSaveState.collectAsStateWithLifecycle()
    val taskAuthoredMutationState by taskViewModel.authoredMutationState.collectAsStateWithLifecycle()
    val operationStatus = taskOperationFeedback.status
    val taskDeletionImpact by taskViewModel.taskDeletionImpact.collectAsStateWithLifecycle()
    val taskDeletionBatchImpact by taskViewModel.taskDeletionBatchImpact.collectAsStateWithLifecycle()
    val pendingTaskUndoMessage = taskOperationFeedback.undoMessage
    val pendingTaskUndoToken = taskOperationFeedback.undoToken
    val pendingQuickAddTaskId = taskOperationFeedback.quickAddedTaskId
    val gymOperationStatus by gymViewModel.operationStatus.collectAsStateWithLifecycle()
    val pendingMachineArchiveUndo by gymViewModel.pendingMachineArchiveUndo.collectAsStateWithLifecycle()
    val habitOperationStatus by habitViewModel.operationStatus.collectAsStateWithLifecycle()
    val goalOperationStatus by goalViewModel.operationStatus.collectAsStateWithLifecycle()
    val trackOperationStatus by trackViewModel.operationStatus.collectAsStateWithLifecycle()
    val lastDeletedTrackEntry by trackViewModel.lastDeletedEntry.collectAsStateWithLifecycle()
    var sessionAreaScopeKey by rememberSaveable {
        mutableStateOf(settingsState.settings.openingAreaScope().storageKey)
    }
    var transientAreaScopeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var transientAreaScopeDelivery by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingAreaBadgeId by rememberSaveable { mutableStateOf<String?>(null) }
    val sessionAreaScope = AreaScope.fromStorageKey(sessionAreaScopeKey)
    val areaScope = transientAreaScopeKey?.let(AreaScope::fromStorageKey) ?: sessionAreaScope
    val requestedLaunchDeliveryId = initialDeliveryId.takeIf { it != 0L }
        ?: 1L.takeIf { initialAction != null }
        ?: 0L

    LaunchedEffect(
        settingsState.taxonomyLoaded,
        settingsState.settings.activeAreaScope,
        settingsState.settings.chosenOpeningAreaScope,
        settingsState.areas,
        sessionAreaScopeKey,
    ) {
        if (!settingsState.taxonomyLoaded) return@LaunchedEffect
        val storedLastUsed = AreaScope.fromStorageKey(settingsState.settings.activeAreaScope)
        val storedChosen = AreaScope.fromStorageKey(settingsState.settings.chosenOpeningAreaScope)
        // A freshly created Area is committed before Room's Area flow emits it.
        // Give that emission a chance to cancel/restart this effect so valid new
        // scopes are not mistaken for stale IDs.
        if (
            listOf(storedLastUsed, storedChosen, AreaScope.fromStorageKey(sessionAreaScopeKey))
                .filterIsInstance<AreaScope.One>()
                .any { scope -> settingsState.areas.none { it.id == scope.areaId } }
        ) {
            kotlinx.coroutines.delay(500)
        }
        val validLastUsed = storedLastUsed.validFor(settingsState.areas)
        val validChosen = storedChosen.validFor(settingsState.areas)
        if (validLastUsed != storedLastUsed || validChosen != storedChosen) {
            settingsViewModel.update { current ->
                current.copy(
                    activeAreaScope = validLastUsed.storageKey,
                    chosenOpeningAreaScope = validChosen.storageKey,
                )
            }
        }
        val validSession = AreaScope.fromStorageKey(sessionAreaScopeKey).validFor(settingsState.areas)
        if (validSession.storageKey != sessionAreaScopeKey) sessionAreaScopeKey = validSession.storageKey
    }

    LaunchedEffect(
        requestedLaunchDeliveryId,
        initialAction,
        initialEntityId,
        initialOccurrenceEpochDay,
        initialAreaScopeStorageKey,
        settingsState.settings.setupCompleted,
        settingsState.taxonomyLoaded,
        settingsState.areas,
        sessionAreaScopeKey,
        state,
        habitState,
        goalState,
        trackState,
    ) {
        if (requestedLaunchDeliveryId == 0L) return@LaunchedEffect
        if (transientAreaScopeDelivery == requestedLaunchDeliveryId) return@LaunchedEffect
        if (!settingsState.settings.setupCompleted) return@LaunchedEffect
        if (!settingsState.taxonomyLoaded) return@LaunchedEffect
        if (initialAreaScopeStorageKey != null) {
            val requestedScope = AreaScope.fromStorageKey(initialAreaScopeStorageKey)
                .validFor(settingsState.areas)
            sessionAreaScopeKey = requestedScope.storageKey
            transientAreaScopeKey = null
            settingsViewModel.setAreaScope(requestedScope)
            transientAreaScopeDelivery = requestedLaunchDeliveryId
            return@LaunchedEffect
        }
        val resolution = resolveLaunchTarget(
            action = initialAction,
            entityId = initialEntityId,
            occurrenceEpochDay = initialOccurrenceEpochDay,
            taskState = state,
            habitState = habitState,
            goalState = goalState,
            trackState = trackState,
        )
        val targetAreaId = when (resolution) {
            is LaunchTargetResolution.Available -> resolution.areaId
            is LaunchTargetResolution.Unavailable -> {
                transientAreaScopeDelivery = requestedLaunchDeliveryId
                return@LaunchedEffect
            }
            LaunchTargetResolution.NotApplicable -> {
                // Commands that create content or open a workspace do not resolve an existing
                // entity area, but they still need to release the launch-delivery gate.
                transientAreaScopeDelivery = requestedLaunchDeliveryId
                return@LaunchedEffect
            }
            LaunchTargetResolution.Pending,
            is LaunchTargetResolution.LoadFailed -> return@LaunchedEffect
        }
        if (!sessionAreaScope.matches(targetAreaId)) {
            val requestedScope =
                targetAreaId?.let(AreaScope::One)
                    ?: settingsState.areas.firstOrNull { !it.archived }?.id?.let(AreaScope::One)
                    ?: AreaScope.All
            sessionAreaScopeKey = requestedScope.storageKey
            settingsViewModel.setAreaScope(requestedScope)
        }
        transientAreaScopeKey = null
        transientAreaScopeDelivery = requestedLaunchDeliveryId
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val adaptiveLayout = selectWhipAdaptiveLayout(maxWidth.value.toInt(), maxHeight.value.toInt(), foldInfo)
        val rootLayoutDirection = LocalLayoutDirection.current
        val rootDensity = LocalDensity.current
        val dialogPlacement = if (
            adaptiveLayout == WhipAdaptiveLayout.BookFold &&
            foldInfo?.orientation == WhipFoldOrientation.Vertical
        ) {
            val rawPaneWidth = with(rootDensity) { foldInfo.leftPx.toDp() }
            val largestPaneWidth = (maxWidth - 300.dp).coerceAtLeast(260.dp)
            val paneWidth = rawPaneWidth.coerceIn(260.dp, largestPaneWidth)
            val hingeWidth = with(rootDensity) { foldInfo.widthPx.toDp() }.coerceAtLeast(1.dp)
            val supportExtent = paneWidth + hingeWidth
            val contentWidth = (maxWidth - supportExtent).coerceAtLeast(280.dp)
            WhipDialogPlacement(
                offsetX = if (rootLayoutDirection == LayoutDirection.Ltr) supportExtent / 2 else -supportExtent / 2,
                maxWidth = minOf(contentWidth * 0.94f, 560.dp),
            )
        } else WhipDialogPlacement(maxWidth = minOf(maxWidth * 0.94f, 560.dp))
        val compactItemExpansionState = rememberCompactItemExpansionState()
        CompositionLocalProvider(
            LocalAreaUiContext provides AreaUiContext(
                areas = settingsState.areas,
                onSelectScope = { pendingAreaBadgeId = it },
            ),
            LocalWhipFirstDayOfWeek provides settingsState.settings.firstDayOfWeek,
            LocalWhipToday provides calendarContext.logicalDate,
            LocalWhipZone provides calendarContext.zoneId,
            LocalWhipDialogPlacement provides dialogPlacement,
            LocalCompactItemLayout provides settingsState.settings.compactItemLayout,
            LocalCompactItemExpansionState provides compactItemExpansionState,
        ) {
            UserDataGenerationBoundary(userDataGeneration) {
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
                    trackState = when (areaScope) {
                        AreaScope.All -> trackState
                        AreaScope.Unassigned -> trackState
                        is AreaScope.One -> trackState.forArea(areaScope.areaId)
                    },
                    unscopedTrackState = trackState,
                    trackViewModel = trackViewModel,
                    userDataGeneration = userDataGeneration,
                    areaScope = areaScope,
                    onSelectAreaScope = { selected ->
                        sessionAreaScopeKey = selected.storageKey
                        transientAreaScopeKey = null
                        settingsViewModel.setAreaScope(selected)
                    },
                    onTemporarilySelectAreaScope = { selected ->
                        transientAreaScopeKey = selected
                            .takeUnless { it.hasSameVisibleAreaAs(sessionAreaScope, settingsState.areas) }
                            ?.storageKey
                    },
                    transientAreaScope = transientAreaScopeKey != null,
                    onRestoreAreaScope = { transientAreaScopeKey = null },
                    pendingAreaBadgeId = pendingAreaBadgeId,
                    onAreaBadgeConsumed = { pendingAreaBadgeId = null },
                    modifier = Modifier.fillMaxSize(),
                    adaptiveLayout = adaptiveLayout,
                    foldInfo = foldInfo,
                    operationStatus = operationStatus,
                    taskEditorSaveState = taskEditorSaveState,
                    onTaskEditorSaveResultConsumed = taskViewModel::consumeEditorSaveResult,
                    taskAuthoredMutationState = taskAuthoredMutationState,
                    onTaskAuthoredMutationResultConsumed = taskViewModel::consumeAuthoredMutationResult,
                    onOperationStatusConsumed = taskViewModel::consumeOperationStatus,
                    taskUndoMessage = pendingTaskUndoMessage,
                    taskUndoToken = pendingTaskUndoToken,
                    quickAddedTaskId = pendingQuickAddTaskId,
                    onTaskUndo = { token -> taskViewModel.undoLastTaskAction(token) },
                    onTaskUndoDismissed = { token -> taskViewModel.clearPendingUndo(token) },
                    gymOperationStatus = gymOperationStatus,
                    onGymOperationStatusConsumed = gymViewModel::consumeOperationStatus,
                    machineArchiveUndoId = pendingMachineArchiveUndo,
                    onMachineArchiveUndo = { id -> gymViewModel.undoLastMachineArchive(id) },
                    onMachineArchiveUndoDismissed = { id -> gymViewModel.clearPendingMachineArchiveUndo(id) },
                    habitOperationStatus = habitOperationStatus,
                    onHabitOperationStatusConsumed = habitViewModel::consumeOperationStatus,
                    goalOperationStatus = goalOperationStatus,
                    onGoalOperationStatusConsumed = goalViewModel::consumeOperationStatus,
                    trackOperationStatus = trackOperationStatus,
                    onTrackOperationStatusConsumed = trackViewModel::consumeOperationStatus,
                    trackEntryUndoId = lastDeletedTrackEntry?.token,
                    onTrackEntryUndo = { id -> trackViewModel.undoEntryDeletion(id) },
                    onTrackEntryUndoDismissed = { id -> trackViewModel.clearEntryUndo(id) },
                    domainRetryActions = DomainRetryActions(
                        tasks = taskViewModel::retryLoading,
                        habits = habitViewModel::retryLoading,
                        goals = goalViewModel::retryLoading,
                        tracks = trackViewModel::retryLoading,
                        gym = gymViewModel::retryLoading,
                    ),
                    onSaveTask = { id, draft, from -> taskViewModel.saveTask(id, draft, from) },
                    onSaveTaskRequest = { taskId, expected, draft, from, requestId ->
                        taskViewModel.saveTask(
                            taskId,
                            draft,
                            from,
                            requestId = requestId,
                            expectedBoundary = expected,
                        )
                    },
                    onQuickAddTask = { capture, date, areaId ->
                        taskViewModel.quickAddTask(capture, date, areaId)
                    },
                    onQuickAddTaskWithResult = { capture, date, areaId, onFinished ->
                        taskViewModel.quickAddTask(capture, date, areaId, onFinished)
                    },
                    onQuickAddTaskRequest = taskViewModel::quickAddTaskRequest,
                    onComplete = taskViewModel::complete,
                    onSkip = taskViewModel::skip,
                    onReschedule = taskViewModel::reschedule,
                    onRescheduleRequest = taskViewModel::rescheduleRequest,
                    onSetStepCompleted = taskViewModel::setStepCompleted,
                    onPromoteStep = taskViewModel::promoteStep,
                    onArchive = taskViewModel::archive,
                    onRestore = taskViewModel::restore,
                    onDeleteTaskPermanently = taskViewModel::deletePermanently,
                    onDeleteTaskPermanentlyRequest = taskViewModel::deletePermanentlyRequest,
                    taskDeletionImpact = taskDeletionImpact,
                    onPreviewTaskDeletion = taskViewModel::previewPermanentDeletion,
                    onClearTaskDeletionPreview = taskViewModel::clearPermanentDeletionPreview,
                    onReopen = taskViewModel::reopen,
                    onReopenOccurrence = taskViewModel::reopenOccurrence,
                    onResetOccurrence = taskViewModel::resetOccurrence,
                    onSetTaskPinned = taskViewModel::setPinned,
                    onBulkCompleteTasks = taskViewModel::completeAll,
                    onBulkArchiveTasks = taskViewModel::archiveAllRequest,
                    onBulkRestoreTasks = taskViewModel::restoreAll,
                    onBulkReopenTasks = taskViewModel::reopenAll,
                    onBulkPinTasks = taskViewModel::pinAll,
                    onBulkPostponeTasks = taskViewModel::postponeAll,
                    onBulkPostponeTasksRequest = taskViewModel::postponeAllRequest,
                    onBulkEditTasks = taskViewModel::editAll,
                    onBulkEditTasksRequest = taskViewModel::editAllRequest,
                    onBulkDeleteTasksPermanently = taskViewModel::deleteAllPermanently,
                    onBulkDeleteTasksPermanentlyRequest = taskViewModel::deleteAllPermanentlyRequest,
                    taskDeletionBatchImpact = taskDeletionBatchImpact,
                    onPreviewBulkTaskDeletion = taskViewModel::previewPermanentDeletions,
                    onClearBulkTaskDeletionPreview = taskViewModel::clearPermanentDeletionBatchPreview,
                    onReorderTasks = taskViewModel::reorder,
                    onPlanMyDay = taskViewModel::planMyDay,
                    onDuplicateTask = taskViewModel::duplicate,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    initialAction = initialAction,
                    initialEntityId = initialEntityId,
                    initialOccurrenceEpochDay = initialOccurrenceEpochDay,
                    initialSharedText = initialSharedText,
                    initialDeliveryId = initialDeliveryId,
                    launchAreaSelectionReady = requestedLaunchDeliveryId == 0L ||
                        transientAreaScopeDelivery == requestedLaunchDeliveryId,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhipScreen(
    state: TaskUiState,
    modifier: Modifier = Modifier,
    unscopedTaskState: TaskUiState = state,
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
    trackState: TrackUiState = TrackUiState(),
    unscopedTrackState: TrackUiState = trackState,
    trackViewModel: TrackViewModel? = null,
    userDataGeneration: Long = 0L,
    areaScope: AreaScope = AreaScope.fromStorageKey(settingsState.settings.activeAreaScope),
    onSelectAreaScope: (AreaScope) -> Unit = { settingsViewModel?.setAreaScope(it) },
    onTemporarilySelectAreaScope: (AreaScope) -> Unit = {},
    transientAreaScope: Boolean = false,
    onRestoreAreaScope: () -> Unit = {},
    pendingAreaBadgeId: String? = null,
    onAreaBadgeConsumed: () -> Unit = {},
    operationStatus: OperationStatus = OperationStatus.Idle,
    taskEditorSaveState: PersistenceRequestState<EntitySaveReceipt> = PersistenceRequestState.Idle,
    onTaskEditorSaveResultConsumed: (String) -> Unit = {},
    taskAuthoredMutationState: PersistenceRequestState<TaskMutationReceipt> = PersistenceRequestState.Idle,
    onTaskAuthoredMutationResultConsumed: (String) -> Unit = {},
    onOperationStatusConsumed: () -> Unit = {},
    taskUndoMessage: String? = null,
    taskUndoToken: Long? = null,
    quickAddedTaskId: Long? = null,
    onTaskUndo: (Long) -> Unit = {},
    onTaskUndoDismissed: (Long) -> Unit = {},
    gymOperationStatus: OperationStatus = OperationStatus.Idle,
    onGymOperationStatusConsumed: () -> Unit = {},
    machineArchiveUndoId: Long? = null,
    onMachineArchiveUndo: (Long) -> Unit = {},
    onMachineArchiveUndoDismissed: (Long) -> Unit = {},
    habitOperationStatus: OperationStatus = OperationStatus.Idle,
    onHabitOperationStatusConsumed: () -> Unit = {},
    goalOperationStatus: OperationStatus = OperationStatus.Idle,
    onGoalOperationStatusConsumed: () -> Unit = {},
    trackOperationStatus: OperationStatus = OperationStatus.Idle,
    onTrackOperationStatusConsumed: () -> Unit = {},
    trackEntryUndoId: Long? = null,
    onTrackEntryUndo: (Long) -> Unit = {},
    onTrackEntryUndoDismissed: (Long) -> Unit = {},
    domainRetryActions: DomainRetryActions = DomainRetryActions(),
    onSaveTask: (Long?, TaskDraft, LocalDate?) -> Unit,
    onSaveTaskRequest: (Long?, TaskEditBoundary?, TaskDraft, LocalDate?, String) -> Boolean = { _, _, _, _, _ -> false },
    onQuickAddTask: (String, LocalDate?, String?) -> Unit = { _, _, _ -> },
    onQuickAddTaskWithResult: (String, LocalDate?, String?, (Boolean) -> Unit) -> Unit = { capture, date, areaId, onFinished ->
        onQuickAddTask(capture, date, areaId)
        onFinished(true)
    },
    onQuickAddTaskRequest: ((String, LocalDate?, String?, String) -> Boolean)? = null,
    onComplete: (ScheduledTask) -> Unit,
    onSkip: (ScheduledTask) -> Unit,
    onReschedule: (ScheduledTask, LocalDate) -> Unit,
    onRescheduleRequest: (ScheduledTask, LocalDate, String) -> Boolean = { item, date, _ ->
        onReschedule(item, date)
        true
    },
    onSetStepCompleted: (ScheduledTask, Long, Boolean) -> Unit = { _, _, _ -> },
    onPromoteStep: (ScheduledTask, Long) -> Unit = { _, _ -> },
    onArchive: (Long) -> Unit,
    onRestore: (Long) -> Unit = {},
    onDeleteTaskPermanently: (Long) -> Unit = {},
    onDeleteTaskPermanentlyRequest: (Long, String, String) -> Boolean = { taskId, _, _ ->
        onDeleteTaskPermanently(taskId)
        true
    },
    taskDeletionImpact: com.whip.app.data.TaskDeletionImpact? = null,
    onPreviewTaskDeletion: (Long) -> Unit = {},
    onClearTaskDeletionPreview: () -> Unit = {},
    onReopen: (ScheduledTask) -> Unit,
    onReopenOccurrence: (ScheduledTask) -> Unit = {},
    onResetOccurrence: (ScheduledTask) -> Unit = {},
    onSetTaskPinned: (Long, Boolean) -> Unit = { _, _ -> },
    onBulkCompleteTasks: (List<ScheduledTask>) -> Unit = {},
    onBulkArchiveTasks: (List<ScheduledTask>, String) -> Boolean = { _, _ -> false },
    onBulkRestoreTasks: (List<ScheduledTask>) -> Unit = {},
    onBulkReopenTasks: (List<ScheduledTask>) -> Unit = {},
    onBulkPinTasks: (List<ScheduledTask>, Boolean) -> Unit = { _, _ -> },
    onBulkPostponeTasks: (List<ScheduledTask>, LocalDate) -> Unit = { _, _ -> },
    onBulkPostponeTasksRequest: (List<ScheduledTask>, LocalDate, String) -> Boolean = { items, date, _ ->
        onBulkPostponeTasks(items, date)
        true
    },
    onBulkEditTasks: (List<ScheduledTask>, TaskBulkEdit) -> Unit = { _, _ -> },
    onBulkEditTasksRequest: (List<ScheduledTask>, TaskBulkEdit, String) -> Boolean = { items, edit, _ ->
        onBulkEditTasks(items, edit)
        true
    },
    onBulkDeleteTasksPermanently: (Set<Long>) -> Unit = {},
    onBulkDeleteTasksPermanentlyRequest: (Set<Long>, Map<Long, String>, String) -> Boolean = { ids, _, _ ->
        onBulkDeleteTasksPermanently(ids)
        true
    },
    taskDeletionBatchImpact: TaskDeletionBatchImpact? = null,
    onPreviewBulkTaskDeletion: (Set<Long>) -> Unit = {},
    onClearBulkTaskDeletionPreview: () -> Unit = {},
    onReorderTasks: (List<ScheduledTask>) -> Unit = {},
    onPlanMyDay: (List<ScheduledTask>, Int) -> Unit = { _, _ -> },
    onDuplicateTask: (Long) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    initialAction: String? = null,
    initialEntityId: Long? = null,
    initialOccurrenceEpochDay: Long? = null,
    initialSharedText: String? = null,
    initialDeliveryId: Long = 0L,
    launchAreaSelectionReady: Boolean = true,
    adaptiveLayout: WhipAdaptiveLayout = WhipAdaptiveLayout.Compact,
    foldInfo: WhipFoldInfo? = null,
) {
    val compactItemExpansionState = LocalCompactItemExpansionState.current
    var appDestination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var settingsCallerDestination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var taskDestination by rememberSaveable { mutableStateOf(TaskDestination.Today) }
    val habitDestinationState: MutableState<HabitDestination> = rememberSaveable {
        mutableStateOf(HabitDestination.Today)
    }
    val goalDestinationState: MutableState<GoalDestination> = rememberSaveable {
        mutableStateOf(GoalDestination.Active)
    }
    var taskPlanningViewRequest by rememberSaveable { mutableStateOf<TaskPlanningView?>(null) }
    var taskEditorOpen by rememberSaveable { mutableStateOf(false) }
    var taskEditorTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskEditorSnapshot by remember { mutableStateOf<com.whip.app.domain.WhipTask?>(null) }
    var taskEditorBoundary by rememberSaveable { mutableStateOf<TaskEditBoundary?>(null) }
    var taskEditorFromEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskEditorSaveAndNew by rememberSaveable { mutableStateOf(false) }
    var taskEditorCapture by rememberSaveable { mutableStateOf("") }
    var taskEditorInitialScheduleEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskEditorInitialPlacement by rememberSaveable { mutableStateOf<TaskPlacement?>(null) }
    var taskEditorSessionId by rememberSaveable { mutableLongStateOf(0L) }
    var actionItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var completedItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var rescheduleItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCompleteItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var globalAddExpanded by rememberSaveable { mutableStateOf(false) }
    var gymAddExpanded by rememberSaveable { mutableStateOf(false) }
    var createHabitRequested by rememberSaveable { mutableStateOf(false) }
    var createGoalRequested by rememberSaveable { mutableStateOf(false) }
    var createTrackRequested by rememberSaveable { mutableStateOf(false) }
    var gymAddRequest by rememberSaveable { mutableStateOf<GymAddRequest?>(null) }
    var recordGoalIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var resetElapsedGoalIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchEntryContext by rememberSaveable { mutableStateOf(WhipSearchEntryContext.AllWhip) }
    var openHabitIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var editHabitIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var openGoalIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var editGoalIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    val openTrackIdRequestedState: MutableState<Long?> = rememberSaveable { mutableStateOf(null) }
    var openTrackIdRequested by openTrackIdRequestedState
    var editTrackIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var openTrackEntryIdRequested by rememberSaveable { mutableStateOf<Long?>(null) }
    var addTrackEntryRequestedForId by rememberSaveable { mutableStateOf<Long?>(null) }
    val trackEditorSessionState: MutableState<Long> = rememberSaveable { mutableLongStateOf(0L) }
    var trackEditorSessionId by trackEditorSessionState
    val trackEditorRouteState: MutableState<TrackEditorRoute?> = rememberSaveable { mutableStateOf(null) }
    var trackEditorRoute by trackEditorRouteState
    val selectedTrackState: MutableState<Long?> = rememberSaveable { mutableStateOf(null) }
    val trackWorkspaceDestinationState: MutableState<TrackWorkspaceDestination> = rememberSaveable {
        mutableStateOf(TrackWorkspaceDestination.Tracks)
    }
    val trackDetailDestinationState: MutableState<TrackDetailDestination> = rememberSaveable {
        mutableStateOf(TrackDetailDestination.Entries)
    }
    var settingsSection by rememberSaveable { mutableStateOf(SettingsSection.Appearance) }
    var openGymSearchDomain by rememberSaveable { mutableStateOf<SearchDomain?>(null) }
    var openGymSearchId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reviewOpen by rememberSaveable { mutableStateOf(false) }
    var areaManagerOpen by rememberSaveable { mutableStateOf(false) }
    var areaMoveNotice by rememberSaveable { mutableStateOf<String?>(null) }
    var areaMoveRestoreScope by rememberSaveable { mutableStateOf<String?>(null) }
    val homeHabitValueItemIdState: MutableState<Long?> = rememberSaveable { mutableStateOf(null) }
    var homeHabitValueItemId by homeHabitValueItemIdState
    var contentPaneExpanded by rememberSaveable { mutableStateOf(false) }
    var consumedLaunchDeliveryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val workspaceStateHolder = rememberSaveableStateHolder()
    val shortcutFocusRequester = remember { FocusRequester() }
    val searchInvokerFocusRequester = remember { FocusRequester() }
    var searchPreviouslyOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val transientFeedbackScope = rememberCoroutineScope()
    var transientFeedbackJob by remember { mutableStateOf<Job?>(null) }
    var activeTransientFeedback by remember { mutableStateOf<TransientFeedbackRequest?>(null) }
    var transientFeedbackGeneration by remember { mutableLongStateOf(0L) }
    val pendingTransientFeedback = remember { ArrayDeque<TransientFeedbackRequest>() }
    fun startTransientFeedback(request: TransientFeedbackRequest) {
        val generation = ++transientFeedbackGeneration
        activeTransientFeedback = request
        transientFeedbackJob = transientFeedbackScope.launch {
            try {
                request.show()
            } finally {
                if (generation == transientFeedbackGeneration) {
                    transientFeedbackJob = null
                    activeTransientFeedback = null
                    pendingTransientFeedback.removeFirstOrNull()?.let(::startTransientFeedback)
                }
            }
        }
    }
    fun presentTransientFeedback(
        source: String,
        priority: Int,
        recoverable: Boolean = false,
        block: suspend () -> Unit,
    ) {
        val request = TransientFeedbackRequest(source, priority, recoverable, block)
        val active = activeTransientFeedback
        if (transientFeedbackJob?.isActive != true || active == null) {
            startTransientFeedback(request)
            return
        }
        when {
            recoverable && active.recoverable && source == active.source -> {
                transientFeedbackJob?.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
                startTransientFeedback(request)
            }
            active.recoverable -> {
                if (priority >= 2) {
                    pendingTransientFeedback.removeAll {
                        it.source == source && it.recoverable == recoverable
                    }
                    pendingTransientFeedback.addLast(request)
                }
            }
            recoverable -> {
                transientFeedbackJob?.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
                startTransientFeedback(request)
            }
            active.priority >= 3 -> {
                if (priority >= 3) pendingTransientFeedback.addLast(request)
            }
            priority >= 3 -> {
                transientFeedbackJob?.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
                startTransientFeedback(request)
            }
            else -> {
                transientFeedbackJob?.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
                startTransientFeedback(request)
            }
        }
    }
    fun invalidateTransientFeedback(source: String, preserveRecoveries: Boolean = false) {
        pendingTransientFeedback.removeAll {
            it.source == source && (!preserveRecoveries || !it.recoverable)
        }
        val active = activeTransientFeedback
        if (active?.source != source || (preserveRecoveries && active.recoverable)) return
        transientFeedbackGeneration++
        transientFeedbackJob?.cancel()
        transientFeedbackJob = null
        activeTransientFeedback = null
        snackbarHostState.currentSnackbarData?.dismiss()
        pendingTransientFeedback.removeFirstOrNull()?.let(::startTransientFeedback)
    }
    fun invalidateTransientRecovery(source: String) {
        pendingTransientFeedback.removeAll { it.source == source && it.recoverable }
        val active = activeTransientFeedback
        if (active?.source != source || !active.recoverable) return
        transientFeedbackGeneration++
        transientFeedbackJob?.cancel()
        transientFeedbackJob = null
        activeTransientFeedback = null
        snackbarHostState.currentSnackbarData?.dismiss()
        pendingTransientFeedback.removeFirstOrNull()?.let(::startTransientFeedback)
    }
    LaunchedEffect(appDestination) {
        pendingTransientFeedback.removeAll {
            !transientFeedbackSurvivesDestinationChange(it.recoverable)
        }
        if (activeTransientFeedback?.let {
                transientFeedbackSurvivesDestinationChange(it.recoverable)
            } == true
        ) {
            return@LaunchedEffect
        }
        transientFeedbackGeneration++
        transientFeedbackJob?.cancel()
        transientFeedbackJob = null
        activeTransientFeedback = null
        snackbarHostState.currentSnackbarData?.dismiss()
        pendingTransientFeedback.removeFirstOrNull()?.let(::startTransientFeedback)
    }
    val allScheduledTasks = unscopedTaskState.inbox + unscopedTaskState.today +
        unscopedTaskState.upcoming + unscopedTaskState.planning +
        unscopedTaskState.completed + unscopedTaskState.archived
    val scheduledTaskByKey = allScheduledTasks.associateBy(ScheduledTask::stableKey)
    val actionItem = actionItemKey?.let(scheduledTaskByKey::get)
    val completedItem = completedItemKey?.let(scheduledTaskByKey::get)
    LaunchedEffect(
        completedItemKey,
        completedItem?.completedAtMillis,
        completedItem?.occurrenceState,
        unscopedTaskState.loading,
    ) {
        if (
            completedItemKey != null &&
            !unscopedTaskState.loading &&
            (completedItem == null || completedItem.completedAtMillis == null)
        ) {
            completedItemKey = null
        }
    }
    var rescheduleItemSnapshot by remember { mutableStateOf<ScheduledTask?>(null) }
    var rescheduleSnapshotTitle by rememberSaveable { mutableStateOf("") }
    var rescheduleSnapshotEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteItemSnapshot by remember { mutableStateOf<ScheduledTask?>(null) }
    var deleteSnapshotTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteSnapshotTitle by rememberSaveable { mutableStateOf("") }
    var deleteSnapshotRecurring by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(rescheduleItemKey) {
        rescheduleItemSnapshot = rescheduleItemKey?.let(scheduledTaskByKey::get)
    }
    LaunchedEffect(deleteItemKey) {
        deleteItemSnapshot = deleteItemKey?.let(scheduledTaskByKey::get)
    }
    val rescheduleItem = rescheduleItemSnapshot ?: rescheduleItemKey?.let(scheduledTaskByKey::get)
    val pendingCompleteItem = pendingCompleteItemKey?.let(scheduledTaskByKey::get)
    val deleteItem = deleteItemSnapshot ?: deleteItemKey?.let(scheduledTaskByKey::get)
    val openGymSearchRequested = openGymSearchDomain?.let { domain ->
        openGymSearchId?.let { id -> WhipSearchResult(domain, id, "", "") }
    }
    fun openSettings() {
        if (appDestination != AppDestination.Settings) settingsCallerDestination = appDestination
        appDestination = AppDestination.Settings
    }
    fun openTrackEditor(intent: TrackEditorIntent) {
        val nextSessionId = trackEditorSessionId + 1L
        val resolvedRoute = resolveTrackEditorRoute(
            intent = intent,
            unscopedTrackState = unscopedTrackState,
            userDataGeneration = userDataGeneration,
            sessionId = nextSessionId,
        )
        if (resolvedRoute == null) {
            presentTransientFeedback(source = "track-editor-target", priority = 3, recoverable = true) {
                snackbarHostState.showSnackbar(
                    "That Track is no longer available. Your existing data was not changed.",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
            }
            return
        }
        if (intent is TrackEditorIntent.Definition) {
            trackViewModel?.clearDefinitionEditorState()
        }
        trackEditorSessionId = nextSessionId
        trackEditorRoute = resolvedRoute
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
    val editorTask = taskEditorTaskId?.let { id ->
        taskEditorSnapshot?.takeIf { it.id == id }
            ?: unscopedTaskState.taskEntities.firstOrNull { it.id == id }
    }
    val editorRequest = if (taskEditorOpen && (taskEditorTaskId == null || editorTask != null)) {
        TaskEditorRequest(
            task = editorTask,
            expectedBoundary = taskEditorBoundary,
            fromOccurrence = taskEditorFromEpochDay?.let(LocalDate::ofEpochDay),
            initialCapture = taskEditorCapture,
            initialScheduleDate = taskEditorInitialScheduleEpochDay?.let(LocalDate::ofEpochDay),
            initialPlacement = taskEditorInitialPlacement,
            sessionId = taskEditorSessionId,
        )
    } else {
        null
    }
    fun openTaskEditor(
        item: ScheduledTask? = null,
        capture: String = "",
        scheduleDate: LocalDate? = null,
        placement: TaskPlacement? = null,
    ) {
        taskEditorOpen = true
        taskEditorTaskId = item?.task?.id
        taskEditorSnapshot = item?.task
        taskEditorBoundary = item?.toEditBoundary()
        taskEditorFromEpochDay = item?.originalDate
            ?.takeIf {
                    item.task.scheduleKind == ScheduleKind.Recurring &&
                    !item.task.archived &&
                    item.completedAtMillis == null &&
                    (item.occurrenceState == null || item.occurrenceState == OccurrenceState.Open)
            }
            ?.toEpochDay()
        taskEditorCapture = capture
        taskEditorInitialScheduleEpochDay = scheduleDate?.toEpochDay()
        taskEditorInitialPlacement = placement
        taskEditorSessionId++
    }
    fun closeTaskEditor() {
        taskEditorOpen = false
        taskEditorTaskId = null
        taskEditorSnapshot = null
        taskEditorBoundary = null
        taskEditorFromEpochDay = null
        taskEditorSaveAndNew = false
        taskEditorCapture = ""
        taskEditorInitialScheduleEpochDay = null
        taskEditorInitialPlacement = null
    }

    val launchDeliveryId = initialDeliveryId.takeIf { it != 0L }
        ?: 1L.takeIf { initialAction != null }
        ?: 0L
    LaunchDeliveryEffect(
        launchDeliveryId = launchDeliveryId,
        consumedLaunchDeliveryId = consumedLaunchDeliveryId,
        areaSelectionReady = launchAreaSelectionReady,
        initialAction = initialAction,
        initialEntityId = initialEntityId,
        initialOccurrenceEpochDay = initialOccurrenceEpochDay,
        initialSharedText = initialSharedText,
        setupCompleted = settingsState.settings.setupCompleted,
        taskState = unscopedTaskState,
        habitState = unscopedHabitState,
        goalState = unscopedGoalState,
        trackState = unscopedTrackState,
        onConsume = { consumedLaunchDeliveryId = it },
        onCommand = { command ->
            when (command) {
                is LaunchDeliveryCommand.LoadFailed -> appDestination = command.destination
                is LaunchDeliveryCommand.Unavailable -> {
                    appDestination = command.destination
                    presentTransientFeedback(source = "launch-target-$launchDeliveryId", priority = 3, recoverable = true) {
                        snackbarHostState.showSnackbar(
                            command.message,
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                        )
                    }
                }
                LaunchDeliveryCommand.OpenTaskAgenda -> {
                    appDestination = AppDestination.Tasks
                    taskDestination = TaskDestination.Today
                }
                LaunchDeliveryCommand.OpenHabitTracking -> appDestination = AppDestination.Habits
                is LaunchDeliveryCommand.AddTask -> {
                    appDestination = AppDestination.Tasks
                    openTaskEditor(scheduleDate = command.date)
                }
                is LaunchDeliveryCommand.CaptureSharedTask -> {
                    appDestination = AppDestination.Tasks
                    openTaskEditor(capture = command.text)
                }
                LaunchDeliveryCommand.AddHabit -> {
                    appDestination = AppDestination.Habits
                    createHabitRequested = true
                }
                is LaunchDeliveryCommand.OpenTask -> {
                    appDestination = AppDestination.Tasks
                    if (command.completed) completedItemKey = command.item.stableKey else {
                        command.destination?.let { taskDestination = it }
                        actionItemKey = command.item.stableKey
                    }
                }
                is LaunchDeliveryCommand.OpenTaskFallback -> {
                    appDestination = AppDestination.Tasks
                    presentTransientFeedback(source = "launch-target-$launchDeliveryId", priority = 3, recoverable = true) {
                        snackbarHostState.showSnackbar(
                            command.message,
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                        )
                    }
                }
                is LaunchDeliveryCommand.OpenHabit -> {
                    openHabitIdRequested = command.id
                    appDestination = AppDestination.Habits
                }
                is LaunchDeliveryCommand.OpenGoal -> {
                    openGoalIdRequested = command.id
                    appDestination = AppDestination.Goals
                }
                LaunchDeliveryCommand.OpenGym -> appDestination = AppDestination.Gym
                is LaunchDeliveryCommand.OpenTrack -> {
                    openTrackIdRequested = command.id
                    appDestination = AppDestination.Tracks
                }
            }
        },
    )

    LaunchedEffect(operationStatus) {
        if (
            operationStatus is OperationStatus.Running ||
            (operationStatus is OperationStatus.Succeeded &&
                operationStatus.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateTransientFeedback("tasks")
        }
        operationStatus.deliverTransientMessage(onOperationStatusConsumed) { message ->
            val quickAddedId = quickAddedTaskId
            val undoMessage = taskUndoMessage
            val undoToken = taskUndoToken
            val succeeded = operationStatus is OperationStatus.Succeeded
            presentTransientFeedback(
                source = "tasks",
                priority = when {
                    operationStatus is OperationStatus.Failed -> 3
                    undoMessage != null -> 2
                    else -> 1
                },
                recoverable = undoToken != null,
            ) {
                try {
                    val result = snackbarHostState.showSnackbar(
                        TaskSnackbarVisuals(
                            message = message,
                            actionLabel = when {
                            !succeeded || undoMessage == null -> null
                            quickAddedId != null -> "Edit"
                            else -> "Undo"
                            },
                            withDismissAction = undoToken != null || !succeeded,
                            duration = if (succeeded) SnackbarDuration.Long else SnackbarDuration.Indefinite,
                            undoToken = undoToken,
                            quickAdd = quickAddedId != null,
                        ),
                    )
                    if (undoToken == null) return@presentTransientFeedback
                    when {
                        quickAddedId != null && result == SnackbarResult.ActionPerformed -> {
                            val item = allScheduledTasks.firstOrNull { it.task.id == quickAddedId }
                            if (item != null) openTaskEditor(item) else {
                                taskEditorOpen = true
                                taskEditorTaskId = quickAddedId
                                taskEditorSnapshot = null
                                taskEditorBoundary = null
                                taskEditorFromEpochDay = null
                                taskEditorCapture = ""
                                taskEditorInitialScheduleEpochDay = null
                                taskEditorInitialPlacement = null
                                taskEditorSessionId++
                            }
                            onTaskUndoDismissed(undoToken)
                        }
                        quickAddedId == null && result == SnackbarResult.ActionPerformed -> onTaskUndo(undoToken)
                        else -> Unit
                    }
                } finally {
                    undoToken?.let(onTaskUndoDismissed)
                }
            }
        }
    }

    LaunchedEffect(gymOperationStatus) {
        if (
            gymOperationStatus is OperationStatus.Running ||
            (gymOperationStatus is OperationStatus.Succeeded &&
                gymOperationStatus.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateTransientFeedback("gym", preserveRecoveries = true)
        }
        gymOperationStatus.deliverTransientMessage(onGymOperationStatusConsumed) { message ->
            val archiveUndoId = (gymOperationStatus as? OperationStatus.Succeeded)
                ?.recoveryToken
                ?.takeIf { it == machineArchiveUndoId }
            val archiveUndoAvailable = archiveUndoId != null
            val succeeded = gymOperationStatus is OperationStatus.Succeeded
            presentTransientFeedback(
                source = "gym",
                priority = when {
                    gymOperationStatus is OperationStatus.Failed -> 3
                    archiveUndoAvailable -> 2
                    else -> 1
                },
                recoverable = archiveUndoAvailable,
            ) {
                try {
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Undo".takeIf { succeeded && archiveUndoAvailable },
                        withDismissAction = archiveUndoAvailable || gymOperationStatus is OperationStatus.Failed,
                        duration = if (gymOperationStatus is OperationStatus.Failed) SnackbarDuration.Indefinite else SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed && archiveUndoId != null) onMachineArchiveUndo(archiveUndoId)
                } finally {
                    archiveUndoId?.let(onMachineArchiveUndoDismissed)
                }
            }
        }
    }
    LaunchedEffect(habitOperationStatus) {
        if (
            habitOperationStatus is OperationStatus.Running ||
            (habitOperationStatus is OperationStatus.Succeeded &&
                habitOperationStatus.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateTransientFeedback("habits")
        }
        habitOperationStatus.deliverTransientMessage(onHabitOperationStatusConsumed) { message ->
            presentTransientFeedback(source = "habits", priority = if (habitOperationStatus is OperationStatus.Failed) 3 else 1) {
                snackbarHostState.showSnackbar(
                    message = message,
                    withDismissAction = habitOperationStatus is OperationStatus.Failed,
                    duration = if (habitOperationStatus is OperationStatus.Failed) SnackbarDuration.Indefinite else SnackbarDuration.Long,
                )
            }
        }
    }
    LaunchedEffect(goalOperationStatus) {
        if (
            goalOperationStatus is OperationStatus.Running ||
            (goalOperationStatus is OperationStatus.Succeeded &&
                goalOperationStatus.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateTransientFeedback("goals")
        }
        goalOperationStatus.deliverTransientMessage(onGoalOperationStatusConsumed) { message ->
            presentTransientFeedback(source = "goals", priority = if (goalOperationStatus is OperationStatus.Failed) 3 else 1) {
                snackbarHostState.showSnackbar(
                    message = message,
                    withDismissAction = goalOperationStatus is OperationStatus.Failed,
                    duration = if (goalOperationStatus is OperationStatus.Failed) SnackbarDuration.Indefinite else SnackbarDuration.Long,
                )
            }
        }
    }
    LaunchedEffect(trackOperationStatus) {
        if (
            trackOperationStatus is OperationStatus.Running ||
            (trackOperationStatus is OperationStatus.Succeeded &&
                trackOperationStatus.feedbackPresentation == OperationFeedbackPresentation.Inline)
        ) {
            invalidateTransientFeedback("tracks", preserveRecoveries = true)
        }
        trackOperationStatus.deliverTransientMessage(onTrackOperationStatusConsumed) { message ->
            val succeeded = trackOperationStatus is OperationStatus.Succeeded
            val undoEntryId = (trackOperationStatus as? OperationStatus.Succeeded)
                ?.recoveryToken
                ?.takeIf { succeeded && it == trackEntryUndoId }
            val undoAvailable = undoEntryId != null
            presentTransientFeedback(
                source = "tracks",
                priority = when {
                    trackOperationStatus is OperationStatus.Failed -> 3
                    undoAvailable -> 2
                    else -> 1
                },
                recoverable = undoAvailable,
            ) {
                try {
                    val result = snackbarHostState.showSnackbar(
                        message,
                        actionLabel = "Undo".takeIf { undoAvailable },
                        withDismissAction = undoAvailable || trackOperationStatus is OperationStatus.Failed,
                        duration = if (trackOperationStatus is OperationStatus.Failed) SnackbarDuration.Indefinite else SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed && undoEntryId != null) onTrackEntryUndo(undoEntryId)
                } finally {
                    undoEntryId?.let(onTrackEntryUndoDismissed)
                }
            }
        }
    }

    LaunchedEffect(machineArchiveUndoId) {
        if (machineArchiveUndoId == null) invalidateTransientRecovery("gym")
    }
    LaunchedEffect(trackEntryUndoId) {
        if (trackEntryUndoId == null) invalidateTransientRecovery("tracks")
    }

    LaunchedEffect(areaMoveNotice) {
        val message = areaMoveNotice ?: return@LaunchedEffect
        val restoreScope = areaMoveRestoreScope
        areaMoveNotice = null
        areaMoveRestoreScope = null
        presentTransientFeedback(source = "area-move", priority = 2, recoverable = true) {
            val result = snackbarHostState.showSnackbar(message, actionLabel = "Restore view", withDismissAction = true)
            if (result == SnackbarResult.ActionPerformed) {
                onSelectAreaScope(AreaScope.fromStorageKey(restoreScope))
            }
        }
    }

    LaunchedEffect(pendingAreaBadgeId) {
        val id = pendingAreaBadgeId ?: return@LaunchedEffect
        val area = settingsState.areas.firstOrNull { it.id == id && !it.archived }
        onAreaBadgeConsumed()
        if (area != null) {
            onSelectAreaScope(AreaScope.One(id))
            presentTransientFeedback(source = "area-badge", priority = 2) {
                val result = snackbarHostState.showSnackbar("Showing ${area.name}", actionLabel = "Show all", withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) onSelectAreaScope(AreaScope.All)
            }
        }
    }

    fun keepSavedItemVisible(areaId: String?, areaVerified: Boolean = true) {
        val availableAreas = settingsState.areas.filterNot(Area::archived)
        val target = if (areaVerified) {
            scopeForSavedArea(areaId, availableAreas.mapTo(mutableSetOf(), Area::id))
        } else AreaScope.All
        val needsMove = when {
            !areaVerified -> areaScope != AreaScope.All
            areaScope == AreaScope.All -> false
            target == AreaScope.All -> true
            else -> !areaScope.matches(areaId)
        }
        if (needsMove) {
            areaMoveRestoreScope = areaScope.storageKey
            onSelectAreaScope(target)
            val label = when (target) {
                AreaScope.All -> "All Areas"
                AreaScope.Unassigned -> "Main"
                is AreaScope.One -> availableAreas.firstOrNull { it.id == target.areaId }?.name ?: "All Areas"
            }
            areaMoveNotice = "Switched to $label to keep the saved item visible"
        }
    }

    val taskEditorSaveCoordinator = rememberEntitySaveCoordinator(
        state = taskEditorSaveState,
        consume = onTaskEditorSaveResultConsumed,
        key = taskEditorSessionId,
        onPersisted = { receipt ->
            keepSavedItemVisible(receipt.areaId, receipt.areaVerified)
            if (taskEditorSaveAndNew) {
                taskEditorSaveAndNew = false
                taskEditorCapture = ""
                taskEditorSessionId++
            } else closeTaskEditor()
        },
    )

    fun requestCompletion(item: ScheduledTask) {
        if (item.subtasks.any { !it.completed }) {
            pendingCompleteItemKey = item.stableKey
        } else {
            onComplete(item)
        }
    }

    var gymDestination by rememberSaveable { mutableStateOf(GymDestination.Workout) }
    var requestedWorkoutExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun selectPrimaryDestination(destination: AppDestination) {
        if (destination == appDestination) return
        compactItemExpansionState?.collapseAll()
        // Each primary workspace is a durable place. Switching roots preserves
        // its last destination, filters, and selected entity; explicit Home
        // shortcuts and deep links still opt into a precise destination.
        appDestination = destination
    }

    val collectionStatusNowMillis = goalState.nowMillis
    val adaptiveSummary = buildAdaptiveSummary(
        taskState = state,
        habitState = habitState,
        goalState = goalState,
        trackState = trackState,
        gymState = gymState,
        settings = settingsState.settings,
        selectedTaskTitle = actionItem?.task?.title,
        gymDestination = gymDestination,
        nowMillis = collectionStatusNowMillis,
    )
    val supportsPaneExpansion = adaptiveLayout in setOf(
        WhipAdaptiveLayout.ExpandedDashboard,
        WhipAdaptiveLayout.BookFold,
        WhipAdaptiveLayout.TabletopFold,
    )
    // Secondary destinations must inherit the host window's navigation mode. In
    // particular, opening Settings from a split/fold layout must not replace the
    // persistent rail with compact bottom navigation.
    val contentPaneIsExpanded = supportsPaneExpansion && contentPaneExpanded
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
    val dialogHingeWidth = with(LocalDensity.current) {
        foldInfo
            ?.takeIf {
                adaptiveLayout == WhipAdaptiveLayout.BookFold &&
                    it.orientation == WhipFoldOrientation.Vertical &&
                    !contentPaneIsExpanded
            }
            ?.widthPx
            ?.toDp()
            ?.coerceAtLeast(1.dp)
            ?: 0.dp
    }
    val dialogPaneWidth = minOf(dialogContentWidth * 0.94f, 720.dp)
    val paneDialogModifier = Modifier
        .absoluteOffset(x = dialogPaneOffset)
        .width(dialogPaneWidth)
    val primaryEditorPaneModifier = Modifier
        .absoluteOffset(x = dialogPaneOffset)
        .width(dialogContentWidth)
    var gymRoutineEditorOpen by rememberSaveable { mutableStateOf(false) }
    var taskSelectionMode by rememberSaveable { mutableStateOf(false) }
    var reorderModeActive by rememberSaveable { mutableStateOf(false) }
    var reorderDismissRequest by rememberSaveable { mutableIntStateOf(0) }
    var reorderOwnsTemporaryAreaScope by rememberSaveable { mutableStateOf(false) }
    val focusedCollectionMode = taskSelectionMode || reorderModeActive
    LaunchedEffect(reorderModeActive, transientAreaScope) {
        if (reorderModeActive && transientAreaScope) {
            reorderOwnsTemporaryAreaScope = true
        } else if (!reorderModeActive && reorderOwnsTemporaryAreaScope) {
            reorderOwnsTemporaryAreaScope = false
            onRestoreAreaScope()
        }
    }
    fun returnToHomeAfterDataReset() {
        settingsCallerDestination = AppDestination.Home
        appDestination = AppDestination.Home
        taskDestination = TaskDestination.Today
        taskPlanningViewRequest = null
        closeTaskEditor()
        actionItemKey = null
        completedItemKey = null
        rescheduleItemKey = null
        rescheduleItemSnapshot = null
        rescheduleSnapshotTitle = ""
        rescheduleSnapshotEpochDay = null
        pendingCompleteItemKey = null
        deleteItemKey = null
        deleteItemSnapshot = null
        deleteSnapshotTaskId = null
        deleteSnapshotTitle = ""
        deleteSnapshotRecurring = false
        globalAddExpanded = false
        gymAddExpanded = false
        createHabitRequested = false
        createGoalRequested = false
        createTrackRequested = false
        gymAddRequest = null
        recordGoalIdRequested = null
        searchOpen = false
        openHabitIdRequested = null
        editHabitIdRequested = null
        openGoalIdRequested = null
        editGoalIdRequested = null
        openTrackIdRequested = null
        editTrackIdRequested = null
        openTrackEntryIdRequested = null
        addTrackEntryRequestedForId = null
        trackEditorRoute = null
        selectedTrackState.value = null
        trackDetailDestinationState.value = TrackDetailDestination.Entries
        openGymSearchDomain = null
        openGymSearchId = null
        reviewOpen = false
        areaManagerOpen = false
        areaMoveNotice = null
        areaMoveRestoreScope = null
        homeHabitValueItemId = null
        contentPaneExpanded = false
        gymRoutineEditorOpen = false
        taskSelectionMode = false
        reorderModeActive = false
        reorderOwnsTemporaryAreaScope = false
        snackbarHostState.currentSnackbarData?.dismiss()
    }
    val addDescription = when (appDestination) {
        AppDestination.Home -> "Add task, habit, goal, track, or workout"
        AppDestination.Tasks -> "Add task"
        AppDestination.Habits -> "Add habit"
        AppDestination.Goals -> "Add goal"
        AppDestination.Gym -> when (gymDestination) {
            GymDestination.Workout -> if (gymState.activeSession == null) "Start workout" else "Add exercise to workout"
            GymDestination.History, GymDestination.Progress -> {
                if (gymState.activeSession == null) "Start workout" else "Add exercise to workout"
            }
            GymDestination.Library -> "Add workout, routine, exercise, machine, or category"
            GymDestination.Routines -> "Create routine"
            GymDestination.Exercises -> "Create exercise"
            GymDestination.Machines -> "Create machine"
            GymDestination.Categories -> "Create category"
            GymDestination.Tools -> "Add"
        }
        AppDestination.Tracks -> selectedTrackState.value?.let { id ->
            trackState.track(id)?.track?.name?.let { "Add entry to $it" }
        } ?: "Add track"
        AppDestination.Settings -> "Add"
    }
    val triggerAdd: () -> Unit = {
        when (appDestination) {
            AppDestination.Home -> globalAddExpanded = true
            AppDestination.Tasks -> openTaskEditor(
                scheduleDate = when (taskDestination) {
                    TaskDestination.Today -> state.currentDate
                    TaskDestination.Upcoming -> state.currentDate.plusDays(1)
                    else -> null
                },
                placement = taskDestination.creationPlacement(),
            )
            AppDestination.Habits -> createHabitRequested = true
            AppDestination.Goals -> createGoalRequested = true
            AppDestination.Gym -> when (gymDestination) {
                GymDestination.Workout -> {
                    gymAddRequest = if (gymState.activeSession == null) {
                        GymAddRequest.StartWorkout
                    } else {
                        GymAddRequest.AddWorkoutExercise
                    }
                }
                GymDestination.History, GymDestination.Progress -> {
                    gymAddRequest = if (gymState.activeSession == null) {
                        GymAddRequest.StartWorkout
                    } else {
                        GymAddRequest.AddWorkoutExercise
                    }
                }
                GymDestination.Library -> gymAddExpanded = true
                GymDestination.Routines -> gymAddRequest = GymAddRequest.CreateRoutine
                GymDestination.Exercises -> gymAddRequest = GymAddRequest.CreateExercise
                GymDestination.Machines -> gymAddRequest = GymAddRequest.CreateMachine
                GymDestination.Categories -> gymAddRequest = GymAddRequest.CreateCategory
                GymDestination.Tools -> Unit
            }
            AppDestination.Tracks -> {
                val selectedTrackId = selectedTrackState.value
                val selectedTrack = selectedTrackId?.let(trackState::track)
                if (selectedTrack != null && !selectedTrack.track.archived) {
                    openTrackEditor(TrackEditorIntent.Entry(selectedTrackId))
                } else if (selectedTrack == null) {
                    createTrackRequested = true
                }
            }
            AppDestination.Settings -> Unit
        }
    }
    val openGymContext: (Int) -> Unit = { index ->
        appDestination = AppDestination.Gym
        if (gymDestination == GymDestination.Workout) {
            requestedWorkoutExerciseId = if (gymState.activeSession != null && index > 0) {
                gymState.activeWorkoutExercises.getOrNull(index - 1)?.workoutExercise?.id
            } else null
        }
        contentPaneExpanded = true
    }
    AdaptiveNavigationFrame(
        modifier = modifier
            .fillMaxSize()
            .testTag("app-background-shell")
            .semantics { if (areaManagerOpen || reviewOpen || trackEditorRoute != null) hideFromAccessibility() }
            .focusRequester(shortcutFocusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && reorderModeActive) {
                    reorderDismissRequest += 1
                    return@onPreviewKeyEvent true
                }
                if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                if (focusedCollectionMode) return@onPreviewKeyEvent true
                when (event.key) {
                    Key.K -> {
                        searchEntryContext = appDestination.searchEntryContext(gymDestination)
                        searchOpen = true
                    }
                    Key.N -> {
                        if (
                            globalAddAvailable(
                                appDestination,
                                gymDestination,
                                gymRoutineEditorOpen,
                                focusedCollectionMode,
                                selectedTrackState.value?.let(trackState::track)?.track?.archived == true,
                            )
                        ) {
                            triggerAdd()
                        } else {
                            return@onPreviewKeyEvent false
                        }
                    }
                    Key.H -> appDestination = AppDestination.Home
                    Key.One -> selectPrimaryDestination(AppDestination.Tasks)
                    Key.Two -> selectPrimaryDestination(AppDestination.Habits)
                    Key.Three -> selectPrimaryDestination(AppDestination.Goals)
                    Key.Four -> selectPrimaryDestination(AppDestination.Tracks)
                    Key.Five -> selectPrimaryDestination(AppDestination.Gym)
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
        navigationEnabled = !gymRoutineEditorOpen && trackEditorRoute == null && !focusedCollectionMode,
        supportContent = if (focusedCollectionMode) null else when (appDestination) {
            AppDestination.Home -> { supportModifier ->
                HomeSupportPane(
                    summary = adaptiveSummary,
                    retryActions = domainRetryActions,
                    onSelect = ::selectPrimaryDestination,
                    modifier = supportModifier,
                )
            }
            AppDestination.Tasks -> { supportModifier ->
                DestinationSupportPane(
                    title = stringResource(R.string.support_tasks_title),
                    domain = stringResource(R.string.nav_tasks),
                    supportingText = stringResource(R.string.support_tasks_description),
                    items = state.today.take(12).map { item ->
                        SupportPaneItem(
                            item.stableKey,
                            item.task.title,
                            item.scheduledDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                ?: stringResource(R.string.task_inbox),
                        )
                    },
                    emptyText = stringResource(R.string.support_tasks_empty),
                    loadState = adaptiveSummary.taskLoadState,
                    statusTagPrefix = "support-pane-tasks",
                    onRetry = domainRetryActions.tasks,
                    onOpen = { key -> actionItemKey = key },
                    modifier = supportModifier,
                )
            }
            AppDestination.Habits -> { supportModifier ->
                DestinationSupportPane(
                    title = stringResource(R.string.support_habits_title),
                    domain = stringResource(R.string.nav_habits),
                    supportingText = stringResource(R.string.support_habits_description),
                    items = habitState.today.sortedBy(HabitDayProgress::isDoneForToday).take(12).map { item ->
                        SupportPaneItem(
                            item.habit.id.toString(),
                            item.habit.name,
                            stringResource(
                                if (item.successful == true) R.string.state_done else R.string.state_needs_attention,
                            ),
                        )
                    },
                    emptyText = stringResource(R.string.support_habits_empty),
                    loadState = adaptiveSummary.habitLoadState,
                    statusTagPrefix = "support-pane-habits",
                    onRetry = domainRetryActions.habits,
                    onOpen = { id -> openHabitIdRequested = id.toLongOrNull() },
                    modifier = supportModifier,
                )
            }
            AppDestination.Goals -> { supportModifier ->
                DestinationSupportPane(
                    title = stringResource(R.string.support_goals_title),
                    domain = stringResource(R.string.nav_goals),
                    supportingText = stringResource(R.string.support_goals_description),
                    items = goalState.active.take(12).map { projection ->
                        SupportPaneItem(
                            projection.goal.id.toString(),
                            projection.goal.name,
                            projection.collectionStatus(goalState.customUnits, collectionStatusNowMillis),
                        )
                    },
                    emptyText = stringResource(R.string.support_goals_empty),
                    loadState = adaptiveSummary.goalLoadState,
                    statusTagPrefix = "support-pane-goals",
                    onRetry = domainRetryActions.goals,
                    onOpen = { id -> openGoalIdRequested = id.toLongOrNull() },
                    modifier = supportModifier,
                )
            }
            AppDestination.Tracks -> { supportModifier ->
                if (adaptiveLayout == WhipAdaptiveLayout.ExpandedDashboard) {
                    TrackOverviewSupportPane(
                        state = trackState,
                        loadState = adaptiveSummary.trackLoadState,
                        onRetry = domainRetryActions.tracks,
                        modifier = supportModifier,
                    )
                } else {
                    TrackSupportPane(
                        projections = trackState.projections,
                        selectedTrackId = selectedTrackState.value,
                        loadState = adaptiveSummary.trackLoadState,
                        onRetry = domainRetryActions.tracks,
                        onSelect = { selectedTrackState.value = it; trackDetailDestinationState.value = TrackDetailDestination.Entries },
                        modifier = supportModifier,
                    )
                }
            }
            AppDestination.Settings -> { supportModifier ->
                SettingsSupportPane(
                    selected = settingsSection,
                    onSelect = { settingsSection = it },
                    modifier = supportModifier,
                )
            }
            AppDestination.Gym -> { supportModifier ->
                FoldContextPane(
                    summary = adaptiveSummary,
                    selected = AppDestination.Gym,
                    onSelect = ::selectPrimaryDestination,
                    modifier = supportModifier,
                    onGymContextSelected = openGymContext,
                    navigationEnabled = !gymRoutineEditorOpen && trackEditorRoute == null && !focusedCollectionMode,
                )
            }
        },
        onGymContextSelected = openGymContext,
        onSelect = {
            if (!gymRoutineEditorOpen && !focusedCollectionMode) {
                if (it == AppDestination.Settings) openSettings() else selectPrimaryDestination(it)
            }
        },
    ) { scaffoldModifier ->
      Scaffold(
        modifier = scaffoldModifier.fillMaxSize(),
        topBar = {
            if (!gymRoutineEditorOpen) TopAppBar(
                modifier = Modifier.testTag("workspace-top-app-bar"),
                title = {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("workspace-header-identity"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (appDestination == AppDestination.Gym) {
                            Column(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics { heading() }
                                    .testTag("gym-header-identity"),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    "Whip",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    stringResource(appDestination.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (!focusedCollectionMode && appDestination in setOf(AppDestination.Home, AppDestination.Tasks, AppDestination.Habits, AppDestination.Goals, AppDestination.Tracks)) {
                            AreaScopeMenu(
                                scope = areaScope,
                                areas = settingsState.areas,
                                usage = settingsState.areaUsage,
                                onSelect = onSelectAreaScope,
                                onManage = { areaManagerOpen = true },
                                onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                                modifier = paneDialogModifier,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    if (
                        globalAddAvailable(
                            appDestination = appDestination,
                            gymDestination = gymDestination,
                            gymRoutineEditorOpen = gymRoutineEditorOpen,
                            taskSelectionMode = focusedCollectionMode,
                            selectedTrackArchived = selectedTrackState.value?.let(trackState::track)?.track?.archived == true,
                        )
                    ) {
                        Box {
                            IconButton(
                                onClick = triggerAdd,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("workspace-add-action")
                                    .semantics { contentDescription = addDescription },
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                                    if (appDestination == AppDestination.Home || (appDestination == AppDestination.Gym && gymDestination == GymDestination.Library)) {
                                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            DropdownMenu(expanded = globalAddExpanded && appDestination == AppDestination.Home, onDismissRequest = { globalAddExpanded = false }) {
                                WhipMenuItem(
                                    label = "New Task",
                                    onClick = {
                                        openTaskEditor(
                                            scheduleDate = state.currentDate,
                                            placement = TaskPlacement.Scheduled,
                                        )
                                        globalAddExpanded = false
                                    },
                                )
                                WhipMenuItem(label = "New Habit", onClick = { appDestination = AppDestination.Habits; createHabitRequested = true; globalAddExpanded = false })
                                WhipMenuItem(label = "New Goal", onClick = { appDestination = AppDestination.Goals; createGoalRequested = true; globalAddExpanded = false })
                                WhipMenuItem(label = "New Track", onClick = { appDestination = AppDestination.Tracks; createTrackRequested = true; globalAddExpanded = false })
                                WhipMenuItem(
                                    label = if (gymState.activeSession == null) "Start Workout" else "Add Exercise to This Workout",
                                    onClick = {
                                        appDestination = AppDestination.Gym
                                        gymDestination = GymDestination.Workout
                                        gymAddRequest = if (gymState.activeSession == null) {
                                            GymAddRequest.StartWorkout
                                        } else {
                                            GymAddRequest.AddWorkoutExercise
                                        }
                                        globalAddExpanded = false
                                    },
                                )
                            }
                            DropdownMenu(expanded = gymAddExpanded && appDestination == AppDestination.Gym, onDismissRequest = { gymAddExpanded = false }) {
                                WhipMenuItem(
                                    label = if (gymState.activeSession == null) "Start Workout" else "Add Exercise to This Workout",
                                    onClick = {
                                        gymDestination = GymDestination.Workout
                                        gymAddRequest = if (gymState.activeSession == null) {
                                            GymAddRequest.StartWorkout
                                        } else {
                                            GymAddRequest.AddWorkoutExercise
                                        }
                                        gymAddExpanded = false
                                    },
                                )
                                WhipMenuItem(label = "New Routine", onClick = { gymDestination = GymDestination.Routines; gymAddRequest = GymAddRequest.CreateRoutine; gymAddExpanded = false })
                                WhipMenuItem(label = "New Exercise", onClick = { gymDestination = GymDestination.Exercises; gymAddRequest = GymAddRequest.CreateExercise; gymAddExpanded = false })
                                WhipMenuItem(label = "New Machine", onClick = { gymDestination = GymDestination.Machines; gymAddRequest = GymAddRequest.CreateMachine; gymAddExpanded = false })
                                WhipMenuItem(label = "New Category", onClick = { gymDestination = GymDestination.Categories; gymAddRequest = GymAddRequest.CreateCategory; gymAddExpanded = false })
                            }
                        }
                    }
                    if (
                        !focusedCollectionMode && supportsPaneExpansion &&
                        appDestination != AppDestination.Settings
                    ) {
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
                    if (!focusedCollectionMode && appDestination != AppDestination.Settings) {
                        IconButton(
                            onClick = {
                                searchEntryContext = appDestination.searchEntryContext(gymDestination)
                                searchOpen = true
                            },
                            modifier = Modifier.focusRequester(searchInvokerFocusRequester).size(52.dp).testTag("workspace-search-action").semantics {
                                contentDescription = if (appDestination == AppDestination.Home) "Search All Whip Data" else "Search ${appDestination.label}"
                            },
                        ) { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(28.dp)) }
                    }
                    if (
                        !focusedCollectionMode &&
                        (adaptiveLayout != WhipAdaptiveLayout.BookFold || contentPaneIsExpanded)
                    ) {
                        IconButton(onClick = { if (appDestination == AppDestination.Settings) closeSettings() else openSettings() }, modifier = Modifier.size(52.dp).testTag("workspace-settings-action").semantics { contentDescription = if (appDestination == AppDestination.Settings) "Close Settings" else "Open Settings" }) {
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
                WhipBottomNavigation(
                    selected = appDestination,
                    onSelect = ::selectPrimaryDestination,
                    enabled = trackEditorRoute == null && !focusedCollectionMode,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(
                    bottom = if (gymState.activeSession != null && appDestination == AppDestination.Gym) 104.dp else 12.dp,
                ),
            ) { data ->
                val taskVisuals = data.visuals as? TaskSnackbarVisuals
                if (taskVisuals?.quickAdd == true && taskVisuals.actionLabel == "Edit") {
                    Snackbar(
                        action = {
                            WhipTextButton(onClick = data::performAction) { Text("Edit") }
                        },
                        dismissAction = {
                            WhipTextButton(
                                onClick = {
                                    taskVisuals.undoToken?.let(onTaskUndo)
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.fillMaxHeight().widthIn(max = 1000.dp)) {
                workspaceStateHolder.SaveableStateProvider(appDestination.name) {
                when (appDestination) {
            AppDestination.Home -> {
                HomeContent(
                    state = state,
                    habitState = habitState,
                    gymState = gymState,
                    goalState = goalState,
                    trackState = trackState,
                    appSettings = settingsState.settings,
                    innerPadding = innerPadding,
                    onQuickHabit = { item ->
                        habitViewModel?.let { vm ->
                            quickHabitAction(item, vm) { homeHabitValueItemId = item.habit.id }
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
                    onUndoHabitSkip = { item -> habitViewModel?.undoSkip(item.habit.id, item.date) },
                    canUndoHabit = { item ->
                        habitState.logs.any { it.habitId == item.habit.id && it.localDate in item.habit.periodBounds(item.date) }
                    },
                    onChecklist = { habitId, itemId, date, completed ->
                        habitViewModel?.toggleChecklist(habitId, itemId, date, completed)
                    },
                    onOpenHabits = { appDestination = AppDestination.Habits },
                    onOpenHabit = { item -> openHabitIdRequested = item.habit.id },
                    onEditHabit = { item -> editHabitIdRequested = item.habit.id },
                    onOpenTasks = { appDestination = AppDestination.Tasks; taskDestination = TaskDestination.Today },
                    onCompleteTask = ::requestCompletion,
                    onOpenTask = { actionItemKey = it.stableKey },
                    onEditTask = ::openTaskEditor,
                    onOpenGym = { appDestination = AppDestination.Gym },
                    onStartRoutine = { routineId, dayId -> gymViewModel?.startRoutine(routineId, dayId) },
                    onOpenGoals = { appDestination = AppDestination.Goals },
                    onOpenGoal = { projection -> openGoalIdRequested = projection.goal.id },
                    onEditGoal = { projection -> editGoalIdRequested = projection.goal.id },
                    onRecordGoal = { projection -> recordGoalIdRequested = projection.goal.id },
                    onResetElapsedGoal = { projection -> resetElapsedGoalIdRequested = projection.goal.id },
                    onToggleMilestone = { boundary, completed ->
                        goalViewModel?.toggleMilestone(boundary, completed)
                    },
                    onOpenTracks = { appDestination = AppDestination.Tracks },
                    onOpenTrack = { projection -> openTrackIdRequested = projection.track.id; appDestination = AppDestination.Tracks },
                    onEditTrack = { projection -> editTrackIdRequested = projection.track.id; appDestination = AppDestination.Tracks },
                    onAddTrackEntry = { projection -> addTrackEntryRequestedForId = projection.track.id; appDestination = AppDestination.Tracks },
                    onSelectHomeTaskFilter = { name ->
                        settingsViewModel?.selectHomeTaskFilter(name)
                    },
                    onOpenReview = { reviewOpen = true },
                    showFullHeader = adaptiveLayout == WhipAdaptiveLayout.Compact || contentPaneIsExpanded,
                    areaScopeLabel = when (areaScope) {
                        AreaScope.All -> null
                        AreaScope.Unassigned -> "Main"
                        is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name
                    },
                    onShowAllAreas = { onSelectAreaScope(AreaScope.All) },
                    onRetryTaskLoading = domainRetryActions.tasks,
                    onRetryHabitLoading = domainRetryActions.habits,
                    onRetryGoalLoading = domainRetryActions.goals,
                    onRetryTrackLoading = domainRetryActions.tracks,
                    onRetryGymLoading = domainRetryActions.gym,
                )
                if (habitViewModel != null) HabitAreaContent(
                    state = habitState,
                    editorState = unscopedHabitState,
                    innerPadding = PaddingValues(),
                    viewModel = habitViewModel,
                    modifier = paneDialogModifier,
                    editorModifier = primaryEditorPaneModifier,
                    openHabitIdRequest = openHabitIdRequested,
                    onOpenHabitRequestConsumed = { openHabitIdRequested = null },
                    editHabitIdRequest = editHabitIdRequested,
                    onEditHabitRequestConsumed = { editHabitIdRequested = null },
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    lowPressureMode = settingsState.settings.lowPressureMode,
                    areas = settingsState.areas,
                    defaultAreaId = areaScope.creationDefaultAreaId(settingsState.areas),
                    onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                    onCreateCustomUnit = { name, symbol, dimension, factor, result ->
                        settingsViewModel?.createCustomUnit(name, symbol, dimension, factor, result)
                            ?: result(Result.failure(IllegalStateException("Settings are unavailable")))
                    },
                    customIdentityEmojis = settingsState.settings.customIdentityEmojis,
                    onSaveIdentityEmoji = { settingsViewModel?.upsertCustomIdentityEmoji(choice = it) },
                    onRemoveSavedIdentityEmoji = { settingsViewModel?.removeCustomIdentityEmoji(it) },
                    onAreaChanged = { keepSavedItemVisible(it.areaId, it.areaVerified) },
                    showWorkspace = false,
                    mutationRequestNamespace = "home-habit-area",
                )
                if (goalViewModel != null) GoalAreaContent(
                    state = goalState,
                    editorState = unscopedGoalState,
                    innerPadding = PaddingValues(),
                    viewModel = goalViewModel,
                    modifier = paneDialogModifier,
                    editorModifier = primaryEditorPaneModifier,
                    recordGoalIdRequest = recordGoalIdRequested,
                    resetElapsedGoalIdRequest = resetElapsedGoalIdRequested,
                    onExternalRequestConsumed = {
                        recordGoalIdRequested = null
                        resetElapsedGoalIdRequested = null
                    },
                    openGoalIdRequest = openGoalIdRequested,
                    onOpenGoalRequestConsumed = { openGoalIdRequested = null },
                    editGoalIdRequest = editGoalIdRequested,
                    onEditGoalRequestConsumed = { editGoalIdRequested = null },
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    areas = settingsState.areas,
                    defaultAreaId = areaScope.creationDefaultAreaId(settingsState.areas),
                    onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                    onCreateCustomUnit = { name, symbol, dimension, factor, result ->
                        settingsViewModel?.createCustomUnit(name, symbol, dimension, factor, result)
                            ?: result(Result.failure(IllegalStateException("Settings are unavailable")))
                    },
                    customIdentityEmojis = settingsState.settings.customIdentityEmojis,
                    onSaveIdentityEmoji = { settingsViewModel?.upsertCustomIdentityEmoji(choice = it) },
                    onRemoveSavedIdentityEmoji = { settingsViewModel?.removeCustomIdentityEmoji(it) },
                    onAreaChanged = { keepSavedItemVisible(it.areaId, it.areaVerified) },
                    showWorkspace = false,
                    mutationRequestNamespace = "home-goal-area",
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
                    onBulkArchiveRequest = onBulkArchiveTasks,
                    onBulkRestore = onBulkRestoreTasks,
                    onBulkReopen = onBulkReopenTasks,
                    onBulkPin = onBulkPinTasks,
                    onBulkPostpone = onBulkPostponeTasks,
                    onBulkPostponeRequest = onBulkPostponeTasksRequest,
                    onBulkEdit = onBulkEditTasks,
                    onBulkEditRequest = onBulkEditTasksRequest,
                    onBulkDeletePermanently = onBulkDeleteTasksPermanently,
                    onBulkDeletePermanentlyRequest = onBulkDeleteTasksPermanentlyRequest,
                    authoredMutationState = taskAuthoredMutationState,
                    onAuthoredMutationResultConsumed = onTaskAuthoredMutationResultConsumed,
                    deletionBatchImpact = taskDeletionBatchImpact,
                    onPreviewBulkDeletion = onPreviewBulkTaskDeletion,
                    onClearBulkDeletionPreview = onClearBulkTaskDeletionPreview,
                    onSetHabitPlanningOverlay = { enabled -> settingsViewModel?.update { it.copy(showHabitsInTaskPlanning = enabled) } },
                    onActiveTaskSortModeChange = { mode ->
                        settingsViewModel?.update { it.copy(activeTaskSortMode = mode) }
                    },
                    onOpenPlanningHabit = { habitId -> openHabitIdRequested = habitId; appDestination = AppDestination.Habits },
                    onReorder = onReorderTasks,
                    onPlanMyDay = onPlanMyDay,
                    onStopFocus = { settingsViewModel?.stopFocusTimer() },
                    onQuickCapture = onQuickAddTaskWithResult,
                    onQuickCaptureRequest = onQuickAddTaskRequest,
                    onAddDetails = { capture ->
                        openTaskEditor(
                            capture = capture,
                            scheduleDate = when (taskDestination) {
                                TaskDestination.Today -> state.currentDate
                                TaskDestination.Upcoming -> state.currentDate.plusDays(1)
                                else -> null
                            },
                            placement = taskDestination.creationPlacement(),
                        )
                    },
                    areas = settingsState.areas,
                    areaScope = areaScope,
                    onSelectAreaScope = onSelectAreaScope,
                    onTemporarilySelectAreaScope = onTemporarilySelectAreaScope,
                    planningViewRequest = taskPlanningViewRequest,
                    onPlanningViewRequestConsumed = { taskPlanningViewRequest = null },
                    allAreaTaskCount = unscopedTaskState.tasksFor(taskDestination)
                        .distinctBy { it.task.id }.size,
                    modifier = paneDialogModifier,
                    onSelectionModeChange = { taskSelectionMode = it },
                    onReorderModeChange = { reorderModeActive = it },
                    reorderDismissRequest = reorderDismissRequest,
                    onRetryLoading = domainRetryActions.tasks,
                )
            }
            AppDestination.Habits -> {
                if (habitViewModel != null) {
                    HabitAreaContent(
                        state = habitState,
                        editorState = unscopedHabitState,
                        innerPadding = innerPadding,
                        viewModel = habitViewModel,
                        createRequested = createHabitRequested,
                        onCreateRequestConsumed = { createHabitRequested = false },
                        openHabitIdRequest = openHabitIdRequested,
                        onOpenHabitRequestConsumed = { openHabitIdRequested = null },
                        editHabitIdRequest = editHabitIdRequested,
                        onEditHabitRequestConsumed = { editHabitIdRequested = null },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        lowPressureMode = settingsState.settings.lowPressureMode,
                        modifier = paneDialogModifier,
                        editorModifier = primaryEditorPaneModifier,
                        areas = settingsState.areas,
                        defaultAreaId = areaScope.creationDefaultAreaId(settingsState.areas),
                        onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                        onCreateCustomUnit = { name, symbol, dimension, factor, result ->
                            settingsViewModel?.createCustomUnit(name, symbol, dimension, factor, result)
                                ?: result(Result.failure(IllegalStateException("Settings are unavailable")))
                        },
                        customIdentityEmojis = settingsState.settings.customIdentityEmojis,
                        onSaveIdentityEmoji = { settingsViewModel?.upsertCustomIdentityEmoji(choice = it) },
                        onRemoveSavedIdentityEmoji = { settingsViewModel?.removeCustomIdentityEmoji(it) },
                        areaScopeLabel = when (areaScope) {
                            AreaScope.All -> null
                            AreaScope.Unassigned -> "Main"
                            is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name
                        },
                        onShowAllAreasForReorder = { onTemporarilySelectAreaScope(AreaScope.All) },
                        onAreaChanged = { keepSavedItemVisible(it.areaId, it.areaVerified) },
                        destinationState = habitDestinationState,
                        onReorderModeChange = { reorderModeActive = it },
                        reorderDismissRequest = reorderDismissRequest,
                        mutationRequestNamespace = "habit-workspace",
                    )
                } else RoadmapEmptyArea("Habits", "Habits are loading.", innerPadding)
            }
            AppDestination.Gym -> {
                if (gymViewModel != null) {
                    GymAreaContent(
                        state = gymState,
                        innerPadding = innerPadding,
                        viewModel = gymViewModel,
                        addRequest = gymAddRequest,
                        onExternalRequestConsumed = { gymAddRequest = null },
                        openSearchRequest = openGymSearchRequested,
                        onOpenSearchRequestConsumed = { openGymSearchDomain = null; openGymSearchId = null },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenBackupSettings = ::openSettings,
                        modifier = paneDialogModifier,
                        onRoutineEditorStateChange = { gymRoutineEditorOpen = it },
                        operationStatus = gymOperationStatus,
                        initialDestination = gymDestination,
                        onDestinationChange = { gymDestination = it },
                        requestedWorkoutExerciseId = requestedWorkoutExerciseId,
                        onRequestedWorkoutExerciseConsumed = { requestedWorkoutExerciseId = null },
                        onReorderModeChange = { reorderModeActive = it },
                        reorderDismissRequest = reorderDismissRequest,
                    )
                } else {
                    RoadmapEmptyArea("Gym", "Gym is loading.", innerPadding)
                }
            }
            AppDestination.Goals -> {
                if (goalViewModel != null) GoalAreaContent(
                    state = goalState,
                    editorState = unscopedGoalState,
                    innerPadding = innerPadding,
                    viewModel = goalViewModel,
                    createRequested = createGoalRequested,
                    recordGoalIdRequest = recordGoalIdRequested,
                    resetElapsedGoalIdRequest = resetElapsedGoalIdRequested,
                    onExternalRequestConsumed = {
                        createGoalRequested = false
                        recordGoalIdRequested = null
                        resetElapsedGoalIdRequested = null
                    },
                    openGoalIdRequest = openGoalIdRequested,
                    onOpenGoalRequestConsumed = { openGoalIdRequested = null },
                    editGoalIdRequest = editGoalIdRequested,
                    onEditGoalRequestConsumed = { editGoalIdRequested = null },
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    modifier = paneDialogModifier,
                    editorModifier = primaryEditorPaneModifier,
                    areas = settingsState.areas,
                    defaultAreaId = areaScope.creationDefaultAreaId(settingsState.areas),
                    onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                        onCreateCustomUnit = { name, symbol, dimension, factor, result ->
                            settingsViewModel?.createCustomUnit(name, symbol, dimension, factor, result)
                                ?: result(Result.failure(IllegalStateException("Settings are unavailable")))
                        },
                        customIdentityEmojis = settingsState.settings.customIdentityEmojis,
                        onSaveIdentityEmoji = { settingsViewModel?.upsertCustomIdentityEmoji(choice = it) },
                        onRemoveSavedIdentityEmoji = { settingsViewModel?.removeCustomIdentityEmoji(it) },
                        areaScopeLabel = when (areaScope) {
                        AreaScope.All -> null
                        AreaScope.Unassigned -> "Main"
                        is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name
                    },
                    onShowAllAreasForReorder = { onTemporarilySelectAreaScope(AreaScope.All) },
                    onAreaChanged = { keepSavedItemVisible(it.areaId, it.areaVerified) },
                    destinationState = goalDestinationState,
                    onReorderModeChange = { reorderModeActive = it },
                    reorderDismissRequest = reorderDismissRequest,
                    mutationRequestNamespace = "goal-workspace",
                )
                else RoadmapEmptyArea("Goals", "Goals are loading.", innerPadding)
            }
            AppDestination.Tracks -> {
                if (trackViewModel != null) TrackAreaContent(
                    state = trackState,
                    viewModel = trackViewModel,
                    innerPadding = innerPadding,
                    areas = settingsState.areas,
                    customUnits = settingsState.customUnits,
                    defaultAreaId = areaScope.creationDefaultAreaId(settingsState.areas),
                    createRequested = createTrackRequested,
                    onCreateRequestConsumed = { createTrackRequested = false },
                    openTrackIdRequest = openTrackIdRequested,
                    onOpenTrackRequestConsumed = { openTrackIdRequested = null },
                    editTrackIdRequest = editTrackIdRequested,
                    onEditTrackRequestConsumed = { editTrackIdRequested = null },
                    openEntryIdRequest = openTrackEntryIdRequested,
                    onOpenEntryRequestConsumed = { openTrackEntryIdRequested = null },
                    addEntryTrackIdRequest = addTrackEntryRequestedForId,
                    onAddEntryTrackRequestConsumed = { addTrackEntryRequestedForId = null },
                    operationStatus = trackOperationStatus,
                    editorOpen = trackEditorRoute != null,
                    onEditorRequest = ::openTrackEditor,
                    onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
                    selectedTrackState = selectedTrackState,
                    workspaceDestinationState = trackWorkspaceDestinationState,
                    destinationState = trackDetailDestinationState,
                    dialogModifier = paneDialogModifier,
                    reorderEnabled = areaScope == AreaScope.All,
                    onShowAllAreasForReorder = { onTemporarilySelectAreaScope(AreaScope.All) },
                    onReorderModeChange = { reorderModeActive = it },
                    reorderDismissRequest = reorderDismissRequest,
                ) else RoadmapEmptyArea("Tracks", "Tracks are loading.", innerPadding)
            }
            AppDestination.Settings -> {
                if (settingsViewModel != null) SettingsContent(
                    settingsState,
                    innerPadding,
                    settingsViewModel,
                    onEditAreas = { areaManagerOpen = true },
                    onDataReset = ::returnToHomeAfterDataReset,
                    selectedSection = settingsSection.takeIf {
                        adaptiveLayout in setOf(WhipAdaptiveLayout.ExpandedDashboard, WhipAdaptiveLayout.BookFold, WhipAdaptiveLayout.TabletopFold) &&
                            !contentPaneIsExpanded
                    },
                    onSectionChange = { settingsSection = it },
                )
                else RoadmapEmptyArea("Settings", "Settings are loading.", innerPadding)
            }
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
            taskState = unscopedTaskState,
            habitState = unscopedHabitState,
            goalState = unscopedGoalState,
            gymState = gymState,
            modifier = primaryEditorPaneModifier,
            trackState = unscopedTrackState,
            onDismiss = { searchOpen = false },
            areaScope = areaScope,
            areaScopeLabel = when (val scope = areaScope) {
                AreaScope.All -> null
                AreaScope.Unassigned -> "Main"
                is AreaScope.One -> settingsState.areas.firstOrNull { it.id == scope.areaId }?.name ?: "All Areas"
            },
            initialScope = searchEntryContext.defaultSearchScope(),
        ) { result ->
            if (result.domain in setOf(SearchDomain.Task, SearchDomain.Habit, SearchDomain.Goal, SearchDomain.Track, SearchDomain.TrackEntry) && !areaScope.matches(result.areaId)) {
                onTemporarilySelectAreaScope(
                    result.areaId?.let(AreaScope::One)
                        ?: settingsState.areas.firstOrNull { !it.archived }?.id?.let(AreaScope::One)
                        ?: AreaScope.All,
                )
            }
            when (result.domain) {
                SearchDomain.Task -> {
                    appDestination = AppDestination.Tasks
                    (unscopedTaskState.inbox + unscopedTaskState.today + unscopedTaskState.upcoming + unscopedTaskState.planning + unscopedTaskState.completed + unscopedTaskState.archived)
                        .firstOrNull { it.task.id == result.id }
                        ?.let { found ->
                            if (found in unscopedTaskState.completed) completedItemKey = found.stableKey else {
                                if (found in unscopedTaskState.inbox) taskDestination = TaskDestination.Inbox
                                else if (found in unscopedTaskState.planning) taskDestination = TaskDestination.Upcoming
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
                SearchDomain.Track -> {
                    openTrackIdRequested = result.id
                    appDestination = AppDestination.Tracks
                }
                SearchDomain.TrackEntry -> {
                    openTrackEntryIdRequested = result.id
                    appDestination = AppDestination.Tracks
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
            taskState = state,
            habitState = habitState,
            goalState = goalState,
            gymState = gymState,
            period = settingsState.settings.reviewPeriod,
            modifier = Modifier.fillMaxSize(),
            wideLeadingPaneWidth = dialogSupportExtent,
            wideHingeWidth = dialogHingeWidth,
            zone = settingsState.settings.zoneId(),
            onPeriodChange = { period -> settingsViewModel?.update { it.copy(reviewPeriod = period) } },
            onDismiss = { reviewOpen = false },
            sections = settingsState.settings.reviewSections,
            onSectionsChange = { settingsViewModel?.setReviewSections(it) },
            onDrillDown = { section ->
                reviewOpen = false
                appDestination = when (section) {
                    ReviewSection.Tasks -> {
                        taskDestination = TaskDestination.Completed
                        AppDestination.Tasks
                    }
                    ReviewSection.Habits -> AppDestination.Habits
                    ReviewSection.Goals -> AppDestination.Goals
                    ReviewSection.Gym -> AppDestination.Gym
                }
            },
            productivityAreaLabel = when (areaScope) {
                AreaScope.All -> null
                AreaScope.Unassigned -> "Main"
                is AreaScope.One -> settingsState.areas.firstOrNull { it.id == areaScope.areaId }?.name ?: "Selected Area"
            },
            trackState = trackState,
            onOpenTracks = {
                reviewOpen = false
                appDestination = AppDestination.Tracks
            },
        )
    }
    HomeHabitValueRoute(
        item = homeHabitValueItem,
        itemIdState = homeHabitValueItemIdState,
        viewModel = habitViewModel,
    )

    editorRequest?.let { request ->
        TaskEditorDialog(
            request = request,
            onDismiss = ::closeTaskEditor,
            onSave = { taskId, draft, fromOccurrence ->
                val requestId = taskEditorSaveCoordinator.begin() ?: return@TaskEditorDialog
                taskEditorSaveAndNew = false
                if (!onSaveTaskRequest(taskId, request.expectedBoundary, draft, fromOccurrence, requestId)) {
                    taskEditorSaveCoordinator.finishFailure("Another Task save is already finishing.")
                }
            },
            onSaveAndNew = { taskId, draft, fromOccurrence ->
                val requestId = taskEditorSaveCoordinator.begin() ?: return@TaskEditorDialog
                taskEditorSaveAndNew = true
                if (!onSaveTaskRequest(taskId, request.expectedBoundary, draft, fromOccurrence, requestId)) {
                    taskEditorSaveAndNew = false
                    taskEditorSaveCoordinator.finishFailure("Another Task save is already finishing.")
                }
            },
            onRequestNotificationPermission = onRequestNotificationPermission,
            defaultRepeatStepPolicy = settingsState.settings.defaultTaskStepPolicy,
            firstDayOfWeek = settingsState.settings.firstDayOfWeek,
            today = state.currentDate,
            naturalLanguageCapture = settingsState.settings.naturalLanguageTaskCapture,
            powerMode = settingsState.settings.powerMode,
            areas = settingsState.areas,
            defaultAreaId = areaScope.creationDefaultAreaId(settingsState.areas),
            inheritedAreaFromScope = areaScope is AreaScope.One,
            onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
            knownTags = (state.inbox + state.today + state.upcoming + state.planning + state.completed + state.archived)
                .flatMap { it.task.tags }.distinct().sorted(),
            customIdentityEmojis = settingsState.settings.customIdentityEmojis,
            onSaveIdentityEmoji = { settingsViewModel?.upsertCustomIdentityEmoji(choice = it) },
            onRemoveSavedIdentityEmoji = { settingsViewModel?.removeCustomIdentityEmoji(it) },
            paneOffsetX = dialogPaneOffset,
            paneMaxWidth = dialogContentWidth,
            saving = taskEditorSaveCoordinator.saving,
            persistenceError = taskEditorSaveCoordinator.errorMessage,
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
                rescheduleItemSnapshot = item
                rescheduleSnapshotTitle = item.task.title
                rescheduleSnapshotEpochDay = (item.scheduledDate ?: state.currentDate).toEpochDay()
                rescheduleItemKey = item.stableKey
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
                deleteItemSnapshot = item
                deleteSnapshotTaskId = item.task.id
                deleteSnapshotTitle = item.task.title
                deleteSnapshotRecurring = item.task.scheduleKind == ScheduleKind.Recurring
                onClearTaskDeletionPreview()
                deleteItemKey = item.stableKey
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
            onStartFocus = { minutes ->
                settingsViewModel?.startFocusTimer(item.task.id, minutes)
                onRequestNotificationPermission()
                actionItemKey = null
            },
            onToggleSubtask = { stepId, checked ->
                onSetStepCompleted(item, stepId, checked)
            },
            onPromoteSubtask = { stepId ->
                onPromoteStep(item, stepId)
            },
            occurrenceHistory = state.occurrences.filter { it.taskId == item.task.id },
            onReopenOccurrence = { occurrence ->
                onReopenOccurrence(
                    ScheduledTask(
                        task = item.task,
                        originalDate = occurrence.originalDate,
                        scheduledDate = occurrence.scheduledDate,
                        completedAtMillis = occurrence.completedAtMillis,
                        occurrenceState = occurrence.state,
                    ),
                )
            },
            onResetOccurrence = { occurrence ->
                onResetOccurrence(
                    ScheduledTask(
                        task = item.task,
                        originalDate = occurrence.originalDate,
                        scheduledDate = occurrence.scheduledDate,
                        completedAtMillis = occurrence.completedAtMillis,
                        occurrenceState = occurrence.state,
                    ),
                )
            },
            modifier = paneDialogModifier,
        )
    }

    pendingCompleteItem?.let { item ->
        PaneAwareAlertDialog(
            modifier = paneDialogModifier,
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
                ) { Text(stringResource(R.string.action_complete_anyway)) }
            },
            dismissButton = {
                WhipTextButton(onClick = { pendingCompleteItemKey = null }) {
                    Text(stringResource(R.string.action_keep_working))
                }
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
                    onReopen(item)
                }
            },
            onDeletePermanently = {
                deleteItemSnapshot = item
                deleteSnapshotTaskId = item.task.id
                deleteSnapshotTitle = item.task.title
                deleteSnapshotRecurring = item.task.scheduleKind == ScheduleKind.Recurring
                onClearTaskDeletionPreview()
                deleteItemKey = item.stableKey
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
                        occurrenceState = occurrence.state,
                    ),
                )
            },
            onResetOccurrence = { occurrence ->
                onResetOccurrence(
                    ScheduledTask(
                        task = item.task,
                        originalDate = occurrence.originalDate,
                        scheduledDate = occurrence.scheduledDate,
                        completedAtMillis = occurrence.completedAtMillis,
                        occurrenceState = occurrence.state,
                    ),
                )
            },
            modifier = paneDialogModifier,
        )
    }

    deleteItemKey?.let { routeKey ->
        val targetTaskId = deleteItem?.task?.id ?: deleteSnapshotTaskId ?: return@let
        TaskPermanentDeleteRoute(
            routeKey = routeKey,
            item = deleteItem,
            taskId = targetTaskId,
            taskTitle = deleteItem?.task?.title ?: deleteSnapshotTitle,
            recurringSeries = deleteItem?.task?.scheduleKind == ScheduleKind.Recurring || deleteSnapshotRecurring,
            impact = taskDeletionImpact,
            state = taskAuthoredMutationState,
            consume = onTaskAuthoredMutationResultConsumed,
            onPreview = onPreviewTaskDeletion,
            onClearPreview = onClearTaskDeletionPreview,
            onDeleteRequest = onDeleteTaskPermanentlyRequest,
            modifier = paneDialogModifier,
            onClose = {
                deleteItemKey = null
                deleteItemSnapshot = null
                deleteSnapshotTaskId = null
                deleteSnapshotTitle = ""
                deleteSnapshotRecurring = false
            },
        )
    }

    rescheduleItemKey?.let { routeKey ->
        TaskRescheduleRoute(
            routeKey = routeKey,
            item = rescheduleItem,
            snapshotTitle = rescheduleSnapshotTitle,
            snapshotEpochDay = rescheduleSnapshotEpochDay,
            currentDate = state.currentDate,
            state = taskAuthoredMutationState,
            consume = onTaskAuthoredMutationResultConsumed,
            onRescheduleRequest = onRescheduleRequest,
            modifier = paneDialogModifier,
            onClose = {
                rescheduleItemKey = null
                rescheduleItemSnapshot = null
                rescheduleSnapshotTitle = ""
                rescheduleSnapshotEpochDay = null
            },
        )
    }
    TrackEditorRouteHost(
        routeState = trackEditorRouteState,
        sessionState = trackEditorSessionState,
        openTrackIdState = openTrackIdRequestedState,
        adaptiveLayout = adaptiveLayout,
        foldInfo = foldInfo,
        contentExpanded = contentPaneIsExpanded,
        userDataGeneration = userDataGeneration,
        unscopedTrackState = unscopedTrackState,
        trackState = trackState,
        settingsState = settingsState,
        settingsViewModel = settingsViewModel,
        defaultAreaId = areaScope.creationDefaultAreaId(settingsState.areas),
        trackViewModel = trackViewModel,
        trackOperationStatus = trackOperationStatus,
        onGenerationInvalidated = {
            presentTransientFeedback(
                source = "track-editor-generation",
                priority = 3,
                recoverable = true,
            ) {
                snackbarHostState.showSnackbar(
                    "Whip data changed while this Track editor was open. Reopen the Track to edit the restored version.",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
            }
        },
        onDefinitionPersisted = { receipt ->
            keepSavedItemVisible(receipt.areaId, receipt.areaVerified)
        },
    )
    FirstRunSetupRoute(
        settingsState = settingsState,
        settingsViewModel = settingsViewModel,
        onRequestNotificationPermission = onRequestNotificationPermission,
    )
}

@Composable
private fun HomeHabitValueRoute(
    item: HabitDayProgress?,
    itemIdState: MutableState<Long?>,
    viewModel: HabitViewModel?,
) {
    item ?: return
    viewModel ?: return
    val authoredMutationState by viewModel.authoredMutationState.collectAsStateWithLifecycle()
    val authoredMutationCoordinator = rememberPersistenceRequestCoordinator(
        state = authoredMutationState,
        consume = viewModel::consumeAuthoredMutationResult,
        key = "home-habit-${item.habit.id}",
        requestNamespace = "home-habit-quick",
        onPersisted = { itemIdState.value = null },
    )
    HabitValueDialog(
        item = item,
        saving = authoredMutationCoordinator.saving,
        persistenceError = authoredMutationCoordinator.errorMessage,
        onDismiss = {
            authoredMutationCoordinator.clear()
            itemIdState.value = null
        },
        onLog = { value, note ->
            val requestId = authoredMutationCoordinator.begin() ?: return@HabitValueDialog
            val accepted = if (item.habit.trackingMode == com.whip.app.domain.HabitTrackingMode.LogOnly) {
                viewModel.log(item.habit.id, value, note = note, requestId = requestId)
            } else {
                viewModel.setPeriodValue(item, requireNotNull(value), note, requestId = requestId)
            }
            if (!accepted) {
                authoredMutationCoordinator.finishFailure(
                    "Another Habit history change is already finishing.",
                )
            }
        },
    )
}

@Composable
private fun FirstRunSetupRoute(
    settingsState: SettingsUiState,
    settingsViewModel: SettingsViewModel?,
    onRequestNotificationPermission: () -> Unit,
) {
    if (settingsState.settings.setupCompleted || settingsViewModel == null) return
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

private fun resolveTrackEditorRoute(
    intent: TrackEditorIntent,
    unscopedTrackState: TrackUiState,
    userDataGeneration: Long,
    sessionId: Long,
): TrackEditorRoute? = when (intent) {
    is TrackEditorIntent.Definition -> {
        val snapshot = intent.trackId?.let(unscopedTrackState::track)
        if (intent.trackId != null && snapshot == null) {
            null
        } else {
            TrackEditorRoute.Definition(
                trackId = intent.trackId,
                initialDraft = snapshot?.let { it.track.toDraft(it.fields, it.options) },
                targetName = snapshot?.track?.name,
                openingBoundary = null,
                openingDataGeneration = userDataGeneration,
                sessionId = sessionId,
            )
        }
    }
    is TrackEditorIntent.Entry -> TrackEditorRoute.Entry(
        trackId = intent.trackId,
        entryId = intent.entryId,
        openingDataGeneration = userDataGeneration,
        sessionId = sessionId,
    )
}

internal enum class TrackEntryRouteAvailability { Available, TrackMissing, EntryMissing }

internal fun trackEditorRouteMatchesGeneration(
    route: TrackEditorRoute,
    userDataGeneration: Long,
): Boolean = route.openingDataGeneration == userDataGeneration

internal fun trackEntryRouteAvailability(
    route: TrackEditorRoute.Entry,
    unscopedTrackState: TrackUiState,
): TrackEntryRouteAvailability {
    val projection = unscopedTrackState.track(route.trackId)
        ?: return TrackEntryRouteAvailability.TrackMissing
    return if (
        route.entryId != null && projection.entries.none { it.entry.id == route.entryId }
    ) {
        TrackEntryRouteAvailability.EntryMissing
    } else {
        TrackEntryRouteAvailability.Available
    }
}

@Composable
private fun TrackEditorRouteHost(
    routeState: MutableState<TrackEditorRoute?>,
    sessionState: MutableState<Long>,
    openTrackIdState: MutableState<Long?>,
    adaptiveLayout: WhipAdaptiveLayout,
    foldInfo: WhipFoldInfo?,
    contentExpanded: Boolean,
    userDataGeneration: Long,
    unscopedTrackState: TrackUiState,
    trackState: TrackUiState,
    settingsState: SettingsUiState,
    settingsViewModel: SettingsViewModel?,
    defaultAreaId: String?,
    trackViewModel: TrackViewModel?,
    trackOperationStatus: OperationStatus,
    onGenerationInvalidated: suspend () -> Unit,
    onDefinitionPersisted: (EntitySaveReceipt) -> Unit,
) {
    val route = routeState.value
    route ?: return
    if (!trackEditorRouteMatchesGeneration(route, userDataGeneration)) {
        LaunchedEffect(route.sessionId, userDataGeneration) {
            if (route is TrackEditorRoute.Definition) {
                trackViewModel?.clearDefinitionEditorState(route.sessionId)
            }
            routeState.value = null
            onGenerationInvalidated()
        }
        return
    }
    val saveStateState = trackViewModel?.definitionSaveState?.collectAsStateWithLifecycle()
    val definitionSaveState = saveStateState?.value ?: PersistenceRequestState.Idle
    val reviewStateState = trackViewModel?.definitionReviewState?.collectAsStateWithLifecycle()
    val definitionReviewState = reviewStateState?.value ?: TrackDefinitionReviewUiState()
    RootEditorHost(
        adaptiveLayout = adaptiveLayout,
        foldInfo = foldInfo,
        contentExpanded = contentExpanded,
    ) { editorModifier ->
        when (route) {
            is TrackEditorRoute.Definition -> TrackDefinitionEditorRouteHost(
                route = route,
                liveInitial = route.trackId?.let(unscopedTrackState::track),
                settingsState = settingsState,
                settingsViewModel = settingsViewModel,
                defaultAreaId = defaultAreaId,
                trackViewModel = trackViewModel,
                definitionSaveState = definitionSaveState,
                definitionReviewState = definitionReviewState,
                modifier = editorModifier,
                onOpeningBoundary = { boundary ->
                    val activeRoute = routeState.value as? TrackEditorRoute.Definition
                    if (
                        activeRoute?.sessionId == route.sessionId &&
                        activeRoute.trackId == route.trackId &&
                        activeRoute.openingBoundary == null
                    ) {
                        routeState.value = activeRoute.copy(openingBoundary = boundary)
                    }
                },
                onPersisted = { receipt ->
                    val activeRoute = routeState.value as? TrackEditorRoute.Definition
                    if (
                        activeRoute?.sessionId == route.sessionId &&
                        activeRoute.trackId == route.trackId &&
                        route.openingDataGeneration == userDataGeneration
                    ) {
                        onDefinitionPersisted(receipt)
                        openTrackIdState.value = receipt.entityId
                        trackViewModel?.clearDefinitionEditorState(route.sessionId)
                        routeState.value = null
                    }
                },
                onDismiss = {
                    trackViewModel?.clearDefinitionEditorState(route.sessionId)
                    val activeRoute = routeState.value as? TrackEditorRoute.Definition
                    if (
                        activeRoute?.sessionId == route.sessionId &&
                        activeRoute.trackId == route.trackId
                    ) routeState.value = null
                },
            )
            is TrackEditorRoute.Entry -> {
                val projection = unscopedTrackState.track(route.trackId)
                val initial = route.entryId?.let { entryId ->
                    projection?.entries?.firstOrNull { it.entry.id == entryId }
                }
                when (trackEntryRouteAvailability(route, unscopedTrackState)) {
                    TrackEntryRouteAvailability.TrackMissing -> TrackEntryUnavailableRoute(
                        title = "Track Unavailable",
                        message = "This Track is no longer available. No Entry was added or changed.",
                        modifier = editorModifier,
                        onDismiss = { routeState.value = null },
                    )
                    TrackEntryRouteAvailability.EntryMissing -> TrackEntryUnavailableRoute(
                        title = "Entry Unavailable",
                        message = "This Entry is no longer available. It was not reinterpreted as a new Entry.",
                        modifier = editorModifier,
                        onDismiss = { routeState.value = null },
                    )
                    TrackEntryRouteAvailability.Available -> {
                    TrackEntryEditor(
                        projection = requireNotNull(projection),
                        initial = initial,
                        customUnits = settingsState.customUnits,
                        today = trackState.currentDate,
                        saving = trackOperationStatus is OperationStatus.Running,
                        modifier = editorModifier,
                        sessionId = route.sessionId,
                        onDismiss = { routeState.value = null },
                        onSave = { draft ->
                            trackViewModel?.saveEntry(route.trackId, route.entryId, draft) {
                                routeState.value = null
                            }
                        },
                        onDelete = route.entryId?.let { entryId ->
                            {
                                trackViewModel?.deleteEntry(entryId)
                                routeState.value = null
                            }
                        },
                        onOpenExisting = { existingId ->
                            val nextSession = sessionState.value + 1L
                            sessionState.value = nextSession
                            routeState.value = TrackEditorRoute.Entry(
                                trackId = route.trackId,
                                entryId = existingId,
                                openingDataGeneration = route.openingDataGeneration,
                                sessionId = nextSession,
                            )
                        },
                    )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackEntryUnavailableRoute(
    title: String,
    message: String,
    modifier: Modifier,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    WhipFullScreenSurface(title = title, modifier = modifier.testTag("track-entry-unavailable")) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, "Close Entry Editor")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                WhipStatusCard(
                    kind = WhipStatusKind.Error,
                    title = title,
                    message = message,
                    actionLabel = "Close",
                    onAction = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TrackDefinitionEditorRouteHost(
    route: TrackEditorRoute.Definition,
    liveInitial: TrackProjection?,
    settingsState: SettingsUiState,
    settingsViewModel: SettingsViewModel?,
    defaultAreaId: String?,
    trackViewModel: TrackViewModel?,
    definitionSaveState: PersistenceRequestState<EntitySaveReceipt>,
    definitionReviewState: TrackDefinitionReviewUiState,
    modifier: Modifier,
    onOpeningBoundary: (com.whip.app.domain.TrackDefinitionBoundary) -> Unit,
    onPersisted: (EntitySaveReceipt) -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(route.sessionId, route.trackId, route.initialDraft) {
        val trackId = route.trackId
        val openingDraft = route.initialDraft
        if (trackId != null && openingDraft != null) {
            trackViewModel?.prepareDefinitionEditor(
                route.sessionId,
                trackId,
                openingDraft,
            )
        }
    }
    LaunchedEffect(
        route.sessionId,
        definitionReviewState.boundary,
        definitionReviewState.sessionId,
    ) {
        val boundary = definitionReviewState.boundary
        if (
            boundary != null &&
            definitionReviewState.sessionId == route.sessionId &&
            route.openingBoundary == null
        ) {
            onOpeningBoundary(boundary)
        }
    }
    val definitionSaveCoordinator = rememberEntitySaveCoordinator(
        state = definitionSaveState,
        consume = { requestId -> trackViewModel?.consumeDefinitionSaveResult(requestId) },
        key = "track-definition-${route.sessionId}",
        requestNamespace = "track-definition-${route.sessionId}",
        orphanedMessage =
            "The previous Track save was interrupted and its outcome is unknown. " +
                "Verify the Track list and do not retry until you know whether it was saved.",
        onPersisted = onPersisted,
    )
    TrackEditor(
        liveInitial = liveInitial,
        targetTrackId = route.trackId,
        openingDraft = route.initialDraft,
        routeOpeningBoundary = route.openingBoundary,
        targetName = route.targetName,
        areas = settingsState.areas,
        customUnits = settingsState.customUnits,
        defaultAreaId = defaultAreaId,
        saving = definitionSaveCoordinator.saving,
        persistenceError = definitionSaveCoordinator.errorMessage,
        definitionReviewState = definitionReviewState,
        modifier = modifier,
        sessionId = route.sessionId,
        onDismiss = {
            definitionSaveCoordinator.clear()
            onDismiss()
        },
        onCreateArea = { name, color, result -> settingsViewModel?.createArea(name, color, result) },
        onCreateCustomUnit = { name, symbol, dimension, factor, result ->
            settingsViewModel?.createCustomUnit(name, symbol, dimension, factor, result)
                ?: result(Result.failure(IllegalStateException("Settings are unavailable")))
        },
        customIdentityEmojis = settingsState.settings.customIdentityEmojis,
        onSaveIdentityEmoji = { settingsViewModel?.upsertCustomIdentityEmoji(choice = it) },
        onRemoveSavedIdentityEmoji = { settingsViewModel?.removeCustomIdentityEmoji(it) },
        onRetryPreparation = {
            val trackId = route.trackId
            val openingDraft = route.initialDraft
            if (trackId != null && openingDraft != null) {
                trackViewModel?.prepareDefinitionEditor(
                    route.sessionId,
                    trackId,
                    openingDraft,
                )
            }
        },
        onReview = { draft, boundary, optionReplacements ->
            val trackId = route.trackId
            if (trackId != null) {
                trackViewModel?.reviewDefinitionUpdate(
                    route.sessionId,
                    trackId,
                    draft,
                    boundary,
                    optionReplacements,
                )
            }
        },
        onSave = { draft, boundary, reviewedRemoval ->
            val requestId = definitionSaveCoordinator.begin()
                ?: return@TrackEditor
            val accepted = trackViewModel?.saveTrack(
                route.trackId,
                draft,
                route.sessionId,
                boundary,
                reviewedRemoval,
                requestId,
            ) == true
            if (!accepted) {
                definitionSaveCoordinator.finishFailure(
                    "Another Track definition change is already finishing.",
                )
            }
        },
        onSaveCopy = { draft ->
            val requestId = definitionSaveCoordinator.begin()
                ?: return@TrackEditor
            val accepted = trackViewModel?.saveTrack(
                id = null,
                draft = draft,
                sessionId = route.sessionId,
                requestId = requestId,
            ) == true
            if (!accepted) {
                definitionSaveCoordinator.finishFailure(
                    "Another Track definition change is already finishing.",
                )
            }
        },
    )
}

@Composable
private fun TaskPermanentDeleteRoute(
    routeKey: String,
    item: ScheduledTask?,
    taskId: Long,
    taskTitle: String,
    recurringSeries: Boolean,
    impact: TaskDeletionImpact?,
    state: PersistenceRequestState<TaskMutationReceipt>,
    consume: (String) -> Unit,
    onPreview: (Long) -> Unit,
    onClearPreview: () -> Unit,
    onDeleteRequest: (Long, String, String) -> Boolean,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(routeKey, taskId, impact?.taskId) {
        if (impact?.taskId != taskId) onPreview(taskId)
    }
    val coordinator = rememberPersistenceRequestCoordinator(
        state = state,
        consume = consume,
        key = "delete-$routeKey",
        requestNamespace = "task-single-delete",
        onPersisted = { onClose() },
    )
    PermanentTaskDeleteDialog(
        item = item,
        taskId = taskId,
        taskTitle = taskTitle,
        recurringSeries = recurringSeries,
        impact = impact,
        modifier = modifier,
        saving = coordinator.saving,
        persistenceError = coordinator.errorMessage,
        onDismiss = {
            coordinator.clear()
            onClose()
            onClearPreview()
        },
        onConfirm = {
            val exactImpact = impact?.takeIf { it.taskId == taskId && it.exists }
                ?: return@PermanentTaskDeleteDialog
            val requestId = coordinator.begin() ?: return@PermanentTaskDeleteDialog
            if (!onDeleteRequest(taskId, exactImpact.revisionToken, requestId)) {
                coordinator.finishFailure("Another Task change is already finishing.")
            }
        },
    )
}

@Composable
private fun TaskRescheduleRoute(
    routeKey: String,
    item: ScheduledTask?,
    snapshotTitle: String,
    snapshotEpochDay: Long?,
    currentDate: LocalDate,
    state: PersistenceRequestState<TaskMutationReceipt>,
    consume: (String) -> Unit,
    onRescheduleRequest: (ScheduledTask, LocalDate, String) -> Boolean,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val coordinator = rememberPersistenceRequestCoordinator(
        state = state,
        consume = consume,
        key = "reschedule-$routeKey",
        requestNamespace = "task-single-reschedule",
        onPersisted = { onClose() },
    )
    if (item != null) {
        WhipDatePickerDialog(
            initialDate = snapshotEpochDay?.let(LocalDate::ofEpochDay) ?: item.scheduledDate ?: currentDate,
            modifier = modifier,
            saving = coordinator.saving,
            persistenceError = coordinator.errorMessage,
            savingLabel = "Saving Task Schedule",
            onDismiss = {
                coordinator.clear()
                onClose()
            },
            onDateSelected = { newDate ->
                val requestId = coordinator.begin() ?: return@WhipDatePickerDialog
                if (!onRescheduleRequest(item, newDate, requestId)) {
                    coordinator.finishFailure("Another Task change is already finishing.")
                }
            },
        )
    } else {
        PaneAwareAlertDialog(
            modifier = modifier,
            onDismissRequest = {
                if (!coordinator.saving) {
                    coordinator.clear()
                    onClose()
                }
            },
            inputBlocked = coordinator.saving,
            inputBlockedLabel = "Saving Task Schedule",
            title = { Text("Move ${snapshotTitle.ifBlank { "Task" }}") },
            text = {
                PersistenceFailureNotice(
                    coordinator.errorMessage
                        ?: "This Task changed or left the visible schedule. Close this message and open it again.",
                    testTag = "task-reschedule-target-problem",
                )
            },
            confirmButton = {},
            dismissButton = {
                WhipTextButton(
                    enabled = !coordinator.saving,
                    onClick = {
                        coordinator.clear()
                        onClose()
                    },
                ) { Text("Close") }
            },
        )
    }
}

@Composable
internal fun AreaScopeMenu(
    scope: AreaScope,
    areas: List<Area>,
    modifier: Modifier = Modifier,
    usage: Map<String, AreaUsageCounts> = emptyMap(),
    onSelect: (AreaScope) -> Unit,
    onManage: () -> Unit = {},
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
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
    Box(Modifier.testTag("workspace-area-action")) {
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
                    WhipMenuItem(
                        label = "Manage Archived Areas…",
                        onClick = { expanded = false; onManage() },
                    )
                }
                return@DropdownMenu
            }
            if (activeAreas.size > 1) {
                DropdownMenuItem(
                    text = { Text("All Areas") },
                    leadingIcon = if (scope == AreaScope.All) {{ Icon(Icons.Outlined.Check, contentDescription = "Selected") }} else null,
                    onClick = { onSelect(AreaScope.All); expanded = false },
                )
            }
            if (activeAreas.size > 8) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(40) },
                        label = { Text("Find area") },
                        singleLine = true,
                    )
                }
            }
            areas.filter { (!it.archived || it.id == selectedArea?.id) && (query.isBlank() || it.name.contains(query, true)) }.forEach { area ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val selected = (scope as? AreaScope.One)?.areaId == area.id ||
                                (scope == AreaScope.All && activeAreas.size == 1)
                            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                if (selected) Icon(Icons.Outlined.Check, contentDescription = "Selected", modifier = Modifier.size(18.dp))
                            }
                            area.colorArgb?.let { color -> Box(Modifier.size(10.dp).clip(CircleShape).background(Color(color))) }
                            Text("${area.name} · ${(usage[area.id]?.total ?: 0).itemCount("item")}")
                            if (area.archived) Text("Archived", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick = { onSelect(AreaScope.One(area.id)); expanded = false },
                )
            }
            HorizontalDivider()
            WhipMenuItem(
                label = "Create Area…",
                onClick = { expanded = false; creating = true },
            )
            WhipMenuItem(
                label = "Manage Areas",
                onClick = { expanded = false; onManage() },
            )
        }
    }
    if (creating) {
        CreateAreaDialog(
            modifier = modifier,
            existingAreas = areas,
            onDismiss = { creating = false },
            onCreate = onCreateArea,
            onSelected = { id, _ -> creating = false; onSelect(AreaScope.One(id)) },
        )
    }
}

private sealed interface AdaptiveLoadState {
    data object Loading : AdaptiveLoadState
    data class Failed(val message: String) : AdaptiveLoadState
    data object Ready : AdaptiveLoadState
}

private fun adaptiveLoadState(loading: Boolean, errorMessage: String?): AdaptiveLoadState = when {
    loading -> AdaptiveLoadState.Loading
    errorMessage != null -> AdaptiveLoadState.Failed(errorMessage)
    else -> AdaptiveLoadState.Ready
}

private val HomeSection.supportLabel: String
    get() = when (this) {
        HomeSection.Tasks -> "Tasks"
        HomeSection.Habits -> "Habits"
        HomeSection.Goals -> "Goals"
        HomeSection.Tracks -> "Tracks"
        HomeSection.Gym -> "Gym"
    }

private data class AdaptiveSummary(
    val date: LocalDate,
    val dueTasks: Int,
    val dueHabits: Int,
    val activeGoals: Int,
    val pinnedTracks: Int,
    val activeWorkout: Boolean,
    val taskLoadState: AdaptiveLoadState,
    val habitLoadState: AdaptiveLoadState,
    val goalLoadState: AdaptiveLoadState,
    val trackLoadState: AdaptiveLoadState,
    val gymLoadState: AdaptiveLoadState,
    val gymDestination: GymDestination,
    val visibleHomeSections: List<HomeSection>,
    val hasAnyUserData: Boolean,
    val taskContext: List<String> = emptyList(),
    val habitContext: List<String> = emptyList(),
    val goalContext: List<String> = emptyList(),
    val gymContextTitle: String = "Workout Context",
    val gymContext: List<String> = emptyList(),
) {
    fun loadState(section: HomeSection): AdaptiveLoadState = when (section) {
        HomeSection.Tasks -> taskLoadState
        HomeSection.Habits -> habitLoadState
        HomeSection.Goals -> goalLoadState
        HomeSection.Tracks -> trackLoadState
        HomeSection.Gym -> gymLoadState
    }
}

private fun buildAdaptiveSummary(
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    trackState: TrackUiState,
    gymState: GymUiState,
    settings: AppSettings,
    selectedTaskTitle: String?,
    gymDestination: GymDestination,
    nowMillis: Long,
): AdaptiveSummary {
    val taskLoadState = adaptiveLoadState(taskState.loading, taskState.errorMessage)
    val habitLoadState = adaptiveLoadState(habitState.loading, habitState.errorMessage)
    val goalLoadState = adaptiveLoadState(goalState.loading, goalState.errorMessage)
    val trackLoadState = adaptiveLoadState(trackState.loading, trackState.errorMessage)
    val gymLoadState = adaptiveLoadState(gymState.loading, gymState.errorMessage)
    return AdaptiveSummary(
        date = taskState.currentDate,
        dueTasks = taskState.today.size,
        dueHabits = habitState.today.count { it.successful != true },
        activeGoals = goalState.active.size,
        pinnedTracks = trackState.pinned.size,
        activeWorkout = gymState.activeSession != null,
        taskLoadState = taskLoadState,
        habitLoadState = habitLoadState,
        goalLoadState = goalLoadState,
        trackLoadState = trackLoadState,
        gymLoadState = gymLoadState,
        gymDestination = gymDestination,
        visibleHomeSections = settings.visibleHomeSections(),
        hasAnyUserData = homeHasAnyUserData(taskState, habitState, goalState, trackState, gymState),
        taskContext = listOfNotNull(selectedTaskTitle) + taskState.today.take(6).map { it.task.title },
        habitContext = habitState.today.take(6).map { item ->
            val target = item.habit.targetMax ?: item.habit.targetMin
            "${item.habit.name} · ${if (item.successful == true) "done" else target?.let {
                "${formatHabitValue(item.value, item.habit.precision)}/${formatHabitValue(it, item.habit.precision)}"
            } ?: "log"}"
        },
        goalContext = goalState.active.take(6).map { item ->
            "${item.goal.name} · ${item.collectionStatus(goalState.customUnits, nowMillis)}"
        },
        gymContextTitle = when (gymDestination) {
            GymDestination.Workout -> "Workout Context"
            GymDestination.History -> "Recent Workouts"
            GymDestination.Progress -> "Tracked Records"
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
                if (gymState.activeSession == null && gymLoadState == AdaptiveLoadState.Ready) add("No Active Workout")
            }
            GymDestination.History -> gymState.history.sortedByDescending { it.localDate }.take(6)
                .map { it.name.ifBlank { "Workout · ${it.localDate}" } }
                .ifEmpty { if (gymLoadState == AdaptiveLoadState.Ready) listOf("Completed workouts will appear here") else emptyList() }
            GymDestination.Progress -> gymState.appSettings.trackedGymRecords
                .filter { selection ->
                    gymState.exercises.firstOrNull { it.uuid == selection.exerciseUuid }
                        ?.let { exercise -> selection.type in exercise.supportedTrackedRecordTypes() } == true
                }
                .groupBy { it.exerciseUuid }
                .entries
                .take(6)
                .mapNotNull { (exerciseUuid, records) ->
                    val exerciseName = gymState.exercises.firstOrNull { it.uuid == exerciseUuid }?.name
                        ?: return@mapNotNull null
                    "$exerciseName · ${records.size} tracked record${if (records.size == 1) "" else "s"}"
                }.ifEmpty {
                    if (gymLoadState == AdaptiveLoadState.Ready) {
                        listOf(
                            quantityLabel(gymState.history.size, "completed workout"),
                            "Choose records to keep at a glance",
                        )
                    } else {
                        emptyList()
                    }
                }
            GymDestination.Routines -> gymState.routines.take(6).map { it.name }
                .ifEmpty { if (gymLoadState == AdaptiveLoadState.Ready) listOf("Create a routine to reuse a training plan") else emptyList() }
            GymDestination.Exercises -> gymState.exercises
                .sortedWith(compareByDescending<com.whip.app.domain.Exercise> { it.favorite }.thenBy { it.name })
                .take(6).map { it.name }
                .ifEmpty { if (gymLoadState == AdaptiveLoadState.Ready) listOf("Create your first reusable exercise") else emptyList() }
            GymDestination.Machines -> gymState.machines.take(6).map { it.displayName }
                .ifEmpty { if (gymLoadState == AdaptiveLoadState.Ready) listOf("Machine profiles preserve equipment setup") else emptyList() }
            GymDestination.Categories -> gymState.categories.take(6).map { it.name }
                .ifEmpty { if (gymLoadState == AdaptiveLoadState.Ready) listOf("Categories are optional exercise filters") else emptyList() }
            GymDestination.Tools -> listOf("1RM Calculator", "Plate Calculator")
            GymDestination.Library -> if (gymLoadState == AdaptiveLoadState.Ready) {
                listOf(
                    "${quantityLabel(gymState.exercises.size, "Exercise")} · ${quantityLabel(gymState.machines.size, "Machine")}",
                    "${quantityLabel(gymState.routines.size, "Routine")} · ${quantityLabel(gymState.history.size, "Completed Workout")}",
                )
            } else {
                emptyList()
            }
        },
    )
}

/**
 * The single app-level owner for destination editors. It visually and
 * interactively replaces the current destination on phones, while a separating
 * Fold keeps its contextual support pane visible, dimmed, and inert.
 */
@Composable
private fun RootEditorHost(
    adaptiveLayout: WhipAdaptiveLayout,
    foldInfo: WhipFoldInfo?,
    contentExpanded: Boolean,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(onClick = {})
                .semantics { hideFromAccessibility() },
        )
        val editorModifier = when {
            contentExpanded -> Modifier.fillMaxSize()
            adaptiveLayout == WhipAdaptiveLayout.BookFold -> {
                val density = LocalDensity.current
                val rawPaneWidth = with(density) { (foldInfo?.leftPx ?: 0).toDp() }
                val rawHingeWidth = with(density) { (foldInfo?.widthPx ?: 0).toDp() }
                val largestPaneWidth = (maxWidth - 300.dp).coerceAtLeast(260.dp)
                val supportWidth = rawPaneWidth.coerceIn(260.dp, largestPaneWidth) + rawHingeWidth.coerceAtLeast(1.dp)
                val contentPaneWidth = (maxWidth - supportWidth).coerceAtLeast(1.dp)
                if (shouldExpandRootEditor(contentPaneWidth, density.fontScale)) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .align(if (LocalLayoutDirection.current == LayoutDirection.Ltr) Alignment.CenterEnd else Alignment.CenterStart)
                        .width(contentPaneWidth)
                        .fillMaxHeight()
                }
            }
            adaptiveLayout == WhipAdaptiveLayout.TabletopFold -> {
                val density = LocalDensity.current
                val rawTopHeight = with(density) { (foldInfo?.topPx ?: 0).toDp() }
                val rawHingeHeight = with(density) { (foldInfo?.heightPx ?: 0).toDp() }
                val largestTopHeight = (maxHeight - 280.dp).coerceAtLeast(180.dp)
                val supportHeight = rawTopHeight.coerceIn(180.dp, largestTopHeight) + rawHingeHeight.coerceAtLeast(1.dp)
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height((maxHeight - supportHeight).coerceAtLeast(1.dp))
            }
            else -> Modifier.fillMaxSize()
        }
        content(editorModifier)
    }
}

internal fun shouldExpandRootEditor(contentPaneWidth: Dp, fontScale: Float): Boolean =
    contentPaneWidth < 320.dp * fontScale.coerceIn(1f, 2f)

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
    supportContent: (@Composable (Modifier) -> Unit)? = null,
    onSelect: (AppDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val frameModifier = modifier.background(MaterialTheme.colorScheme.background)
    val currentContent by rememberUpdatedState(content)
    val stableContent = remember {
        movableContentOf<Modifier> { contentModifier -> currentContent(contentModifier) }
    }
    if (contentExpanded) {
        Box(frameModifier.testTag("expanded-content-pane")) {
            stableContent(Modifier.fillMaxSize())
        }
        return
    }
    when (layout) {
        WhipAdaptiveLayout.Compact -> Box(frameModifier) { stableContent(Modifier.fillMaxSize()) }

        WhipAdaptiveLayout.NavigationRail -> Row(frameModifier) {
            WhipNavigationRail(selected, onSelect, navigationEnabled)
            stableContent(Modifier.weight(1f))
        }

        WhipAdaptiveLayout.ExpandedDashboard -> Row(frameModifier) {
            WhipNavigationRail(selected, onSelect, navigationEnabled)
            if (supportContent != null) {
                Surface(
                    modifier = Modifier.width(320.dp).fillMaxHeight().testTag("expanded-support-pane"),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) { supportContent(Modifier.fillMaxSize()) }
                VerticalDivider()
                stableContent(Modifier.weight(1f))
            } else {
                stableContent(Modifier.weight(1f))
                Surface(
                    modifier = Modifier.width(300.dp).fillMaxHeight().testTag("expanded-support-pane"),
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
        }

        WhipAdaptiveLayout.BookFold -> BoxWithConstraints(frameModifier) {
            val density = LocalDensity.current
            val rawPaneWidth = with(density) { (foldInfo?.leftPx ?: 0).toDp() }
            val rawHingeWidth = with(density) { (foldInfo?.widthPx ?: 0).toDp() }
            val largestPaneWidth = (maxWidth - 300.dp).coerceAtLeast(260.dp)
            val paneWidth = rawPaneWidth.coerceIn(260.dp, largestPaneWidth)
            val hingeWidth = rawHingeWidth.coerceAtLeast(1.dp)
            Row(Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .width(paneWidth)
                        .fillMaxHeight()
                        .testTag("fold-support-pane"),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Row(Modifier.fillMaxSize()) {
                        WhipNavigationRail(selected, onSelect, navigationEnabled)
                        if (supportContent != null) {
                            supportContent(Modifier.weight(1f).fillMaxHeight())
                        } else {
                            FoldContextPane(
                                summary,
                                selected,
                                onSelect,
                                Modifier.weight(1f),
                                onGymContextSelected = onGymContextSelected,
                                navigationEnabled = navigationEnabled,
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .width(hingeWidth)
                        .fillMaxHeight()
                        .semantics { contentDescription = "Device hinge separator" },
                    color = MaterialTheme.colorScheme.outlineVariant,
                ) {}
                stableContent(Modifier.weight(1f))
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
                Surface(
                    modifier = Modifier
                        .height(topHeight)
                        .fillMaxWidth()
                        .testTag("fold-support-pane"),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Column(Modifier.fillMaxSize()) {
                        TabletopNavigation(selected, onSelect, navigationEnabled)
                        if (supportContent != null) {
                            supportContent(Modifier.weight(1f).fillMaxWidth())
                        } else {
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
                    }
                }
                Surface(
                    modifier = Modifier
                        .height(hingeHeight)
                        .fillMaxWidth()
                        .semantics { contentDescription = "Device hinge separator" },
                    color = MaterialTheme.colorScheme.outlineVariant,
                ) {}
                stableContent(Modifier.weight(1f))
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
    showLabel: Boolean = true,
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
                modifier = Modifier.size(width = 48.dp, height = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }
        if (showLabel) {
            Surface(color = Color.Transparent, contentColor = itemColor) {
                label()
            }
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
    showLabel: Boolean = true,
) {
    val itemColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .width(80.dp)
            .heightIn(min = if (showLabel) 72.dp else 56.dp)
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
        if (showLabel) {
            Surface(color = Color.Transparent, contentColor = itemColor) {
                label()
            }
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                // Persistent navigation must not follow the IME. The active
                // content pane owns keyboard avoidance while the rail keeps
                // the same position users established before typing.
                .width(80.dp),
        ) {
            var stableRailHeight by remember { mutableStateOf(maxHeight) }
            LaunchedEffect(maxHeight) {
                if (maxHeight > stableRailHeight) stableRailHeight = maxHeight
            }
            // Large text must not turn the primary navigation into a row of
            // symbols that users have to memorize. The rail has a fixed width
            // and enough stable pre-IME height for its one-line labels.
            val showLabels = stableRailHeight >= 560.dp
            val destinationHeight = if (showLabels) 72.dp else 56.dp
            val stableTopOffset = (
                (stableRailHeight - destinationHeight * (primaryAppDestinations.size + 2)) / 2
            ).coerceAtLeast(12.dp)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = stableTopOffset)
                    .testTag("adaptive-navigation-rail-destinations"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WhipNavigationRailItem(
                    modifier = Modifier.semantics {
                        contentDescription = if (selected == AppDestination.Home) "Home" else "Go to Home"
                    },
                    selected = selected == AppDestination.Home,
                    enabled = enabled,
                    showLabel = showLabels,
                    onClick = { onSelect(AppDestination.Home) },
                    icon = { WhipBrandMark(Modifier.size(34.dp)) },
                    label = {
                        Text(
                            stringResource(R.string.nav_home),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                primaryAppDestinations.forEach { destination ->
                    val destinationLabel = stringResource(destination.labelRes)
                    val destinationTabDescription = stringResource(R.string.nav_tab_description, destinationLabel)
                    WhipNavigationRailItem(
                        modifier = Modifier.semantics { contentDescription = destinationTabDescription },
                        selected = destination == selected,
                        enabled = enabled,
                        showLabel = showLabels,
                        onClick = { onSelect(destination) },
                        icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(28.dp)) },
                        label = {
                            Text(
                                destinationLabel,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
                WhipNavigationRailItem(
                    modifier = Modifier.semantics { contentDescription = "Settings tab" },
                    selected = selected == AppDestination.Settings,
                    enabled = enabled,
                    showLabel = showLabels,
                    onClick = { onSelect(AppDestination.Settings) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(28.dp)) },
                    label = {
                        Text(
                            stringResource(R.string.nav_settings),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
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
private fun WhipBottomNavigation(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    enabled: Boolean = true,
) {
    PrimaryDestinationNavigationBar(
        selected = selected,
        onSelect = onSelect,
        enabled = enabled,
        modifier = Modifier.testTag("adaptive-bottom-navigation"),
    )
}

@Composable
private fun PrimaryDestinationNavigationBar(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val showLabels = LocalDensity.current.fontScale < 1.5f
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        WhipNavigationBarItem(
            modifier = Modifier.semantics {
                contentDescription = if (selected == AppDestination.Home) "Home" else "Go to Home"
            },
            selected = selected == AppDestination.Home,
            enabled = enabled,
            showLabel = showLabels,
            onClick = { onSelect(AppDestination.Home) },
            icon = { WhipBrandMark(Modifier.size(28.dp)) },
            label = {
                Text(
                    stringResource(R.string.nav_home),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
        primaryAppDestinations.forEach { destination ->
            val destinationLabel = stringResource(destination.labelRes)
            val destinationTabDescription = stringResource(R.string.nav_tab_description, destinationLabel)
            WhipNavigationBarItem(
                modifier = Modifier.semantics { contentDescription = destinationTabDescription },
                selected = destination == selected,
                enabled = enabled,
                showLabel = showLabels,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(26.dp)) },
                label = {
                    Text(
                        destinationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun TabletopNavigation(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    enabled: Boolean = true,
) {
    PrimaryDestinationNavigationBar(
        selected = selected,
        onSelect = onSelect,
        enabled = enabled,
        modifier = Modifier.testTag("adaptive-tabletop-navigation"),
    )
}

private data class SupportPaneItem(val id: String, val title: String, val supportingText: String)

private val SupportPaneContentPadding = PaddingValues(
    horizontal = WhipSpacing.compact,
    vertical = WhipSpacing.standard,
)
private val SupportPaneItemSpacing = 10.dp

@Composable
private fun SupportPaneTitle(title: String) {
    Text(
        title,
        modifier = Modifier.testTag("support-pane-title"),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SupportPaneDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().testTag("support-pane-description"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        minLines = 2,
    )
}

@Composable
private fun HomeSupportPane(
    summary: AdaptiveSummary,
    retryActions: DomainRetryActions,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allVisibleDomainsReady = summary.visibleHomeSections.all { summary.loadState(it) == AdaptiveLoadState.Ready }
    val hasContext = summary.visibleHomeSections.any { section ->
        when (section) {
            HomeSection.Tasks -> summary.dueTasks > 0
            HomeSection.Habits -> summary.dueHabits > 0
            HomeSection.Goals -> summary.activeGoals > 0
            HomeSection.Tracks -> summary.pinnedTracks > 0
            HomeSection.Gym -> summary.activeWorkout
        }
    }
    LazyColumn(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = SupportPaneContentPadding,
        verticalArrangement = Arrangement.spacedBy(SupportPaneItemSpacing),
    ) {
        item { SupportPaneTitle(stringResource(R.string.home_support_today_title)) }
        item {
            SupportPaneDescription(summary.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
        }
        summary.visibleHomeSections.forEach { section ->
            val loadState = summary.loadState(section)
            if (loadState != AdaptiveLoadState.Ready) {
                item(key = "home-support-status-${section.name}") {
                    AdaptiveLoadNotice(
                        domain = section.supportLabel,
                        loadState = loadState,
                        onRetry = when (section) {
                            HomeSection.Tasks -> retryActions.tasks
                            HomeSection.Habits -> retryActions.habits
                            HomeSection.Goals -> retryActions.goals
                            HomeSection.Tracks -> retryActions.tracks
                            HomeSection.Gym -> retryActions.gym
                        },
                        modifier = Modifier.testTag(
                            "home-support-${section.name.lowercase()}-${if (loadState == AdaptiveLoadState.Loading) "loading" else "error"}",
                        ),
                    )
                }
            }
        }
        if (allVisibleDomainsReady && !hasContext) {
            if (summary.hasAnyUserData) {
                item {
                    WhipNoticeCard(
                        title = stringResource(R.string.home_support_clear_title),
                        message = stringResource(R.string.home_support_clear_message),
                        modifier = Modifier.testTag("home-support-clear-day"),
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .testTag("home-support-introduction"),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Home,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                stringResource(R.string.home_support_introduction_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.home_support_introduction_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.home_support_introduction_nudge),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.home_support_privacy_note),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            summary.visibleHomeSections.forEach { section ->
                val loadState = summary.loadState(section)
                val hasPartialEvidence = when (section) {
                    HomeSection.Tasks -> summary.dueTasks > 0
                    HomeSection.Habits -> summary.dueHabits > 0
                    HomeSection.Goals -> summary.activeGoals > 0
                    HomeSection.Tracks -> summary.pinnedTracks > 0
                    HomeSection.Gym -> summary.activeWorkout
                }
                if (loadState == AdaptiveLoadState.Ready || hasPartialEvidence) {
                    item(key = "home-support-summary-${section.name}") {
                        when (section) {
                            HomeSection.Tasks -> AdaptiveSummaryCard(stringResource(R.string.support_tasks_title), summary.dueTasks.toString(), AppDestination.Tasks, Modifier.fillMaxWidth(), onClick = { onSelect(AppDestination.Tasks) })
                            HomeSection.Habits -> AdaptiveSummaryCard(stringResource(R.string.home_support_habits_remaining), summary.dueHabits.toString(), AppDestination.Habits, Modifier.fillMaxWidth(), onClick = { onSelect(AppDestination.Habits) })
                            HomeSection.Goals -> AdaptiveSummaryCard(stringResource(R.string.support_goals_title), summary.activeGoals.toString(), AppDestination.Goals, Modifier.fillMaxWidth(), onClick = { onSelect(AppDestination.Goals) })
                            HomeSection.Tracks -> AdaptiveSummaryCard(stringResource(R.string.home_support_pinned_tracks), summary.pinnedTracks.toString(), AppDestination.Tracks, Modifier.fillMaxWidth(), onClick = { onSelect(AppDestination.Tracks) })
                            HomeSection.Gym -> AdaptiveSummaryCard(stringResource(R.string.home_support_workout), stringResource(if (summary.activeWorkout) R.string.state_in_progress else R.string.state_ready), AppDestination.Gym, Modifier.fillMaxWidth(), onClick = { onSelect(AppDestination.Gym) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveLoadNotice(
    domain: String,
    loadState: AdaptiveLoadState,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    when (loadState) {
        AdaptiveLoadState.Loading -> WhipNoticeCard(
            title = stringResource(R.string.support_domain_loading_title, domain),
            message = stringResource(R.string.support_domain_loading_message, domain),
            tone = WhipNoticeTone.Informative,
            modifier = modifier,
        )
        is AdaptiveLoadState.Failed -> WhipNoticeCard(
            title = stringResource(R.string.home_domain_unavailable, domain),
            message = loadState.message,
            tone = WhipNoticeTone.Error,
            actionLabel = onRetry?.let { stringResource(R.string.action_try_again) },
            onAction = onRetry,
            modifier = modifier,
        )
        AdaptiveLoadState.Ready -> Unit
    }
}

@Composable
private fun DestinationSupportPane(
    title: String,
    domain: String,
    supportingText: String,
    items: List<SupportPaneItem>,
    emptyText: String,
    loadState: AdaptiveLoadState,
    statusTagPrefix: String,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(SupportPaneContentPadding),
        verticalArrangement = Arrangement.spacedBy(SupportPaneItemSpacing),
    ) {
        SupportPaneTitle(title)
        SupportPaneDescription(supportingText)
        if (loadState != AdaptiveLoadState.Ready) {
            AdaptiveLoadNotice(
                domain = domain,
                loadState = loadState,
                onRetry = onRetry,
                modifier = Modifier.testTag(
                    "$statusTagPrefix-${if (loadState == AdaptiveLoadState.Loading) "loading" else "error"}",
                ),
            )
        }
        if (items.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = SupportPaneItem::id) { item ->
                    NavigationRow(
                        title = item.title,
                        supportingText = item.supportingText,
                        onClick = { onOpen(item.id) },
                    )
                }
            }
        } else if (loadState == AdaptiveLoadState.Ready) {
            SupportPaneEmptyMessage(
                text = emptyText,
                testTag = "$statusTagPrefix-empty",
            )
        }
    }
}

@Composable
private fun SupportPaneEmptyMessage(
    text: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WhipSpacing.compact)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TrackSupportPane(
    projections: List<TrackProjection>,
    selectedTrackId: Long?,
    loadState: AdaptiveLoadState,
    onRetry: () -> Unit,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(SupportPaneContentPadding),
        verticalArrangement = Arrangement.spacedBy(SupportPaneItemSpacing),
    ) {
        SupportPaneTitle(stringResource(R.string.nav_tracks))
        SupportPaneDescription(stringResource(R.string.support_tracks_description))
        if (loadState != AdaptiveLoadState.Ready) {
            AdaptiveLoadNotice(
                domain = stringResource(R.string.nav_tracks),
                loadState = loadState,
                onRetry = onRetry,
                modifier = Modifier.testTag(
                    "track-support-${if (loadState == AdaptiveLoadState.Loading) "loading" else "error"}",
                ),
            )
        }
        if (projections.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("track-support-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(projections, key = { it.track.id }) { projection ->
                    val selected = projection.track.id == selectedTrackId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onSelect(projection.track.id) },
                            )
                            .semantics { contentDescription = "Open ${projection.track.name}" },
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            WhipIdentityEmoji(projection.track.icon)
                            Column(Modifier.weight(1f)) {
                                Text(projection.track.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${pluralStringResource(R.plurals.entry_count, projection.entries.size, projection.entries.size)} · ${projection.track.area}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                            Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
                        }
                    }
                }
            }
        } else if (loadState == AdaptiveLoadState.Ready) {
            SupportPaneEmptyMessage(
                text = stringResource(R.string.support_tracks_empty),
                testTag = "track-support-empty",
            )
        }
    }
}

@Composable
private fun TrackOverviewSupportPane(
    state: TrackUiState,
    loadState: AdaptiveLoadState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(SupportPaneContentPadding)
            .testTag("track-overview-support"),
        verticalArrangement = Arrangement.spacedBy(SupportPaneItemSpacing),
    ) {
        SupportPaneTitle(stringResource(R.string.support_track_overview_title))
        SupportPaneDescription(stringResource(R.string.support_track_overview_description))
        if (loadState != AdaptiveLoadState.Ready) {
            AdaptiveLoadNotice(
                domain = stringResource(R.string.nav_tracks),
                loadState = loadState,
                onRetry = onRetry,
                modifier = Modifier.testTag(
                    "track-overview-support-${if (loadState == AdaptiveLoadState.Loading) "loading" else "error"}",
                ),
            )
        }
        if (loadState == AdaptiveLoadState.Ready || state.active.isNotEmpty()) Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    stringResource(R.string.support_active_tracks) to state.active.size.toString(),
                    stringResource(R.string.support_entries) to state.active.sumOf { it.entries.size }.toString(),
                    "Fields" to state.active.sumOf { it.fields.size }.toString(),
                ).forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        state.active.mapNotNull { projection ->
            projection.entries.maxByOrNull { it.entry.createdAtMillis }?.let { projection to it }
        }.maxByOrNull { it.second.entry.createdAtMillis }?.let { (projection, entry) ->
            Text(stringResource(R.string.support_recently_active), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${projection.track.icon} ${projection.track.name} · ${projection.primaryText(entry)}",
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsSupportPane(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(SupportPaneContentPadding),
        verticalArrangement = Arrangement.spacedBy(SupportPaneItemSpacing),
    ) {
        SupportPaneTitle(stringResource(R.string.nav_settings))
        SupportPaneDescription("Choose a category.")
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("settings-support-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SettingsSection.entries, key = SettingsSection::name) { section ->
                val active = section == selected
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-support-section-${section.label}")
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(section) }),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(section.label, fontWeight = FontWeight.SemiBold)
                            Text(section.supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        }
                        Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
                    }
                }
            }
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
        Triple(AppDestination.Tasks, stringResource(R.string.support_tasks_title), summary.dueTasks.toString()),
        Triple(AppDestination.Habits, stringResource(R.string.home_support_habits_remaining), summary.dueHabits.toString()),
        Triple(AppDestination.Goals, stringResource(R.string.support_goals_title), summary.activeGoals.toString()),
        Triple(
            AppDestination.Gym,
            stringResource(R.string.home_support_workout),
            stringResource(if (summary.activeWorkout) R.string.state_in_progress else R.string.state_ready),
        ),
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
                .padding(SupportPaneContentPadding),
            verticalArrangement = Arrangement.spacedBy(SupportPaneItemSpacing),
        ) {
            SupportPaneTitle(
                if (selected == AppDestination.Home) stringResource(R.string.home_support_today_title)
                else selected.label,
            )
            SupportPaneDescription(summary.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
            val contextLines = when (selected) {
                AppDestination.Tasks -> summary.taskContext
                AppDestination.Habits -> summary.habitContext
                AppDestination.Goals -> summary.goalContext
                AppDestination.Gym -> summary.gymContext
                AppDestination.Tracks -> emptyList()
                else -> emptyList()
            }
            if (selected == AppDestination.Gym) {
                if (summary.gymLoadState != AdaptiveLoadState.Ready) {
                    AdaptiveLoadNotice(
                        domain = "Gym",
                        loadState = summary.gymLoadState,
                        modifier = Modifier.testTag(
                            "support-pane-gym-${if (summary.gymLoadState == AdaptiveLoadState.Loading) "loading" else "error"}",
                        ),
                    )
                }
                val emptyWorkout = summary.gymLoadState == AdaptiveLoadState.Ready &&
                    !summary.activeWorkout &&
                    summary.gymDestination == GymDestination.Workout
                if (emptyWorkout) {
                    SupportPaneEmptyMessage(
                        text = stringResource(R.string.support_gym_empty),
                        testTag = "support-pane-gym-empty",
                    )
                } else if (contextLines.isNotEmpty()) {
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
                        AppDestination.Tracks -> "Track context"
                        else -> "Context"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                contextLines.forEach { line ->
                    Text(
                        line,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                            Text(line, Modifier.width(220.dp).padding(vertical = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            if (summary.activeWorkout) "Workout · In progress" else "Workout · Ready when you are",
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
                                Text(line, Modifier.fillMaxWidth().padding(vertical = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Text(
                            if (summary.activeWorkout) "Workout · In progress" else "Workout · Ready when you are",
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
    WhipMetricTile(
        label = label,
        value = value,
        onClickLabel = "Open ${destination.label}",
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .widthIn(min = 150.dp),
    )
}

@Composable
private fun HomeContent(
    state: TaskUiState,
    habitState: HabitUiState,
    gymState: GymUiState,
    goalState: GoalUiState,
    trackState: TrackUiState,
    appSettings: AppSettings,
    innerPadding: PaddingValues,
    onQuickHabit: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onHabitValue: (com.whip.app.domain.HabitDayProgress, Double) -> Unit,
    onSetHabitValue: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onDecrementHabit: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onUndoHabit: (com.whip.app.domain.HabitDayProgress) -> Unit,
    onUndoHabitSkip: (com.whip.app.domain.HabitDayProgress) -> Unit,
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
    onResetElapsedGoal: (com.whip.app.domain.GoalProjection) -> Unit,
    onToggleMilestone: (com.whip.app.domain.GoalMilestoneBoundary, Boolean) -> Unit,
    onOpenTracks: () -> Unit,
    onOpenTrack: (com.whip.app.domain.TrackProjection) -> Unit,
    onEditTrack: (com.whip.app.domain.TrackProjection) -> Unit,
    onAddTrackEntry: (com.whip.app.domain.TrackProjection) -> Unit,
    onSelectHomeTaskFilter: (String?) -> Unit,
    onOpenReview: () -> Unit,
    showFullHeader: Boolean = true,
    areaScopeLabel: String? = null,
    onShowAllAreas: () -> Unit = {},
    onRetryTaskLoading: () -> Unit = {},
    onRetryHabitLoading: () -> Unit = {},
    onRetryGoalLoading: () -> Unit = {},
    onRetryTrackLoading: () -> Unit = {},
    onRetryGymLoading: () -> Unit = {},
) {
    val homeTaskFilter = appSettings.savedTaskFilters.firstOrNull { it.name == appSettings.homeTaskFilterName }
    val homeTasks = state.today.filter { homeTaskFilter == null || it.matches(homeTaskFilter, state.currentDate, appSettings.zoneId()) }
    val homePinnedTasks = homeTasks.filter { it.task.pinned }
    val homeOtherTasks = homeTasks.filterNot { it.task.pinned }
    val homeHabitSections = habitState.today.dailyHabitSections()
    val homePinnedHabits = homeHabitSections.remaining.filter { it.habit.pinned }
    val homeOtherHabits = homeHabitSections.remaining.filterNot { it.habit.pinned }
    val homeGoals = pinnedHomeSummary(goalState.active, limit = 3) { it.goal.pinned }
    val homePinnedGoals = homeGoals.filter { it.goal.pinned }
    val homeOtherGoals = homeGoals.filterNot { it.goal.pinned }
    val pinnedRoutines = gymState.routines.filter { it.pinned }
    val gymHomeCount = gymHomeItemCount(gymState.activeSession != null, pinnedRoutines.size)
    val homeDoneHabitIds = homeHabitSections.done.mapTo(linkedSetOf()) { it.habit.id }
    val compactItemExpansionState = LocalCompactItemExpansionState.current
    var homeDoneExpanded by rememberSaveable(habitState.currentDate.toEpochDay()) {
        mutableStateOf(false)
    }
    var knownHomeDoneHabitIds by remember(habitState.currentDate) { mutableStateOf(homeDoneHabitIds) }
    var homeCompletionTrackingReady by remember(habitState.currentDate) { mutableStateOf(false) }
    LaunchedEffect(habitState.loading, homeDoneHabitIds, habitState.currentDate, compactItemExpansionState) {
        if (habitState.loading) return@LaunchedEffect
        if (homeCompletionTrackingReady) {
            (homeDoneHabitIds - knownHomeDoneHabitIds).forEach { habitId ->
                compactItemExpansionState?.collapse(
                    habitCompactExpansionKey(habitId, habitState.currentDate),
                )
            }
        } else {
            homeCompletionTrackingReady = true
        }
        knownHomeDoneHabitIds = homeDoneHabitIds
    }
    val visibleHomeSections = appSettings.visibleHomeSections()
    val allVisibleHomeDomainsSettled = visibleHomeSections.all { section ->
        when (section) {
            HomeSection.Tasks -> !state.loading
            HomeSection.Habits -> !habitState.loading
            HomeSection.Goals -> !goalState.loading
            HomeSection.Tracks -> !trackState.loading
            HomeSection.Gym -> !gymState.loading
        }
    }
    val hasVisibleHomeDomainError = visibleHomeSections.any { section ->
        when (section) {
            HomeSection.Tasks -> state.errorMessage != null
            HomeSection.Habits -> habitState.errorMessage != null
            HomeSection.Goals -> goalState.errorMessage != null
            HomeSection.Tracks -> trackState.errorMessage != null
            HomeSection.Gym -> gymState.errorMessage != null
        }
    }
    val emptyStateEligible = homeEmptyStateEligible(
        visibleHomeSections,
        state,
        habitState,
        goalState,
        trackState,
        gymState,
    )
    val hasHomeContent =
        (HomeSection.Tasks in visibleHomeSections && homeTasks.isNotEmpty()) ||
            (HomeSection.Habits in visibleHomeSections && habitState.today.isNotEmpty()) ||
            (HomeSection.Goals in visibleHomeSections && goalState.active.isNotEmpty()) ||
            (HomeSection.Tracks in visibleHomeSections && trackState.pinned.isNotEmpty()) ||
            (HomeSection.Gym in visibleHomeSections && gymHomeCount > 0)
    val hasReviewEvidence =
        state.completed.isNotEmpty() ||
            habitState.logs.isNotEmpty() ||
            (goalState.active + goalState.completed + goalState.archived).any { it.entries.isNotEmpty() } ||
            gymState.history.any { it.state == com.whip.app.domain.WorkoutSessionState.Finished }
    val hasAnyUserData = homeHasAnyUserData(state, habitState, goalState, trackState, gymState)
    val showGettingStarted = shouldShowHomeGettingStarted(hasAnyUserData)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("home-list"),
        contentPadding = WhipPageContentPadding,
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
                date = state.currentDate,
                taskTotal = state.today.size,
                habitCompleted = habitState.today.count { it.successful == true },
                habitTotal = habitState.today.size,
                onOpenTasks = onOpenTasks,
                onOpenHabits = onOpenHabits,
                showFullHeader = showFullHeader,
            )
        }
        if (hasReviewEvidence) {
            item { NavigationRow("Review & Trends", onOpenReview, supportingText = "Reflect on outcomes and recent progress.") }
        }
        if (!hasHomeContent && emptyStateEligible) {
            if (hasReviewEvidence) {
                item {
                    WhipEmptyState(
                        title = stringResource(R.string.home_support_clear_title),
                        supportingText = stringResource(R.string.home_clear_review_message),
                        primaryActionLabel = stringResource(R.string.home_clear_review_action),
                        onPrimaryAction = onOpenReview,
                    )
                }
            } else if (showGettingStarted) {
                item {
                    HomeGettingStarted(
                        onOpenTasks = onOpenTasks,
                        onOpenHabits = onOpenHabits,
                        onOpenGoals = onOpenGoals,
                        onOpenTracks = onOpenTracks,
                        onOpenGym = onOpenGym,
                        onOpenReview = onOpenReview,
                    )
                }
            } else {
                item {
                    WhipEmptyState(
                        title = stringResource(R.string.home_support_clear_title),
                        supportingText = stringResource(R.string.home_clear_existing_message),
                    )
                }
            }
        }
        if (hasHomeContent || !allVisibleHomeDomainsSettled || hasVisibleHomeDomainError) visibleHomeSections.forEach { section ->
            val collapsed = section in appSettings.collapsedHomeSections
            when (section) {
                HomeSection.Tasks -> {
                    item { SectionHeading("Tasks", homeTasks.size, onOpenTasks) }
                    if (state.loading) item { HomeDomainLoadingStatus("Tasks") }
                    else if (state.errorMessage != null) item {
                        HomeDomainLoadNotice("Tasks", state.errorMessage, onRetryTaskLoading)
                    } else if (!collapsed) {
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
                        if (homeTasks.isEmpty()) item {
                            Text(
                                if (homeTaskFilter == null) {
                                    areaScopeLabel?.let { "No tasks are scheduled or carried over in $it today." }
                                        ?: "No tasks are scheduled or carried over today."
                                } else {
                                    "No Today tasks match ${homeTaskFilter.name}."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (homePinnedTasks.isNotEmpty()) item {
                            HomeItemGroupHeading("Pinned for today", homePinnedTasks.size, pinned = true, testTag = "home-pinned-tasks")
                        }
                        items(homePinnedTasks, key = ScheduledTask::stableKey) { task ->
                            TaskRow(
                                item = task,
                                completed = false,
                                onComplete = { onCompleteTask(task) },
                                onOpenActions = { onOpenTask(task) },
                                onEdit = { onEditTask(task) },
                            )
                        }
                        if (homePinnedTasks.isNotEmpty() && homeOtherTasks.isNotEmpty()) item {
                            HomeItemGroupHeading("Other tasks for today", homeOtherTasks.size)
                        }
                        items(homeOtherTasks, key = ScheduledTask::stableKey) { task ->
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
                    item { SectionHeading("Habits", homeHabitSections.remaining.size, onOpenHabits) }
                    if (habitState.loading) item { HomeDomainLoadingStatus("Habits") }
                    else if (habitState.errorMessage != null) item {
                        HomeDomainLoadNotice("Habits", habitState.errorMessage, onRetryHabitLoading)
                    } else if (!collapsed) {
                        if (habitState.today.isEmpty()) item { HomeStatusCard(areaScopeLabel?.let { "No habits due in $it" } ?: "No habits due", "Create a habit on the Habits screen.", onOpenHabits) }
                        if (homePinnedHabits.isNotEmpty()) item {
                            HomeItemGroupHeading("Pinned and due", homePinnedHabits.size, pinned = true, testTag = "home-pinned-habits")
                        }
                        items(homePinnedHabits, key = { "home-habit-${it.habit.id}" }) { habit ->
                            HabitProgressCard(
                                item = habit,
                                onOpen = { onOpenHabit(habit) },
                                onEdit = { onEditHabit(habit) },
                                onQuick = { onQuickHabit(habit) },
                                onQuickValue = { value -> onHabitValue(habit, value) },
                                onSetValue = { onSetHabitValue(habit) },
                                onDecrement = { onDecrementHabit(habit) },
                                onUndo = { onUndoHabit(habit) },
                                onUndoSkip = { onUndoHabitSkip(habit) },
                                canUndo = canUndoHabit(habit),
                                onChecklist = onChecklist,
                                lowPressureMode = appSettings.lowPressureMode,
                            )
                        }
                        if (homePinnedHabits.isNotEmpty() && homeOtherHabits.isNotEmpty()) item {
                            HomeItemGroupHeading("Other habits due", homeOtherHabits.size)
                        }
                        items(homeOtherHabits, key = { "home-habit-${it.habit.id}" }) { habit ->
                            HabitProgressCard(
                                item = habit,
                                onOpen = { onOpenHabit(habit) },
                                onEdit = { onEditHabit(habit) },
                                onQuick = { onQuickHabit(habit) },
                                onQuickValue = { value -> onHabitValue(habit, value) },
                                onSetValue = { onSetHabitValue(habit) },
                                onDecrement = { onDecrementHabit(habit) },
                                onUndo = { onUndoHabit(habit) },
                                onUndoSkip = { onUndoHabitSkip(habit) },
                                canUndo = canUndoHabit(habit),
                                onChecklist = onChecklist,
                                lowPressureMode = appSettings.lowPressureMode,
                            )
                        }
                        if (homeHabitSections.done.isNotEmpty()) {
                            item {
                                DoneHabitsDisclosure(
                                    count = homeHabitSections.done.size,
                                    expanded = homeDoneExpanded,
                                    onToggle = { homeDoneExpanded = !homeDoneExpanded },
                                )
                            }
                            if (homeDoneExpanded) items(homeHabitSections.done, key = { "home-habit-${it.habit.id}" }) { habit ->
                                HabitProgressCard(
                                    item = habit,
                                    onOpen = { onOpenHabit(habit) },
                                    onEdit = { onEditHabit(habit) },
                                    onQuick = { onQuickHabit(habit) },
                                    onQuickValue = { value -> onHabitValue(habit, value) },
                                    onSetValue = { onSetHabitValue(habit) },
                                    onDecrement = { onDecrementHabit(habit) },
                                    onUndo = { onUndoHabit(habit) },
                                    onUndoSkip = { onUndoHabitSkip(habit) },
                                    canUndo = canUndoHabit(habit),
                                    onChecklist = onChecklist,
                                    lowPressureMode = appSettings.lowPressureMode,
                                )
                            }
                        }
                    }
                }
                HomeSection.Goals -> {
                    item { SectionHeading("Goals", goalState.active.size, onOpenGoals) }
                    if (goalState.loading) item { HomeDomainLoadingStatus("Goals") }
                    else if (goalState.errorMessage != null) item {
                        HomeDomainLoadNotice("Goals", goalState.errorMessage, onRetryGoalLoading)
                    } else if (!collapsed) {
                        if (goalState.active.isEmpty()) item { HomeStatusCard(areaScopeLabel?.let { "No active goals in $it" } ?: "No active goals", "Create a measurable or milestone goal.", onOpenGoals) }
                        if (homePinnedGoals.isNotEmpty()) item {
                            HomeItemGroupHeading("Pinned goals", homePinnedGoals.size, pinned = true, testTag = "home-pinned-goals")
                        }
                        items(homePinnedGoals, key = { "home-goal-${it.goal.id}" }) { projection ->
                            GoalCard(
                                projection = projection,
                                customUnits = goalState.customUnits,
                                nowMillis = goalState.nowMillis,
                                onOpen = { onOpenGoal(projection) },
                                onEdit = { onEditGoal(projection) },
                                onRecord = { onRecordGoal(projection) },
                                onResetElapsed = { onResetElapsedGoal(projection) },
                                onToggleMilestone = onToggleMilestone,
                            )
                        }
                        if (homePinnedGoals.isNotEmpty() && homeOtherGoals.isNotEmpty()) item {
                            HomeItemGroupHeading("Other active goals", homeOtherGoals.size)
                        }
                        items(homeOtherGoals, key = { "home-goal-${it.goal.id}" }) { projection ->
                            GoalCard(
                                projection = projection,
                                customUnits = goalState.customUnits,
                                nowMillis = goalState.nowMillis,
                                onOpen = { onOpenGoal(projection) },
                                onEdit = { onEditGoal(projection) },
                                onRecord = { onRecordGoal(projection) },
                                onResetElapsed = { onResetElapsedGoal(projection) },
                                onToggleMilestone = onToggleMilestone,
                            )
                        }
                    }
                }
                HomeSection.Tracks -> {
                    if (trackState.loading || trackState.errorMessage != null || trackState.pinned.isNotEmpty()) {
                        item { SectionHeading("Quick Log", trackState.pinned.size, onOpenTracks) }
                        if (trackState.loading) item { HomeDomainLoadingStatus("Tracks") }
                        else if (trackState.errorMessage != null) item {
                            HomeDomainLoadNotice("Tracks", trackState.errorMessage, onRetryTrackLoading)
                        } else if (!collapsed) {
                            items(trackState.pinned, key = { "home-track-${it.track.id}" }) { projection ->
                                TrackRow(
                                    projection = projection,
                                    onOpen = { onOpenTrack(projection) },
                                    onEdit = { onEditTrack(projection) },
                                    onAddEntry = { onAddTrackEntry(projection) },
                                )
                            }
                            item { NavigationRow("All Tracks", onOpenTracks, supportingText = "Open every structured log and its entries.") }
                        }
                    }
                }
                HomeSection.Gym -> {
                    item {
                        SectionHeading(
                            if (areaScopeLabel == null) "Gym" else "Gym · All data",
                            gymHomeCount,
                            onOpenGym,
                        )
                    }
                    if (gymState.loading) item { HomeDomainLoadingStatus("Gym") }
                    else if (gymState.errorMessage != null) item {
                        HomeDomainLoadNotice("Gym", gymState.errorMessage, onRetryGymLoading)
                    } else if (!collapsed) {
                        item {
                            val active = gymState.activeSession
                            val workoutSummary = gymState.summary
                            HomeStatusCard(
                                when {
                                    active != null -> active.name.ifBlank { "Active workout" }
                                    else -> "No active workout"
                                },
                                when {
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
                        if (gymState.activeSession == null) {
                            items(pinnedRoutines, key = { "pinned-routine-${it.id}" }) { routine ->
                                val days = gymState.routineDays.filter { it.routineId == routine.id }.sortedBy { it.position }
                                if (days.size <= 1) {
                                    HomeStatusCard(routine.name, "Pinned · ${days.firstOrNull()?.name ?: "Start routine"}") {
                                        onStartRoutine(routine.id, days.firstOrNull()?.id)
                                    }
                                } else {
                                    Card(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.PushPin, contentDescription = "Pinned", modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(routine.name, fontWeight = FontWeight.Bold)
                                            }
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
private fun HomeDomainLoadNotice(
    domain: String,
    errorMessage: String,
    onRetry: () -> Unit,
) {
    WhipNoticeCard(
        title = stringResource(R.string.home_domain_unavailable, domain),
        message = errorMessage,
        tone = WhipNoticeTone.Error,
        actionLabel = stringResource(R.string.action_try_again),
        onAction = onRetry,
        modifier = Modifier.testTag("home-${domain.lowercase()}-load-error"),
    )
}

@Composable
private fun HomeDomainLoadingStatus(domain: String) {
    WhipStatusCard(
        kind = WhipStatusKind.Loading,
        title = "Loading $domain",
        message = "Whip is preparing ${domain.lowercase()}.",
        modifier = Modifier.testTag("home-${domain.lowercase()}-loading"),
    )
}

@Composable
private fun HomeGettingStarted(
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenGym: () -> Unit,
    onOpenReview: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("home-getting-started"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Build Your Day",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Whip gives each kind of progress a clear home. Start with what helps now; Home brings it all together.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HomeDestinationLinks(
            onOpenTasks = onOpenTasks,
            onOpenHabits = onOpenHabits,
            onOpenGoals = onOpenGoals,
            onOpenTracks = onOpenTracks,
            onOpenGym = onOpenGym,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Learn from your progress",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HomeComponentCard(
                title = "Review & Trends",
                supportingText = "Reflect on completed work and spot patterns as your history grows.",
                icon = Icons.Outlined.Insights,
                onClick = onOpenReview,
                modifier = Modifier.testTag("home-destination-review"),
            )
        }
    }
}

private data class HomeComponentItem(
    val title: String,
    val supportingText: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/*
 * Home's first-run component guide intentionally shows every primary tool,
 * even when a user has hidden some sections from the populated Home feed.
 * Section visibility controls the dashboard, not what Whip teaches here.
 */
@Composable
internal fun HomeDestinationLinks(
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenGym: () -> Unit,
    sections: List<HomeSection> = HomeSection.entries,
    modifier: Modifier = Modifier,
) {
    val destinations = sections.distinct().map { section ->
        when (section) {
            HomeSection.Tasks -> HomeComponentItem(
                title = "Tasks",
                supportingText = "Capture one-off actions and decide when they need attention.",
                icon = Icons.Outlined.CheckCircle,
                onClick = onOpenTasks,
            )
            HomeSection.Habits -> HomeComponentItem(
                title = "Habits",
                supportingText = "Build repeatable practices with check-ins, values, and timers.",
                icon = Icons.Outlined.Autorenew,
                onClick = onOpenHabits,
            )
            HomeSection.Goals -> HomeComponentItem(
                title = "Goals",
                supportingText = "Turn longer-term outcomes into measurable progress or milestones.",
                icon = Icons.Outlined.Flag,
                onClick = onOpenGoals,
            )
            HomeSection.Tracks -> HomeComponentItem(
                title = "Tracks",
                supportingText = "Create structured logs for anything worth observing over time.",
                icon = Icons.Outlined.TableRows,
                onClick = onOpenTracks,
            )
            HomeSection.Gym -> HomeComponentItem(
                title = "Gym",
                supportingText = "Plan routines, run workouts, and keep your training history.",
                icon = Icons.Outlined.FitnessCenter,
                onClick = onOpenGym,
            )
        }
    }
    val startingComponents = destinations.filter { it.title == "Tasks" || it.title == "Habits" }
    val expandingComponents = destinations - startingComponents.toSet()
    Column(
        modifier = modifier.fillMaxWidth().testTag("home-destination-links"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (startingComponents.isNotEmpty()) {
            HomeComponentGroup("Start here", startingComponents)
        }
        if (expandingComponents.isNotEmpty()) {
            HomeComponentGroup("Add when useful", expandingComponents)
        }
    }
}

@Composable
private fun HomeComponentGroup(title: String, components: List<HomeComponentItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        components.forEach { component ->
            HomeComponentCard(
                title = component.title,
                supportingText = component.supportingText,
                icon = component.icon,
                onClick = component.onClick,
                modifier = Modifier.testTag("home-destination-${component.title.lowercase()}"),
            )
        }
    }
}

@Composable
private fun HomeComponentCard(
    title: String,
    supportingText: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WhipCollectionCard(
        modifier = modifier,
        onClick = onClick,
        onClickLabel = "Open $title",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
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
    onBulkArchiveRequest: (List<ScheduledTask>, String) -> Boolean,
    onBulkRestore: (List<ScheduledTask>) -> Unit,
    onBulkReopen: (List<ScheduledTask>) -> Unit,
    onBulkPin: (List<ScheduledTask>, Boolean) -> Unit,
    onBulkPostpone: (List<ScheduledTask>, LocalDate) -> Unit,
    onBulkPostponeRequest: (List<ScheduledTask>, LocalDate, String) -> Boolean,
    onBulkEdit: (List<ScheduledTask>, TaskBulkEdit) -> Unit,
    onBulkEditRequest: (List<ScheduledTask>, TaskBulkEdit, String) -> Boolean,
    onBulkDeletePermanently: (Set<Long>) -> Unit,
    onBulkDeletePermanentlyRequest: (Set<Long>, Map<Long, String>, String) -> Boolean,
    authoredMutationState: PersistenceRequestState<TaskMutationReceipt>,
    onAuthoredMutationResultConsumed: (String) -> Unit,
    deletionBatchImpact: TaskDeletionBatchImpact?,
    onPreviewBulkDeletion: (Set<Long>) -> Unit,
    onClearBulkDeletionPreview: () -> Unit,
    onSetHabitPlanningOverlay: (Boolean) -> Unit,
    onActiveTaskSortModeChange: (String) -> Unit,
    onOpenPlanningHabit: (Long) -> Unit,
    onReorder: (List<ScheduledTask>) -> Unit,
    onPlanMyDay: (List<ScheduledTask>, Int) -> Unit,
    onStopFocus: () -> Unit,
    onQuickCapture: (String, LocalDate?, String?, (Boolean) -> Unit) -> Unit,
    onQuickCaptureRequest: ((String, LocalDate?, String?, String) -> Boolean)?,
    onAddDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    areas: List<Area> = emptyList(),
    areaScope: AreaScope = AreaScope.All,
    onSelectAreaScope: (AreaScope) -> Unit = {},
    onTemporarilySelectAreaScope: (AreaScope) -> Unit = {},
    planningViewRequest: TaskPlanningView? = null,
    onPlanningViewRequestConsumed: () -> Unit = {},
    allAreaTaskCount: Int = 0,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onReorderModeChange: (Boolean) -> Unit = {},
    reorderDismissRequest: Int = 0,
    onRetryLoading: () -> Unit = {},
) {
    val initialWorkspaceRoute = destination.toWorkspaceRoute()
    if (state.loading || state.errorMessage != null) {
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            DestinationTabBar(
                selected = initialWorkspaceRoute.destination,
                destinations = allTaskWorkspaceDestinations,
                onSelect = { selected ->
                    onDestinationChange(TaskWorkspaceRoute(selected, initialWorkspaceRoute.historySection).dataDestination())
                },
                label = TaskWorkspaceDestination::label,
                testTagPrefix = "task-destination",
                barTestTag = "task-workspace-navigation",
            )
            DomainLoadContent("tasks", PaddingValues(), state.errorMessage, onRetryLoading)
        }
        return
    }
    val dialogModifier = modifier
    var planningView by rememberSaveable { mutableStateOf(TaskPlanningView.List) }
    var historySection by rememberSaveable {
        mutableStateOf(destination.toWorkspaceRoute().historySection)
    }
    var focusClockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var textQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable {
        mutableStateOf(
            if (destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)) {
                appSettings.activeTaskSortMode
            } else {
                "Smart"
            },
        )
    }
    var sortDirection by rememberSaveable { mutableStateOf(SortDirection.Ascending) }
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
    var reordering by rememberSaveable { mutableStateOf(false) }
    var enterReorderWhenReady by rememberSaveable { mutableStateOf(false) }
    var selectionActionsOpen by rememberSaveable { mutableStateOf(false) }
    var selectedKeys by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var pendingBulkCompleteKeys by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var archivePreviewKeys by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var archiveTargetRevisions by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var deletePreviewTaskIds by rememberSaveable { mutableStateOf<Set<Long>?>(null) }
    var bulkEditOpen by rememberSaveable { mutableStateOf(false) }
    var bulkDatePickerOpen by rememberSaveable { mutableStateOf(false) }
    var bulkEditTargetKeys by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var bulkDateTargetKeys by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var bulkEditTargetRevisions by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var bulkDateTargetRevisions by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var quickMoveTargetKeys by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var quickMoveTargetRevisions by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var quickMoveEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var bulkEditTargetSnapshot by remember { mutableStateOf<List<ScheduledTask>>(emptyList()) }
    var bulkDateTargetSnapshot by remember { mutableStateOf<List<ScheduledTask>>(emptyList()) }
    var calendarMonth by rememberSaveable(state.currentDate) { mutableStateOf(YearMonth.from(state.currentDate)) }
    var selectedDate by rememberSaveable(state.currentDate) { mutableStateOf(state.currentDate) }
    var dayCapacityText by rememberSaveable { mutableStateOf("240") }
    var showDayPlanner by rememberSaveable { mutableStateOf(false) }
    var dayPlanCandidateKeys by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var taskToolsExpanded by rememberSaveable { mutableStateOf(false) }
    var quickCapture by rememberSaveable { mutableStateOf("") }
    var legacyQuickCaptureSubmitting by remember { mutableStateOf(false) }
    var submittedQuickCapture by rememberSaveable { mutableStateOf("") }
    val quickCaptureCoordinator = rememberPersistenceRequestCoordinator(
        state = authoredMutationState,
        consume = onAuthoredMutationResultConsumed,
        key = "task-quick-capture",
        requestNamespace = "task-quick-capture",
        onPersisted = { receipt ->
            if (receipt.kind == TaskMutationKind.Created &&
                quickCapture.trim() == submittedQuickCapture
            ) {
                quickCapture = ""
            }
            submittedQuickCapture = ""
        },
    )
    val quickCaptureSubmitting = quickCaptureCoordinator.saving || legacyQuickCaptureSubmitting
    val quickCaptureAssumptions = remember(
        quickCapture,
        state.currentDate,
        appSettings.naturalLanguageTaskCapture,
    ) {
        if (appSettings.naturalLanguageTaskCapture) {
            TaskQuickCaptureParser.parse(quickCapture, state.currentDate).assumptions
        } else {
            emptyList()
        }
    }
    val quickCaptureStateDescription = quickCaptureAssumptions.smartCaptureStateDescription(
        "These highlighted phrases will be applied when the Task is added",
    )
    val workspaceDestination = destination.toWorkspaceRoute().destination
    LaunchedEffect(selectionMode, reordering) {
        onSelectionModeChange(selectionMode)
        onReorderModeChange(reordering || enterReorderWhenReady)
    }
    var previousDestination by rememberSaveable { mutableStateOf(destination) }
    LaunchedEffect(destination) {
        destination.toWorkspaceRoute().takeIf { it.destination == TaskWorkspaceDestination.History }
            ?.let { historySection = it.historySection }
        planningView = workspaceDestination.normalizePlanningView(planningView)
        if (previousDestination != destination) {
            selectionMode = false
            reordering = false
            enterReorderWhenReady = false
            selectionActionsOpen = false
            selectedKeys = emptySet()
            pendingBulkCompleteKeys = null
            archivePreviewKeys = null
            archiveTargetRevisions = emptySet()
            bulkEditOpen = false
            bulkDatePickerOpen = false
            bulkEditTargetKeys = null
            bulkDateTargetKeys = null
            bulkEditTargetRevisions = emptySet()
            bulkDateTargetRevisions = emptySet()
            quickMoveTargetKeys = null
            quickMoveTargetRevisions = emptySet()
            quickMoveEpochDay = null
            bulkEditTargetSnapshot = emptyList()
            bulkDateTargetSnapshot = emptyList()
        }
        previousDestination = destination
        dateMode = when (destination) {
            TaskDestination.Completed -> dateMode.takeIf { it in setOf("Any", "Today", "Last7Days") } ?: "Any"
            TaskDestination.Archived -> "Any"
            else -> dateMode
        }
        val supportedSortModes = when (destination) {
            TaskDestination.Completed -> setOf("Smart", "Completion Date", "Priority", "Title")
            TaskDestination.Archived -> setOf("Smart", "Archived Date", "Priority", "Title")
            else -> setOf("Smart", "Manual", "Scheduled Date", "Deadline", "Priority", "Title")
        }
        sortMode = if (destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)) {
            appSettings.activeTaskSortMode.takeIf { it in supportedSortModes } ?: "Smart"
        } else {
            sortMode.takeIf { it in supportedSortModes } ?: "Smart"
        }
        val supportedGroupModes = when (destination) {
            TaskDestination.Completed -> setOf("None", "Completion Date", "Area", "Priority")
            TaskDestination.Archived -> setOf("None", "Archived Date", "Area", "Priority")
            else -> setOf("None", "Scheduled Date", "Area", "Priority")
        }
        if (groupMode !in supportedGroupModes) groupMode = "None"
    }
    LaunchedEffect(planningViewRequest, workspaceDestination) {
        planningViewRequest?.let {
            planningView = workspaceDestination.normalizePlanningView(it)
            onPlanningViewRequestConsumed()
        }
    }
    BackHandler(enabled = reordering || enterReorderWhenReady) {
        reordering = false
        enterReorderWhenReady = false
    }
    LaunchedEffect(reorderDismissRequest) {
        if (reorderDismissRequest > 0) {
            reordering = false
            enterReorderWhenReady = false
        }
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
    val allTasks = state.inbox + state.today + state.upcoming + state.planning + state.completed + state.archived
    LaunchedEffect(bulkEditOpen, bulkEditTargetKeys, allTasks) {
        if (bulkEditOpen && bulkEditTargetSnapshot.isEmpty()) {
            val keys = bulkEditTargetKeys.orEmpty()
            bulkEditTargetSnapshot = allTasks.filter { it.stableKey in keys }
        }
    }
    LaunchedEffect(bulkDatePickerOpen, bulkDateTargetKeys, allTasks) {
        if (bulkDatePickerOpen && bulkDateTargetSnapshot.isEmpty()) {
            val keys = bulkDateTargetKeys.orEmpty()
            bulkDateTargetSnapshot = allTasks.filter { it.stableKey in keys }
        }
    }
    val quickMoveCoordinator = rememberPersistenceRequestCoordinator(
        state = authoredMutationState,
        consume = onAuthoredMutationResultConsumed,
        key = "task-bulk-quick-date",
        requestNamespace = "task-bulk-quick-date",
        onPersisted = {
            quickMoveTargetKeys = null
            quickMoveTargetRevisions = emptySet()
            quickMoveEpochDay = null
            selectionMode = false
            selectionActionsOpen = false
            selectedKeys = emptySet()
        },
    )
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
        sortDescending = sortDirection == SortDirection.Descending,
        groupMode = groupMode,
    )
    // `upcoming` is the authoritative 30-day, preference-aware collection. The
    // year-long `planning` collection exists for search and future planning, but
    // using it here made Agenda and Calendar silently ignore the user's recurring
    // occurrence preference and contradicted the "next 30 days" heading.
    val sourceTasks = state.tasksFor(destination)
    val filtered = sourceTasks
        .filter { it.matches(currentFilter, state.currentDate, appSettings.zoneId()) }
        .sortedForWorkspace(sortMode, sortDirection)
    val visibleTasks = filtered.forPlanningView(planningView, selectedDate, appSettings.zoneId())
    val selectedItems = visibleTasks.filter { it.stableKey in selectedKeys }
    val existingTodayMinutes = state.today
        .distinctBy(ScheduledTask::stableKey)
        .sumOf(ScheduledTask::estimatedDurationMinutes)
    val existingTodayAssumptions = state.today.distinctBy(ScheduledTask::stableKey).count { it.task.durationMinutes == null }
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
    val reorderDestinationEligible = destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)
    val reorderHasConstraints =
        textQuery.isNotBlank() || activeFilterCount > 0 || areaScope != AreaScope.All ||
            groupMode != "None" || planningView != TaskPlanningView.List
    LaunchedEffect(reordering, reorderHasConstraints, sortMode) {
        if (reordering && (reorderHasConstraints || sortMode != "Manual")) reordering = false
    }
    LaunchedEffect(enterReorderWhenReady, reorderHasConstraints) {
        if (enterReorderWhenReady && !reorderHasConstraints) {
            sortMode = "Manual"
            onActiveTaskSortModeChange("Manual")
            reordering = true
            enterReorderWhenReady = false
        }
    }

    fun applyFilter(filter: SavedTaskFilter) {
        val normalized = filter.normalizedForWorkspace()
        priorities = normalized.priorities
        onSelectAreaScope(normalized.restoredAreaScope())
        pinnedOnly = normalized.pinnedOnly
        selectedTags = normalized.tags
        requireAllTags = normalized.requireAllTags
        dateMode = normalized.dateMode
        deadlineOnly = normalized.deadlineOnly
        efforts = normalized.efforts
        maximumDuration = normalized.maximumDurationMinutes?.toString().orEmpty()
        textQuery = normalized.textQuery
        sortMode = normalized.sortMode
        if (destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)) {
            onActiveTaskSortModeChange(normalized.sortMode)
        }
        sortDirection = if (normalized.sortDescending) SortDirection.Descending else SortDirection.Ascending
        groupMode = normalized.groupMode
        runCatching { TaskDestination.valueOf(normalized.destination) }.getOrNull()?.let(onDestinationChange)
        planningView = runCatching { TaskPlanningView.valueOf(normalized.planningView) }
            .getOrDefault(TaskPlanningView.List)
        showFilters = false
    }
    fun submitQuickCapture() {
        if (quickCapture.isBlank() || quickCaptureSubmitting) return
        submittedQuickCapture = quickCapture.trim()
        if (areaScope.requiresExplicitCreationArea(availableAreas)) {
            onAddDetails(submittedQuickCapture)
            quickCapture = ""
            submittedQuickCapture = ""
            return
        }
        val targetDate = state.currentDate.takeIf { destination == TaskDestination.Today }
        val targetAreaId = areaScope.creationDefaultAreaId(availableAreas)
        val request = onQuickCaptureRequest
        if (request != null) {
            val requestId = quickCaptureCoordinator.begin() ?: return
            if (!request(submittedQuickCapture, targetDate, targetAreaId, requestId)) {
                quickCaptureCoordinator.finishFailure("Another Task change is already finishing.")
            }
        } else {
            legacyQuickCaptureSubmitting = true
            onQuickCapture(submittedQuickCapture, targetDate, targetAreaId) { succeeded ->
                if (succeeded && quickCapture.trim() == submittedQuickCapture) quickCapture = ""
                legacyQuickCaptureSubmitting = false
                submittedQuickCapture = ""
            }
        }
    }

    fun finishSelection() {
        pendingBulkCompleteKeys = null
        archivePreviewKeys = null
        archiveTargetRevisions = emptySet()
        selectionMode = false
        selectionActionsOpen = false
        selectedKeys = emptySet()
        bulkEditTargetKeys = null
        bulkDateTargetKeys = null
        bulkEditTargetRevisions = emptySet()
        bulkDateTargetRevisions = emptySet()
        quickMoveTargetKeys = null
        quickMoveTargetRevisions = emptySet()
        quickMoveEpochDay = null
        bulkEditTargetSnapshot = emptyList()
        bulkDateTargetSnapshot = emptyList()
    }
    BackHandler(enabled = selectionMode) {
        if (!quickMoveCoordinator.saving) finishSelection()
    }
    val canReorderTaskList =
        reorderDestinationEligible && maxOf(sourceTasks.distinctBy { it.task.id }.size, allAreaTaskCount) > 1
    val canSelectTaskList = visibleTasks.isNotEmpty()
    fun startTaskReorder() {
        textQuery = ""
        priorities = emptySet()
        selectedTags = emptySet()
        pinnedOnly = false
        dateMode = "Any"
        deadlineOnly = false
        efforts = emptySet()
        maximumDuration = ""
        groupMode = "None"
        planningView = TaskPlanningView.List
        if (areaScope != AreaScope.All) onTemporarilySelectAreaScope(AreaScope.All)
        if (reorderHasConstraints) {
            enterReorderWhenReady = true
        } else {
            sortMode = "Manual"
            onActiveTaskSortModeChange("Manual")
            reordering = true
        }
        taskToolsExpanded = false
    }
    fun startTaskSelection() {
        selectionMode = true
        planningView = TaskPlanningView.List
        selectedKeys = emptySet()
        taskToolsExpanded = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .then(if (quickMoveCoordinator.saving) Modifier.clearAndSetSemantics { } else Modifier),
    ) {
        DestinationTabBar(
            selected = workspaceDestination,
            destinations = allTaskWorkspaceDestinations,
            onSelect = { selected ->
                textQuery = ""
                val nextRoute = TaskWorkspaceRoute(selected, historySection)
                onDestinationChange(nextRoute.dataDestination())
            },
            label = TaskWorkspaceDestination::label,
            testTagPrefix = "task-destination",
            barTestTag = "task-workspace-navigation",
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
                resetCompactItemExpansionOnChange = true,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!selectionMode) WhipPageHeader(
                    title = destination.label,
                    supportingText = taskDestinationSupportingText(destination, visibleTasks.size),
                ) {
                if (!reordering) WhipPageIconAction(
                        icon = Icons.Outlined.FilterList,
                        label = if (activeFilterCount == 0) "Filter & Sort Tasks" else "Filter & Sort Tasks · $activeFilterCount active",
                        onClick = { showFilters = true },
                    )
                if (!reordering && canReorderTaskList && canSelectTaskList) Box {
                    WhipPageIconAction(
                        icon = Icons.Outlined.MoreVert,
                        label = "More task list actions",
                        onClick = { taskToolsExpanded = true },
                    )
                    DropdownMenu(
                        expanded = taskToolsExpanded,
                        onDismissRequest = { taskToolsExpanded = false },
                    ) {
                        WhipMenuItem(
                            label = when {
                                areaScope != AreaScope.All -> "Show All Areas & Reorder"
                                reorderHasConstraints -> "Clear Filters & Reorder All"
                                else -> "Reorder Tasks"
                            },
                            onClick = ::startTaskReorder,
                        )
                        WhipMenuItem(label = "Select Tasks", onClick = ::startTaskSelection)
                    }
                } else if (!reordering && canReorderTaskList) {
                    WhipTextButton(onClick = ::startTaskReorder) { Text("Reorder") }
                } else if (!reordering && canSelectTaskList) {
                    WhipTextButton(onClick = ::startTaskSelection) { Text("Select") }
                }
                }
            if (selectionMode) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${selectedItems.size} selected", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            WhipTextButton(onClick = ::finishSelection) { Text("Done") }
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
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            WhipTextButton(onClick = {
                                selectedKeys = if (selectedItems.size == visibleTasks.size) {
                                    emptySet()
                                } else {
                                    visibleTasks.mapTo(linkedSetOf(), ScheduledTask::stableKey)
                                }
                            }) {
                                Text(if (selectedItems.size == visibleTasks.size && visibleTasks.isNotEmpty()) "Clear Selection" else "Select All")
                            }
                            if (activeFilterCount > 0) {
                                WhipTextButton(onClick = { showFilters = true }) { Text("Filters · $activeFilterCount") }
                            }
                        }
                        PersistenceFailureNotice(
                            quickMoveCoordinator.errorMessage,
                            testTag = "task-bulk-quick-date-problem",
                        )
                        if (quickMoveCoordinator.saving) {
                            Text(
                                "Saving selected Task schedules…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().testTag("task-selection-actions"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val actionModifier = Modifier.widthIn(min = 104.dp).heightIn(min = 48.dp)
                            when (destination) {
                                TaskDestination.Completed -> WhipButton(
                                    enabled = selectedItems.isNotEmpty(),
                                    onClick = { onBulkReopen(selectedItems); finishSelection() },
                                    modifier = actionModifier.testTag("task-selection-reopen"),
                                ) { Text("Reopen", maxLines = 1) }
                                TaskDestination.Archived -> WhipButton(
                                    enabled = selectedItems.isNotEmpty(),
                                    onClick = { onBulkRestore(selectedItems); finishSelection() },
                                    modifier = actionModifier.testTag("task-selection-restore"),
                                ) { Text("Restore", maxLines = 1) }
                                else -> WhipButton(
                                    enabled = selectedItems.isNotEmpty(),
                                    onClick = {
                                        val completionItems = selectedItems.distinctBy(ScheduledTask::stableKey)
                                        if (completionItems.any { item -> item.subtasks.any { !it.completed } }) {
                                            pendingBulkCompleteKeys = completionItems.mapTo(linkedSetOf(), ScheduledTask::stableKey)
                                        } else {
                                            onBulkComplete(completionItems)
                                            finishSelection()
                                        }
                                    },
                                    modifier = actionModifier.testTag("task-selection-complete"),
                                ) { Text("Complete", maxLines = 1) }
                            }
                            WhipOutlinedButton(
                                enabled = selectedItems.isNotEmpty(),
                                onClick = {
                                    bulkEditTargetSnapshot = selectedItems.distinctBy(ScheduledTask::stableKey)
                                    bulkEditTargetKeys = bulkEditTargetSnapshot
                                        .mapTo(linkedSetOf(), ScheduledTask::stableKey)
                                    bulkEditTargetRevisions = bulkEditTargetSnapshot
                                        .mapTo(linkedSetOf(), ScheduledTask::taskMutationRevision)
                                    bulkEditOpen = true
                                },
                                modifier = actionModifier.testTag("task-selection-edit"),
                            ) { Text("Edit", maxLines = 1) }
                            WhipOverflowMenu(
                                label = "More selected Task actions",
                                expanded = selectionActionsOpen,
                                onExpandedChange = { selectionActionsOpen = it },
                                enabled = selectedItems.isNotEmpty(),
                                modifier = Modifier.testTag("task-selection-more"),
                            ) {
                                if (destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)) {
                                    WhipMenuItem(label = "Pin to Whip Home", onClick = {
                                        onBulkPin(selectedItems, true); finishSelection()
                                    })
                                    WhipMenuItem(label = "Unpin from Whip Home", onClick = {
                                        onBulkPin(selectedItems, false); finishSelection()
                                    })
                                    WhipMenuItem(label = "Move to Tomorrow", onClick = {
                                        val targets = selectedItems.distinctBy(ScheduledTask::stableKey)
                                        quickMoveTargetKeys = targets.mapTo(linkedSetOf(), ScheduledTask::stableKey)
                                        quickMoveTargetRevisions = targets.mapTo(linkedSetOf(), ScheduledTask::taskMutationRevision)
                                        quickMoveEpochDay = state.currentDate.plusDays(1).toEpochDay()
                                        selectionActionsOpen = false
                                        val requestId = quickMoveCoordinator.begin() ?: return@WhipMenuItem
                                        if (!onBulkPostponeRequest(targets, state.currentDate.plusDays(1), requestId)) {
                                            quickMoveCoordinator.finishFailure("Another Task change is already finishing.")
                                        }
                                    })
                                    WhipMenuItem(label = "Move to Next Week", onClick = {
                                        val targets = selectedItems.distinctBy(ScheduledTask::stableKey)
                                        quickMoveTargetKeys = targets.mapTo(linkedSetOf(), ScheduledTask::stableKey)
                                        quickMoveTargetRevisions = targets.mapTo(linkedSetOf(), ScheduledTask::taskMutationRevision)
                                        quickMoveEpochDay = state.currentDate.plusWeeks(1).toEpochDay()
                                        selectionActionsOpen = false
                                        val requestId = quickMoveCoordinator.begin() ?: return@WhipMenuItem
                                        if (!onBulkPostponeRequest(targets, state.currentDate.plusWeeks(1), requestId)) {
                                            quickMoveCoordinator.finishFailure("Another Task change is already finishing.")
                                        }
                                    })
                                    WhipMenuItem(label = "Choose Date", onClick = {
                                        bulkDateTargetSnapshot = selectedItems.distinctBy(ScheduledTask::stableKey)
                                        bulkDateTargetKeys = bulkDateTargetSnapshot
                                            .mapTo(linkedSetOf(), ScheduledTask::stableKey)
                                        bulkDateTargetRevisions = bulkDateTargetSnapshot
                                            .mapTo(linkedSetOf(), ScheduledTask::taskMutationRevision)
                                        bulkDatePickerOpen = true
                                        selectionActionsOpen = false
                                    })
                                }
                                if (destination != TaskDestination.Archived) {
                                    WhipMenuItem(
                                        label = "Archive",
                                        onClick = {
                                            archivePreviewKeys = selectedItems.mapTo(linkedSetOf(), ScheduledTask::stableKey)
                                            archiveTargetRevisions = selectedItems
                                                .mapTo(linkedSetOf(), ScheduledTask::taskMutationRevision)
                                            selectionActionsOpen = false
                                        },
                                        modifier = Modifier.testTag("task-selection-archive"),
                                    )
                                }
                                WhipMenuItem(
                                    label = "Delete Permanently",
                                    onClick = {
                                        val ids = selectedItems.mapTo(linkedSetOf()) { it.task.id }
                                        onClearBulkDeletionPreview()
                                        deletePreviewTaskIds = ids
                                        selectionActionsOpen = false
                                    },
                                    role = WhipMenuItemRole.Destructive,
                                    modifier = Modifier.testTag("task-selection-delete"),
                                )
                            }
                        }
                    }
                }
            } else {
                if (reordering) {
                    WhipReorderModeBar(
                        itemLabel = "Tasks",
                        onDone = { reordering = false },
                        boundaryNote = "Pinned and other Tasks reorder separately.",
                    )
                }
                val planningViews = workspaceDestination.allowedPlanningViews()
                if (!reordering && planningViews.size > 1) {
                    SegmentedChoiceBar(
                        selected = planningView,
                        choices = planningViews,
                        onSelect = { planningView = workspaceDestination.normalizePlanningView(it) },
                        label = TaskPlanningView::name,
                        modifier = Modifier.fillMaxWidth(),
                        resetCompactItemExpansionOnChange = true,
                    )
                }
                if (!reordering) WhipActiveFilterRow(
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
                            sortMode.takeIf { it != "Smart" }?.let { mode ->
                                "Sorted by ${if (mode == "Manual") "Custom Order" else mode}" + sortDirection.label.takeIf { mode != "Manual" }?.let { " · $it" }.orEmpty()
                            },
                            groupMode.takeIf { it != "None" }?.let { "Grouped by $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (
                    reordering && sortMode == "Manual" &&
                    (textQuery.isNotBlank() || activeFilterCount > 0 || areaScope != AreaScope.All)
                ) {
                    Text(
                        "Drag ordering is available in All Areas after clearing search and filters, so hidden Tasks are never moved unexpectedly.",
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
        WhipReorderLazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = WhipPageContentPadding,
            verticalArrangement = Arrangement.spacedBy(
                if (appSettings.compactItemLayout) WhipSpacing.micro else WhipSpacing.compact,
            ),
        ) {
        appSettings.focusTimerDeadlineMillis?.takeIf { it > focusClockMillis && !selectionMode && !reordering }?.let { deadline ->
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
        if (!selectionMode && !reordering && destination in setOf(TaskDestination.Today, TaskDestination.Inbox)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = quickCapture,
                        onValueChange = { quickCapture = it },
                        enabled = !quickCaptureSubmitting,
                        label = { Text("Quick Capture to ${destination.label}") },
                        placeholder = {
                            Text(
                                if (appSettings.naturalLanguageTaskCapture) {
                                    "Try: Send report tomorrow at 9am #work"
                                } else {
                                    "What needs doing?"
                                },
                            )
                        },
                        visualTransformation = SmartTaskCaptureVisualTransformation(
                            assumptions = quickCaptureAssumptions,
                            highlightColor = MaterialTheme.colorScheme.primaryContainer,
                            highlightedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        trailingIcon = {
                            IconButton(
                                enabled = quickCapture.isNotBlank() && !quickCaptureSubmitting,
                                onClick = ::submitQuickCapture,
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Add task now")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitQuickCapture() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                quickCaptureStateDescription?.let { stateDescription = it }
                            }
                            .testTag("task-quick-capture"),
                    )
                    PersistenceFailureNotice(
                        quickCaptureCoordinator.errorMessage,
                        testTag = "task-quick-capture-save-problem",
                    )
                    SmartTaskCapturePreview(
                        assumptions = quickCaptureAssumptions,
                        actionText = "Highlighted phrases will be applied when you add this Task. Parsing stays on this device.",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "smart-task-quick-preview",
                    )
                    if (quickCapture.isNotBlank() || quickCaptureSubmitting) Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (quickCaptureSubmitting) {
                            Text(
                                "Saving…",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else Spacer(Modifier.weight(1f))
                        WhipTextButton(
                            enabled = quickCapture.isNotBlank() && !quickCaptureSubmitting,
                            onClick = { onAddDetails(quickCapture) },
                        ) { Text("Add Details") }
                    }
                }
            }
        }
        if (
            !selectionMode && !reordering &&
            filtered.isNotEmpty() &&
            destination == TaskDestination.Inbox
        ) {
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
                            "Set your total capacity for Today. Existing Today tasks count first; new tasks are ranked by priority, then deadline. Tasks without a duration count as 30 minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            dayCapacityText,
                            { dayCapacityText = it.filter(Char::isDigit).take(4) },
                            label = { Text("Available Minutes") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            buildString {
                                append("Already planned: $existingTodayMinutes minutes")
                                if (existingTodayAssumptions > 0) append(" · $existingTodayAssumptions without estimates counted as 30 min")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        WhipButton(
                            enabled = (dayCapacityText.toIntOrNull() ?: 0) > existingTodayMinutes && filtered.isNotEmpty(),
                            onClick = {
                                val remainingCapacity = ((dayCapacityText.toIntOrNull() ?: 240) - existingTodayMinutes)
                                    .coerceAtLeast(0)
                                dayPlanCandidateKeys = selectTasksForCapacity(
                                    filtered,
                                    remainingCapacity,
                                ).mapTo(linkedSetOf(), ScheduledTask::stableKey)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Preview Plan") }
                        dayPlanCandidateKeys?.let { selected ->
                            val capacity = dayCapacityText.toIntOrNull() ?: 240
                            val selectedMinutes = filtered.filter { it.stableKey in selected }
                                .sumOf(ScheduledTask::estimatedDurationMinutes)
                            val totalMinutes = existingTodayMinutes + selectedMinutes
                            HorizontalDivider()
                            Text(
                                "Proposed: ${selected.size} of ${filtered.size} new tasks · $totalMinutes of $capacity minutes total",
                                fontWeight = FontWeight.Bold,
                            )
                            filtered.forEach { candidate ->
                                val checked = candidate.stableKey in selected
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .toggleable(
                                            value = checked,
                                            role = Role.Checkbox,
                                            onValueChange = {
                                                dayPlanCandidateKeys = if (checked) selected - candidate.stableKey
                                                else selected + candidate.stableKey
                                            },
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(candidate.task.title)
                                        Text(
                                            if (checked) {
                                                "Selected · ${candidate.estimatedDurationMinutes()} min${if (candidate.task.durationMinutes == null) " assumed" else ""} · ${candidate.task.priority.name.lowercase()} Priority"
                                            } else if (totalMinutes + candidate.estimatedDurationMinutes() > capacity) {
                                                "Skipped · would exceed capacity"
                                            } else {
                                                "Skipped · lower planning rank"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = null,
                                        modifier = Modifier.clearAndSetSemantics { },
                                    )
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WhipTextButton(onClick = { dayPlanCandidateKeys = null }) { Text("Cancel") }
                                WhipButton(
                                    enabled = selected.isNotEmpty() && totalMinutes <= capacity,
                                    onClick = {
                                        onPlanMyDay(filtered.filter { it.stableKey in selected }, capacity)
                                        dayPlanCandidateKeys = null
                                    },
                                ) { Text("Apply Plan") }
                            }
                            if (totalMinutes > capacity) Text(
                                "Remove ${totalMinutes - capacity} minutes before applying.",
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
        } else if (
            visibleTasks.isEmpty() &&
            !(allTasks.isEmpty() && destination in setOf(TaskDestination.Today, TaskDestination.Inbox))
        ) {
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
                item(key = "agenda-${date ?: "unscheduled"}") {
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
            val groupedTasks = visibleTasks.groupBy { it.groupingLabel(groupMode, appSettings.zoneId()) }
            val sortedGroups = when (groupMode) {
                "Scheduled Date", "Completion Date" -> groupedTasks.entries.sortedBy { (_, tasks) ->
                    tasks.mapNotNull { it.groupingDate(groupMode, appSettings.zoneId()) }.minOrNull() ?: LocalDate.MAX
                }
                "Archived Date" -> groupedTasks.entries.sortedByDescending { (_, tasks) ->
                    tasks.maxOfOrNull { it.task.updatedAtMillis } ?: Long.MIN_VALUE
                }
                "Priority" -> groupedTasks.entries.sortedByDescending { (_, tasks) ->
                    tasks.maxOfOrNull { it.task.priority.ordinal } ?: Int.MIN_VALUE
                }
                else -> groupedTasks.entries.sortedBy { it.key }
            }
            sortedGroups.forEach { (label, tasks) ->
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
        } else items(visibleTasks.size, key = { visibleTasks[it].stableKey }) { index ->
            val item = visibleTasks[index]
            Column {
                if (
                    reordering &&
                    (index == 0 || visibleTasks[index - 1].task.pinned != item.task.pinned)
                ) {
                    Text(
                        if (item.task.pinned) "Pinned Tasks" else "Other Tasks",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
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
                manualOrder = visibleTasks.takeIf {
                    reordering && sortMode == "Manual" && textQuery.isBlank() && activeFilterCount == 0 && areaScope == AreaScope.All
                }.orEmpty(),
                onReorder = onReorder,
                )
            }
        }
        }
    }
    PersistenceSavingOverlay(
        active = quickMoveCoordinator.saving,
        label = "Saving selected Task schedules",
    )
    }

    if (showFilters) {
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { showFilters = false },
            title = { Text("Sort, Group & Filter Tasks") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val sortOptions = when (destination) {
                        TaskDestination.Completed -> listOf("Smart", "Completion Date", "Priority", "Title")
                        TaskDestination.Archived -> listOf("Smart", "Archived Date", "Priority", "Title")
                        else -> listOf("Smart", "Manual", "Scheduled Date", "Deadline", "Priority", "Title")
                    }
                    SelectionField(
                        label = "Sort By",
                        values = sortOptions,
                        selected = sortMode.takeIf { it in sortOptions } ?: "Smart",
                        valueText = { if (it == "Manual") "Custom Order" else it },
                        onSelect = { selected ->
                            sortMode = selected
                            if (destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)) {
                                onActiveTaskSortModeChange(selected)
                            }
                            if (selected == "Manual" && !reorderHasConstraints && reorderDestinationEligible) {
                                showFilters = false
                                enterReorderWhenReady = true
                            } else {
                                reordering = false
                            }
                        },
                    )
                    if (sortMode !in setOf("Smart", "Manual")) {
                        SelectionField(
                            label = "Order",
                            values = SortDirection.entries,
                            selected = sortDirection,
                            valueText = SortDirection::label,
                            onSelect = { sortDirection = it },
                        )
                    }
                    val dateGroup = when (destination) {
                        TaskDestination.Completed -> "Completion Date"
                        TaskDestination.Archived -> "Archived Date"
                        else -> "Scheduled Date"
                    }
                    val groupOptions = if (destination == TaskDestination.Archived) {
                        if (areaScope == AreaScope.All) listOf("None", dateGroup, "Area", "Priority") else listOf("None", dateGroup, "Priority")
                    } else if (areaScope == AreaScope.All) {
                        listOf("None", dateGroup, "Area", "Priority")
                    } else {
                        listOf("None", dateGroup, "Priority")
                    }
                    SelectionField(
                        label = "Group By",
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
                    if (destination != TaskDestination.Archived) {
                        Text(
                            if (destination == TaskDestination.Completed) "Completion Date" else "Scheduled Date & Deadline",
                            fontWeight = FontWeight.Bold,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val options = if (destination == TaskDestination.Completed) {
                                listOf("Any" to "Any Date", "Today" to "Completed Today", "Last7Days" to "Last 7 Days")
                            } else {
                                listOf(
                                    "Any" to "Any Date",
                                    "Today" to "Scheduled Today",
                                    "PastScheduled" to "Past Scheduled Date",
                                    "Next7Days" to "Next 7 Days",
                                    "NoDate" to "No Scheduled Date",
                                )
                            }
                            options.forEach { (value, filterLabel) ->
                                WhipFilterChip(dateMode == value, { dateMode = value }, { Text(filterLabel) })
                            }
                            if (destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)) {
                                WhipFilterChip(dateMode == "Overdue", { dateMode = "Overdue" }, { Text("Deadline Overdue") })
                                WhipFilterChip(selected = deadlineOnly, onClick = { deadlineOnly = !deadlineOnly }, label = { Text("Has Deadline") })
                            }
                            WhipFilterChip(selected = pinnedOnly, onClick = { pinnedOnly = !pinnedOnly }, label = { Text("Pinned Only") })
                        }
                    }
                    Text("Effort & Duration", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskEffort.entries.forEach { value ->
                            WhipFilterChip(value in efforts, { efforts = if (value in efforts) efforts - value else efforts + value }, { Text(value.label) })
                        }
                    }
                    OutlinedTextField(
                        maximumDuration,
                        { maximumDuration = it.filter(Char::isDigit).take(4) },
                        label = { Text("Maximum Minutes") },
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
            confirmButton = { WhipButton(onClick = { showFilters = false }) { Text("Done") } },
            dismissButton = {
                WhipTextButton(onClick = {
                    sortMode = "Smart"; sortDirection = SortDirection.Ascending; groupMode = "None"
                    if (destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)) {
                        onActiveTaskSortModeChange("Smart")
                    }
                    priorities = emptySet(); pinnedOnly = false
                    selectedTags = emptySet(); requireAllTags = true; dateMode = "Any"; deadlineOnly = false
                    efforts = emptySet(); maximumDuration = ""; textQuery = ""
                }) { Text("Reset") }
            },
        )
    }

    if (saveFilterOpen) {
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = { saveFilterOpen = false },
            title = { Text("Save Task Filter") },
            text = { OutlinedTextField(filterName, { filterName = it }, label = { Text("Filter Name") }, singleLine = true) },
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
    pendingBulkCompleteKeys?.let { keys ->
        val completionItems = allTasks
            .filter { it.stableKey in keys }
            .distinctBy(ScheduledTask::stableKey)
        val tasksWithUnfinishedSubtasks = completionItems.count { item -> item.subtasks.any { !it.completed } }
        val unfinishedSubtaskCount = completionItems.sumOf { item -> item.subtasks.count { !it.completed } }
        val unfinishedSummary = pluralStringResource(
            R.plurals.task_bulk_unfinished_subtasks,
            unfinishedSubtaskCount,
            unfinishedSubtaskCount,
        )
        val taskSummary = pluralStringResource(
            R.plurals.task_bulk_selected_tasks,
            tasksWithUnfinishedSubtasks,
            tasksWithUnfinishedSubtasks,
        )
        PaneAwareAlertDialog(
            modifier = dialogModifier.testTag("task-bulk-completion-review"),
            onDismissRequest = { pendingBulkCompleteKeys = null },
            title = { Text(stringResource(R.string.task_bulk_completion_review_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.task_bulk_completion_review_message,
                        unfinishedSummary,
                        taskSummary,
                    ),
                )
            },
            confirmButton = {
                WhipTextButton(
                    enabled = completionItems.isNotEmpty() && unfinishedSubtaskCount > 0,
                    onClick = {
                        onBulkComplete(completionItems)
                        finishSelection()
                    },
                    modifier = Modifier.testTag("confirm-task-bulk-completion"),
                ) { Text(stringResource(R.string.action_complete_anyway)) }
            },
            dismissButton = {
                WhipTextButton(
                    onClick = { pendingBulkCompleteKeys = null },
                    modifier = Modifier.testTag("cancel-task-bulk-completion"),
                ) { Text(stringResource(R.string.action_keep_working)) }
            },
        )
    }
    archivePreviewKeys?.let { keys ->
        val affected = allTasks.filter { it.stableKey in keys }.distinctBy(ScheduledTask::stableKey)
        val targetChanged = affected.mapTo(linkedSetOf(), ScheduledTask::stableKey) != keys ||
            affected.mapTo(linkedSetOf(), ScheduledTask::taskMutationRevision) != archiveTargetRevisions
        val series = affected.distinctBy { it.task.id }
        val recurringSeries = series.count { it.task.scheduleKind == ScheduleKind.Recurring }
        val mutationCoordinator = rememberPersistenceRequestCoordinator(
            state = authoredMutationState,
            consume = onAuthoredMutationResultConsumed,
            key = keys.sorted().joinToString(prefix = "archive-"),
            requestNamespace = "task-bulk-archive",
            onPersisted = {
                archivePreviewKeys = null
                archiveTargetRevisions = emptySet()
                finishSelection()
            },
        )
        PaneAwareAlertDialog(
            modifier = dialogModifier,
            onDismissRequest = {
                if (!mutationCoordinator.saving) {
                    mutationCoordinator.clear()
                    archivePreviewKeys = null
                    archiveTargetRevisions = emptySet()
                }
            },
            title = { Text("Archive ${series.size} ${if (series.size == 1) "Task" else "Tasks"}?") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("task-bulk-archive-impact"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "This hides ${series.size} ${if (series.size == 1) "task" else "tasks"} from active views. " +
                            if (recurringSeries > 0) "$recurringSeries repeating ${if (recurringSeries == 1) "series is" else "series are"} archived in full, not just the selected occurrence." else "No repeating series are included.",
                    )
                    series.take(8).forEach { Text("• ${it.task.title}") }
                    if (series.size > 8) Text("…and ${series.size - 8} more")
                    Text("Completed history is kept. You can restore these from Archived.", style = MaterialTheme.typography.bodySmall)
                    PersistenceFailureNotice(
                        mutationCoordinator.errorMessage ?: if (targetChanged) {
                            "One or more selected Tasks changed. Cancel and select them again."
                        } else null,
                        testTag = "task-bulk-archive-problem",
                    )
                }
            },
            confirmButton = {
                WhipButton(
                    enabled = affected.isNotEmpty() && !targetChanged && !mutationCoordinator.saving,
                    onClick = {
                        val requestId = mutationCoordinator.begin() ?: return@WhipButton
                        if (!onBulkArchiveRequest(affected, requestId)) {
                            mutationCoordinator.finishFailure("Another Task change is already finishing.")
                        }
                    },
                ) { Text(if (mutationCoordinator.saving) "Archiving…" else "Archive ${series.size}") }
            },
            dismissButton = {
                WhipTextButton(
                    enabled = !mutationCoordinator.saving,
                    onClick = {
                        mutationCoordinator.clear()
                        archivePreviewKeys = null
                        archiveTargetRevisions = emptySet()
                    },
                ) { Text("Cancel") }
            },
        )
    }
    deletePreviewTaskIds?.let { taskIds ->
        LaunchedEffect(taskIds, deletionBatchImpact?.requestedTaskIds) {
            if (deletionBatchImpact?.requestedTaskIds != taskIds) onPreviewBulkDeletion(taskIds)
        }
        val mutationCoordinator = rememberPersistenceRequestCoordinator(
            state = authoredMutationState,
            consume = onAuthoredMutationResultConsumed,
            key = taskIds.sorted().joinToString(prefix = "delete-"),
            requestNamespace = "task-bulk-delete",
            onPersisted = {
                deletePreviewTaskIds = null
                finishSelection()
            },
        )
        PermanentTaskBatchDeleteDialog(
            taskIds = taskIds,
            impact = deletionBatchImpact,
            modifier = dialogModifier,
            saving = mutationCoordinator.saving,
            persistenceError = mutationCoordinator.errorMessage,
            onDismiss = {
                mutationCoordinator.clear()
                deletePreviewTaskIds = null
                onClearBulkDeletionPreview()
            },
            onConfirm = {
                val exactImpact = deletionBatchImpact?.takeIf {
                    it.requestedTaskIds == taskIds && it.taskIds == taskIds
                } ?: return@PermanentTaskBatchDeleteDialog
                val requestId = mutationCoordinator.begin() ?: return@PermanentTaskBatchDeleteDialog
                if (!onBulkDeletePermanentlyRequest(taskIds, exactImpact.revisionTokens, requestId)) {
                    mutationCoordinator.finishFailure("Another Task change is already finishing.")
                }
            },
        )
    }
    if (bulkDatePickerOpen) {
        val targetKeys = bulkDateTargetKeys.orEmpty()
        val targets = bulkDateTargetSnapshot
            .ifEmpty { allTasks.filter { it.stableKey in targetKeys } }
        val currentTargets = allTasks.filter { it.stableKey in targetKeys }
        val targetChanged = currentTargets.mapTo(linkedSetOf(), ScheduledTask::stableKey) != targetKeys ||
            currentTargets.mapTo(linkedSetOf(), ScheduledTask::taskMutationRevision) != bulkDateTargetRevisions
        val mutationCoordinator = rememberPersistenceRequestCoordinator(
            state = authoredMutationState,
            consume = onAuthoredMutationResultConsumed,
            key = targetKeys.sorted().joinToString(prefix = "date-"),
            requestNamespace = "task-bulk-date",
            onPersisted = {
                bulkDatePickerOpen = false
                finishSelection()
            },
        )
        WhipDatePickerDialog(
            initialDate = state.currentDate,
            modifier = dialogModifier,
            saving = mutationCoordinator.saving,
            persistenceError = mutationCoordinator.errorMessage ?: if (targetChanged) {
                "One or more selected Tasks changed. Cancel and select them again."
            } else null,
            savingLabel = "Saving Task Schedules",
            onDismiss = {
                mutationCoordinator.clear()
                bulkDatePickerOpen = false
                bulkDateTargetKeys = null
                bulkDateTargetRevisions = emptySet()
                bulkDateTargetSnapshot = emptyList()
            },
            onDateSelected = { date ->
                if (targetChanged || targets.isEmpty()) {
                    mutationCoordinator.finishFailure(
                        "One or more selected Tasks changed. Cancel and select them again.",
                    )
                    return@WhipDatePickerDialog
                }
                val requestId = mutationCoordinator.begin() ?: return@WhipDatePickerDialog
                if (!onBulkPostponeRequest(targets, date, requestId)) {
                    mutationCoordinator.finishFailure("Another Task change is already finishing.")
                }
            },
        )
    }
    if (bulkEditOpen) {
        val targetKeys = bulkEditTargetKeys.orEmpty()
        val targets = bulkEditTargetSnapshot
            .ifEmpty { allTasks.filter { it.stableKey in targetKeys } }
        val currentTargets = allTasks.filter { it.stableKey in targetKeys }
        val targetChanged = currentTargets.mapTo(linkedSetOf(), ScheduledTask::stableKey) != targetKeys ||
            currentTargets.mapTo(linkedSetOf(), ScheduledTask::taskMutationRevision) != bulkEditTargetRevisions
        val mutationCoordinator = rememberPersistenceRequestCoordinator(
            state = authoredMutationState,
            consume = onAuthoredMutationResultConsumed,
            key = targetKeys.sorted().joinToString(prefix = "edit-"),
            requestNamespace = "task-bulk-edit",
            onPersisted = {
                bulkEditOpen = false
                finishSelection()
            },
        )
        TaskBulkEditDialog(
            count = targets.map { it.task.id }.distinct().size,
            modifier = dialogModifier,
            knownAreas = availableAreas,
            knownTags = availableTags,
            saving = mutationCoordinator.saving,
            persistenceError = mutationCoordinator.errorMessage ?: if (targetChanged) {
                "One or more selected Tasks changed. Cancel and select them again."
            } else null,
            onDismiss = {
                mutationCoordinator.clear()
                bulkEditOpen = false
                bulkEditTargetKeys = null
                bulkEditTargetRevisions = emptySet()
                bulkEditTargetSnapshot = emptyList()
            },
            onApply = { edit ->
                if (targetChanged || targets.isEmpty()) {
                    mutationCoordinator.finishFailure(
                        "One or more selected Tasks changed. Cancel and select them again.",
                    )
                    return@TaskBulkEditDialog
                }
                val requestId = mutationCoordinator.begin() ?: return@TaskBulkEditDialog
                if (!onBulkEditRequest(targets, edit, requestId)) {
                    mutationCoordinator.finishFailure("Another Task change is already finishing.")
                }
            },
        )
    }
}

private fun ScheduledTask.taskMutationRevision(): String {
    val values = buildList {
        add(stableKey)
        add(task.id.toString())
        add(task.uuid)
        add(task.createdAtMillis.toString())
        add(task.updatedAtMillis.toString())
        add(task.title)
        add(task.notes)
        add(task.scheduleKind.name)
        add(task.date?.toEpochDay()?.toString().orEmpty())
        add(task.recurrence?.toString().orEmpty())
        add(task.completedAtMillis?.toString().orEmpty())
        add(task.archived.toString())
        add(task.inbox.toString())
        add(task.areaId.orEmpty())
        add(task.area)
        add(task.tags.sorted().joinToString("\u001f"))
        add(task.priority.name)
        add(task.effort.name)
        add(originalDate?.toEpochDay()?.toString().orEmpty())
        add(scheduledDate?.toEpochDay()?.toString().orEmpty())
        add(completedAtMillis?.toString().orEmpty())
        subtasks.sortedBy { it.step.id }.forEach { subtask ->
            add("${subtask.step.id}:${subtask.step.updatedAtMillis}:${subtask.completed}")
        }
    }
    return values.joinToString(separator = "") { value -> "${value.length}:$value" }
}

@Composable
private fun PermanentTaskBatchDeleteDialog(
    taskIds: Set<Long>,
    impact: TaskDeletionBatchImpact?,
    modifier: Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    saving: Boolean = false,
    persistenceError: String? = null,
) {
    val exactImpact = impact?.takeIf {
        it.requestedTaskIds == taskIds && it.taskIds == taskIds
    }
    val count = taskIds.size
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!saving) onDismiss() },
        inputBlocked = saving,
        inputBlockedLabel = "Deleting Tasks Permanently",
        title = { Text("Delete $count ${if (count == 1) "Task" else "Tasks"} Permanently?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersistenceFailureNotice(
                    persistenceError,
                    testTag = "task-bulk-delete-save-problem",
                )
                when {
                    impact == null || impact.requestedTaskIds != taskIds -> {
                        Text("Calculating the exact deletion impact…")
                    }
                    exactImpact == null -> {
                        Text(
                            "One or more selected tasks changed or no longer exist. Close this confirmation and select the tasks again.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    else -> {
                        exactImpact.titles.take(8).forEach { Text("• $it") }
                        if (exactImpact.titles.size > 8) Text("…and ${exactImpact.titles.size - 8} more")
                        if (exactImpact.recurringSeriesCount > 0) {
                            Text(
                                "${exactImpact.recurringSeriesCount} repeating ${if (exactImpact.recurringSeriesCount == 1) "series is" else "series are"} included in full.",
                            )
                        }
                        Text("Removed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${exactImpact.recordedOccurrenceCount} recorded occurrence${if (exactImpact.recordedOccurrenceCount == 1) "" else "s"} " +
                                "(${exactImpact.completedOccurrenceCount} completed, ${exactImpact.skippedOccurrenceCount} skipped, ${exactImpact.openOccurrenceCount} open)",
                        )
                        Text("${exactImpact.stepCount} subtask${if (exactImpact.stepCount == 1) "" else "s"}")
                        Text("${exactImpact.linkRuleIds.size} Goal link${if (exactImpact.linkRuleIds.size == 1) "" else "s"}")
                        Text("${exactImpact.automationRuleIds.size} Automation${if (exactImpact.automationRuleIds.size == 1) "" else "s"}")
                        Text(
                            "This cannot be undone. Export a backup first if you may need this history.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !saving && exactImpact != null,
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm-task-selection-delete"),
            ) { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TaskBulkEditDialog(
    count: Int,
    modifier: Modifier,
    knownAreas: List<Area>,
    knownTags: List<String>,
    onDismiss: () -> Unit,
    onApply: (TaskBulkEdit) -> Unit,
    saving: Boolean = false,
    persistenceError: String? = null,
) {
    var applyArea by rememberSaveable { mutableStateOf(false) }
    var areaId by rememberSaveable { mutableStateOf<String?>(null) }
    var areaName by rememberSaveable { mutableStateOf("") }
    var applyTags by rememberSaveable { mutableStateOf(false) }
    var tags by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("") }
    var effort by rememberSaveable { mutableStateOf("") }
    val areaSelectionMissing = applyArea && areaId.isNullOrBlank()
    val canApply = !areaSelectionMissing &&
        (applyArea || applyTags || priority.isNotBlank() || effort.isNotBlank())
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!saving) onDismiss() },
        inputBlocked = saving,
        inputBlockedLabel = "Saving Task Changes",
        title = { Text("Edit $count ${if (count == 1) "Task" else "Tasks"}") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
            ) {
                PersistenceFailureNotice(
                    persistenceError,
                    testTag = "task-bulk-edit-save-problem",
                )
                Text("Only enabled fields change. Tags replace the selected tasks’ complete tag set.", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Area, tags, priority, and effort update each selected Task’s full definition, including an entire repeating series. Date moves affect only the selected occurrences.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WhipSettingsRow(
                    title = "Change Area",
                    supportingText = "Move every selected Task to one Area.",
                    checked = applyArea,
                    onCheckedChange = { applyArea = it },
                    modifier = Modifier.testTag("task-bulk-edit-area-toggle"),
                )
                if (applyArea) {
                    AreaSelectionDropdown(
                        areas = knownAreas,
                        selectedAreaId = areaId,
                        selectedAreaName = areaName,
                        onSelect = { id, name -> areaId = id; areaName = name },
                    )
                    if (areaSelectionMissing) {
                        Text(
                            "Choose an Area before applying this change.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                WhipSettingsRow(
                    title = "Replace Tags",
                    supportingText = "Use one shared tag set for every selected Task.",
                    checked = applyTags,
                    onCheckedChange = { applyTags = it },
                    modifier = Modifier.testTag("task-bulk-edit-tags-toggle"),
                )
                if (applyTags) {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Replacement Tags") },
                        supportingText = { Text("Comma-separated. Leave blank to remove all tags.") },
                        modifier = Modifier.fillMaxWidth().testTag("task-bulk-edit-tags"),
                    )
                }
                if (applyTags && knownTags.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                    knownTags.forEach { value ->
                        val current = tags.split(',').map(String::trim).filter(String::isNotBlank)
                        WhipFilterChip(current.any { it.equals(value, true) }, {
                            tags = if (current.any { it.equals(value, true) }) current.filterNot { it.equals(value, true) }.joinToString(", ")
                            else (current + value).joinToString(", ")
                        }, { Text("#$value") })
                    }
                }
                Text("Priority · ${priority.ifBlank { "Keep existing" }}", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                    WhipFilterChip(priority.isBlank(), { priority = "" }, { Text("Keep") })
                    TaskPriority.entries.forEach { value -> WhipFilterChip(priority == value.name, { priority = value.name }, { Text(value.name) }) }
                }
                val selectedEffortLabel = effort.takeIf(String::isNotBlank)
                    ?.let { stored -> TaskEffort.entries.firstOrNull { it.name == stored }?.label }
                    ?: "Keep existing"
                Text("Effort · $selectedEffortLabel", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                    WhipFilterChip(effort.isBlank(), { effort = "" }, { Text("Keep") })
                    TaskEffort.entries.forEach { value -> WhipFilterChip(effort == value.name, { effort = value.name }, { Text(value.label) }) }
                }
            }
        },
        confirmButton = {
            WhipButton(
                enabled = !saving && canApply,
                onClick = {
                    onApply(
                        TaskBulkEdit(
                            updateArea = applyArea,
                            areaId = areaId,
                            areaName = areaName,
                            tags = tags.split(',').map(String::trim).filter(String::isNotBlank).toSet().takeIf { applyTags },
                            priority = priority.takeIf(String::isNotBlank)?.let(TaskPriority::valueOf),
                            effort = effort.takeIf(String::isNotBlank)?.let(TaskEffort::valueOf),
                        ),
                    )
                },
            ) { Text("Apply Changes") }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
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
            "Overdue" -> isDeadlineOverdue
            "PastScheduled" -> isPastScheduledDate
            "Next7Days" -> planningDate(zoneId)?.let { it in today..today.plusDays(7) } == true
            "Last7Days" -> planningDate(zoneId)?.let { it in today.minusDays(6)..today } == true
            "NoDate" -> planningDate(zoneId) == null
            else -> true
        }

private fun ScheduledTask.planningDate(zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()): LocalDate? =
    completedAtMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
        ?: scheduledDate
        ?: task.deadline

private fun ScheduledTask.groupingDate(mode: String, zoneId: java.time.ZoneId): LocalDate? = when (mode) {
    "Scheduled Date" -> scheduledDate
    "Completion Date" -> completedAtMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
    "Archived Date" -> java.time.Instant.ofEpochMilli(task.updatedAtMillis).atZone(zoneId).toLocalDate()
    else -> null
}

private fun ScheduledTask.groupingLabel(mode: String, zoneId: java.time.ZoneId): String = when (mode) {
    "Scheduled Date", "Completion Date", "Archived Date" ->
        groupingDate(mode, zoneId)?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "No Date"
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
        val habitSkips = skips.filter { it.habitId == habit.id }
        if (habit.archived || habit.hasEnded(habitLogs, date, habitPauses, customUnits, habitSkips)) return@mapNotNull null
        val explicitlyPaused = habitPauses.any { pause ->
            !date.isBefore(pause.startDate) && (pause.endDate == null || !date.isAfter(pause.endDate))
        }
        if (explicitlyPaused) return@mapNotNull null
        val flexible = habit.flexibleProgress(habitLogs, date, habitPauses, habitSkips)
        val weekCount = flexible?.completed.takeIf { habit.scheduleType == com.whip.app.domain.HabitScheduleType.FlexibleTimesPerWeek } ?: 0
        val monthCount = flexible?.completed.takeIf { habit.scheduleType == com.whip.app.domain.HabitScheduleType.FlexibleTimesPerMonth } ?: 0
        current.copy(
            date = date,
            scheduled = habit.isScheduledOn(date, weekCount, monthCount),
            value = habit.valueForPeriod(habitLogs, date, customUnits),
            successful = habit.outcomeForPeriod(habitLogs, date, customUnits),
            dayState = habit.dayStateOn(date, currentDate, habitLogs, habitPauses, habitSkips, customUnits),
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
    reorderMode: Boolean = false,
) {
    val completed = destination == TaskDestination.Completed ||
        (destination == TaskDestination.Archived && item.completedAtMillis != null)
    val completionAvailable = destination !in setOf(TaskDestination.Completed, TaskDestination.Archived)
    TaskRow(
        item = item,
        completed = completed,
        onComplete = if (completionAvailable) ({ onCompleteTask(item) }) else null,
        onOpenActions = if (destination == TaskDestination.Completed) ({ onOpenCompleted(item) }) else ({ onOpenTask(item) }),
        onEdit = { onEditTask(item) },
        selectionMode = selectionMode,
        selected = item.stableKey in selectedKeys,
        onSelectionToggle = {
            onSelectionChange(
                if (item.stableKey in selectedKeys) selectedKeys - item.stableKey
                else selectedKeys + item.stableKey,
            )
        },
        reorderMode = reorderMode,
        showCompletionControl = destination != TaskDestination.Archived,
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
    val unique = manualOrder.distinctBy { it.task.id }
    val partition = unique.filter { it.task.pinned == item.task.pinned }
    val index = partition.indexOfFirst { it.task.id == item.task.id }
    val reorderable = index >= 0 && !selectionMode &&
        manualOrder.indexOfFirst { it.task.id == item.task.id } == manualOrder.indexOf(item)
    val reorderInteraction = rememberWhipReorderInteractionState()
    Row(
        modifier = Modifier.whipReorderItem(
            reorderInteraction,
            layoutPosition = index + 1,
            layoutScope = "task-browse-${item.task.pinned}",
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (reorderable) {
            WhipReorderHandle(
                label = item.task.title,
                canMovePrevious = index > 0,
                canMoveNext = index in 0 until partition.lastIndex,
                position = index + 1,
                total = partition.size,
                interactionState = reorderInteraction,
                moveWholeItem = true,
                layoutScope = "task-browse-${item.task.pinned}",
                reserveWhenUnavailable = true,
                onMove = { delta ->
                    val moved = moveListItem(partition, index, delta)
                    val iterator = moved.iterator()
                    onReorder(unique.map { candidate ->
                        if (candidate.task.pinned == item.task.pinned) iterator.next() else candidate
                    })
                },
            )
        }
        Box(Modifier.weight(1f)) {
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
            reorderMode = manualOrder.isNotEmpty(),
            )
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
private fun HomeItemGroupHeading(
    label: String,
    count: Int,
    pinned: Boolean = false,
    testTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(testTag?.let(Modifier::testTag) ?: Modifier)
            .semantics { heading() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pinned) {
            Icon(
                Icons.Outlined.PushPin,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "$label ($count)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeStatusCard(title: String, detail: String, onClick: (() -> Unit)? = null) {
    WhipCollectionCard(
        onClick = onClick,
        onClickLabel = "Open $title",
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
        }
    }
}

@Composable
internal fun TodayHeader(
    date: LocalDate,
    taskTotal: Int,
    habitCompleted: Int,
    habitTotal: Int,
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    showFullHeader: Boolean = true,
) {
    val habitProgress = if (habitTotal == 0) 0f else habitCompleted.toFloat() / habitTotal
    Column(
        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
    ) {
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
        if (taskTotal > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("home-tasks-today-record")
                    .clickable(onClickLabel = "Open Tasks Today", onClick = onOpenTasks)
                    .semantics {
                        contentDescription = "Tasks Due Today: $taskTotal. Open Tasks Today"
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tasks Due Today", style = MaterialTheme.typography.titleSmall)
                Text(
                    taskTotal.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
            }
        }
        if (taskTotal > 0 && habitTotal > 0) Spacer(Modifier.height(4.dp))
        if (habitTotal > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home-habit-progress-record")
                    .clickable(onClickLabel = "Open Habits Today", onClick = onOpenHabits)
                    .semantics {
                        contentDescription = "Habit Progress: $habitCompleted of $habitTotal. Open Habits Today"
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Habit Progress", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "$habitCompleted of $habitTotal",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
                }
                LinearProgressIndicator(
                    progress = { habitProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}

private fun taskDestinationSupportingText(destination: TaskDestination, count: Int): String {
    val description = when (destination) {
        TaskDestination.Inbox -> "Captured and waiting for a decision"
        TaskDestination.Today -> "What needs your attention now"
        TaskDestination.Upcoming -> "The next 30 days"
        TaskDestination.Completed -> "Your latest completed tasks"
        TaskDestination.Archived -> "Stored safely until you restore them"
    }
    return if (count > 0) "$description · ${count.itemCount("task")}" else description
}

private fun Int.itemCount(noun: String): String = "$this $noun${if (this == 1) "" else "s"}"

private fun String.taskDateModeLabel(): String = when (this) {
    "Today" -> "Scheduled Today"
    "Overdue" -> "Deadline Overdue"
    "PastScheduled" -> "Past Scheduled Date"
    "Next7Days" -> "Next 7 Days"
    "Last7Days" -> "Last 7 Days"
    "NoDate" -> "No Date"
    else -> this
}

@Composable
private fun EmptyTasks(
    destination: TaskDestination,
    areaLabel: String? = null,
    constrained: Boolean = false,
) {
    val supportingText = if (constrained) {
        "No tasks match the current view or filters. Change the view or remove a filter to see more."
    } else when (destination) {
            TaskDestination.Inbox -> areaLabel?.let { "No Inbox tasks in $it." } ?: "No tasks in Inbox. Quick captures can wait here until you triage them."
            TaskDestination.Today -> areaLabel?.let { "Nothing scheduled or carried over in $it." } ?: "Nothing scheduled or carried over today."
            TaskDestination.Upcoming -> areaLabel?.let { "No tasks in $it over the next 30 days." } ?: "No tasks in the next 30 days."
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
        TaskDestination.Completed -> "Completed"
        TaskDestination.Archived -> "Archived"
    }

internal fun TaskDestination.creationPlacement(): TaskPlacement = when (this) {
    TaskDestination.Inbox -> TaskPlacement.Inbox
    TaskDestination.Today, TaskDestination.Upcoming -> TaskPlacement.Scheduled
    TaskDestination.Completed, TaskDestination.Archived -> TaskPlacement.Inbox
}

private val AppDestination.label: String
    get() = when (this) {
        AppDestination.Home -> "Home"
        AppDestination.Tasks -> "Tasks"
        AppDestination.Habits -> "Habits"
        AppDestination.Gym -> "Gym"
        AppDestination.Goals -> "Goals"
        AppDestination.Tracks -> "Tracks"
        AppDestination.Settings -> "Settings"
    }

@get:StringRes
private val AppDestination.labelRes: Int
    get() = when (this) {
        AppDestination.Home -> R.string.nav_home
        AppDestination.Tasks -> R.string.nav_tasks
        AppDestination.Habits -> R.string.nav_habits
        AppDestination.Goals -> R.string.nav_goals
        AppDestination.Gym -> R.string.nav_gym
        AppDestination.Tracks -> R.string.nav_tracks
        AppDestination.Settings -> R.string.nav_settings
    }

private val AppDestination.icon: ImageVector
    get() = when (this) {
        AppDestination.Home -> Icons.Outlined.Home
        AppDestination.Tasks -> Icons.Outlined.CheckCircle
        AppDestination.Habits -> Icons.Outlined.Autorenew
        AppDestination.Gym -> Icons.Outlined.FitnessCenter
        AppDestination.Goals -> Icons.Outlined.Flag
        AppDestination.Tracks -> Icons.Outlined.TableRows
        AppDestination.Settings -> Icons.Outlined.Settings
    }
