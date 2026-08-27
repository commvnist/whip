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
