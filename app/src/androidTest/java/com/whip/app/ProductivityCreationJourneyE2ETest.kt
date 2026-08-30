package com.whip.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TaskPriority
import com.whip.app.core.HomeSection
import java.time.DayOfWeek
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
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .revokeRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
            check(
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_DENIED,
            ) {
                "The creation journey must run with notification permission denied"
            }
        }
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            it.copy(
                setupCompleted = true,
                powerMode = false,
                naturalLanguageTaskCapture = false,
                compactItemLayout = false,
                hiddenHomeSections = it.hiddenHomeSections + HomeSection.Tasks,
                collapsedHomeSections = it.collapsedHomeSections + HomeSection.Tasks,
            )
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
            compose.onNodeWithText("Activity").assertIsDisplayed()
            if (compose.onAllNodesWithText("Options").fetchSemanticsNodes().isEmpty()) {
                compose.onNode(
                    hasContentDescription("More Task options") and
                        hasAnyAncestor(hasTestTag("task-actions-surface")),
                ).performClick()
            }
            compose.onNodeWithText("Options").performClick()
            compose.onNodeWithText("Pin to Whip Home").assertIsDisplayed().performClick()
            runBlocking {
                awaitPersistence("Task pin and Home reveal") {
                    val taskPinned = app.taskRepository.tasks.first { tasks -> tasks.any { it.title == "Journey task" && it.pinned } }
                    val settings = app.settingsRepository.current()
                    taskPinned.any { it.title == "Journey task" && it.pinned } &&
                        HomeSection.Tasks !in settings.hiddenHomeSections &&
                        HomeSection.Tasks !in settings.collapsedHomeSections
                }
            }
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
                compose.onNodeWithContentDescription("Edit Habit").assertIsDisplayed()
            }
            selectDetailSection("habit", "Options", "habit-detail-surface")
            compose.onNodeWithTag("entity-inspector-content-options").assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit Habit").assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit Habit").performSemanticsAction(SemanticsActions.OnClick)
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
            compose.waitUntil(15_000) {
                compose.onAllNodesWithContentDescription("Open goal details for Journey target").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Edit goal Journey target")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.waitUntil(2_500) {
                compose.onAllNodesWithTag("goal-editor-surface").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("goal-editor-surface").assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Goal editing")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithContentDescription("Open goal details for Journey target")
                .performSemanticsAction(SemanticsActions.OnClick)
            listOf("Overview", "History").forEach { section ->
                compose.onNodeWithTag("goal-detail-section-$section")
                    .performSemanticsAction(SemanticsActions.OnClick)
                compose.onNodeWithContentDescription("Edit Goal").assertIsDisplayed()
            }
            selectDetailSection("goal", "Automations", "goal-detail-surface")
            compose.onNodeWithTag("entity-inspector-content-automation").assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit Goal").assertIsDisplayed()
            selectDetailSection("goal", "Options", "goal-detail-surface")
            compose.onNodeWithContentDescription("Edit Goal").assertIsDisplayed()
            compose.onNodeWithContentDescription("Edit Goal")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithTag("goal-editor-surface").assertIsDisplayed()
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

    @Test
    fun smartTaskCaptureExplainsHighlightsPersistsAndReversesThroughTheRealUi() {
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.waitForIdle()

            openPlanningSettings()
            compose.onNodeWithTag("settings-list")
                .performScrollToNode(hasTestTag("settings-smart-task-capture"))
            compose.onNodeWithTag("settings-smart-task-capture").performClick()
            compose.waitUntil(5_000) { app.settingsRepository.current().naturalLanguageTaskCapture }
            compose.onNodeWithTag("smart-task-capture-examples").assertIsDisplayed()
            compose.onNodeWithText("Send report tomorrow at 9am #work").assertIsDisplayed()
            compose.onNodeWithText("Review notes every Mon & Thu for 30m").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Submit expenses by next Friday !high").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Settings").performClick()

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            val today = app.clock.today()
            val deadline = today.plusDays(7)
            compose.onNodeWithTag("task-quick-capture").performTextReplacement(
                "Write report tomorrow at 9am deadline $deadline !high for 45m light effort #work remind me",
            )
            compose.onNodeWithTag("task-quick-capture").assert(
                SemanticsMatcher("announces the assumptions applied by Quick Capture") { node ->
                    val description = node.config[SemanticsProperties.StateDescription]
                    description.contains("Tomorrow") && description.contains(deadline.toString()) &&
                        description.contains("Time · 9:00 AM") &&
                        description.contains("Priority · High") &&
                        description.contains("Duration · 45 min") &&
                        description.contains("Effort · Light") &&
                        description.contains("Tag · #work") &&
                        description.contains("Reminder · At scheduled time") &&
                        description.contains("will be applied")
                },
            )
            compose.onNodeWithTag("smart-task-quick-preview").assertIsDisplayed()
            compose.onNodeWithContentDescription("Add task now").performClick()

            val smartTask = runBlocking {
                awaitPersistence("smart-captured task") {
                    app.taskRepository.tasks.first { tasks -> tasks.any { task -> task.title == "Write report" } }
                        .single { task -> task.title == "Write report" }
                }
            }
            check(smartTask.scheduleKind == ScheduleKind.Once)
            check(smartTask.date == today.plusDays(1))
            check(smartTask.deadline == deadline)
            check(smartTask.timeMinutes == 9 * 60)
            check(smartTask.reminderEnabled)
            check(smartTask.reminderOffsetsMinutes == listOf(0))
            check(smartTask.priority == TaskPriority.High)
            check(smartTask.durationMinutes == 45)
            check(smartTask.effort == TaskEffort.Light)
            check(smartTask.tags == setOf("work"))
            check(!smartTask.inbox)

            compose.onNodeWithTag("task-quick-capture").performTextReplacement(
                "TRT every Monday and Thursday",
            )
            compose.onNodeWithTag("smart-task-quick-preview").assertIsDisplayed()
            compose.onNodeWithText("Repeat · Monday, Thursday").assertIsDisplayed()
            compose.onNodeWithContentDescription("Add task now").performClick()
            val weekdayTask = runBlocking {
                awaitPersistence("weekday Smart Capture task") {
                    app.taskRepository.tasks.first { tasks -> tasks.any { task -> task.title == "TRT" } }
                        .single { task -> task.title == "TRT" }
                }
            }
            check(weekdayTask.scheduleKind == ScheduleKind.Recurring)
            check(
                weekdayTask.recurrence?.weekdays ==
                    setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            )

            openPlanningSettings()
            compose.onNodeWithTag("settings-list")
                .performScrollToNode(hasTestTag("settings-smart-task-capture"))
            compose.onNodeWithTag("settings-smart-task-capture").performClick()
            compose.waitUntil(5_000) { !app.settingsRepository.current().naturalLanguageTaskCapture }
            compose.onAllNodesWithTag("smart-task-capture-examples").assertCountEquals(0)
            compose.onNodeWithContentDescription("Close Settings").performClick()

            compose.onNodeWithTag("task-quick-capture").performTextReplacement("Literal task tomorrow")
            compose.onAllNodesWithTag("smart-task-quick-preview").assertCountEquals(0)
            compose.onNodeWithContentDescription("Add task now").performClick()
            val literalTask = runBlocking {
                awaitPersistence("literal task after Smart Capture was disabled") {
                    app.taskRepository.tasks.first { tasks -> tasks.any { task -> task.title == "Literal task tomorrow" } }
                        .single { task -> task.title == "Literal task tomorrow" }
                }
            }
            check(literalTask.scheduleKind == ScheduleKind.Once)
            check(literalTask.date == today)

            openPlanningSettings()
            compose.onNodeWithTag("settings-list")
                .performScrollToNode(hasTestTag("settings-smart-task-capture"))
            compose.onNodeWithTag("settings-smart-task-capture").performClick()
            compose.waitUntil(5_000) { app.settingsRepository.current().naturalLanguageTaskCapture }
            compose.onNodeWithContentDescription("Close Settings").performClick()

            val seriesStart = today.plusDays(2)
            compose.onNodeWithTag("task-quick-capture").performTextReplacement(
                "Review metrics every 2 weeks on $seriesStart",
            )
            compose.onNodeWithText("Add Details").performClick()
            compose.onNodeWithTag("smart-task-editor-preview").assertIsDisplayed()
            compose.onNodeWithTag("smart-task-capture-apply").performClick()
            compose.onNodeWithText("Save").performClick()
            val repeatingTask = runBlocking {
                awaitPersistence("reviewed Smart Capture task") {
                    app.taskRepository.tasks.first { tasks -> tasks.any { task -> task.title == "Review metrics" } }
                        .single { task -> task.title == "Review metrics" }
                }
            }
            check(repeatingTask.scheduleKind == ScheduleKind.Recurring)
            val recurrence = requireNotNull(repeatingTask.recurrence)
            check(recurrence.unit == RecurrenceUnit.Weeks)
            check(recurrence.interval == 2)
            check(recurrence.startDate == seriesStart)
        }
    }

    private fun selectDestination(testTag: String, label: String) {
        if (compose.onAllNodesWithTag(testTag).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag(testTag).performSemanticsAction(SemanticsActions.OnClick)
        } else {
            compose.onNodeWithContentDescription("More Goal destinations").performClick()
            compose.onNodeWithText(label).performClick()
        }
        compose.waitForIdle()
    }

    private fun openPlanningSettings() {
        if (compose.onAllNodesWithContentDescription("Open Settings").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Open Settings").performClick()
        } else {
            compose.onNodeWithContentDescription("App actions").performClick()
            compose.onNodeWithText("Open Settings").performClick()
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("settings-list").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("settings-category-list").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("settings-support-list").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("settings-wide-section-list").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("settings-section-Planning & Units").fetchSemanticsNodes().isNotEmpty()
        }
        // Root destination state is intentionally retained. Returning to
        // Settings may reopen the previously selected Planning detail directly.
        if (compose.onAllNodesWithTag("settings-list").fetchSemanticsNodes().isNotEmpty()) return
        when {
            compose.onAllNodesWithTag("settings-category-list").fetchSemanticsNodes().isNotEmpty() -> {
                compose.onNodeWithTag("settings-category-list")
                    .performScrollToNode(hasTestTag("settings-section-Planning & Units"))
                compose.onNodeWithTag("settings-section-Planning & Units").performClick()
            }
            compose.onAllNodesWithTag("settings-support-list").fetchSemanticsNodes().isNotEmpty() -> {
                compose.onNodeWithTag("settings-support-list")
                    .performScrollToNode(hasTestTag("settings-support-section-Planning & Units"))
                compose.onNodeWithTag("settings-support-section-Planning & Units").performClick()
            }
            compose.onAllNodesWithTag("settings-wide-section-list").fetchSemanticsNodes().isNotEmpty() -> {
                val planningSection = hasText("Planning & Units") and
                    hasAnyAncestor(hasTestTag("settings-wide-section-list"))
                compose.onNodeWithTag("settings-wide-section-list")
                    .performScrollToNode(planningSection)
                compose.onNode(planningSection).performClick()
            }
            else -> compose.onNodeWithTag("settings-section-Planning & Units").performClick()
        }
        compose.waitForIdle()
    }

    private fun selectDetailSection(prefix: String, label: String, surfaceTag: String) {
        val sectionTag = "$prefix-detail-section-$label"
        val sectionVisible = compose.onAllNodesWithTag(sectionTag).fetchSemanticsNodes()
            .any { node -> runCatching { node.layoutInfo.isPlaced }.getOrDefault(false) }
        if (sectionVisible) {
            compose.onNodeWithTag(sectionTag).performSemanticsAction(SemanticsActions.OnClick)
        } else {
            val overflowDescription = when (prefix) {
                "habit" -> "More Habit options"
                "goal" -> "More Goal options"
                else -> error("Missing inspector overflow description for $prefix")
            }
            compose.onNode(
                hasContentDescription(overflowDescription) and hasAnyAncestor(hasTestTag(surfaceTag)),
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
