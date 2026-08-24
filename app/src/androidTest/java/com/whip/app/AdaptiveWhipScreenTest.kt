package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
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
import com.whip.app.domain.Track
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveWhipScreenTest {
    @get:Rule
    val compose = createComposeRule()

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
        compose.onNodeWithContentDescription("Search All Whip Data").assertIsDisplayed().performClick()
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
        compose.onNodeWithText("No tasks or habits need attention today.").assertIsDisplayed()
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
        compose.onNodeWithContentDescription("Tasks Today: 0. Open Tasks").assertIsDisplayed()
        // A scrollable destination bar must stay content-height in a narrow fold
        // pane so the page header and task content remain in the viewport.
        compose.onNodeWithTag("page-title").assertIsDisplayed()
        compose.onNodeWithTag("task-quick-capture").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Habits Remaining: 0. Open Habits").assertCountEquals(0)
        compose.onNodeWithContentDescription("Gym tab").performClick()
        compose.onAllNodesWithText("Whip").assertCountEquals(0)
        compose.onNodeWithContentDescription("App actions").performClick()
        compose.onNodeWithText("Open Settings").performClick()
        compose.onNodeWithContentDescription("Close Settings").assertIsDisplayed()
        compose.onNodeWithTag("fold-support-pane").assertIsDisplayed()
        compose.onNodeWithTag("adaptive-navigation-rail").assertIsDisplayed()
        compose.onAllNodesWithTag("adaptive-bottom-navigation").assertCountEquals(0)
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
        compose.onAllNodesWithText("Pane-safe task")[1].performClick()
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
        compose.onNodeWithContentDescription("Films, 0 Entries. Open Track").performClick()
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
