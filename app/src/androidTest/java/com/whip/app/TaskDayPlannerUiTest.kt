package com.whip.app

import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.*
import com.whip.app.ui.*
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class TaskDayPlannerUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val today = LocalDate.of(2026, 9, 28)

    @Test fun allAreaCapacityAndFailedApplyKeepTheRequestOwnedPreview() {
        val candidate = task(1, "Scoped Inbox candidate", minutes = 30)
        val other = task(2, "Other Area workload", minutes = 100, scheduled = true)
        val mutation = mutableStateOf<PersistenceRequestState<TaskMutationReceipt>>(PersistenceRequestState.Idle)
        var request: String? = null
        var calls = 0
        val state = TaskUiState(inbox = listOf(candidate), currentDate = today, loading = false)
        compose.setContent { WhipTheme(dynamicColor = false) {
            WhipScreen(state = state, unscopedTaskState = state.copy(today = listOf(other)),
                onSaveTask = { _, _, _ -> }, onComplete = {}, onSkip = {}, onReschedule = { _, _ -> }, onArchive = {}, onReopen = {},
                taskAuthoredMutationState = mutation.value,
                onTaskAuthoredMutationResultConsumed = { mutation.value = PersistenceRequestState.Idle },
                onPlanMyDayRequest = { selected, capacity, id ->
                    assertEquals(listOf(candidate), selected)
                    assertEquals(240, capacity)
                    calls++; request = id; mutation.value = PersistenceRequestState.Running(id); true
                })
        } }
        openPlanner()
        compose.onNodeWithText("Already planned across all Areas: 100 minutes").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Preview Plan").performScrollTo().performClick()
        compose.onNodeWithText("Proposed: 1 of 1 new tasks · 130 of 240 minutes total").performScrollTo().assertExists()
        compose.onNodeWithTag("task-day-plan-apply").performScrollTo().performClick()
        compose.onAllNodesWithTag("task-day-plan-apply").assertCountEquals(0) // Entire form is shielded during the request.
        compose.runOnIdle { assertEquals(1, calls); mutation.value = PersistenceRequestState.Finished(requireNotNull(request), WhipResult.Failure("Today changed; review capacity")) }
        compose.onNodeWithTag("task-day-plan-error").performScrollTo().assertIsDisplayed()
            .assertContentDescriptionContains("Today changed; review capacity", substring = true)
        compose.onNodeWithText("Proposed: 1 of 1 new tasks · 130 of 240 minutes total").performScrollTo().assertExists()
        captureVisualCatalogSurface("ux-audit2.task-day-plan.failure")
        compose.onNodeWithTag("task-day-plan-apply").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(2, calls)
            mutation.value = PersistenceRequestState.Finished(requireNotNull(request), WhipResult.Success(TaskMutationReceipt(
                TaskMutationKind.BulkRescheduled, setOf(candidate.task.id), effectiveDate = today)))
        }
        compose.onAllNodesWithTag("task-day-plan-apply").assertCountEquals(0)
    }

    @Test @AndroidFontScale fun manyCandidatesAndExhaustedCapacityRemainReachableWithKeyboard() {
        val candidates = (1L..16L).map { task(it, "Candidate $it with a deliberately useful long title", minutes = if (it % 2L == 0L) null else 15) }
        val existing = task(100, "Already planned", 120, scheduled = true)
        val state = TaskUiState(inbox = candidates, today = listOf(existing), currentDate = today, loading = false)
        compose.setContent { WhipTheme(dynamicColor = false) {
            WhipScreen(state = state, unscopedTaskState = state, onSaveTask = { _, _, _ -> }, onComplete = {}, onSkip = {},
                onReschedule = { _, _ -> }, onArchive = {}, onReopen = {})
        } }
        openPlanner()
        compose.onNodeWithTag("task-day-plan-capacity").performScrollTo().performClick().performTextReplacement("120")
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        compose.waitUntil(10_000) { automation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } }
        compose.onNodeWithTag("task-day-plan-capacity").assertIsDisplayed()
        assertEquals(2f, InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.fontScale, 0.01f)
        captureVisualCatalogSurface("ux-audit2.task-day-plan.keyboard.large")
        closeSoftKeyboard()
        compose.onNodeWithText("Preview Plan").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Today's existing plan fills this capacity.", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("task-day-plan-capacity").performScrollTo().performTextReplacement("240")
        closeSoftKeyboard()
        compose.onNodeWithText("Preview Plan").performScrollTo().performClick()
        compose.onNodeWithTag("task-day-plan-apply").performScrollTo().assertIsDisplayed().assertIsEnabled()
        captureVisualCatalogSurface("ux-audit2.task-day-plan.short-large")
    }

    @Test @AndroidFontScale fun shortLargeTextSelectionKeepsResultsAndCompletionReachable() {
        val rows = (1L..12L).map { task(it, "A long selected Task title $it", 15, scheduled = true) }
        compose.setContent { WhipTheme(dynamicColor = false) {
            WhipScreen(state = TaskUiState(today = rows, currentDate = today, loading = false),
                onSaveTask = { _, _, _ -> }, onComplete = {}, onSkip = {}, onReschedule = { _, _ -> }, onArchive = {}, onReopen = {})
        } }
        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithTag("workspace-top-app-bar").assertIsDisplayed()
        if (compose.onAllNodesWithContentDescription("More task list actions").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("More task list actions").performClick()
            compose.onNodeWithText("Select Tasks").performClick()
        } else compose.onNodeWithText("Select").performClick()
        compose.onNodeWithText("Select All").performClick()
        compose.onNodeWithTag("workspace-top-app-bar").assertDoesNotExist()
        compose.onNodeWithTag("task-workspace-list").assertHeightIsAtLeast(160.dp)
        compose.onNodeWithTag("task-selection-complete").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("task-selection-more").assertIsDisplayed().assertIsEnabled()
        captureVisualCatalogSurface("ux-audit2.task-selection.short-large")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithTag("workspace-top-app-bar").assertIsDisplayed()
        compose.onNodeWithTag("workspace-search-action").assertIsDisplayed().assertIsEnabled()
    }

    private fun openPlanner() {
        compose.onNodeWithContentDescription("Tasks tab").performClick()
        compose.onNodeWithTag("task-destination-Inbox").performClick().assertIsSelected()
        compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("Plan My Day"))
        compose.onNodeWithText("Plan My Day").performClick()
    }
    private fun task(id: Long, title: String, minutes: Int?, scheduled: Boolean = false): ScheduledTask {
        val task = WhipTask(id, title, "", if (scheduled) ScheduleKind.Once else ScheduleKind.Anytime, today.takeIf { scheduled }, null, null,
            false, false, null, 1, 1, durationMinutes = minutes, areaId = if (scheduled) "other" else "scope", inbox = !scheduled)
        return ScheduledTask(task, today.takeIf { scheduled }, today.takeIf { scheduled })
    }
}
