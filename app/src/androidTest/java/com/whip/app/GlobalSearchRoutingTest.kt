package com.whip.app

import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import android.graphics.Rect
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.ScheduleKind
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlobalSearchRoutingTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test
    @AndroidFontScale
    fun largeTextSearchRetainsQueryFiltersAndResultsAcrossRecreation() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val taskId = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true, dynamicColor = false) }
            app.trackRepository.create(TrackDraft(
                name = "Native search journal",
                fields = listOf(TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true)),
            ))
            app.taskRepository.create(TaskDraft(
                title = "Native search report", scheduleKind = ScheduleKind.Once, date = app.clock.today(),
            ))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("workspace-search-action").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("workspace-search-action").performClick()
            waitForSearchQuery()
            compose.onNodeWithTag("unified-search-query").performTextReplacement("Native search")
            waitForResultCount(1)
            awaitKeyboard()
            compose.onNodeWithTag("unified-search-results-list")
                .performScrollToNode(hasTestTag("unified-search-result-Task-$taskId"))
            compose.onNode(
                hasText("Native search report") and hasAnyAncestor(hasTestTag("unified-search-results-list")),
                useUnmergedTree = true,
            ).performScrollTo()
            captureNativeSearch(app, "shared.search.native-large")
            assertNativeResultAboveKeyboard("Native search report")

            scrollControlIntoView("unified-search-all-whip")
            compose.onNodeWithTag("unified-search-all-whip").performClick()
            waitForResultCount(2)
            scrollControlIntoView("search-filter-disclosure")
            compose.onNodeWithTag("search-filter-disclosure").performClick()
            val filterListTag = if (compose.onAllNodesWithTag("unified-search-filter-pane").fetchSemanticsNodes().isNotEmpty()) {
                "unified-search-filter-pane"
            } else "unified-search-results-list"
            compose.onNodeWithTag(filterListTag).performScrollToNode(hasTestTag("search-terms-Match All"))
            if (compose.onAllNodesWithTag("search-terms-Match Any").fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag("search-terms-Match All").performClick()
            }
            compose.onNodeWithTag("search-terms-Match Any").performClick()
            compose.onNodeWithTag("unified-search-query").performClick()
            compose.onNodeWithTag("unified-search-query").performTextReplacement("Native missing")
            waitForResultCount(2)
            compose.onNodeWithTag(filterListTag).performScrollToNode(hasTestTag("search-terms-Match Any"))
            captureNativeSearch(app, "shared.search.native-filters-large")

            scenario.recreate()
            waitForSearchQuery()
            awaitKeyboard()
            compose.onNodeWithTag("unified-search-query").assertTextContains("Native missing")
            scrollControlIntoView("search-filter-disclosure")
            compose.onNodeWithText("Filters (2)").assertIsDisplayed()
            waitForResultCount(2)
            captureNativeSearch(app, "shared.search.native-recreated-large")
            compose.onNodeWithTag("search-filter-disclosure").performClick()
            compose.onNodeWithTag("unified-search-results-list")
                .performScrollToNode(hasTestTag("unified-search-result-Task-$taskId"))
            compose.onNodeWithTag("unified-search-result-Task-$taskId").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("entity-inspector-header").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText("Native search report") and hasAnyAncestor(hasTestTag("entity-inspector-header")))
                .assertIsDisplayed()
        }
    }

    private fun waitForSearchQuery() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-query").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun assertNativeResultAboveKeyboard(title: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        device.waitForIdle()
        val ime = Rect()
        checkNotNull(instrumentation.uiAutomation.windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }).getBoundsInScreen(ime)
        val result = device.findObjects(By.text(title)).singleOrNull()?.visibleBounds
        val layout = compose.onNode(
            hasText(title) and hasAnyAncestor(hasTestTag("unified-search-results-list")),
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val density = instrumentation.targetContext.resources.displayMetrics.density
        val pane = compose.onNodeWithTag("unified-search-results-pane").getUnclippedBoundsInRoot()
        assertTrue("Result must be visibly reachable above the keyboard: $result versus $ime; title $layout; pane $pane; density $density",
            result != null && result.height() >= (layout.bottom - layout.top).value * density - 1f && result.bottom <= ime.top)
    }

    private fun waitForResultCount(count: Int) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Results · $count").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodes(hasContentDescription("$count search result", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollControlIntoView(tag: String) {
        if (compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() || !compose.onNodeWithTag(tag).isDisplayed()) {
            compose.onNodeWithTag("unified-search-results-list").performScrollToNode(hasTestTag(tag))
        }
    }

    private fun awaitKeyboard() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.waitUntil(10_000) {
            instrumentation.uiAutomation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        }
        compose.waitForIdle()
        instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
        UiDevice.getInstance(instrumentation).waitForIdle()
    }

    private fun captureNativeSearch(app: WhipApplication, id: String) {
        awaitKeyboard()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            captureVisualCatalogSurface(id)
        } else {
            val device = UiDevice.getInstance(instrumentation)
            compose.waitForIdle()
            instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
            device.waitForIdle()
            val directory = checkNotNull(app.getExternalFilesDir("native-search"))
            check(directory.isDirectory || directory.mkdirs())
            check(device.takeScreenshot(File(directory, "$id.png")))
            device.dumpWindowHierarchy(File(directory, "$id.xml"))
        }
        compose.assertDialogFontScale()
        compose.assertEditorHeaderVisibleWithKeyboard("Search", "Close Search", titleTag = "unified-search-title")
        val device = UiDevice.getInstance(instrumentation)
        val status = device.findObject(By.res("com.android.systemui", "status_bar"))?.visibleBounds
        val title = checkNotNull(device.findObject(By.text("Search"))).visibleBounds
        assertTrue("Search heading must stay below the status bar: $title versus $status",
            status == null || title.top >= status.bottom)
        if (status != null && status.height() > 0) {
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                assertEquals("Search must paint its status-bar backdrop with the workspace background",
                    bitmap.getPixel(bitmap.width / 2, title.top - 1),
                    bitmap.getPixel(bitmap.width / 2, status.centerY()))
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test fun everyNonTaskSearchDomainLandsOnTheExactActiveOrArchivedRecord() {
        runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val habitId = app.habitRepository.create(HabitDraft(name = "Searchable archived habit", startDate = app.clock.today()))
        app.habitRepository.setArchived(habitId, true)
        val goalId = app.goalRepository.create(GoalDraft(name = "Searchable archived goal", type = GoalType.ReachValue, targetMin = 10.0, startDate = app.clock.today()))
        app.goalRepository.setStatus(goalId, GoalStatus.Archived)
        val exerciseId = app.gymRepository.createExercise(ExerciseDraft("Searchable archived exercise"))
        val workoutId = app.gymRepository.startWorkout("Searchable discarded workout")
        app.gymRepository.addExerciseToWorkout(workoutId, exerciseId)
        app.gymRepository.discardWorkout(workoutId)
        val routineId = app.routineRepository.createRoutine(
            RoutineDraft("Searchable archived routine", days = listOf(RoutineDayDraft("Day", listOf(RoutineExerciseDraft(exerciseId))))),
        )
        val trackId = app.trackRepository.create(
            TrackDraft(
                name = "Searchable archived track",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
                ),
            ),
        )
        app.trackRepository.setArchived(trackId, true)
        app.routineRepository.setRoutineArchived(routineId, true)
        app.gymRepository.setExerciseArchived(exerciseId, true)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        val intent = Intent(app, MainActivity::class.java).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            // Compact navigation labels the destination "Home"; the expanded
            // Fold rail does not. Wait for the action this journey actually
            // needs so launch readiness is layout-independent.
            compose.waitUntil(20_000) {
                SEARCH_DESCRIPTIONS.any { description ->
                    compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
                } || compose.onAllNodesWithTag("workspace-search-action").fetchSemanticsNodes().isNotEmpty()
            }

            searchFor("Searchable archived habit")
            compose.onNodeWithText("Archived Habits").assertIsDisplayed()
            compose.onNodeWithText("Restore").assertIsDisplayed()

            searchFor("Searchable archived goal")
            compose.onNodeWithTag("goal-detail-section-Options").performClick()
            compose.onNodeWithText("Delete Permanently").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Goal details").performClick()

            searchFor("Searchable archived exercise")
            compose.onNodeWithText("Duplicate").assertIsDisplayed()
            compose.onNodeWithContentDescription("Close Exercise details").performClick()

            searchFor("Searchable discarded workout")
            compose.onNodeWithText("Workout History").assertIsDisplayed()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Restore to History").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Restore to History").performScrollTo().assertIsDisplayed()

            searchFor("Searchable archived routine")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Searchable archived routine").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Searchable archived routine").assertIsDisplayed()

            searchFor("Searchable archived track")
            compose.onNodeWithTag("track-workspace-destination-Archived").assertIsSelected()
            compose.onAllNodesWithText("Searchable archived track")[0].assertIsDisplayed()
        }
        }
    }

    private fun searchFor(title: String) {
        // A detail can remain in the expanded support pane after a result is
        // opened. Return to the stable app shell before starting the next
        // cross-domain search instead of inheriting that detail's local scope.
        if (compose.onAllNodesWithContentDescription("Go to Home").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Go to Home").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithTag("workspace-search-action").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("unified-search-query").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Search All Whip").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Search All Whip").performClick()
        }
        // Search state is intentionally retained while navigating an expanded
        // pane. Replace the prior query instead of appending to it.
        compose.onNodeWithTag("unified-search-query").performTextReplacement(title.removePrefix("Searchable "))
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(title).assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Search").fetchSemanticsNodes().isEmpty()
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        val SEARCH_DESCRIPTIONS = listOf(
            "Search All Whip Data",
            "Search Habits",
            "Search Goals",
            "Search Tracks & Entries",
            "Search Gym",
            "Search Tasks & Steps",
        )
    }
}
