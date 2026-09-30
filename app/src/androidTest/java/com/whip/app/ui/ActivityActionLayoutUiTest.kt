package com.whip.app.ui

import android.content.Intent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.MainActivity
import com.whip.app.WhipApplication
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.launchMainActivity
import com.whip.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/** Activity journeys own only their uniquely named records; existing data is never reset. */
class ActivityActionLayoutUiTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private val suffix get() = if (app.resources.configuration.fontScale >= 1.5f) "large" else "normal"

    private fun habit(mode: HabitTrackingMode, label: String, quickActions: List<Double> = emptyList()): Long = runBlocking {
        app.habitRepository.create(HabitDraft(
            name = "QA $label ${System.nanoTime()}", startDate = app.clock.today(), trackingMode = mode,
            dimension = if (mode == HabitTrackingMode.Duration) UnitDimension.Duration else UnitDimension.Count,
            unitId = if (mode == HabitTrackingMode.Duration) "second" else "count",
            comparison = TargetComparison.None,
            quickActions = quickActions,
        ))
    }

    private fun scrollCard(kind: String, id: Long): SemanticsNodeInteraction {
        val matcher = hasTestTag("$kind-card-$id")
        compose.onNode(hasScrollToIndexAction() and !hasTestTag("destination-support-content")).performScrollToNode(matcher)
        return compose.onNode(matcher)
    }

    private fun card(kind: String, id: Long, matcher: SemanticsMatcher): SemanticsNodeInteraction {
        scrollCard(kind, id)
        return compose.onNode(matcher and hasAnyAncestor(hasTestTag("$kind-card-$id")))
    }

    private fun waitGone(tag: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
    }

    private fun capture(name: String) = captureVisualCatalogSurface("action-layout.qa.$name.$suffix")

    private fun assertLeadingCardAction(kind: String, id: Long, label: String) {
        card(kind, id, hasText(label)).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val action = compose.onNodeWithTag("$kind-primary-action-$id").fetchSemanticsNode().boundsInRoot
        val status = compose.onNodeWithTag("$kind-card-status-$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val bounds = compose.onNodeWithTag("$kind-card-$id").fetchSemanticsNode().boundsInRoot
        assertTrue("Activity action must lead at the status edge: $action / $status", abs(action.left - status.left) <= 1f)
        assertTrue("Activity action must follow visible status", action.top >= status.bottom)
        assertTrue("Activity action must fit within its card", action.left >= bounds.left && action.right <= bounds.right)
        compose.onNode(hasText(label) and hasAnyAncestor(hasTestTag("$kind-card-$id")), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
                val results = mutableListOf<TextLayoutResult>()
                getResults(results)
                assertTrue(results.isNotEmpty())
                results.forEach { result ->
                    assertFalse("Action label cannot be ellipsized", (0 until result.lineCount).any(result::isLineEllipsized))
                    assertTrue("Action label must fit its measured width", (0 until result.lineCount).all { result.getLineRight(it) <= result.size.width + 1f })
                    assertTrue(result.multiParagraph.height <= result.size.height + 1f)
                }
            }
    }

    private fun assertLoggingFooter(tag: String, label: String) {
        val submit = compose.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        submit.assertTextContains(label)
        val primary = submit.fetchSemanticsNode().boundsInRoot
        val cancel = compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).fetchSemanticsNode().boundsInRoot
        val inputTag = if (tag.startsWith("habit")) "habit-value-input" else "goal-measurement-value"
        val input = compose.onNodeWithTag(inputTag).fetchSemanticsNode().boundsInRoot
        assertTrue("Logging submit must align with the input edge", abs(primary.left - input.left) <= 1f)
        assertTrue("Logging submit must precede Cancel, allowing leading wrapping",
            (abs(primary.top - cancel.top) <= 1f && primary.right <= cancel.left) ||
                (cancel.top >= primary.bottom && abs(primary.left - cancel.left) <= 1f))
    }

    private fun value(habitId: Long): Double = runBlocking {
        app.habitRepository.logs.first().filter { it.habitId == habitId && it.localDate == app.clock.today() }.sumOf { it.canonicalValue ?: 0.0 }
    }

    private fun assertSameButtonSize(expected: Rect, actual: Rect, context: String, compareWidth: Boolean = true) {
        if (compareWidth) assertEquals("$context width", expected.width, actual.width, 1f)
        assertEquals("$context height", expected.height, actual.height, 1f)
    }

    private fun sizingLabel(label: String) = hasText(label, substring = label.startsWith("+1000000000") || label.startsWith("−"))

    private fun SemanticsNodeInteraction.sizingBounds(): Rect = getUnclippedBoundsInRoot().let { bounds ->
        with(compose.density) { Rect(bounds.left.toPx(), bounds.top.toPx(), bounds.right.toPx(), bounds.bottom.toPx()) }
    }

    private fun assertSizedButtons(scope: SemanticsMatcher, vararg labels: String, scroll: Boolean = false): Rect {
        val bounds = labels.map { label ->
            val button = compose.onNode(scope and sizingLabel(label) and hasClickAction())
            if (scroll) button.performScrollTo()
            button.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            val result = button.sizingBounds()
            val text = compose.onNode(scope and sizingLabel(label) and hasAnyAncestor(hasClickAction()) and
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            val layouts = mutableListOf<TextLayoutResult>()
            text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertTrue("$label must expose actual text layout", layouts.isNotEmpty())
                    layouts.forEach { layout ->
                        val details = "label=$label; size=${layout.size}; constraints=${layout.layoutInput.constraints}; " +
                            "paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}; " +
                            "overflowWidth=${layout.didOverflowWidth}; overflowHeight=${layout.didOverflowHeight}; " +
                            "button=$result; text=${text.sizingBounds()}; actualFontScale=${app.resources.configuration.fontScale}; " +
                            "layoutFontScale=${layout.layoutInput.density.fontScale}; lines=" +
                            (0 until layout.lineCount).map { line -> listOf(layout.getLineLeft(line), layout.getLineRight(line),
                                layout.getLineTop(line), layout.getLineBottom(line)) } +
                            "; glyphs=" + layout.layoutInput.text.text.indices.map(layout::getBoundingBox)
                        val glyphs = layout.layoutInput.text.text.indices.map(layout::getBoundingBox)
                        val clipped = glyphs.any { it.left < -1f || it.top < -1f ||
                            it.right > layout.size.width + 1f || it.bottom > layout.size.height + 1f }
                        if (clipped) runCatching {
                            captureVisualCatalogSurface("action-sizing.qa.text-overflow.$suffix")
                        }
                        assertFalse("$label glyphs must fit their actual text bounds: $details", clipped)
                        assertEquals("$label must render every character: $details", layout.layoutInput.text.length,
                            layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
                        assertFalse("$label must not be ellipsized", (0 until layout.lineCount).any(layout::isLineEllipsized))
                        assertTrue("$label glyphs must fit horizontally", (0 until layout.lineCount).all {
                            layout.getLineLeft(it) >= -1f && layout.getLineRight(it) <= layout.size.width + 1f
                        })
                        assertTrue("$label glyphs must fit vertically", layout.multiParagraph.height <= layout.size.height + 1f)
                        assertEquals("$label must use actual Android text scale", app.resources.configuration.fontScale,
                            layout.layoutInput.density.fontScale, 0.01f)
                    }
            result
        }
        bounds.drop(1).forEach { assertSameButtonSize(bounds.first(), it, labels.joinToString(" / ")) }
        val current = labels.map { compose.onNode(scope and sizingLabel(it) and hasClickAction()).sizingBounds() }
        current.zipWithNext().forEach { (first, next) ->
            assertTrue("Related controls must not overlap", first.right <= next.left + 1f || first.bottom <= next.top + 1f)
        }
        return current.first()
    }

    @Test fun relatedActivityButtonsShareSizeAndKeepFullLabels() = exerciseActionSizing()

    @Test @AndroidFontScale
    fun largeTextActivityButtonsShareSizeAndKeepFullLabels() = exerciseActionSizing()

    private fun exerciseActionSizing() {
        val duration = habit(HabitTrackingMode.Duration, "sizing duration")
        val quantity = habit(HabitTrackingMode.Count, "sizing quantity", listOf(1_000_000_000.0))
        val outside = runBlocking { app.habitRepository.create(HabitDraft(
            name = "QA sizing outside ${System.nanoTime()}", startDate = app.clock.today(),
            trackingMode = HabitTrackingMode.Checklist, scheduleType = HabitScheduleType.SelectedWeekdays,
            weekdays = setOf(app.clock.today().plusDays(1).dayOfWeek),
            checklistItems = listOf(HabitChecklistItemDraft(name = "QA step", position = 0)),
        )) }
        val goal = runBlocking { app.goalRepository.create(GoalDraft(
            name = "QA sizing completions ${System.nanoTime()}", type = GoalType.Consistency,
            aggregation = GoalAggregation.CompletionCount, startDate = app.clock.today(),
            targetMin = 3.0, consistencyPeriod = GoalConsistencyPeriod.Week, consistencyRequiredPeriods = 12,
        )) }
        val dialog = hasAnyAncestor(isDialog())
        fun captureSize(name: String) {
            try {
                captureVisualCatalogSurface("action-sizing.qa.$name.$suffix")
            } catch (failure: IllegalStateException) {
                // PNG is exported first; a clipped neighboring cell can omit its label from UI XML.
                if (!failure.message.orEmpty().contains("contains an unlabeled Whip interactive node")) throw failure
            }
        }
        fun cardGroup(kind: String, id: Long, vararg labels: String): Rect {
            scrollCard(kind, id)
            val scope = hasAnyAncestor(hasTestTag("$kind-card-$id"))
            assertSizedButtons(scope, *labels, scroll = true)
            scrollCard(kind, id)
            val result = compose.onNode(scope and sizingLabel(labels.first()) and hasClickAction()).sizingBounds()
            val status = compose.onNodeWithTag("$kind-card-status-$id", useUnmergedTree = true).sizingBounds()
            assertEquals("Activity group must remain leading", status.left, result.left, 1f)
            assertTrue("Activity group must follow status", result.top >= status.bottom)
            return result
        }
        fun openInspector(kind: String, id: Long) {
            scrollCard(kind, id)
            compose.onNodeWithTag("$kind-card-title-$id", useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed().performClick()
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Habits tab").performClick()
            val reference = cardGroup("habit", duration, "Start Timer", "Enter Duration")
            captureSize("duration-card")
            val numeric = cardGroup("habit", quantity, "+1", "+1000000000", "Add Amount", "Set Total", "−0", "Undo Last Entry")
            assertSameButtonSize(reference, numeric, "Habit card groups")
            captureSize("numeric-card")
            card("habit", quantity, hasText("+1")).performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(10_000) { value(quantity) == 1.0 }

            card("habit", duration, hasText("Enter Duration")).performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithTag("habit-value-input").performTextReplacement("12")
            closeSoftKeyboard()
            val footer = assertSizedButtons(dialog, "Log Duration", "Cancel")
            assertLoggingFooter("habit-value-save", "Log Duration")
            assertSameButtonSize(reference, footer, "Card and logging footer", compareWidth = suffix == "normal")
            captureSize("duration-footer")
            compose.onNodeWithTag("habit-value-save").assertIsDisplayed().performClick()
            waitGone("habit-value-dialog")
            compose.waitUntil(10_000) { value(duration) == 12.0 }

            openInspector("habit", duration)
            val inspectorScope = hasAnyAncestor(hasTestTag("habit-detail-surface"))
            val dock = assertSizedButtons(inspectorScope, "Start Timer")
            assertSameButtonSize(reference, dock, "Card and inspector dock", compareWidth = suffix == "normal")
            captureSize("duration-inspector")
            device.pressBack()
            waitGone("habit-detail-surface")
            openInspector("habit", outside)
            val outsideDock = assertSizedButtons(inspectorScope, "Mark Today Complete Outside Schedule")
            assertSameButtonSize(dock, outsideDock, "Short and long Habit inspector labels")
            captureSize("outside-inspector")
            device.pressBack()
            waitGone("habit-detail-surface")

            compose.onNodeWithContentDescription("Goals tab").performClick()
            val goalButtons = cardGroup("goal", goal, "Record Completion", "Complete Goal")
            assertSameButtonSize(reference, goalButtons, "Habit and Goal card groups")
            captureSize("goal-card")
            card("goal", goal, hasText("Record Completion")).performScrollTo().assertIsDisplayed().performClick()
            assertSizedButtons(dialog, "Record Completion", "Cancel")
            compose.onNodeWithTag("goal-measurement-note").performTextReplacement("QA sizing saved")
            closeSoftKeyboard()
            captureSize("goal-footer")
            compose.onNodeWithTag("goal-measurement-save").assertIsDisplayed().performClick()
            waitGone("goal-measurement-dialog")
            val measurementId = runBlocking { app.goalRepository.get(goal)!!.measurementId }
            compose.waitUntil(10_000) { runBlocking {
                app.goalRepository.measurementEntries.first().count { it.measurementId == measurementId } == 1
            } }
            assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(goal)!!.status })
            openInspector("goal", goal)
            val goalScope = hasAnyAncestor(hasTestTag("goal-detail-surface"))
            val goalDock = assertSizedButtons(goalScope, "Record Completion")
            assertSameButtonSize(dock, goalDock, "Habit and Goal inspector docks")
            captureSize("goal-inspector")
            compose.onNodeWithTag("goal-detail-section-History").assertIsDisplayed().performClick()
            compose.onNode(hasScrollToIndexAction() and goalScope).performScrollToNode(hasText("QA sizing saved", substring = true))
            compose.onNode(goalScope and hasText("QA sizing saved", substring = true), useUnmergedTree = true)
                .performScrollTo().assertIsDisplayed().performClick()
            assertSizedButtons(dialog, "Save Changes", "Delete", "Cancel")
            captureSize("goal-correction-footer")
            compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
            waitGone("goal-measurement-dialog")
            device.pressBack()
            waitGone("goal-detail-surface")
            card("goal", goal, hasTestTag("goal-card-complete-$goal")).performScrollTo().assertIsDisplayed().performClick()
            assertSizedButtons(dialog, "Cancel", "Complete Goal")
            compose.onNodeWithText("Completing saves this outcome in History.", substring = true).performScrollTo().assertIsDisplayed()
            captureSize("goal-confirmation")
            compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
            waitGone("goal-completion-dialog")
            assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(goal)!!.status })
        }
    }

    @Test fun habitDurationTimerCheckInAndAmountsKeepTheirMeaning() {
        val duration = habit(HabitTrackingMode.Duration, "duration")
        val quantity = habit(HabitTrackingMode.Count, "quantity")
        val checkOff = habit(HabitTrackingMode.CheckOff, "check-in")
        runBlocking { app.habitRepository.log(duration, 5.0, note = "fixture original") }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            assertLeadingCardAction("habit", duration, "Start Timer")
            card("habit", duration, hasText("Enter Duration")).assertHeightIsAtLeast(48.dp)
            capture("duration-ready")
            card("habit", duration, hasText("Enter Duration")).performClick()
            compose.onNodeWithTag("habit-value-input").performTextReplacement("30")
            closeSoftKeyboard()
            assertLoggingFooter("habit-value-save", "Log Duration")
            capture("duration-log")
            compose.onNodeWithTag("habit-value-save").performTouchInput { click(); click() }
            waitGone("habit-value-dialog")
            compose.waitUntil(10_000) { value(duration) == 35.0 }
            assertEquals(2, runBlocking { app.habitRepository.logs.first().count { it.habitId == duration } })
            card("habit", duration, hasText("Start Timer")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.get(duration)?.timerSessionId != null } }
            assertLeadingCardAction("habit", duration, "Stop & Log")
            capture("timer-running")
            scenario.recreate()
            assertLeadingCardAction("habit", duration, "Stop & Log")
            card("habit", duration, hasText("Stop & Log")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.get(duration)?.timerSessionId == null } }
            assertLeadingCardAction("habit", duration, "Start Timer")
            assertEquals(3, runBlocking { app.habitRepository.logs.first().count { it.habitId == duration } })

            card("habit", quantity, hasText("+1")).performClick()
            compose.waitUntil(10_000) { value(quantity) == 1.0 }
            card("habit", quantity, hasText("Add Amount")).performClick()
            compose.onNodeWithTag("habit-value-input").performTextReplacement("2")
            closeSoftKeyboard()
            assertLoggingFooter("habit-value-save", "Add Amount")
            compose.onNodeWithTag("habit-value-save").performClick()
            waitGone("habit-value-dialog")
            compose.waitUntil(10_000) { value(quantity) == 3.0 }
            card("habit", quantity, hasText("Set Total")).performClick()
            compose.onNodeWithTag("habit-value-input").performTextReplacement("10")
            closeSoftKeyboard()
            assertLoggingFooter("habit-value-save", "Set Total")
            capture("set-total")
            compose.onNodeWithTag("habit-value-save").performClick()
            waitGone("habit-value-dialog")
            compose.waitUntil(10_000) { value(quantity) == 10.0 }

            val name = runBlocking { app.habitRepository.get(checkOff)!!.name }
            card("habit", checkOff, hasContentDescription("Check off habit $name")).assertHeightIsAtLeast(48.dp).performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.logs.first().any { it.habitId == checkOff && it.status == HabitLogStatus.Success } } }
            capture("check-in-saved")
            card("habit", checkOff, hasContentDescription("Mark habit $name incomplete")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.logs.first().none { it.habitId == checkOff && it.status == HabitLogStatus.Success } } }
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithContentDescription("Habits tab").performClick()
            scrollCard("habit", quantity)
            compose.onNodeWithTag("habit-card-status-$quantity", useUnmergedTree = true).assertTextContains("10", substring = true)
            capture("habit-saved-return")
        }
    }

    @Test fun goalLoggingCorrectionAndManualCompletionStaySeparate() {
        val id = runBlocking { app.goalRepository.create(GoalDraft(
            name = "QA progress ${System.nanoTime()}", type = GoalType.ReachValue,
            startDate = app.clock.today(), targetMin = 100.0, baseline = 0.0,
        )) }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            assertLeadingCardAction("goal", id, "Log Progress")
            card("goal", id, hasTestTag("goal-card-complete-$id")).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            capture("goal-ready")
            card("goal", id, hasText("Log Progress")).performClick()
            compose.onNodeWithTag("goal-measurement-value").performTextReplacement("25")
            compose.onNodeWithTag("goal-measurement-note").performTextReplacement("QA original observation")
            closeSoftKeyboard()
            assertLoggingFooter("goal-measurement-save", "Log Progress")
            capture("goal-log")
            compose.onNodeWithTag("goal-measurement-save").performClick()
            waitGone("goal-measurement-dialog")
            val measurementId = runBlocking { app.goalRepository.get(id)!!.measurementId }
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.measurementEntries.first().count { it.measurementId == measurementId } == 1 } }
            assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(id)!!.status })
            compose.onNodeWithTag("goal-card-progress-label-$id", useUnmergedTree = true).assertTextEquals("25%")
            card("goal", id, hasTestTag("goal-card-complete-$id")).performClick()
            compose.onNodeWithTag("goal-completion-dialog").assertIsDisplayed()
            compose.onNodeWithText("This Goal has not reached its measured target.", substring = true).assertIsDisplayed()
            capture("goal-complete-confirm")
            compose.onNodeWithText("Cancel").performClick()
            waitGone("goal-completion-dialog")
            assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(id)!!.status })
            scrollCard("goal", id).performClick()
            compose.onNodeWithTag("entity-inspector-primary-log-progress").assertIsDisplayed()
            capture("goal-inspector")
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            val historyList = hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("goal-detail-surface"))
            compose.onNode(historyList).performScrollToNode(hasText("QA original observation", substring = true))
            compose.onNode(hasText("QA original observation", substring = true) and hasAnyAncestor(hasTestTag("goal-detail-surface"))).performClick()
            compose.onNodeWithTag("goal-measurement-value").performTextReplacement("30")
            closeSoftKeyboard()
            compose.onNodeWithTag("goal-measurement-save").assertTextContains("Save Changes")
            capture("goal-edit")
            compose.onNodeWithTag("goal-measurement-save").performClick()
            waitGone("goal-measurement-dialog")
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.measurementEntries.first().single { it.measurementId == measurementId }.enteredValue == 30.0 } }
            scenario.recreate()
            compose.onNodeWithTag("goal-detail-section-History").assertIsSelected()
            device.pressBack()
            waitGone("goal-detail-surface")
            card("goal", id, hasTestTag("goal-card-complete-$id")).performClick()
            compose.onNodeWithTag("goal-completion-confirm").performTouchInput { click(); click() }
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.get(id)?.status == GoalStatus.Completed } }
            waitGone("goal-completion-dialog")
            compose.onNodeWithTag("goal-destination-History").performClick()
            scrollCard("goal", id).assertIsDisplayed()
            compose.onAllNodesWithTag("goal-primary-action-$id").assertCountEquals(0)
            compose.onAllNodesWithTag("goal-card-complete-$id").assertCountEquals(0)
            assertEquals(1, runBlocking { app.goalRepository.measurementEntries.first().count { it.measurementId == measurementId } })
            capture("goal-completed-history")
        }
    }

    @Test @AndroidFontScale fun largeTextDurationKeyboardAndCancelPreserveDraft() {
        val id = habit(HabitTrackingMode.Duration, "large duration")
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            assertLeadingCardAction("habit", id, "Start Timer")
            card("habit", id, hasText("Enter Duration")).assertIsDisplayed()
            capture("duration-ready")
            card("habit", id, hasText("Enter Duration")).performClick()
            compose.onNodeWithTag("habit-value-input").performClick()
            compose.waitUntil(10_000) { instrumentation.uiAutomation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } }
            compose.onNodeWithTag("habit-value-input").performTextReplacement("45")
            assertLoggingFooter("habit-value-save", "Log Duration")
            capture("duration-keyboard")
            closeSoftKeyboard()
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithText("Keep Editing").performClick()
            compose.onNodeWithTag("habit-value-input").assertTextContains("45")
            scenario.recreate()
            compose.onNodeWithTag("habit-value-input").assertTextContains("45")
            device.pressBack()
            compose.onNodeWithText("Discard Changes").performClick()
            waitGone("habit-value-dialog")
            assertEquals(0, runBlocking { app.habitRepository.logs.first().count { it.habitId == id } })
            card("habit", id, hasText("Enter Duration")).performClick()
            compose.onNodeWithTag("habit-value-input").performTextReplacement("45")
            closeSoftKeyboard()
            compose.onNodeWithTag("habit-value-save").performClick()
            waitGone("habit-value-dialog")
            compose.waitUntil(10_000) { value(id) == 45.0 }
            capture("duration-saved")
        }
    }
}
