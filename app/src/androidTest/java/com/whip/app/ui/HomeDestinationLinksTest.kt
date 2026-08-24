package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.theme.WhipTheme
import com.whip.app.core.HomeSection
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeDestinationLinksTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun fiveDestinationsUseCenteredThreeOverTwoLayout() {
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

        assertNear(tasks.center.y, habits.center.y, "Tasks and Habits must share the first row")
        assertNear(habits.center.y, goals.center.y, "Habits and Goals must share the first row")
        assertNear(tracks.center.y, gym.center.y, "Tracks and Gym must share the second row")
        check(tracks.top > tasks.bottom) { "The two destination rows must not overlap" }

        assertNear((tasks.left + goals.right) / 2f, container.center.x, "The three-item row must be centered")
        assertNear((tracks.left + gym.right) / 2f, container.center.x, "The two-item row must be centered")
        listOf(tasks, habits, goals, tracks, gym).zipWithNext().forEach { (first, second) ->
            assertNear(first.width, second.width, "All destination controls must have equal width")
        }
    }

    @Test
    fun hiddenHomeSectionsDoNotRemainAsEmptyDayShortcuts() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
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

        compose.onNodeWithTag("home-destination-tasks").fetchSemanticsNode()
        compose.onNodeWithTag("home-destination-habits").fetchSemanticsNode()
        compose.onNodeWithTag("home-destination-tracks").fetchSemanticsNode()
        compose.onAllNodesWithTag("home-destination-goals").assertCountEquals(0)
        compose.onAllNodesWithTag("home-destination-gym").assertCountEquals(0)
    }

    private fun bounds(label: String): Rect =
        compose.onNodeWithTag("home-destination-$label").fetchSemanticsNode().boundsInRoot

    private fun assertNear(actual: Float, expected: Float, message: String) {
        check(abs(actual - expected) <= 1f) { "$message: actual=$actual expected=$expected" }
    }
}
