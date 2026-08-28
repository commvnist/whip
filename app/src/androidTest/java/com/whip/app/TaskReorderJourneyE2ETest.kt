package com.whip.app

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.TaskDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-app proof for the complete reorder consequence chain. Repository tests
 * still cover normalization in depth; this class proves that the user-visible
 * grip actually drives that durable contract and that cancellation recovers.
 */
@RunWith(AndroidJUnit4::class)
class TaskReorderJourneyE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun seed() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            it.copy(setupCompleted = true, compactItemLayout = false)
        }
        app.taskRepository.create(TaskDraft("First reorder task"))
        app.taskRepository.create(TaskDraft("Second reorder task"))
        app.taskRepository.create(TaskDraft("Third reorder task"))
        Unit
    }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun dragPreviewsPersistsCancelsAndSurvivesRecreation() {
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use { scenario ->
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("Tasks tab").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("task-destination-Inbox").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("task-destination-Inbox").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("More task list actions").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("More task list actions").performClick()
            compose.onNodeWithText("Reorder Tasks").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("reorder-mode-tasks").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("reorder-mode-tasks").assertIsDisplayed()
            compose.onAllNodesWithTag("task-quick-capture").assertCountEquals(0)
            compose.onAllNodesWithContentDescription("More task list actions").assertCountEquals(0)
            compose.onAllNodesWithContentDescription("Complete task First reorder task").assertCountEquals(0)
            compose.onAllNodesWithTag("workspace-add-action").assertCountEquals(0)
            compose.onAllNodesWithTag("workspace-search-action").assertCountEquals(0)
            compose.onAllNodesWithTag("workspace-settings-action").assertCountEquals(0)
            compose.onAllNodesWithTag("workspace-area-action").assertCountEquals(0)
            compose.onNodeWithContentDescription("Tasks tab").assertIsNotEnabled()

            val firstHandle = compose.onNodeWithContentDescription("Reorder First reorder task")
            val firstBeforeBounds = firstHandle.fetchSemanticsNode().boundsInRoot
            val secondBeforeBounds = compose.onNodeWithContentDescription("Reorder Second reorder task")
                .fetchSemanticsNode().boundsInRoot
            val dragStep = secondBeforeBounds.center.y - firstBeforeBounds.center.y + with(compose.density) { 8f * density }
            firstHandle.performTouchInput {
                down(center)
                moveBy(Offset(0f, dragStep), 300L)
                up()
            }

            awaitOrder(listOf("Second reorder task", "First reorder task", "Third reorder task"))
            compose.waitUntil(5_000) {
                runCatching {
                    compose.onNodeWithContentDescription("Reorder First reorder task")
                        .fetchSemanticsNode().boundsInRoot.top >
                        compose.onNodeWithContentDescription("Reorder Second reorder task")
                            .fetchSemanticsNode().boundsInRoot.top
                }.getOrDefault(false)
            }

            // A cancelled gesture may preview a new slot, but must restore the
            // pre-gesture order and never leave a partial persistent result.
            compose.onNodeWithContentDescription("Reorder First reorder task").performTouchInput {
                down(center)
                moveBy(Offset(0f, dragStep), 300L)
                cancel()
            }
            awaitOrder(listOf("Second reorder task", "First reorder task", "Third reorder task"))

            compose.onNodeWithText("Done").performClick()
            scenario.recreate()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("Open task details for First reorder task")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val secondTop = compose.onNodeWithContentDescription("Open task details for Second reorder task")
                .fetchSemanticsNode().boundsInRoot.top
            val firstTop = compose.onNodeWithContentDescription("Open task details for First reorder task")
                .fetchSemanticsNode().boundsInRoot.top
            assertTrue("Persisted custom order was not still presented after recreation", secondTop < firstTop)
        }
    }

    private fun awaitOrder(expected: List<String>) = runBlocking {
        withTimeout(5_000) {
            val tasks = app.taskRepository.tasks.first { current ->
                current.size == expected.size &&
                    current.sortedBy { it.manualPosition }.map { it.title } == expected
            }
            assertEquals(expected, tasks.sortedBy { it.manualPosition }.map { it.title })
        }
    }
}
