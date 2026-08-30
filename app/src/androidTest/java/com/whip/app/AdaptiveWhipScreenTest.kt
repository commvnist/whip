package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.GoalUiState
import com.whip.app.ui.DomainLoadContent
import com.whip.app.ui.DomainRetryActions
import com.whip.app.ui.GoalViewModel
import com.whip.app.ui.GymUiState
import com.whip.app.ui.GymViewModel
import com.whip.app.ui.HabitUiState
import com.whip.app.ui.HabitViewModel
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.SettingsUiState
import com.whip.app.ui.TrackUiState
import com.whip.app.ui.TrackViewModel
import com.whip.app.ui.ResponsiveFieldPair
import com.whip.app.ui.WhipAdaptiveLayout
import com.whip.app.ui.WhipFoldInfo
import com.whip.app.ui.WhipFoldOrientation
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.theme.WhipTheme
import com.whip.app.core.OperationFeedbackPresentation
import com.whip.app.core.OperationStatus
import com.whip.app.core.AppSettings
import com.whip.app.core.WhipLaunchActions
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.Track
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.UnitDimension
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveWhipScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun adaptiveHomeWaitsForVisibleDomainsBeforeShowingAStableEmptyState() {
        val taskState = mutableStateOf(TaskUiState())
        val habitState = mutableStateOf(HabitUiState())
        val goalState = mutableStateOf(GoalUiState())
        val trackState = mutableStateOf(TrackUiState())
        val gymState = mutableStateOf(GymUiState())
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = taskState.value,
                    habitState = habitState.value,
                    goalState = goalState.value,
                    trackState = trackState.value,
                    gymState = gymState.value,
                    settingsState = SettingsUiState(settings = AppSettings(setupCompleted = true)),
                    adaptiveLayout = WhipAdaptiveLayout.ExpandedDashboard,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithTag("home-support-tasks-loading").assertIsDisplayed()
        compose.onAllNodesWithTag("home-support-introduction").assertCountEquals(0)
        compose.onAllNodesWithTag("home-support-clear-day").assertCountEquals(0)

        compose.runOnIdle {
            taskState.value = TaskUiState(loading = false)
            habitState.value = HabitUiState(loading = false)
            goalState.value = GoalUiState(loading = false)
            trackState.value = TrackUiState(loading = false)
            gymState.value = GymUiState(loading = false)
        }
        compose.onNodeWithTag("home-support-introduction").assertIsDisplayed()
        compose.onAllNodesWithTag("home-support-tasks-loading").assertCountEquals(0)
    }

    @Test
    fun adaptiveDestinationSupportSeparatesErrorLoadingAndRealEmpty() {
        val today = LocalDate.of(2026, 8, 29)
        val partialTask = ScheduledTask(
            task = WhipTask(
                id = 77,
                title = "Cached task",
                notes = "",
                scheduleKind = ScheduleKind.Once,
                date = today,
                recurrence = null,
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
            originalDate = today,
            scheduledDate = today,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(
                        today = listOf(partialTask),
                        currentDate = today,
                        loading = false,
                        errorMessage = "Task refresh failed",
                    ),
                    habitState = HabitUiState(loading = true),
                    goalState = GoalUiState(loading = false),
                    trackState = TrackUiState(loading = false),
                    gymState = GymUiState(loading = false),
                    settingsState = SettingsUiState(settings = AppSettings(setupCompleted = true)),
                    adaptiveLayout = WhipAdaptiveLayout.ExpandedDashboard,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithTag("support-pane-tasks-error").assertIsDisplayed()
        check(compose.onAllNodesWithText("Task refresh failed").fetchSemanticsNodes().isNotEmpty())
        check(compose.onAllNodesWithText("Cached Task").fetchSemanticsNodes().isNotEmpty())
        compose.onAllNodesWithTag("support-pane-tasks-empty").assertCountEquals(0)

        compose.onNodeWithContentDescription("Habits tab").performClick()
        compose.onNodeWithTag("support-pane-habits-loading").assertIsDisplayed()
        compose.onAllNodesWithTag("support-pane-habits-empty").assertCountEquals(0)

        compose.onNodeWithContentDescription("Goals tab").performClick()
        compose.onNodeWithTag("support-pane-goals-empty").assertIsDisplayed()
        compose.onAllNodesWithTag("support-pane-goals-loading").assertCountEquals(0)
        compose.onAllNodesWithTag("support-pane-goals-error").assertCountEquals(0)
    }

    @Test
    fun expandedTrackSupportSeparatesLoadingFailureRetryAndRealZeroMetrics() {
        val trackState = mutableStateOf(TrackUiState())
        var retries = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    trackState = trackState.value,
                    gymState = GymUiState(loading = false),
                    settingsState = SettingsUiState(settings = AppSettings(setupCompleted = true)),
                    domainRetryActions = DomainRetryActions(tracks = { retries += 1 }),
                    adaptiveLayout = WhipAdaptiveLayout.ExpandedDashboard,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-overview-support-loading").assertIsDisplayed()
        compose.onAllNodesWithText("Active Tracks").assertCountEquals(0)

        compose.runOnIdle {
            trackState.value = TrackUiState(loading = false, errorMessage = "Track refresh failed")
        }
        compose.onNodeWithTag("track-overview-support-error").assertIsDisplayed()
        compose.onNodeWithText("Track refresh failed").assertIsDisplayed()
        compose.onNodeWithText("Try Again").performClick()
        compose.runOnIdle { check(retries == 1) }
        compose.onAllNodesWithText("Active Tracks").assertCountEquals(0)

        compose.runOnIdle { trackState.value = TrackUiState(loading = false) }
        compose.onAllNodesWithTag("track-overview-support-loading").assertCountEquals(0)
        compose.onAllNodesWithTag("track-overview-support-error").assertCountEquals(0)
        compose.onNodeWithText("Active Tracks").assertIsDisplayed()
    }

    @Test
    fun bookFoldTrackSupportNeverShowsEmptyBeforeTheDomainSettles() {
        val trackState = mutableStateOf(TrackUiState())
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    trackState = trackState.value,
                    gymState = GymUiState(loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.BookFold,
                    foldInfo = WhipFoldInfo(
                        orientation = WhipFoldOrientation.Vertical,
                        leftPx = 700,
                        topPx = 0,
                        rightPx = 740,
                        bottomPx = 1_800,
                        separating = false,
                        halfOpened = false,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-support-loading").assertIsDisplayed()
        compose.onAllNodesWithTag("track-support-empty").assertCountEquals(0)

        compose.runOnIdle { trackState.value = TrackUiState(loading = false) }
        compose.onAllNodesWithTag("track-support-loading").assertCountEquals(0)
        compose.onNodeWithTag("track-support-empty").assertIsDisplayed()
    }

    @Test
    fun sharedLoadingErrorRetryIsActionableAndNeverShowsFalseSuccess() {
        val error = mutableStateOf<String?>(null)
        var retries = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                DomainLoadContent(
                    domain = "Tasks",
                    innerPadding = PaddingValues(0.dp),
                    errorMessage = error.value,
                    onRetry = {
                        retries += 1
                        error.value = null
                    },
                )
            }
        }

        compose.onNodeWithText("Loading Tasks…").assertIsDisplayed()
        compose.runOnIdle { error.value = "Storage is temporarily unavailable" }
        compose.onNodeWithText("Could not load Tasks").assertIsDisplayed()
        compose.onNodeWithText("Storage is temporarily unavailable").assertIsDisplayed()
        compose.onAllNodesWithText("Loading Tasks…").assertCountEquals(0)

        compose.onNodeWithText("Try Again").performClick()
        compose.runOnIdle { check(retries == 1) }
        compose.onNodeWithText("Loading Tasks…").assertIsDisplayed()
        compose.onAllNodesWithText("Could not load Tasks").assertCountEquals(0)
    }

    @Test
    fun homeSuppressesFalseEmptyStateAndOffersCompactDomainRetry() {
        var trackRetries = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    gymState = GymUiState(loading = false),
                    trackState = TrackUiState(loading = false, errorMessage = "Track storage is unavailable"),
                    settingsState = SettingsUiState(settings = AppSettings(setupCompleted = true)),
                    domainRetryActions = DomainRetryActions(tracks = { trackRetries += 1 }),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onAllNodesWithTag("home-getting-started").assertCountEquals(0)
        compose.onAllNodesWithText("Your Day Is Clear").assertCountEquals(0)
        compose.onNodeWithTag("home-tracks-load-error").assertIsDisplayed()
        compose.onNodeWithText("Track storage is unavailable").assertIsDisplayed()
        compose.onNodeWithText("Try Again").performClick()
        compose.runOnIdle { check(trackRetries == 1) }
    }

    @Test
    fun missingDeepLinkTargetIsConsumedWithOneUnavailableMessage() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    gymState = GymUiState(loading = false),
                    trackState = TrackUiState(loading = false),
                    settingsState = SettingsUiState(settings = AppSettings(setupCompleted = true)),
                    initialAction = WhipLaunchActions.ACTION_OPEN_TRACK,
                    initialEntityId = 404,
                    initialDeliveryId = 88,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("This Track is no longer available.").fetchSemanticsNodes().size == 1
        }
        compose.onAllNodesWithText("This Track is no longer available.").assertCountEquals(1)
    }

    @Test
    fun newerTaskUndoSnackbarKeepsItsOwnRecoveryToken() {
        val status = mutableStateOf<OperationStatus>(OperationStatus.Idle)
        val undoMessage = mutableStateOf<String?>(null)
        val undoToken = mutableStateOf<Long?>(null)
        val gymStatus = mutableStateOf<OperationStatus>(OperationStatus.Idle)
        val machineUndoId = mutableStateOf<Long?>(null)
        var undoDismissals = 0
        val undoneTokens = mutableListOf<Long>()
        val restoredMachineIds = mutableListOf<Long>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    operationStatus = status.value,
                    taskUndoMessage = undoMessage.value,
                    taskUndoToken = undoToken.value,
                    onOperationStatusConsumed = { status.value = OperationStatus.Idle },
                    onTaskUndo = { undoneTokens += it },
                    onTaskUndoDismissed = { token ->
                        undoDismissals += 1
                        if (undoToken.value == token) {
                            undoMessage.value = null
                            undoToken.value = null
                        }
                    },
                    gymOperationStatus = gymStatus.value,
                    onGymOperationStatusConsumed = { gymStatus.value = OperationStatus.Idle },
                    machineArchiveUndoId = machineUndoId.value,
                    onMachineArchiveUndo = { restoredMachineIds += it },
                    onMachineArchiveUndoDismissed = { id ->
                        if (machineUndoId.value == id) machineUndoId.value = null
                    },
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.runOnIdle {
            undoMessage.value = "Skip can be undone"
            undoToken.value = 1L
            status.value = OperationStatus.Succeeded(
                "Occurrence skipped",
                OperationFeedbackPresentation.Snackbar,
            )
        }
        compose.onNodeWithText("Occurrence skipped").assertIsDisplayed()

        compose.runOnIdle {
            undoMessage.value = "Move can be undone"
            undoToken.value = 2L
            status.value = OperationStatus.Succeeded(
                "Task moved",
                OperationFeedbackPresentation.Snackbar,
            )
        }
        compose.onAllNodesWithText("Occurrence skipped").assertCountEquals(0)
        compose.onNodeWithText("Task moved").assertIsDisplayed()
        compose.runOnIdle {
            gymStatus.value = OperationStatus.Succeeded("Exercise saved")
        }
        compose.onNodeWithText("Task moved").assertIsDisplayed()
        compose.onNodeWithText("Undo").performClick()
        compose.runOnIdle { check(undoneTokens == listOf(2L)) }
        compose.runOnIdle { check(undoDismissals > 0) }

        compose.runOnIdle {
            undoMessage.value = "Skip can be undone"
            undoToken.value = 3L
            status.value = OperationStatus.Succeeded(
                "Another occurrence skipped",
                OperationFeedbackPresentation.Snackbar,
            )
        }
        compose.onNodeWithText("Another occurrence skipped").assertIsDisplayed()
        compose.runOnIdle {
            undoMessage.value = null
            undoToken.value = null
            status.value = OperationStatus.Running("Completing task…")
        }
        compose.onAllNodesWithText("Another occurrence skipped").assertCountEquals(0)
        compose.onAllNodesWithText("Undo").assertCountEquals(0)
        compose.onAllNodesWithText("Completing task…").assertCountEquals(0)

        compose.runOnIdle {
            machineUndoId.value = 9L
            gymStatus.value = OperationStatus.Succeeded(
                "Workout saved to history",
                OperationFeedbackPresentation.Snackbar,
            )
        }
        compose.onNodeWithText("Workout saved to history").assertIsDisplayed()
        compose.onAllNodesWithText("Undo").assertCountEquals(0)

        compose.runOnIdle {
            gymStatus.value = OperationStatus.Succeeded(
                "Machine archived",
                OperationFeedbackPresentation.Snackbar,
                recoveryToken = 9L,
            )
        }
        compose.onNodeWithText("Machine archived").assertIsDisplayed()
        compose.runOnIdle {
            machineUndoId.value = 10L
            gymStatus.value = OperationStatus.Succeeded(
                "Machine archived again",
                OperationFeedbackPresentation.Snackbar,
                recoveryToken = 10L,
            )
        }
        compose.onAllNodesWithText("Machine archived", substring = false).assertCountEquals(0)
        compose.onNodeWithText("Machine archived again").assertIsDisplayed()
        compose.onNodeWithText("Undo").performClick()
        compose.runOnIdle { check(restoredMachineIds == listOf(10L)) }
    }

    @Test
    fun reviewUsesTheWholeWideCanvasAsAHingeAwareDashboard() {
        val wideDensity = Density(1f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides wideDensity) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        habitState = HabitUiState(loading = false),
                        goalState = GoalUiState(loading = false),
                        gymState = GymUiState(loading = false),
                        trackState = TrackUiState(loading = false),
                        adaptiveLayout = WhipAdaptiveLayout.BookFold,
                        foldInfo = WhipFoldInfo(
                            orientation = WhipFoldOrientation.Vertical,
                            leftPx = 320,
                            topPx = 0,
                            rightPx = 360,
                            bottomPx = 1_800,
                            separating = true,
                            halfOpened = true,
                        ),
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("home-destination-review").assertIsDisplayed().performClick()
        compose.onNodeWithTag("review-wide-dashboard").assertIsDisplayed()
        compose.onNodeWithTag("review-control-pane").assertIsDisplayed()
        compose.onNodeWithTag("review-overview-pane").assertIsDisplayed()
        compose.onNodeWithTag("review-hinge-gutter").assertIsDisplayed()
        compose.onAllNodesWithTag("review-compact-dashboard").assertCountEquals(0)
        compose.onNodeWithText("Included Sections").assertIsDisplayed()
        compose.onNodeWithText("Overview").assertIsDisplayed()

        val dashboard = compose.onNodeWithTag("review-wide-dashboard").fetchSemanticsNode().boundsInRoot
        val controls = compose.onNodeWithTag("review-control-pane").fetchSemanticsNode().boundsInRoot
        val gutter = compose.onNodeWithTag("review-hinge-gutter").fetchSemanticsNode().boundsInRoot
        val overview = compose.onNodeWithTag("review-overview-pane").fetchSemanticsNode().boundsInRoot
        check(dashboard.width > 900f) { "Review should own the wide window, not a dialog-width strip: $dashboard" }
        check(controls.right <= gutter.left + 1f && gutter.right <= overview.left + 1f) {
            "Review content must respect the hinge: controls=$controls gutter=$gutter overview=$overview"
        }
        val reviewTitle = compose.onNodeWithTag("review-destination-title").fetchSemanticsNode().boundsInRoot
        val reviewClose = compose.onNodeWithTag("review-close-action").fetchSemanticsNode().boundsInRoot
        check(reviewClose.center.x > reviewTitle.center.x) {
            "Review's dismiss action must stay on the trailing side like Settings: title=$reviewTitle close=$reviewClose"
        }
        compose.onNodeWithContentDescription("Close Review & Trends").performClick()
        compose.onAllNodesWithTag("review-wide-dashboard").assertCountEquals(0)
    }

    @Test
    fun bookFoldUsesHingeAwareSupportPaneAndPersistentNavigation() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    trackState = TrackUiState(loading = false),
                    gymState = GymUiState(loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.BookFold,
                    foldInfo = WhipFoldInfo(
                        orientation = WhipFoldOrientation.Vertical,
                        leftPx = 700,
                        topPx = 0,
                        rightPx = 740,
                        bottomPx = 1_800,
                        separating = false,
                        halfOpened = false,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithTag("fold-support-pane").assertIsDisplayed()
        compose.onNodeWithTag("adaptive-navigation-rail").assertIsDisplayed()
        compose.onNodeWithContentDescription("Device hinge separator").assertIsDisplayed()
        compose.onNodeWithContentDescription("Home").assertIsDisplayed()
        compose.onNodeWithTag("workspace-search-action").assertIsDisplayed().performClick()
        compose.onNodeWithTag("unified-search-query").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close Search").performClick()
        val navigationTops = listOf("Tasks tab", "Habits tab", "Goals tab", "Tracks tab", "Gym tab").map { description ->
            compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.top
        }
        check(navigationTops.zipWithNext().all { (before, after) -> before < after }) {
            "Primary navigation must be ordered Tasks, Habits, Goals, Tracks, Gym: $navigationTops"
        }
        val supportPixels = compose.onNodeWithTag("fold-support-pane").captureToImage().toPixelMap()
        val topRailBackground = supportPixels[10, 10]
        val topOverviewBackground = supportPixels[supportPixels.width * 3 / 4, 10]
        check(topRailBackground.luminance() < 0.25f && topOverviewBackground.luminance() < 0.25f) {
            "Dark fold top inset exposed a light/transparent background: rail=$topRailBackground overview=$topOverviewBackground"
        }
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNodeWithText("Your Day, Brought Together").assertIsDisplayed()
        compose.onNodeWithText("Start small. Add only what helps.").assertIsDisplayed()
        compose.onNodeWithText("Private by default.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("home-getting-started").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("home-destination-tasks").assertIsDisplayed()
        compose.onNodeWithTag("home-destination-gym").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("home-destination-review").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand content pane").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithContentDescription("Restore split view").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search All Whip Data").assertIsDisplayed()
        check(compose.onAllNodesWithTag("fold-support-pane").fetchSemanticsNodes().isEmpty())
        check(compose.onAllNodesWithTag("adaptive-navigation-rail").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithContentDescription("Restore split view").assertIsDisplayed().performClick()
        compose.onNodeWithTag("fold-support-pane").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tasks tab")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithContentDescription("Tasks tab").assertIsDisplayed()
        compose.onNodeWithText("Tasks Today").assertIsDisplayed()
        compose.onNodeWithText("No Tasks need attention today.").assertIsDisplayed()
        // A scrollable destination bar must stay content-height in a narrow fold
        // pane so the page header and task content remain in the viewport.
        compose.onNodeWithTag("page-title").assertIsDisplayed()
        compose.onNodeWithTag("task-quick-capture").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Habits Remaining: 0. Open Habits").assertCountEquals(0)
        compose.onNodeWithContentDescription("Gym tab").performClick()
        compose.onNodeWithTag("workspace-top-app-bar").assertIsDisplayed()
        compose.onNodeWithTag("workspace-search-action").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings tab").assertIsDisplayed()
        val visibleGymLabels = compose.onAllNodesWithText("Gym").fetchSemanticsNodes().count { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
        check(visibleGymLabels > 0) { "The Gym identity or destination label must remain visible" }
        compose.onAllNodesWithTag("workspace-area-action").assertCountEquals(0)
        compose.onNodeWithContentDescription("Settings tab").performClick()
        compose.onNodeWithTag("fold-support-pane").assertIsDisplayed()
        compose.onNodeWithTag("adaptive-navigation-rail").assertIsDisplayed()
        compose.onNodeWithTag("settings-support-list").assertIsDisplayed()
        compose.onAllNodesWithTag("adaptive-bottom-navigation").assertCountEquals(0)
    }

    @Test
    fun bookFoldSupportTitlesUseReadableThemeContrast() {
        val darkTheme = mutableStateOf(true)
        compose.setContent {
            WhipTheme(darkTheme = darkTheme.value, dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.BookFold,
                    foldInfo = WhipFoldInfo(
                        orientation = WhipFoldOrientation.Vertical,
                        leftPx = 700,
                        topPx = 0,
                        rightPx = 740,
                        bottomPx = 1_800,
                        separating = true,
                        halfOpened = true,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Tasks tab").performClick()

        listOf(true, false).forEach { useDarkTheme ->
            compose.runOnIdle { darkTheme.value = useDarkTheme }
            compose.waitForIdle()
            val pixels = compose.onNodeWithTag("support-pane-title").captureToImage().toPixelMap()
            val luminances = buildList {
                for (y in 0 until pixels.height) for (x in 0 until pixels.width) add(pixels[x, y].luminance())
            }
            if (useDarkTheme) {
                check(luminances.maxOrNull()!! > 0.5f) {
                    "Dark support title did not render with a light foreground"
                }
            } else {
                check(luminances.minOrNull()!! < 0.5f) {
                    "Light support title did not render with a dark foreground"
                }
            }
        }
    }

    @Test
    fun tabletopFoldKeepsNavigationAndOverviewAboveTheHinge() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.TabletopFold,
                    foldInfo = WhipFoldInfo(
                        orientation = WhipFoldOrientation.Horizontal,
                        leftPx = 0,
                        topPx = 700,
                        rightPx = 1_800,
                        bottomPx = 740,
                        separating = true,
                        halfOpened = true,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithTag("fold-support-pane").assertIsDisplayed()
        compose.onNodeWithTag("adaptive-tabletop-navigation").assertIsDisplayed()
        compose.onNodeWithContentDescription("Device hinge separator").assertIsDisplayed()
    }

    @Test
    fun productivityEditorsStayInsideTheContentPaneOnABookFold() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val habitViewModel: HabitViewModel = viewModel()
                val goalViewModel: GoalViewModel = viewModel()
                val trackViewModel: TrackViewModel = viewModel()
                val gymViewModel: GymViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    habitViewModel = habitViewModel,
                    goalState = GoalUiState(loading = false),
                    goalViewModel = goalViewModel,
                    trackState = TrackUiState(loading = false),
                    trackViewModel = trackViewModel,
                    gymState = GymUiState(loading = false),
                    gymViewModel = gymViewModel,
                    adaptiveLayout = WhipAdaptiveLayout.BookFold,
                    foldInfo = WhipFoldInfo(
                        orientation = WhipFoldOrientation.Vertical,
                        leftPx = 700,
                        topPx = 0,
                        rightPx = 740,
                        bottomPx = 1_800,
                        separating = false,
                        halfOpened = false,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        fun assertEditorInsideContentPane(tag: String) {
            val hinge = compose.onNodeWithContentDescription("Device hinge separator").fetchSemanticsNode().boundsInRoot
            val editor = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            check(editor.left >= hinge.right - 1f) { "$tag crossed the hinge: editor=$editor hinge=$hinge" }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
        compose.onNodeWithText("New Task").performClick()
        assertEditorInsideContentPane("task-editor-surface")
        compose.onNodeWithContentDescription("Cancel Task editing").performClick()

        compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
        compose.onNodeWithText("New Habit").performClick()
        assertEditorInsideContentPane("habit-editor-surface")
        compose.onNodeWithContentDescription("Cancel Habit editing").performClick()

        compose.onNodeWithContentDescription("Goals tab").performClick()
        compose.onNodeWithContentDescription("Add goal").performClick()
        assertEditorInsideContentPane("goal-editor-surface")
        compose.onNodeWithContentDescription("Cancel Goal editing").performClick()

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithText("Create First Track").performClick()
        compose.onNodeWithTag("track-editor-surface").fetchSemanticsNode()
        check(
            compose.onNodeWithTag("app-background-shell").fetchSemanticsNode().config
                .contains(SemanticsProperties.HideFromAccessibility),
        ) { "The root-owned Track editor must hide the underlying shell from accessibility" }
        compose.onNodeWithContentDescription("Close Track Editor").performClick()

        compose.onNodeWithContentDescription("Go to Home").performClick()
        compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
        compose.onNodeWithText("New Exercise").performClick()
        assertEditorInsideContentPane("exercise-editor-surface")
        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
    }

    @Test
    fun taskDetailsStayInsideTheContentPaneOnAFlatBookFold() {
        val today = LocalDate.of(2026, 8, 19)
        val item = ScheduledTask(
            task = WhipTask(
                id = 42,
                title = "Pane-safe task",
                notes = "",
                scheduleKind = ScheduleKind.Once,
                date = today,
                recurrence = null,
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
            originalDate = today,
            scheduledDate = today,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(today = listOf(item), currentDate = today, loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.BookFold,
                    foldInfo = WhipFoldInfo(
                        orientation = WhipFoldOrientation.Vertical,
                        leftPx = 700,
                        topPx = 0,
                        rightPx = 740,
                        bottomPx = 1_800,
                        separating = false,
                        halfOpened = false,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithContentDescription("Open task details for Pane-safe task")
            .performSemanticsAction(SemanticsActions.OnClick)
        val hinge = compose.onNodeWithContentDescription("Device hinge separator").fetchSemanticsNode().boundsInRoot
        val dialog = compose.onNodeWithTag("task-actions-surface").fetchSemanticsNode().boundsInRoot
        check(dialog.left >= hinge.right - 1f) { "Task details crossed the flat-fold pane: dialog=$dialog hinge=$hinge" }
    }

    @Test
    fun expandedInnerDisplayKeepsOverviewBesidePrimaryContent() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.ExpandedDashboard,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithTag("adaptive-navigation-rail").assertIsDisplayed()
        compose.onNodeWithTag("expanded-support-pane").assertIsDisplayed()
    }

    @Test
    fun wideNavigationCentersTheCompleteDestinationGroup() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.NavigationRail,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        val rail = compose.onNodeWithTag("adaptive-navigation-rail").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val destinations = compose.onNodeWithTag("adaptive-navigation-rail-destinations").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        check(kotlin.math.abs(destinations.center.y - rail.center.y) <= compose.density.density * 2f) {
            "The wide navigation destinations must be vertically centered: rail=$rail destinations=$destinations"
        }
        val orderedDestinations = listOf("Home", "Tasks tab", "Habits tab", "Goals tab", "Tracks tab", "Gym tab")
            .map { description ->
                compose.onNodeWithContentDescription(description).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            }
        check(orderedDestinations.zipWithNext().all { (before, after) -> before.top < after.top }) {
            "The centered rail must retain Home-first destination order: $orderedDestinations"
        }
    }

    @Test
    fun compactNavigationMatchesTheRailAndKeepsWhipHomeReachable() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.Compact,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithTag("adaptive-bottom-navigation").assertIsDisplayed()
        compose.onNodeWithContentDescription("Home").assertIsDisplayed().assertIsSelected()
        val orderedDestinations = listOf("Home", "Tasks tab", "Habits tab", "Goals tab", "Tracks tab", "Gym tab")
            .map { description ->
                compose.onNodeWithContentDescription(description).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            }
        check(orderedDestinations.zipWithNext().all { (before, after) -> before.left < after.left }) {
            "The compact bar must retain the rail's Home-first destination order: $orderedDestinations"
        }
        check(orderedDestinations.all { it.width >= compose.density.density * 48f }) {
            "Every compact navigation destination must retain a 48 dp touch target: $orderedDestinations"
        }

        compose.onNodeWithContentDescription("Tasks tab").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Go to Home").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Home").assertIsSelected()
    }

    @Test
    fun everyPrimaryWorkspaceUsesTheSameHeaderAndNavigationGeometry() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val habitViewModel: HabitViewModel = viewModel()
                val goalViewModel: GoalViewModel = viewModel()
                val trackViewModel: TrackViewModel = viewModel()
                val gymViewModel: GymViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    habitViewModel = habitViewModel,
                    goalState = GoalUiState(loading = false),
                    goalViewModel = goalViewModel,
                    trackState = TrackUiState(loading = false),
                    trackViewModel = trackViewModel,
                    gymState = GymUiState(loading = false),
                    gymViewModel = gymViewModel,
                    adaptiveLayout = WhipAdaptiveLayout.Compact,
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        data class Geometry(val headerTop: Float, val headerHeight: Float, val navigationTop: Float, val navigationHeight: Float, val addLeft: Float, val actionsLeft: Float)
        val snapshots = listOf(
            Triple("Tasks tab", "task-workspace-navigation", true),
            Triple("Habits tab", "habit-workspace-navigation", true),
            Triple("Goals tab", "goal-workspace-navigation", true),
            Triple("Tracks tab", "track-workspace-navigation", true),
            Triple("Gym tab", "gym-workspace-navigation", false),
        ).map { (tab, navigationTag, hasArea) ->
            compose.onNodeWithContentDescription(tab).performClick()
            compose.onNodeWithTag(navigationTag).assertIsDisplayed()
            if (hasArea) compose.onNodeWithContentDescription("Area scope: Main").assertIsDisplayed()
            else {
                compose.onAllNodesWithTag("workspace-area-action").assertCountEquals(0)
                compose.onAllNodesWithText("Gym").assertCountEquals(2)
            }
            compose.onAllNodesWithTag("workspace-add-action").assertCountEquals(1)
            compose.onAllNodesWithTag("workspace-search-action").assertCountEquals(1)
            compose.onAllNodesWithTag("workspace-settings-action").assertCountEquals(1)
            val header = compose.onNodeWithTag("workspace-top-app-bar").fetchSemanticsNode().boundsInRoot
            val navigation = compose.onNodeWithTag(navigationTag).fetchSemanticsNode().boundsInRoot
            val add = compose.onNodeWithTag("workspace-add-action").fetchSemanticsNode().boundsInRoot
            val actions = compose.onNodeWithTag("workspace-settings-action").fetchSemanticsNode().boundsInRoot
            check(add.top >= header.top && add.bottom <= header.bottom) {
                "Compact Add must stay inside the shared app bar: add=$add header=$header"
            }
            Geometry(header.top, header.height, navigation.top, navigation.height, add.left, actions.left)
        }
        val expected = snapshots.first()
        snapshots.drop(1).forEach { actual ->
            check(kotlin.math.abs(actual.headerTop - expected.headerTop) <= 1f)
            check(kotlin.math.abs(actual.headerHeight - expected.headerHeight) <= 1f)
            check(kotlin.math.abs(actual.navigationTop - expected.navigationTop) <= 1f)
            check(kotlin.math.abs(actual.navigationHeight - expected.navigationHeight) <= 1f)
            check(kotlin.math.abs(actual.addLeft - expected.addLeft) <= 1f)
            check(kotlin.math.abs(actual.actionsLeft - expected.actionsLeft) <= 1f)
        }
        compose.onNodeWithTag("workspace-search-action").assertIsDisplayed()
        compose.onNodeWithTag("workspace-settings-action").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Expand content pane").assertCountEquals(0)

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-workspace-destination-Activity").performClick().assertIsSelected()
        compose.onNodeWithText("A chronological view of Entries across visible Tracks", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-destination-Insights").performClick().assertIsSelected()
        compose.onNodeWithText("Patterns across visible Tracks.").assertIsDisplayed()

        compose.onNodeWithContentDescription("Gym tab").performClick()
        compose.onNodeWithTag("gym-destination-Library").performClick().assertIsSelected()
        compose.onNodeWithText("Routines").performClick()
        compose.onAllNodesWithTag("workspace-add-action").assertCountEquals(1)
        compose.onNodeWithContentDescription("Create routine").assertIsDisplayed().performClick()
        compose.onNodeWithTag("routine-builder").assertIsDisplayed()
    }

    @Test
    fun expandedWorkspacesKeepSearchAndSettingsInOneSharedColumn() {
        val wideDensity = Density(1f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides wideDensity) {
                WhipTheme(dynamicColor = false) {
                    val habitViewModel: HabitViewModel = viewModel()
                    val goalViewModel: GoalViewModel = viewModel()
                    val trackViewModel: TrackViewModel = viewModel()
                    val gymViewModel: GymViewModel = viewModel()
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        habitState = HabitUiState(loading = false),
                        habitViewModel = habitViewModel,
                        goalState = GoalUiState(loading = false),
                        goalViewModel = goalViewModel,
                        trackState = TrackUiState(loading = false),
                        trackViewModel = trackViewModel,
                        gymState = GymUiState(loading = false),
                        gymViewModel = gymViewModel,
                        adaptiveLayout = WhipAdaptiveLayout.ExpandedDashboard,
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }

        val actionColumns = listOf("Tasks tab", "Habits tab", "Goals tab", "Tracks tab", "Gym tab").map { tab ->
            compose.onNodeWithContentDescription(tab).performClick()
            if (tab == "Tracks tab") compose.onNodeWithTag("track-overview-support").assertIsDisplayed()
            val search = compose.onNodeWithTag("workspace-search-action").fetchSemanticsNode().boundsInRoot
            val settings = compose.onNodeWithTag("workspace-settings-action").fetchSemanticsNode().boundsInRoot
            search.left to settings.left
        }
        val expected = actionColumns.first()
        actionColumns.drop(1).forEach { actual ->
            check(kotlin.math.abs(actual.first - expected.first) <= 1f) { "Search moved between expanded workspaces: $actionColumns" }
            check(kotlin.math.abs(actual.second - expected.second) <= 1f) { "Settings moved between expanded workspaces: $actionColumns" }
        }
        compose.onAllNodesWithTag("workspace-area-action").assertCountEquals(0)
        compose.onNodeWithText("Whip").assertIsDisplayed()
    }

    @Test
    fun expandingContentPreservesSelectedTrackAndProvidesAHomeRoute() {
        val projection = TrackProjection(
            track = Track(1, "track-1", "Films", "", "🎬", "main", "Main", emptyList(), false, false, 0, 1, 1),
            fields = listOf(
                TrackField(1, "title", 1, "Title", TrackFieldType.ShortText, 0, true, true, false, null, null, 0, null, null, "", "", 1, 1),
            ),
            options = emptyList(),
            entries = emptyList(),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val trackViewModel: TrackViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    trackState = TrackUiState(projections = listOf(projection), loading = false),
                    trackViewModel = trackViewModel,
                    adaptiveLayout = WhipAdaptiveLayout.BookFold,
                    foldInfo = WhipFoldInfo(WhipFoldOrientation.Vertical, 700, 0, 740, 1_800, separating = true, halfOpened = true),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-workspace-navigation").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open Films").performClick()
        compose.onNodeWithTag("track-workspace-navigation").assertIsDisplayed()
        val backToTracks = compose.onNodeWithContentDescription("Back to Tracks").fetchSemanticsNode().boundsInRoot
        check(backToTracks.width > 0f && backToTracks.height > 0f) { "Back to Tracks must remain laid out in a narrow Fold pane: $backToTracks" }
        val workspaceNavigation = compose.onNodeWithTag("track-workspace-navigation").fetchSemanticsNode().boundsInRoot
        val detailNavigation = compose.onNodeWithTag("track-destination-Entries").fetchSemanticsNode().boundsInRoot
        val appBounds = compose.onNodeWithTag("workspace-top-app-bar").fetchSemanticsNode().boundsInRoot
        check(detailNavigation.width > 0f && detailNavigation.height > 0f && detailNavigation.top >= appBounds.bottom) {
            "Track detail navigation must be laid out below the app header: detail=$detailNavigation appHeader=$appBounds"
        }
        check(workspaceNavigation.bottom <= detailNavigation.top + 1f) {
            "Track detail navigation must remain subordinate: workspace=$workspaceNavigation detail=$detailNavigation"
        }
        compose.onNodeWithContentDescription("Back to Tracks").assertIsDisplayed()
        compose.onNodeWithContentDescription("Expand content pane")
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithContentDescription("Back to Tracks").assertIsDisplayed()
        compose.onNodeWithContentDescription("Go to Home").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Home").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("home-list").assertIsDisplayed()
    }

    @Test
    fun compactTaskEditorRemainsReachableAtTwoHundredPercentText() {
        val density = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                WhipTheme(dynamicColor = false) {
                    val habitViewModel: HabitViewModel = viewModel()
                    val goalViewModel: GoalViewModel = viewModel()
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        habitState = HabitUiState(loading = false),
                        habitViewModel = habitViewModel,
                        goalState = GoalUiState(loading = false),
                        goalViewModel = goalViewModel,
                        adaptiveLayout = WhipAdaptiveLayout.Compact,
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").assertIsDisplayed().performClick()
        compose.onNodeWithText("New Task").assertIsDisplayed().performClick()
        compose.onNodeWithTag("task-editor-title").assertIsDisplayed()
        compose.onNodeWithTag("task-editor-more-details").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Planning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Effort").performScrollTo().assertIsDisplayed()
        listOf("Unspecified", "Light", "Medium", "High").forEach { storedName ->
            compose.onNodeWithTag("task-effort-$storedName").performScrollTo().assertIsDisplayed()
        }
        compose.onAllNodesWithText("Moderate").assertCountEquals(0)
        compose.onAllNodesWithText("High effort").assertCountEquals(0)
        compose.onNodeWithContentDescription("Cancel Task editing").assertIsDisplayed().performClick()
    }

    @Test
    fun compactHabitEditorRemainsReachableAtTwoHundredPercentText() {
        val density = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                WhipTheme(dynamicColor = false) {
                    val habitViewModel: HabitViewModel = viewModel()
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        habitState = HabitUiState(loading = false),
                        habitViewModel = habitViewModel,
                        adaptiveLayout = WhipAdaptiveLayout.Compact,
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
        compose.onNodeWithText("New Habit").performClick()
        compose.onNodeWithTag("habit-editor-name").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel Habit editing").performClick()
    }

    @Test
    fun compactGoalEditorRemainsReachableAtTwoHundredPercentText() {
        val density = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                WhipTheme(dynamicColor = false) {
                    val goalViewModel: GoalViewModel = viewModel()
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        goalState = GoalUiState(loading = false),
                        goalViewModel = goalViewModel,
                        adaptiveLayout = WhipAdaptiveLayout.Compact,
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
        compose.onNodeWithText("New Goal").performClick()
        compose.onNodeWithTag("goal-editor-name").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel Goal editing").performClick()
    }

    @Test
    fun expandedLayoutRetainsNavigationAndPaneInRtl() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        adaptiveLayout = WhipAdaptiveLayout.ExpandedDashboard,
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("adaptive-navigation-rail").assertIsDisplayed()
        compose.onNodeWithTag("expanded-support-pane").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tasks tab").assertIsDisplayed().performClick()
    }

    @Test
    fun rtlBookFoldPlacesProductivityEditorInTheOppositeContentPane() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhipTheme(dynamicColor = false) {
                    val habitViewModel: HabitViewModel = viewModel()
                    val goalViewModel: GoalViewModel = viewModel()
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        habitState = HabitUiState(loading = false),
                        habitViewModel = habitViewModel,
                        goalState = GoalUiState(loading = false),
                        goalViewModel = goalViewModel,
                        adaptiveLayout = WhipAdaptiveLayout.BookFold,
                        foldInfo = WhipFoldInfo(
                            orientation = WhipFoldOrientation.Vertical,
                            leftPx = 700,
                            topPx = 0,
                            rightPx = 740,
                            bottomPx = 1_800,
                            separating = true,
                            halfOpened = true,
                        ),
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
        compose.onNodeWithText("New Goal").performClick()
        val hinge = compose.onNodeWithContentDescription("Device hinge separator").fetchSemanticsNode().boundsInRoot
        val editor = compose.onNodeWithTag("goal-editor-surface").fetchSemanticsNode().boundsInRoot
        check(editor.right <= hinge.left + 1f) { "RTL goal editor crossed the hinge: editor=$editor hinge=$hinge" }
    }

    @Test
    fun bookFoldGoalSupportPaneUsesTheSameTypeSpecificStatusAsCompactRows() {
        val nowMillis = System.currentTimeMillis()
        val goal = Goal(
            id = 41,
            uuid = "elapsed-goal-41",
            metricId = "elapsed-metric-41",
            name = "Quit Alcohol",
            description = "",
            area = "Main",
            tags = emptyList(),
            icon = "❤️",
            type = GoalType.ElapsedSince,
            dimension = UnitDimension.Count,
            unitId = "count",
            precision = 0,
            baseline = null,
            targetMin = null,
            targetMax = null,
            direction = GoalDirection.Increase,
            startDate = LocalDate.of(2026, 8, 25),
            deadline = null,
            aggregation = GoalAggregation.Latest,
            paceType = GoalPaceType.None,
            reminderMinutes = null,
            status = GoalStatus.Active,
            pinned = false,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            elapsedStartMillis = nowMillis - 2L * 86_400_000L,
            elapsedDisplayUnit = ElapsedDisplayUnit.Days,
        )
        val projection = GoalProjection(
            goal = goal,
            currentValue = null,
            progress = null,
            deltaFromBaseline = null,
            expectedProgress = null,
            paceDelta = null,
            forecastDate = null,
            onPace = null,
            milestones = emptyList(),
            entries = emptyList(),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    goalState = GoalUiState(active = listOf(projection), loading = false),
                    adaptiveLayout = WhipAdaptiveLayout.BookFold,
                    foldInfo = WhipFoldInfo(
                        orientation = WhipFoldOrientation.Vertical,
                        leftPx = 700,
                        topPx = 0,
                        rightPx = 740,
                        bottomPx = 1_800,
                        separating = true,
                        halfOpened = true,
                    ),
                    onSaveTask = { _, _, _ -> },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Goals tab").performClick()
        compose.onNodeWithText("2 days").assertIsDisplayed()
        compose.onAllNodesWithText("0% progress").assertCountEquals(0)
    }

    @Test
    fun responsiveFieldPairsStackAtTwoHundredPercentText() {
        val density = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                ResponsiveFieldPair(
                    first = { field -> Box(field.height(56.dp).background(Color.Red).testTag("responsive-first")) },
                    second = { field -> Box(field.height(56.dp).background(Color.Blue).testTag("responsive-second")) },
                )
            }
        }

        val first = compose.onNodeWithTag("responsive-first").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("responsive-second").fetchSemanticsNode().boundsInRoot
        check(second.top > first.bottom) { "Large-text fields must stack instead of clipping: first=$first second=$second" }
    }
}
