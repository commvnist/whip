package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.GoalUiState
import com.whip.app.ui.GoalViewModel
import com.whip.app.ui.HabitUiState
import com.whip.app.ui.HabitViewModel
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.ResponsiveFieldPair
import com.whip.app.ui.WhipAdaptiveLayout
import com.whip.app.ui.WhipFoldInfo
import com.whip.app.ui.WhipFoldOrientation
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.theme.WhipTheme
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
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
        compose.onAllNodesWithText("Home").assertCountEquals(1)
        val navigationTops = listOf("Tasks tab", "Habits tab", "Goals tab", "Gym tab").map { description ->
            compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.top
        }
        check(navigationTops.zipWithNext().all { (before, after) -> before < after }) {
            "Primary navigation must be ordered Tasks, Habits, Goals, Gym: $navigationTops"
        }
        val supportPixels = compose.onNodeWithTag("fold-support-pane").captureToImage().toPixelMap()
        val topRailBackground = supportPixels[10, 10]
        val topOverviewBackground = supportPixels[supportPixels.width * 3 / 4, 10]
        check(topRailBackground.luminance() < 0.25f && topOverviewBackground.luminance() < 0.25f) {
            "Dark fold top inset exposed a light/transparent background: rail=$topRailBackground overview=$topOverviewBackground"
        }
        val summaryWidths = listOf(
            "Tasks due: 0. Open Tasks",
            "Habits remaining: 0. Open Habits",
            "Active goals: 0. Open Goals",
            "Workout: Ready. Open Gym",
        ).map { description ->
            compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.width
        }
        check(summaryWidths.max() - summaryWidths.min() <= 1f) {
            "Fold overview cards must share one width: $summaryWidths"
        }
        compose.onNodeWithContentDescription("Expand content pane").assertIsDisplayed().performClick()
        compose.onNodeWithTag("expanded-content-pane").assertIsDisplayed()
        check(compose.onAllNodesWithTag("fold-support-pane").fetchSemanticsNodes().isEmpty())
        check(compose.onAllNodesWithTag("adaptive-navigation-rail").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithContentDescription("Restore split view").assertIsDisplayed().performClick()
        compose.onNodeWithTag("fold-support-pane").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithContentDescription("Tasks tab").assertIsDisplayed()
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

        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
        compose.onNodeWithText("Task").performClick()
        assertEditorInsideContentPane("task-editor-surface")
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
        compose.onNodeWithText("Habit").performClick()
        assertEditorInsideContentPane("habit-editor-surface")
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithContentDescription("Goals tab").performClick()
        compose.onNodeWithContentDescription("Add goal").performClick()
        assertEditorInsideContentPane("goal-editor-surface")
        compose.onNodeWithText("Cancel").performClick()
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

        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").assertIsDisplayed().performClick()
        compose.onNodeWithText("Task").assertIsDisplayed().performClick()
        compose.onNodeWithTag("task-editor-title").assertIsDisplayed()
        compose.onNodeWithText("Show advanced options").assertIsDisplayed().performClick()
        compose.onNodeWithText("Notes (optional)").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
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
        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
        compose.onNodeWithText("Habit").performClick()
        compose.onNodeWithTag("habit-editor-name").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
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
        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
        compose.onNodeWithText("Goal").performClick()
        compose.onNodeWithTag("goal-editor-name").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
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

        compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
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
