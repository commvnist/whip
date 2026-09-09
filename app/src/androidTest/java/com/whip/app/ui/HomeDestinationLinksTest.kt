package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.theme.WhipTheme
import com.whip.app.core.HomeSection
import com.whip.app.captureVisualCatalogSurface
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeDestinationLinksTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun fiveDestinationsUseConsistentOrderedIntroductionCards() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    HomeDestinationLinks({}, {}, {}, {}, {})
                }
            }
        }

        val container = compose.onNodeWithTag("home-destination-links").fetchSemanticsNode().boundsInRoot
        val tasks = bounds("tasks")
        val habits = bounds("habits")
        val goals = bounds("goals")
        val tracks = bounds("tracks")
        val gym = bounds("gym")

        check(listOf(tasks, habits, goals, tracks, gym).zipWithNext().all { (first, second) -> first.bottom < second.top }) {
            "Component cards must be presented in a clear vertical reading order"
        }
        assertNear(tasks.left, container.left, "Cards must align to the container's leading edge")
        assertNear(tasks.right, container.right, "Cards must align to the container's trailing edge")
        listOf(tasks, habits, goals, tracks, gym).zipWithNext().forEach { (first, second) ->
            assertNear(first.width, second.width, "All destination controls must have equal width")
        }
    }

    @Test
    fun todaySummaryKeepsNeutralOutcomesAndActionsReadableAtLargeTextAndRtl() {
        val expandedText = mutableStateOf(false)
        val opens = AtomicInteger()
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density, if (expandedText.value) 2f else 1f),
                LocalLayoutDirection provides if (expandedText.value) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.fillMaxSize()) {
                        Box {
                            Column(Modifier.width(if (expandedText.value) 320.dp else 360.dp).verticalScroll(rememberScrollState())) {
                                TodayHeader(
                                    date = LocalDate.of(2026, 9, 8),
                                    taskTotal = 125,
                                    habitSummary = HomeHabitSummary(completed = 3, total = 4, skipped = 2, timersToReview = 1),
                                    onOpenTasks = { opens.incrementAndGet() },
                                    onOpenHabits = { opens.incrementAndGet() },
                                    onOpenReview = { opens.incrementAndGet() },
                                )
                            }
                        }
                    }
                }
            }
        }
        val compactTasks = compose.onNodeWithTag("home-tasks-today-record").fetchSemanticsNode().boundsInRoot
        val compactHabits = compose.onNodeWithTag("home-habit-progress-record").fetchSemanticsNode().boundsInRoot
        check(compactTasks.right < compactHabits.left)
        assertNear(compactTasks.top, compactHabits.top, "Compact summaries share one row")
        assertNear(compactTasks.height, compactHabits.height, "Compact summaries share one height")

        compose.runOnIdle { expandedText.value = true }
        compose.onNodeWithText("Review & Trends").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-tasks-today-record").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-habit-progress-record").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(
            "Habit progress: 3 of 4 complete. 2 skipped. 1 timer to review. Open Habits Today",
        ).assertIsDisplayed()
        val tile = compose.onNodeWithTag("home-habit-progress-record").fetchSemanticsNode().boundsInRoot
        listOf("Habit progress", "3 of 4 complete", "2 skipped · 1 timer to review").forEach { label ->
            val text = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            check(text.left >= tile.left && text.right <= tile.right && text.bottom <= tile.bottom) {
                "Large-text summary must contain all of '$label'"
            }
            if (label.first().isDigit()) {
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(label, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                check(layouts.single().getParagraphDirection(0) == ResolvedTextDirection.Ltr) {
                    "Numeric English phrases must retain their reading order in an RTL layout"
                }
            }
        }
        check(opens.get() == 3)
        captureVisualCatalogSurface("shared.home.summary-large-rtl")
    }

    @Test
    fun conditionalTodayRecordsAndVisibleDestinationsHideEmptyShortcuts() {
        val taskTotal = mutableIntStateOf(2)
        val habitCompleted = mutableIntStateOf(0)
        val habitTotal = mutableIntStateOf(0)
        val taskOpens = AtomicInteger()
        val habitOpens = AtomicInteger()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    TodayHeader(
                        date = LocalDate.of(2026, 8, 29),
                        taskTotal = taskTotal.intValue,
                        habitSummary = HomeHabitSummary(completed = habitCompleted.intValue, total = habitTotal.intValue),
                        onOpenTasks = { taskOpens.incrementAndGet() },
                        onOpenHabits = { habitOpens.incrementAndGet() },
                    )
                    HomeDestinationLinks(
                        onOpenTasks = {},
                        onOpenHabits = {},
                        onOpenGoals = {},
                        onOpenTracks = {},
                        onOpenGym = {},
                        sections = listOf(HomeSection.Tasks, HomeSection.Habits, HomeSection.Tracks),
                    )
                }
            }
        }

        compose.onNodeWithTag("home-tasks-today-record").assertIsDisplayed().performClick()
        check(taskOpens.get() == 1)
        compose.onAllNodesWithTag("home-habit-progress-record").assertCountEquals(0)
        compose.onNodeWithTag("home-destination-tasks").fetchSemanticsNode()
        compose.onNodeWithTag("home-destination-habits").fetchSemanticsNode()
        compose.onNodeWithTag("home-destination-tracks").fetchSemanticsNode()
        compose.onAllNodesWithTag("home-destination-goals").assertCountEquals(0)
        compose.onAllNodesWithTag("home-destination-gym").assertCountEquals(0)

        compose.runOnIdle {
            taskTotal.intValue = 0
            habitCompleted.intValue = 1
            habitTotal.intValue = 2
        }
        compose.onAllNodesWithTag("home-tasks-today-record").assertCountEquals(0)
        compose.onNodeWithTag("home-habit-progress-record").assertIsDisplayed().performClick()
        check(habitOpens.get() == 1)

        compose.runOnIdle { habitTotal.intValue = 0 }
        compose.onAllNodesWithTag("home-tasks-today-record").assertCountEquals(0)
        compose.onAllNodesWithTag("home-habit-progress-record").assertCountEquals(0)
    }

    private fun bounds(label: String): Rect =
        compose.onNodeWithTag("home-destination-$label").fetchSemanticsNode().boundsInRoot

    private fun assertNear(actual: Float, expected: Float, message: String) {
        check(abs(actual - expected) <= 1f) { "$message: actual=$actual expected=$expected" }
    }
}
