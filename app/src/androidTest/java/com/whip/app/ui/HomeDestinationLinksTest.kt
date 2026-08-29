package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
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
                        habitCompleted = habitCompleted.intValue,
                        habitTotal = habitTotal.intValue,
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
