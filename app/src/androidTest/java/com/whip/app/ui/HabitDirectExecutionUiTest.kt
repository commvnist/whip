package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.WhipApplication
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.domain.*
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDirectExecutionUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val today = LocalDate.of(2026, 9, 29)

    @Test fun collectionAndTodayRunEveryTrackingModeWithoutOpeningDetails() = exerciseModes()

    @Test @AndroidFontScale
    fun collectionAndTodayKeepDirectInputsReachableAtLargeText() = exerciseModes()

    private fun exerciseModes() {
        var mode by mutableStateOf(HabitTrackingMode.CheckOff)
        var management by mutableStateOf(true)
        var opened = 0
        var quick = 0
        var amount = 0
        var total = 0
        var checklist = 0
        val increments = mutableListOf<Double>()
        compose.setContent {
            CompositionLocalProvider(LocalWhipToday provides today) {
                WhipTheme(dynamicColor = false) { Surface {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                        HabitProgressCard(
                            item = progress(mode), onOpen = { opened++ }, onEdit = {}, onQuick = { quick++ },
                            onQuickValue = { increments += it }, onSetValue = { total++ }, onAddAmount = { amount++ },
                            onDecrement = {}, onUndo = {}, onUndoSkip = {},
                            onChecklist = { habitId, itemId, date, completed ->
                                assertEquals(1L, habitId); assertEquals(2L, itemId); assertEquals(today, date)
                                assertTrue(completed); checklist++
                            },
                            management = management,
                        )
                    }
                } }
            }
        }
        listOf(true, false).forEach { browse ->
            compose.runOnIdle { management = browse }
            HabitTrackingMode.entries.forEach { tracking ->
                compose.runOnIdle { mode = tracking }
                when (tracking) {
                    HabitTrackingMode.CheckOff -> compose.onNodeWithContentDescription("Check off habit Practice").performScrollTo().performClick()
                    HabitTrackingMode.Checklist -> {
                        compose.onNodeWithContentDescription("Complete checklist item Prepare").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
                        compose.onNodeWithContentDescription("Check off habit Practice").performScrollTo().performClick()
                    }
                    HabitTrackingMode.Count, HabitTrackingMode.Decimal -> {
                        compose.onNodeWithText("+1").performScrollTo().performClick()
                        compose.onNodeWithText("Add Amount").performScrollTo().performClick()
                        compose.onNodeWithText("Set Total").performScrollTo().performClick()
                    }
                    HabitTrackingMode.Duration -> {
                        compose.onNodeWithText("Start").performScrollTo().performClick()
                        compose.onNodeWithText("Enter Duration").performScrollTo().performClick()
                    }
                    HabitTrackingMode.Rating -> compose.onNodeWithText("Rate").performScrollTo().performClick()
                    HabitTrackingMode.LogOnly -> compose.onNodeWithText("Log").performScrollTo().performClick()
                }
                compose.onAllNodesWithTag("habit-detail-surface").assertCountEquals(0)
                compose.runOnIdle { assertEquals(0, opened) }
            }
        }
        compose.runOnIdle {
            assertEquals(10, quick)
            assertEquals(listOf(1.0, 1.0, 1.0, 1.0), increments)
            assertEquals(6, amount)
            assertEquals(4, total)
            assertEquals(2, checklist)
            mode = HabitTrackingMode.Checklist
            management = true
        }
        compose.onNodeWithContentDescription("Complete checklist item Prepare").performScrollTo().assertIsDisplayed()
        captureVisualCatalogSurface(if (compose.density.fontScale >= 1.5f) "direct-execution.habits-checklist.large" else "direct-execution.habits-checklist")
    }

    @Test fun collectionEntriesSaveThroughFocusedDialogsWithoutInspector() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val date = app.clock.today()
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true) }
            listOf(HabitTrackingMode.Count, HabitTrackingMode.Duration, HabitTrackingMode.Rating, HabitTrackingMode.LogOnly).forEach { mode ->
                app.habitRepository.create(HabitDraft(
                    name = mode.name, trackingMode = mode,
                    dimension = when (mode) {
                        HabitTrackingMode.Duration -> UnitDimension.Duration
                        HabitTrackingMode.Rating, HabitTrackingMode.LogOnly -> UnitDimension.Unitless
                        else -> UnitDimension.Count
                    },
                    unitId = when (mode) {
                        HabitTrackingMode.Duration -> "second"
                        HabitTrackingMode.Rating, HabitTrackingMode.LogOnly -> "unitless"
                        else -> "count"
                    },
                    comparison = TargetComparison.None, startDate = date,
                ))
            }
        }
        try {
            compose.setContent {
                CompositionLocalProvider(LocalWhipToday provides date) {
                    WhipTheme(dynamicColor = false) {
                        val vm: HabitViewModel = viewModel()
                        val state by vm.uiState.collectAsStateWithLifecycle()
                        HabitAreaContent(state = state, innerPadding = PaddingValues(), viewModel = vm)
                    }
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Count").fetchSemanticsNodes().isNotEmpty() }
            listOf("Add Amount" to "2", "Enter Duration" to "30", "Rate" to "4", "Log" to "").forEach { (action, value) ->
                compose.onNodeWithText(action).performScrollTo().performClick()
                compose.onNodeWithTag("habit-value-dialog").assertIsDisplayed()
                compose.onAllNodesWithTag("habit-detail-surface").assertCountEquals(0)
                compose.onNodeWithTag("habit-value-input").performTextReplacement(value)
                compose.onNodeWithTag("habit-value-note").performTextReplacement("Direct $action")
                closeSoftKeyboard()
                compose.onNodeWithTag("habit-value-save").performClick()
                compose.waitUntil(10_000) { compose.onAllNodesWithTag("habit-value-dialog").fetchSemanticsNodes().isEmpty() }
                compose.onNodeWithTag("habit-destination-All").assertIsSelected()
                compose.onAllNodesWithTag("habit-detail-surface").assertCountEquals(0)
            }
            val logs = runBlocking { app.habitRepository.logs.first() }
            assertEquals(4, logs.size)
            assertEquals(2.0, logs.single { it.note == "Direct Add Amount" }.value!!, 0.0)
            assertEquals(30.0, logs.single { it.note == "Direct Enter Duration" }.value!!, 0.0)
            assertEquals(4.0, logs.single { it.note == "Direct Rate" }.value!!, 0.0)
            assertNull(logs.single { it.note == "Direct Log" }.value)
            assertTrue(logs.all { it.localDate == date })
        } finally {
            runBlocking { app.backupRepository.deleteAllData() }
        }
    }

    @Test fun unavailableRowsHideEntryControlsButKeepTimerRecoveryAndSkipUndo() {
        var item by mutableStateOf(progress(HabitTrackingMode.Count))
        var reordering by mutableStateOf(false)
        var quick = 0
        var undo = 0
        compose.setContent {
            CompositionLocalProvider(LocalWhipToday provides today) {
                WhipTheme(dynamicColor = false) { Surface {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                        HabitProgressCard(item, {}, {}, { quick++ }, onDecrement = {}, onUndo = {},
                            onUndoSkip = { undo++ }, onChecklist = { _, _, _, _ -> }, onAddAmount = {},
                            management = true, reorderMode = reordering)
                    }
                } }
            }
        }
        val ordinary = progress(HabitTrackingMode.Count)
        listOf(
            ordinary.copy(habit = ordinary.habit.copy(paused = true)),
            ordinary.copy(habit = ordinary.habit.copy(archived = true)),
            ordinary.copy(habit = ordinary.habit.copy(sourceMeasurementId = "linked")),
            ordinary.copy(scheduled = false),
            ordinary.copy(date = today.minusDays(1)),
            ordinary.copy(dayState = HabitDayState.Paused),
        ).forEach { unavailable ->
            compose.runOnIdle { item = unavailable }
            compose.onAllNodesWithText("+1").assertCountEquals(0)
            compose.onAllNodesWithText("Add Amount").assertCountEquals(0)
            compose.onAllNodesWithText("Set Total").assertCountEquals(0)
        }
        compose.runOnIdle { item = ordinary; reordering = true }
        compose.onAllNodesWithText("+1").assertCountEquals(0)
        compose.onAllNodesWithText("Add Amount").assertCountEquals(0)
        compose.runOnIdle {
            reordering = false
            item = progress(HabitTrackingMode.Duration).let { it.copy(scheduled = false,
                habit = it.habit.copy(archived = true, timerStartedAtMillis = 1L, timerSessionId = "timer", timerNeedsReview = true)) }
        }
        compose.onNodeWithText("Review").performScrollTo().performClick()
        compose.onAllNodesWithText("Enter Duration").assertCountEquals(0)
        compose.runOnIdle { item = ordinary.copy(dayState = HabitDayState.Skipped) }
        compose.onNodeWithText("Undo").performScrollTo().performClick()
        compose.onAllNodesWithText("Add Amount").assertCountEquals(0)
        compose.runOnIdle { assertEquals(1, quick); assertEquals(1, undo) }
    }

    @Test @AndroidFontScale
    fun extremeIncrementKeepsItsFullAmountVisibleAndItsPrimaryActionReadable() {
        var logged: Double? = null
        val item = progress(HabitTrackingMode.Count).let { it.copy(habit = it.habit.copy(quickIncrement = 1_000_000_000.0)) }
        compose.setContent {
            CompositionLocalProvider(LocalWhipToday provides today) {
                WhipTheme(dynamicColor = false) { Surface {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                        HabitProgressCard(item, {}, {}, {}, onQuickValue = { logged = it }, onDecrement = {},
                            onUndo = {}, onUndoSkip = {}, onChecklist = { _, _, _, _ -> }, onAddAmount = {}, management = true)
                    }
                } }
            }
        }
        listOf("Add", "+1000000000").forEach { label ->
            val node = compose.onNodeWithText(label, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.size)
            val layout = layouts.single()
            assertEquals(2f, layout.layoutInput.density.fontScale, 0.01f)
            // Check glyph extent, allowing only integer pixel rounding from Compose.
            repeat(layout.lineCount) { line ->
                assertTrue("$label must remain complete: right=${layout.getLineRight(line)}, width=${layout.size.width}",
                    layout.getLineRight(line) <= layout.size.width + 1f)
                assertFalse("$label must not ellipsize", layout.isLineEllipsized(line))
            }
            assertFalse("$label must fit vertically", layout.didOverflowHeight)
            assertEquals(label.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        }
        compose.onNodeWithContentDescription("Add 1000000000 to Practice").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1_000_000_000.0, logged!!, 0.0) }
        val width = compose.onNodeWithTag("habit-primary-action-1", useUnmergedTree = true).getUnclippedBoundsInRoot().width
        assertTrue(width <= 112.dp)
        captureVisualCatalogSurface("direct-execution.habits-extreme-increment.large")
    }

    private fun progress(mode: HabitTrackingMode): HabitDayProgress {
        val habit = Habit(id = 1, uuid = "habit-1", measurementId = "measurement", name = "Practice",
            notes = "", area = "Main", tags = emptyList(), icon = "✓", trackingMode = mode,
            dimension = if (mode == HabitTrackingMode.Duration) UnitDimension.Duration else UnitDimension.Count,
            unitId = if (mode == HabitTrackingMode.Duration) "second" else "count", precision = 0,
            comparison = TargetComparison.AtLeast, targetMin = 1.0, targetMax = null,
            targetPeriod = TargetPeriod.Day, rollingDays = null, scheduleType = HabitScheduleType.Daily,
            scheduleInterval = 1, weekdays = emptySet(), flexibleTimesPerWeek = null, startDate = today,
            endType = HabitEndType.Never, endDate = null, endValue = null, quickIncrement = 1.0,
            quickActions = emptyList(), reminderMinutes = emptyList(), weekdayReminderMinutes = emptyMap(),
            weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = null, pinned = false, position = 0,
            archived = false, paused = false, createdAtMillis = 1, updatedAtMillis = 1)
        val items = if (mode == HabitTrackingMode.Checklist) listOf(
            HabitChecklistItem(2, "item-2", 1, "Prepare", 0, false, 1, 1) to false,
        ) else emptyList()
        return HabitDayProgress(habit, today, true, 0.0, null, null, items, 0, 0.0)
    }
}
