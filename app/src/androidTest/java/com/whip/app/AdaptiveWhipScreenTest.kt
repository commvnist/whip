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
import com.whip.app.ui.GoalViewModel
import com.whip.app.ui.GymUiState
import com.whip.app.ui.GymViewModel
import com.whip.app.ui.HabitUiState
import com.whip.app.ui.HabitViewModel
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.TrackUiState
import com.whip.app.ui.TrackViewModel
import com.whip.app.ui.ResponsiveFieldPair
import com.whip.app.ui.WhipAdaptiveLayout
import com.whip.app.ui.WhipFoldInfo
import com.whip.app.ui.WhipFoldOrientation
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.theme.WhipTheme
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

        compose.onNodeWithText("Review & Trends").assertIsDisplayed().performClick()
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
        if (compose.onAllNodesWithTag("workspace-search-action").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("workspace-search-action").assertIsDisplayed().performClick()
        } else {
            compose.onNodeWithContentDescription("App actions").assertIsDisplayed().performClick()
            compose.onNodeWithTag("workspace-search-menu-action").assertIsDisplayed().performClick()
        }
        compose.onNodeWithTag("unified-search-query").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
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
        compose.onNodeWithContentDescription("Tasks Today: 0. Open Tasks").assertIsDisplayed()
        compose.onNodeWithContentDescription("App actions").assertIsDisplayed().performClick()
        compose.onNodeWithTag("expand-content-pane-action").assertIsDisplayed()
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
        val gymBrand = compose.onNodeWithText("Whip").fetchSemanticsNode().boundsInRoot
        val gymHeader = compose.onNodeWithTag("workspace-top-app-bar").fetchSemanticsNode().boundsInRoot
        check(gymBrand.width > 0f && gymBrand.height > 0f && gymBrand.top < gymHeader.bottom && gymBrand.bottom > gymHeader.top) {
            "Whip/Gym identity must remain inside the shared header: brand=$gymBrand header=$gymHeader"
        }
        val visibleGymLabels = compose.onAllNodesWithText("Gym").fetchSemanticsNodes().count { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
        check(visibleGymLabels > 0) { "The Gym identity or destination label must remain visible" }
        compose.onAllNodesWithTag("workspace-area-action").assertCountEquals(0)
        compose.onNodeWithContentDescription("App actions").performClick()
        compose.onNodeWithText("Open Settings").performClick()
        compose.onNodeWithContentDescription("Close Settings").assertIsDisplayed()
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
        compose.onNodeWithText("Task").performClick()
        assertEditorInsideContentPane("task-editor-surface")
        compose.onNodeWithContentDescription("Cancel Task editing").performClick()

        compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
        compose.onNodeWithText("Habit").performClick()
        assertEditorInsideContentPane("habit-editor-surface")
        compose.onNodeWithContentDescription("Cancel Habit editing").performClick()

        compose.onNodeWithContentDescription("Goals tab").performClick()
        compose.onNodeWithContentDescription("Add goal").performClick()
        assertEditorInsideContentPane("goal-editor-surface")
        compose.onNodeWithContentDescription("Cancel Goal editing").performClick()

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithContentDescription("Add Track").performClick()
        compose.onNodeWithTag("track-editor-surface").fetchSemanticsNode()
        check(
            compose.onNodeWithTag("app-background-shell").fetchSemanticsNode().config
                .contains(SemanticsProperties.HideFromAccessibility),
        ) { "The root-owned Track editor must hide the underlying shell from accessibility" }
        compose.onNodeWithContentDescription("Close Track Editor").performClick()

        compose.onNodeWithContentDescription("Gym tab").performClick()
        compose.onNodeWithContentDescription("Add exercise or workout").performClick()
        compose.onNodeWithText("Exercise").performClick()
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

        data class Geometry(val headerTop: Float, val headerHeight: Float, val navigationTop: Float, val navigationHeight: Float, val searchLeft: Float, val settingsLeft: Float)
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
            val header = compose.onNodeWithTag("workspace-top-app-bar").fetchSemanticsNode().boundsInRoot
            val navigation = compose.onNodeWithTag(navigationTag).fetchSemanticsNode().boundsInRoot
            val search = compose.onNodeWithTag("workspace-search-action").fetchSemanticsNode().boundsInRoot
            val settings = compose.onNodeWithTag("workspace-settings-action").fetchSemanticsNode().boundsInRoot
            Geometry(header.top, header.height, navigation.top, navigation.height, search.left, settings.left)
        }
        val expected = snapshots.first()
        snapshots.drop(1).forEach { actual ->
            check(kotlin.math.abs(actual.headerTop - expected.headerTop) <= 1f)
            check(kotlin.math.abs(actual.headerHeight - expected.headerHeight) <= 1f)
            check(kotlin.math.abs(actual.navigationTop - expected.navigationTop) <= 1f)
            check(kotlin.math.abs(actual.navigationHeight - expected.navigationHeight) <= 1f)
            check(kotlin.math.abs(actual.searchLeft - expected.searchLeft) <= 1f)
            check(kotlin.math.abs(actual.settingsLeft - expected.settingsLeft) <= 1f)
        }

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-workspace-destination-Activity").performClick().assertIsSelected()
        compose.onNodeWithText("A chronological view of Entries across visible Tracks", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-destination-Insights").performClick().assertIsSelected()
        compose.onNodeWithText("Patterns and automation health across visible Tracks.").assertIsDisplayed()
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
        compose.onNodeWithContentDescription("App actions").performClick()
        compose.onNodeWithTag("expand-content-pane-action").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithContentDescription("Back to Tracks").assertIsDisplayed()
        compose.onNodeWithContentDescription("Go to Home").assertIsDisplayed().performClick()
        compose.onNodeWithText("Home").assertIsDisplayed()
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
        compose.onNodeWithText("Task").assertIsDisplayed().performClick()
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
        compose.onNodeWithText("Habit").performClick()
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
        compose.onNodeWithText("Goal").performClick()
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
        compose.onNodeWithText("Goal").performClick()
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
