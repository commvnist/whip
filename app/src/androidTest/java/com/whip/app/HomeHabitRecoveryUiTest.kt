package com.whip.app

import android.content.Intent
import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.core.HomeSection
import com.whip.app.core.SavedTaskFilter
import com.whip.app.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeHabitRecoveryUiTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app get() = ApplicationProvider.getApplicationContext<WhipApplication>()

    @Before fun prepare() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true, activeAreaScope = "all", hiddenHomeSections = emptySet(), collapsedHomeSections = emptySet()) }
    }
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }
    private fun launch() = launchMainActivity(Intent(app, MainActivity::class.java).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true))
    private fun awaitText(text: String) = compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun awaitFilterDismissed() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Sort, Group & Filter Tasks").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(750L, 5_000L)
    }
    private fun openTaskCalendar() {
        val adaptive = compose.onAllNodesWithContentDescription("Choose view. Selected List").fetchSemanticsNodes().isNotEmpty()
        if (adaptive) {
            compose.onNodeWithContentDescription("Choose view. Selected List").performClick()
        }
        compose.onNodeWithText("Calendar").performClick()
        if (adaptive) {
            compose.onNodeWithContentDescription("Choose view. Selected Calendar").assertExists()
        } else compose.onNodeWithText("Calendar").assertIsSelected()
        // Include Habits precedes the Calendar in the lazy list. At short height
        // the Calendar need not be composed until the user scrolls to it.
        compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasTestTag("task-calendar"))
        compose.onNodeWithTag("task-calendar").assertExists()
    }
    private fun openHabits() {
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Habits tab").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Habits tab").performClick()
    }

    @Test @AndroidFontScale
    fun filteredHomeKeepsRecoveryReachableAndRestoresSavedTasksAtLargeText() {
        runBlocking {
            app.taskRepository.create(TaskDraft("Still due today", scheduleKind = ScheduleKind.Once, date = app.clock.today(), inbox = false))
            app.settingsRepository.update { it.copy(homeSections = listOf(HomeSection.Tasks), savedTaskFilters = listOf(SavedTaskFilter("Only pinned", pinnedOnly = true)), homeTaskFilterName = "Only pinned") }
        }
        launch().use {
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("home-list").performScrollToNode(hasText("Show All Tasks"))
            compose.onNodeWithText("Show All Tasks").assertIsDisplayed()
            compose.onAllNodesWithText("Your Day Is Clear").assertCountEquals(0)
            captureVisualCatalogSurface("ux-upgrades.home-filter-recovery.large")
            compose.onNodeWithText("Show All Tasks").performClick()
            awaitText("Still due today")
            compose.onNodeWithText("Still due today").performScrollTo().assertIsDisplayed()
            check(runBlocking { app.settingsRepository.current().homeTaskFilterName } == null)
        }
    }

    @Test @AndroidFontScale
    fun offScheduleHabitsOpenSavedCollectionAtLargeText() {
        runBlocking { app.habitRepository.create(HabitDraft(name = "Tomorrow practice", startDate = app.clock.today().plusDays(1))) }
        launch().use {
            openHabits()
            awaitText("No Habits Due Today")
            compose.onNodeWithText("View All Habits").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("ux-upgrades.habits-no-due.large")
            compose.onNodeWithText("View All Habits").performClick()
            compose.onNodeWithTag("habit-destination-All").assertIsSelected()
            compose.onNodeWithText("Tomorrow practice").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun emptyAreaCanRecoverExistingHabitsWithoutCreatingDuplicates() {
        val storedScope = runBlocking {
            app.habitRepository.create(HabitDraft(name = "Saved practice", startDate = app.clock.today()))
            val emptyArea = app.areaRepository.create("Quiet space")
            val scope = AreaScope.One(emptyArea).storageKey
            app.settingsRepository.update { it.copy(activeAreaScope = scope) }
            scope
        }
        launch().use {
            openHabits()
            awaitText("No Habits in Quiet space")
            compose.onNodeWithText("Show All Areas").performScrollTo().performClick()
            awaitText("Saved practice")
            compose.onNodeWithText("Saved practice").assertIsDisplayed()
            compose.onNodeWithContentDescription("Area scope: All Areas").assertIsDisplayed()
            check(runBlocking { app.settingsRepository.current().activeAreaScope } == storedScope)
            check(runBlocking { app.habitRepository.habits.first().size } == 1)
        }
    }

    @Test fun firstHabitStillOffersTemplates() {
        launch().use {
            openHabits()
            awaitText("Browse Templates")
            compose.onNodeWithText("Browse Templates").performScrollTo().performClick()
            compose.onNodeWithText("Habit Templates").assertIsDisplayed()
        }
    }

    @Test fun calendarExplainsClearDayAndBoundsLoadedPlanningWindow() {
        val plannedDate = app.clock.today().plusDays(3)
        runBlocking {
            app.taskRepository.create(TaskDraft("Future plan", scheduleKind = ScheduleKind.Once, date = plannedDate, inbox = false))
            app.taskRepository.create(TaskDraft("Other matching date", scheduleKind = ScheduleKind.Once, date = plannedDate.plusDays(1), priority = TaskPriority.High, inbox = false))
            app.taskRepository.create(TaskDraft("Later dated plan", scheduleKind = ScheduleKind.Once, date = app.clock.today().plusDays(45), inbox = false))
            app.settingsRepository.update { it.copy(savedTaskFilters = listOf(SavedTaskFilter("High priority plans", priorities = setOf(TaskPriority.High)))) }
        }
        launch().use {
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Tasks tab").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Upcoming").performClick()
            openTaskCalendar()
            compose.onNodeWithContentDescription("Previous Month").performScrollTo().assertIsNotEnabled()
            compose.onNodeWithText("Tomorrow").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Selected: ${app.clock.today().plusDays(1).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))}")
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("View Upcoming List"))
            compose.onAllNodesWithText("No Matching Tasks").assertCountEquals(0)
            compose.onNodeWithText("View Upcoming List").assertIsDisplayed()
            captureVisualCatalogSurface("ux-upgrades.tasks-calendar-clear")
            compose.onNodeWithText("View Upcoming List").performClick()
            compose.onNodeWithText("Future plan").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("Later dated plan"))
            compose.onNodeWithText("Later dated plan").assertIsDisplayed()
            compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
            compose.onNodeWithText("High priority plans").performScrollTo().performClick()
            awaitFilterDismissed()
            openTaskCalendar()
            if (plannedDate.month != app.clock.today().plusDays(1).month) compose.onNodeWithContentDescription("Next Month").performScrollTo().performClick()
            compose.onNode(hasContentDescription(plannedDate.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL)), substring = true))
                .performScrollTo().performClick()
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("No tasks match your filters on this date. Your other upcoming tasks remain saved."))
            compose.onNodeWithText("No tasks match your filters on this date. Your other upcoming tasks remain saved.").assertIsDisplayed()
            compose.onNodeWithText("View Upcoming List").performScrollTo().performClick()
            compose.onNodeWithText("Other matching date").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Future plan").assertCountEquals(0)
        }
    }

    @Test fun habitInsightsShowRealActivityAndOpenThatHabitsHistory() {
        val habitId = runBlocking {
            val id = app.habitRepository.create(HabitDraft(name = "Reading record", trackingMode = HabitTrackingMode.LogOnly, comparison = TargetComparison.None, startDate = app.clock.today().minusDays(20)))
            app.habitRepository.log(id, null, date = app.clock.today().minusDays(14), note = "Earlier note")
            id
        }
        launch().use {
            openHabits()
            awaitText("Reading record")
            compose.onNode(hasText("Log") and hasAnyAncestor(hasTestTag("habit-card-$habitId"))).performScrollTo().performClick()
            compose.onNodeWithText("Add an Entry").assertIsDisplayed()
            check(compose.onNodeWithTag("habit-value-input").fetchSemanticsNode().config[SemanticsProperties.EditableText].text.isEmpty())
            compose.onNodeWithTag("habit-value-note").performScrollTo().performTextInput("A note without a number")
            compose.onNodeWithText("Add Entry").assertIsEnabled().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("habit-value-dialog").fetchSemanticsNodes().isEmpty() }
            awaitText("Entry recorded today")
            val savedNote = runBlocking { app.habitRepository.logs.first().single { it.habitId == habitId && it.localDate == app.clock.today() } }
            check(savedNote.note == "A note without a number")
            check(savedNote.value == null && savedNote.canonicalValue == null && savedNote.enteredUnitId == null && savedNote.measurementEntryId == null)
            compose.onNodeWithTag("habit-destination-Insights").performClick()
            compose.onNodeWithTag("habit-insights-list").performScrollToNode(hasTestTag("habit-rate-chart"))
            compose.onNodeWithTag("habit-rate-chart").assertIsDisplayed()
            compose.onNodeWithTag("habit-rate-chart").assertContentDescriptionContains("recorded activity chart", substring = true)
            captureVisualCatalogSurface("ux-upgrades.habits-real-insights")
            compose.onNodeWithTag("habit-insights-history-$habitId").performScrollTo().performClick()
            compose.onNodeWithTag("habit-detail-section-History").assertIsSelected()
            compose.onNodeWithText("A note without a number", substring = true).assertExists()
        }
    }

    @Test @AndroidFontScale
    fun denseTaskCalendarKeepsDateAndCountsLegibleAtLargeText() {
        val date = app.clock.today().plusDays(1)
        runBlocking {
            repeat(12) { app.taskRepository.create(TaskDraft("Dense day task $it", scheduleKind = ScheduleKind.Once, date = date, inbox = false)) }
            repeat(3) { app.habitRepository.create(HabitDraft(name = "Projected habit $it", startDate = app.clock.today())) }
            app.settingsRepository.update { it.copy(showHabitsInTaskPlanning = true) }
        }
        launch().use {
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Tasks tab").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Upcoming").performClick()
            openTaskCalendar()
            val denseDate = hasContentDescription("12 tasks, 3 habits", substring = true)
            compose.onNode(denseDate).performScrollTo().assertIsDisplayed().performClick()
            captureVisualCatalogSurface("ux-upgrades.tasks-calendar-dense.large")
            fun assertOneCompleteLine(node: SemanticsNodeInteraction) {
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    getResults(layouts)
                    check(layouts.isNotEmpty()) { "Expected a Calendar text layout" }
                    layouts.forEach { result ->
                        val fontScale = result.layoutInput.density.fontScale
                        val diagnostic = "text=${result.layoutInput.text.text}; lines=${result.lineCount}; " +
                            "overflow=${result.hasVisualOverflow}; widthOverflow=${result.didOverflowWidth}; " +
                            "heightOverflow=${result.didOverflowHeight}; fontScale=$fontScale; size=${result.size}; " +
                            "paragraphWidth=${result.multiParagraph.width}; lineRight=${result.getLineRight(0)}"
                        check(result.lineCount == 1 && !result.hasVisualOverflow) {
                            "Calendar dates/counts must occupy one complete line: $diagnostic"
                        }
                        check(kotlin.math.abs(fontScale - 2f) <= 0.01f) {
                            "Calendar text must use actual 200% scale (tolerance0.01): $diagnostic"
                        }
                    }
                }
            }
            (listOf(date) + (28..date.lengthOfMonth()).map(date::withDayOfMonth)).distinct().forEach { day ->
                assertOneCompleteLine(compose.onNodeWithTag("task-calendar-day-${day.toEpochDay()}", useUnmergedTree = true))
            }
            listOf("12T", "3H").forEach { label ->
                assertOneCompleteLine(compose.onNode(hasText(label) and hasAnyAncestor(denseDate), useUnmergedTree = true))
            }
            compose.onNodeWithText("T Tasks · H Habits").assertExists()
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("Dense day task 0"))
            compose.onNodeWithText("Dense day task 0").assertIsDisplayed()
        }
    }

    @Test fun taskFiltersShowLiveMatchCountAndSavedRecipesBeforeAdvancedCriteria() {
        runBlocking {
            app.taskRepository.create(TaskDraft("Visible filter task", scheduleKind = ScheduleKind.Once, date = app.clock.today(), inbox = false))
            app.settingsRepository.update { it.copy(savedTaskFilters = listOf(SavedTaskFilter("Pinned work", pinnedOnly = true))) }
        }
        launch().use {
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Tasks tab").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
            compose.onNodeWithTag("task-filter-result-count").assertTextContains("1 of 1", substring = true)
            compose.onNodeWithText("Pinned work").performScrollTo().performClick()
            awaitFilterDismissed()
            compose.onNode(hasContentDescription("Filter & Sort Tasks", substring = true)).performClick()
            compose.onNodeWithTag("task-filter-result-count").performScrollTo().assertTextContains("0 of 1", substring = true)
            captureVisualCatalogSurface("ux-upgrades.tasks-filter-feedback")
            compose.onNodeWithText("Reset").performClick()
            compose.onNodeWithTag("task-filter-result-count").performScrollTo().assertTextContains("1 of 1", substring = true)
        }
    }
    @Test @AndroidFontScale
    fun manyTaskFiltersKeepTheResultsViewportReachableAtLargeText() {
        runBlocking {
            app.taskRepository.create(TaskDraft("Workspace filter fixture", scheduleKind = ScheduleKind.Once, date = app.clock.today(), inbox = false))
            app.settingsRepository.update { it.copy(savedTaskFilters = listOf(SavedTaskFilter(
                "Detailed recipe", priorities = setOf(TaskPriority.High, TaskPriority.Urgent),
                tags = setOf("long project reference", "another project reference"), pinnedOnly = true,
                dateMode = "Today", deadlineOnly = true, efforts = setOf(TaskEffort.High),
                maximumDurationMinutes = 90, textQuery = "Workspace filter",
            ))) }
        }
        launch().use {
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Tasks tab").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithContentDescription("Filter & Sort Tasks").performClick()
            compose.onNodeWithText("Detailed recipe").performScrollTo().performClick()
            awaitFilterDismissed()
            captureVisualCatalogSurface("ux-upgrades.tasks-many-filters.large")
            val metrics = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics
            val viewport = compose.onNodeWithTag("task-workspace-list").getUnclippedBoundsInRoot()
            Log.i("HomeHabitRecovery", "manyFilters font200% screen=${metrics.widthPixels}x${metrics.heightPixels}px density=${metrics.density}; results=$viewport")
            compose.onNodeWithTag("task-workspace-list").assertHeightIsAtLeast(96.dp)
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("No Matching Tasks"))
            compose.onNodeWithText("No Matching Tasks").assertIsDisplayed()
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("Pinned"))
            compose.onNodeWithText("Pinned").assertIsDisplayed().performClick()
            compose.onAllNodesWithContentDescription("Remove Pinned").assertCountEquals(0)
        }
    }

}
