package com.whip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.WhipApplication
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.domain.*
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class FreshProductivityUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val today = LocalDate.of(2026, 9, 29)

    @Test fun managementHabitShowsScheduleAndPauseWithoutExpandedAreaRows() {
        val item = progress(habit().copy(paused = true, scheduleType = HabitScheduleType.SelectedWeekdays,
            weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)))
        compose.setContent {
            WhipTheme(dynamicColor = false) { Surface { Column(Modifier.width(320.dp).padding(16.dp)) {
                HabitProgressCard(item, {}, {}, {}, onDecrement = {}, onUndo = {}, onUndoSkip = {},
                    onChecklist = { _, _, _, _ -> }, management = true)
            } } }
        }
        compose.onNodeWithText("Main · Tue, Fri").assertIsDisplayed()
        compose.onNodeWithText("Paused · no check-in expected").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit habit Reading").assertIsDisplayed()
        captureVisualCatalogSurface("fresh-productivity.habit-management")
    }

    @Test fun invalidHabitAmountExplainsTheProblemAndDoesNotSave() {
        var saved = false
        compose.setContent { WhipTheme(dynamicColor = false) {
            HabitValueDialog(progress(habit().copy(trackingMode = HabitTrackingMode.Count)), {}, { _, _ -> saved = true })
        } }
        compose.onNodeWithTag("habit-value-input").performTextReplacement("oops")
        closeSoftKeyboard()
        compose.onNodeWithTag("habit-value-save").performClick()
        compose.onNodeWithText("Enter a valid, finite number").assertIsDisplayed()
        compose.runOnIdle { assertFalse(saved) }
        compose.onNodeWithTag("habit-value-input").performTextReplacement("-2")
        closeSoftKeyboard()
        compose.onNodeWithTag("habit-value-save").performClick()
        compose.runOnIdle { assertTrue(saved) }
    }

    @Test fun archivedHabitExplainsNewEntryPrerequisiteBeforeOpeningAForm() {
        val item = progress(habit().copy(archived = true))
        compose.setContent { WhipTheme(dynamicColor = false) {
            HabitActionsDialog(item, onDismiss = {}, onEdit = {}, onDuplicate = {}, onPin = {}, onPause = {},
                onSchedulePause = {}, onQuick = {}, onSkip = {}, onUndoSkip = {}, onUndoHistoricalSkip = {},
                logs = emptyList(), skips = emptyList(), pauses = emptyList(), onAddHistoricalLog = {},
                onEditLog = {}, onEditPause = {}, onArchive = {}, onDelete = {}, openHistory = true)
        } }
        compose.onNodeWithTag("entity-inspector-action-add-past-entry").assertIsNotEnabled()
        compose.onNodeWithText("Restore this Habit before adding an entry. Existing history can still be corrected.").assertIsDisplayed()
    }

    @Test fun invalidHistoricalHabitAmountExplainsWhyItCannotBeRecorded() {
        var saved = false
        compose.setContent { WhipTheme(dynamicColor = false) {
            HabitHistoryLogDialog(progress(habit().copy(trackingMode = HabitTrackingMode.Count)), null,
                today.minusDays(1), {}, { _, _, _, _ -> saved = true })
        } }
        compose.onNodeWithText("Log Progress").performClick()
        compose.onNodeWithText("Enter a valid, finite number").assertIsDisplayed()
        compose.runOnIdle { assertFalse(saved) }
    }

    @Test fun reachedGoalKeepsLoggingAndDirectCompletionWithoutOpeningDetails() {
        var opened = false
        var logged = false
        var completed = false
        val projection = projection(goal()).copy(currentValue = 30.0, progress = 1.25)
        compose.setContent { WhipTheme(dynamicColor = false) { Surface {
            Column(Modifier.width(320.dp).padding(16.dp)) {
                GoalCard(projection, onOpen = { opened = true }, onEdit = {}, onRecord = { logged = true },
                    onResetElapsed = {}, onToggleMilestone = { _, _ -> }, onComplete = { completed = true })
            }
        } } }
        compose.onNodeWithText("125%").assertIsDisplayed()
        compose.onNodeWithText("30 → 24 · Target reached").assertIsDisplayed()
        compose.onNodeWithText("Log Progress").performClick()
        compose.onNodeWithTag("goal-card-complete-2").performClick()
        compose.runOnIdle { assertFalse(opened); assertTrue(logged); assertTrue(completed) }
        captureVisualCatalogSurface("fresh-productivity.goal-reached")
    }

    @Test @AndroidFontScale fun goalLogAndResetRemainFullyReadableAtLargeText() {
        val reached = projection(goal().copy(name = "Read books")).copy(currentValue = 30.0, progress = 1.25)
        val elapsed = projection(goal().copy(id = 3, name = "Elapsed", type = GoalType.ElapsedSince, elapsedStartMillis = 0L))
        compose.setContent { WhipTheme(dynamicColor = false) { Surface {
            Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                listOf(reached, elapsed).forEach { item ->
                    GoalCard(item, onOpen = {}, onEdit = {}, onRecord = {}, onResetElapsed = {}, onToggleMilestone = { _, _ -> })
                }
            }
        } } }
        listOf("Log Progress" to 2, "Reset Timer" to 3).forEach { (label, id) ->
            val node = compose.onNodeWithText(label, useUnmergedTree = true)
            node.performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.size)
            val layout = layouts.single()
            assertEquals(2f, layout.layoutInput.density.fontScale, 0.01f)
            assertEquals(1, layout.lineCount)
            // Compose may flag fractional paragraph width rounded into an integer size.
            assertTrue(
                "$label must not clip: size=${layout.size}, constraints=${layout.layoutInput.constraints}, " +
                    "lineRight=${layout.getLineRight(0)}",
                layout.getLineRight(0) <= layout.size.width + 1f,
            )
            assertFalse("$label must fit vertically", layout.didOverflowHeight)
            assertFalse("$label must not ellipsize", layout.isLineEllipsized(0))
            assertEquals(label.length, layout.getLineEnd(0, visibleEnd = true))
            val slot = compose.onNodeWithTag("goal-primary-action-$id", useUnmergedTree = true)
                .assertWidthIsAtLeast(64.dp).assertHeightIsAtLeast(48.dp).getUnclippedBoundsInRoot()
            val status = compose.onNodeWithTag("goal-card-status-$id", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val card = compose.onNodeWithTag("goal-card-$id").getUnclippedBoundsInRoot()
            assertEquals("$label must share the leading status edge", status.left.value, slot.left.value, 0.5f)
            assertTrue("$label must follow the visible status", slot.top >= status.bottom)
            assertTrue("$label must stay inside its card", slot.left >= card.left && slot.right <= card.right)
            assertTrue("$label must retain natural width", slot.right - slot.left < card.right - card.left)
        }
        captureVisualCatalogSurface("fresh-productivity.goal-actions.large")
    }

    @Test fun collapsedGoalActionsRespectEveryTypeAndLifecycle() {
        var item by mutableStateOf(goal())
        var reorder by mutableStateOf(false)
        var logged = 0
        var reset = 0
        var completed = 0
        var toggled = 0
        var opened = 0
        val milestone = GoalMilestone(91, "milestone-91", item.id, "Publish", 0, 1.0, false, null, "", 1, 1)
        compose.setContent { WhipTheme(dynamicColor = false) { Surface {
            Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                GoalCard(projection(item).copy(progress = .5, milestones = listOf(milestone)),
                    onOpen = { opened++ }, onEdit = {}, onRecord = { logged++ },
                    onResetElapsed = { reset++ }, onToggleMilestone = { boundary, value ->
                        assertEquals(91L, boundary.milestoneId); assertTrue(value); toggled++
                    }, onComplete = { completed++ }, reorderMode = reorder)
            }
        } } }
        GoalType.entries.forEach { type ->
            compose.runOnIdle { item = goal().copy(type = type, aggregation = type.defaultAggregation(), elapsedStartMillis = 0L) }
            when (type) {
                GoalType.WeightedMilestones -> compose.onNodeWithTag("goal-milestone-91", useUnmergedTree = true)
                    .assertIsEnabled().performClick()
                GoalType.ElapsedSince -> compose.onNodeWithTag("goal-card-reset-2", useUnmergedTree = true).performClick()
                else -> compose.onNodeWithContentDescription(
                    if (type.defaultAggregation() == GoalAggregation.CompletionCount) "Record a completion for Read 24 books"
                    else "Log progress for Read 24 books",
                ).performClick()
            }
            compose.onNodeWithTag("goal-card-complete-2", useUnmergedTree = true).performScrollTo().performClick()
        }
        compose.runOnIdle {
            assertEquals(7, logged); assertEquals(1, reset); assertEquals(1, toggled)
            assertEquals(9, completed); assertEquals(0, opened)
        }
        GoalType.entries.forEach { type ->
            listOf(GoalStatus.Paused, GoalStatus.Completed, GoalStatus.Abandoned, GoalStatus.Archived).forEach { status ->
                compose.runOnIdle { item = goal().copy(type = type, status = status) }
                compose.onNodeWithTag("goal-primary-action-2", useUnmergedTree = true).assertDoesNotExist()
                compose.onNodeWithTag("goal-card-complete-2", useUnmergedTree = true).assertDoesNotExist()
                compose.onNodeWithTag("goal-milestone-91", useUnmergedTree = true).assertDoesNotExist()
            }
        }
        compose.runOnIdle { item = goal().copy(archived = true) }
        compose.onNodeWithTag("goal-primary-action-2", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("goal-card-complete-2", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { item = goal(); reorder = true }
        compose.onNodeWithTag("goal-primary-action-2", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("goal-card-complete-2", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun directCompletionConfirmsBelowTargetAndSavesFrozenOutcome() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val id = runBlocking {
            app.goalRepository.create(GoalDraft(name = "Direct completion", type = GoalType.ReachValue,
                dimension = UnitDimension.Count, unitId = "count", baseline = 0.0, targetMin = 10.0,
                startDate = app.clock.today())).also { app.goalRepository.recordMeasurement(it, 4.0) }
        }
        compose.setContent { WhipTheme(dynamicColor = false) {
            val vm: GoalViewModel = viewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            GoalAreaContent(state = state.copy(active = state.active.filter { it.goal.id == id }),
                innerPadding = PaddingValues(), viewModel = vm)
        } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("goal-card-complete-$id").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("goal-card-complete-$id").performScrollTo().performClick()
        compose.onNodeWithTag("goal-detail-surface").assertDoesNotExist()
        compose.onNodeWithText("40% complete").assertIsDisplayed()
        compose.onNodeWithText("This Goal has not reached its measured target.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(id) }!!.status) }
        compose.onNodeWithTag("goal-card-complete-$id").performClick()
        compose.onNodeWithTag("goal-completion-confirm").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("goal-completion-dialog").fetchSemanticsNodes().isEmpty() }
        runBlocking {
            assertEquals(GoalStatus.Completed, app.goalRepository.get(id)!!.status)
            val closure = app.database.goalDao().getClosureSnapshots(id).single()
            assertEquals(.4, closure.progress!!, 0.0)
            assertEquals(4.0, closure.value!!, 0.0)
        }
    }

    @Test fun cancellingHabitDefinitionEditReturnsToHistory() {
        val item = progress(habit())
        var openRequest by mutableStateOf<Long?>(item.habit.id)
        compose.setContent { WhipTheme(dynamicColor = false) {
            val vm: HabitViewModel = viewModel()
            HabitAreaContent(state = HabitUiState(all = listOf(item), today = listOf(item), currentDate = today, loading = false),
                innerPadding = PaddingValues(), viewModel = vm, openHabitIdRequest = openRequest,
                onOpenHabitRequestConsumed = { openRequest = null }, showWorkspace = false)
        } }
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithTag("entity-inspector-edit").performClick()
        compose.onNodeWithTag("habit-editor-surface").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel Habit editing").performClick()
        compose.onNodeWithText("History").assertIsSelected()
        compose.onNodeWithTag("entity-inspector-action-add-past-entry").assertIsDisplayed()
    }

    @Test fun cancellingGoalDefinitionEditReturnsToHistory() {
        val projection = projection(goal())
        var openRequest by mutableStateOf<Long?>(projection.goal.id)
        compose.setContent { WhipTheme(dynamicColor = false) {
            val vm: GoalViewModel = viewModel()
            GoalAreaContent(state = GoalUiState(active = listOf(projection), currentDate = today, loading = false),
                innerPadding = PaddingValues(), viewModel = vm, openGoalIdRequest = openRequest,
                onOpenGoalRequestConsumed = { openRequest = null }, showWorkspace = false)
        } }
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithTag("entity-inspector-edit").performClick()
        compose.onNodeWithTag("goal-editor-surface").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel Goal editing").performClick()
        compose.onNodeWithText("History").assertIsSelected()
        compose.onNodeWithText("No progress updates yet.").assertIsDisplayed()
    }

    @Test fun elapsedResetIsSecondaryAndCancelReturnsToTheInspector() {
        val now = today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val projection = projection(goal().copy(type = GoalType.ElapsedSince, elapsedStartMillis = now - 86_400_000L))
        var openRequest by mutableStateOf<Long?>(projection.goal.id)
        compose.setContent { WhipTheme(dynamicColor = false) {
            val vm: GoalViewModel = viewModel()
            GoalAreaContent(state = GoalUiState(active = listOf(projection), currentDate = today, nowMillis = now, loading = false),
                innerPadding = PaddingValues(), viewModel = vm, openGoalIdRequest = openRequest,
                onOpenGoalRequestConsumed = { openRequest = null }, showWorkspace = false)
        } }
        compose.onNodeWithTag("entity-inspector-action-reset-timer").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("goal-detail-surface").assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-action-reset-timer").assertIsDisplayed()
    }

    @Test fun savingGoalDefinitionReturnsToHistoryAndDoesNotFollowLaterExternalEdits() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val draft = GoalDraft(name = "Inspector return", type = GoalType.ReachValue, dimension = UnitDimension.Count,
            unitId = "count", baseline = 0.0, targetMin = 24.0, startDate = app.clock.today())
        val id = runBlocking { app.goalRepository.create(draft) }
        var openRequest by mutableStateOf<Long?>(id)
        var latestSourceName: String? = null
        compose.setContent { WhipTheme(dynamicColor = false) {
            val vm: GoalViewModel = viewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            SideEffect { latestSourceName = state.active.firstOrNull { it.goal.id == id }?.goal?.name }
            GoalAreaContent(state = state, innerPadding = PaddingValues(), viewModel = vm, openGoalIdRequest = openRequest,
                onOpenGoalRequestConsumed = { openRequest = null }, showWorkspace = false)
        } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("goal-detail-surface").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithTag("entity-inspector-edit").performClick()
        compose.onNodeWithTag("goal-editor-name").performTextReplacement("Renamed inspector return")
        closeSoftKeyboard()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("entity-inspector-edit").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("History").assertIsSelected()
        compose.onNodeWithText("Renamed inspector return").assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-edit").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("entity-inspector-edit").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("History").assertIsSelected()
        runBlocking { app.goalRepository.update(id, draft.copy(name = "External definition change")) }
        compose.waitUntil(5_000) { latestSourceName == "External definition change" }
        compose.onNodeWithText("Renamed inspector return").assertIsDisplayed()
        compose.onAllNodesWithText("External definition change").assertCountEquals(0)
    }

    private fun progress(habit: Habit) = HabitDayProgress(habit, today, true, 0.0, null, null, emptyList(), 0, 0.0)
    private fun habit() = Habit(id = 1, uuid = "habit-1", measurementId = "habit-measurement", name = "Reading",
        notes = "", area = "Main", areaId = "main", tags = emptyList(), icon = "📖", trackingMode = HabitTrackingMode.CheckOff,
        dimension = UnitDimension.Count, unitId = "count", precision = 0, comparison = TargetComparison.AtLeast,
        targetMin = 1.0, targetMax = null, targetPeriod = TargetPeriod.Day, rollingDays = null,
        scheduleType = HabitScheduleType.Daily, scheduleInterval = 1, weekdays = emptySet(), flexibleTimesPerWeek = null,
        startDate = today, endType = HabitEndType.Never, endDate = null, endValue = null,
        quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(), weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = null, pinned = false, position = 0, archived = false,
        paused = false, createdAtMillis = 1, updatedAtMillis = 1)
    private fun goal() = Goal(id = 2, uuid = "goal-2", measurementId = "goal-measurement", name = "Read 24 books",
        description = "", area = "", tags = emptyList(), icon = "📚", type = GoalType.ReachValue,
        dimension = UnitDimension.Count, unitId = "count", precision = 0, baseline = 0.0,
        targetMin = 24.0, targetMax = null, direction = GoalDirection.Increase, startDate = today,
        deadline = null, aggregation = GoalAggregation.Latest, paceType = GoalPaceType.None,
        reminderMinutes = null, status = GoalStatus.Active, pinned = false, position = 0, createdAtMillis = 1, updatedAtMillis = 1)
    private fun projection(goal: Goal) = GoalProjection(goal, null, null, null, null, null, null, null, emptyList(), emptyList())
}
