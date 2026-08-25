package com.whip.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductivityCreationJourneyE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            check(
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_DENIED,
            ) {
                "The creation journey must run with notification permission denied"
            }
        }
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            it.copy(setupCompleted = true, powerMode = false)
        }
    }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun taskHabitAndGoalCanBeCreatedAndUsedThroughTheRealUi() {
        val intent = Intent(app, MainActivity::class.java).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use { scenario ->
            compose.waitForIdle()

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-quick-capture").performTextReplacement("Journey task")
            compose.onNodeWithContentDescription("Add task now").performClick()
            runBlocking {
                awaitPersistence("quick-captured task") {
                    app.taskRepository.tasks.first { tasks -> tasks.any { it.title == "Journey task" } }
                }
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Edit").fetchSemanticsNodes().any { node ->
                    runCatching { node.layoutInfo.isPlaced }.getOrDefault(false)
                }
            }
            compose.onNodeWithText("Edit").assertIsDisplayed()
            compose.waitUntil(5_000) {
                runCatching {
                    compose.onNodeWithContentDescription("Open task details for Journey task").fetchSemanticsNode()
                    true
                }.getOrDefault(false)
            }
            compose.onNodeWithContentDescription("Open task details for Journey task").performClick()
            compose.onNodeWithText("Schedule").assertIsDisplayed()
            if (compose.onAllNodesWithText("Options").fetchSemanticsNodes().isEmpty()) {
                compose.onNode(
                    hasContentDescription("Open Pages") and
                        hasAnyAncestor(hasTestTag("task-actions-surface")),
                ).performClick()
            }
            compose.onNodeWithText("Options").performClick()
            compose.onNodeWithText("Pin to Home").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithContentDescription("Add habit").performClick()
            compose.onNodeWithTag("habit-editor-name").performTextReplacement("Journey water")
            compose.onNodeWithText("Count").performClick()
            compose.onNodeWithText("Save").performClick()
            val habitId = runBlocking {
                awaitPersistence("created habit") {
                    app.habitRepository.habits.first { habits -> habits.any { it.name == "Journey water" } }
                        .first { it.name == "Journey water" }.id
                }
            }
            compose.waitUntil(5_000) {
                runCatching {
                    compose.onNodeWithTag("habit-card-$habitId").fetchSemanticsNode()
                    true
                }.getOrDefault(false)
            }
            compose.onNodeWithText("+1").performClick()
            runBlocking {
                awaitPersistence("habit check-off") {
                    app.habitRepository.logs.first { logs -> logs.any { it.habitId == habitId } }
                }
            }
            compose.onNodeWithTag("habit-list-Today").performScrollToNode(
                hasContentDescription("Edit habit Journey water"),
            )
            compose.onNodeWithContentDescription("Edit habit Journey water")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Edit Habit").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Habit editing")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithContentDescription("Open habit details for Journey water")
                .performSemanticsAction(SemanticsActions.OnClick)
            listOf("Today", "History").forEach { section ->
                compose.onNodeWithTag("habit-detail-section-$section")
                    .performSemanticsAction(SemanticsActions.OnClick)
                compose.onNodeWithText("Edit Habit").assertIsDisplayed()
            }
            selectDetailSection("habit", "Automation", "habit-detail-surface")
            compose.onNodeWithTag("habit-detail-section-Automation").assertIsDisplayed()
            compose.onNodeWithText("Edit Habit").assertIsDisplayed()
            selectDetailSection("habit", "Options", "habit-detail-surface")
            compose.onNodeWithText("Edit Habit").assertIsDisplayed()
            compose.onNodeWithText("Edit Habit").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Edit Habit").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Habit editing")
                .performSemanticsAction(SemanticsActions.OnClick)

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithContentDescription("Add goal").performClick()
            compose.onNodeWithTag("goal-editor-name").performTextReplacement("Journey target")
            // Do not inject Android Back to dismiss an IME here. On Samsung's
            // expanded Fold layout the semantics edit may not open the IME, so
            // Back correctly leaves the editor instead of dismissing a keyboard.
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasTestTag("goal-editor-target"))
            compose.waitForIdle()
            compose.onNodeWithTag("goal-editor-target").assertIsDisplayed().performTextReplacement("10")
            compose.onNodeWithText("Save").performClick()
            val goalId = runBlocking {
                awaitPersistence("created goal") {
                    app.goalRepository.goals.first { goals -> goals.any { it.name == "Journey target" } }
                        .first { it.name == "Journey target" }.id
                }
            }
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Goal Created").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("goal-created-done").performClick()
            compose.onNodeWithContentDescription("Edit goal Journey target")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Edit Goal").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Goal editing")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithContentDescription("Open goal details for Journey target")
                .performSemanticsAction(SemanticsActions.OnClick)
            listOf("Overview", "History").forEach { section ->
                compose.onNodeWithTag("goal-detail-section-$section")
                    .performSemanticsAction(SemanticsActions.OnClick)
                compose.onNodeWithText("Edit Goal").assertIsDisplayed()
            }
            selectDetailSection("goal", "Automation", "goal-detail-surface")
            compose.onNodeWithTag("goal-detail-section-Automation").assertIsDisplayed()
            compose.onNodeWithText("Edit Goal").assertIsDisplayed()
            selectDetailSection("goal", "Options", "goal-detail-surface")
            compose.onNodeWithText("Edit Goal").assertIsDisplayed()
            compose.onNodeWithText("Edit Goal").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Edit Goal").assertIsDisplayed()
            compose.onNodeWithTag("emoji-picker-trigger").performClick()
            compose.onNodeWithTag("emoji-preset-Fitness").performClick()
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(2_500) {
                compose.onAllNodesWithTag("goal-editor-surface").fetchSemanticsNodes().isEmpty()
            }
            check(runBlocking { app.goalRepository.goals.first().single { it.id == goalId }.icon } == "💪")
            selectDestination("goal-destination-Archived", "Archived")
            compose.onNodeWithText("Archived Goals").assertIsDisplayed()
            selectDestination("goal-destination-Insights", "Insights")
            compose.onNodeWithText("Goal Insights").assertIsDisplayed()
            compose.onNodeWithTag("goal-insight-$goalId").assertIsDisplayed()
        }
    }

    private fun selectDestination(testTag: String, label: String) {
        if (compose.onAllNodesWithTag(testTag).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag(testTag).performSemanticsAction(SemanticsActions.OnClick)
        } else {
            compose.onNodeWithContentDescription("Open Pages").performClick()
            compose.onNodeWithText(label).performClick()
        }
        compose.waitForIdle()
    }

    private fun selectDetailSection(prefix: String, label: String, surfaceTag: String) {
        val sectionTag = "$prefix-detail-section-$label"
        if (compose.onAllNodesWithTag(sectionTag).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag(sectionTag).performSemanticsAction(SemanticsActions.OnClick)
        } else {
            compose.onNode(
                hasContentDescription("Open Pages") and hasAnyAncestor(hasTestTag(surfaceTag)),
            ).performClick()
            compose.onNodeWithText(label).performClick()
        }
        compose.waitForIdle()
    }

    private suspend fun <T> awaitPersistence(label: String, block: suspend () -> T): T = try {
        withTimeout(15_000) { block() }
    } catch (timeout: TimeoutCancellationException) {
        throw AssertionError("$label did not reach the repository within 15 seconds", timeout)
    }
}
