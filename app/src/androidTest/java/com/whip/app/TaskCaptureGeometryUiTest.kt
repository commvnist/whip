package com.whip.app

import android.content.Intent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.SavedTaskFilter
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.ui.WhipPageContentPadding
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Real route restoration must not let optional controls move inline capture. */
@RunWith(AndroidJUnit4::class)
class TaskCaptureGeometryUiTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app get() = ApplicationProvider.getApplicationContext<WhipApplication>()
    private val savedView = SavedTaskFilter(
        name = "Capture geometry", destination = "All", pinnedOnly = true,
        textQuery = "Capture match", sortMode = "Title", sortDescending = true,
    )
    private val emptyView = savedView.copy(name = "Empty capture geometry", textQuery = "No matching title")
    private val draft = "Keep this capture draft"

    @Before fun prepare() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Dark,
                savedTaskFilters = listOf(savedView, emptyView))
        }
        listOf("Capture match A", "Capture match Z", "Other task").forEach { title ->
            val id = app.taskRepository.create(TaskDraft(title, scheduleKind = ScheduleKind.Once,
                date = app.clock.today(), inbox = false))
            if (title.startsWith("Capture match")) app.taskRepository.setPinned(id, true)
        }
    }

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test @AndroidFontScale(1f)
    fun addTaskBoundsStayAlignedAcrossRoutes() = verifyGeometry(1f, "normal", restoredViews = false)

    @Test @AndroidFontScale
    fun addTaskBoundsStayAlignedAcrossRoutesAtNative200() = verifyGeometry(2f, "native200", restoredViews = false)

    @Test @AndroidFontScale(1f)
    fun addTaskBoundsStayAlignedWithRestoredViews() = verifyGeometry(1f, "normal", restoredViews = true)

    @Test @AndroidFontScale
    fun addTaskBoundsStayAlignedWithRestoredViewsAtNative200() = verifyGeometry(2f, "native200", restoredViews = true)

    private fun verifyGeometry(fontScale: Float, suffix: String, restoredViews: Boolean) {
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("primary-navigation-Tasks").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("primary-navigation-Tasks").performClick()
            awaitWorkspace()
            if (!restoredViews) {
                val empty = geometry(fontScale, "")
                compose.onNodeWithTag("task-destination-Today").performClick()
                assertEquals("Idle Today capture must align with Tasks", empty, geometry(fontScale, ""))
                compose.onNodeWithTag("task-destination-Tasks").performClick()
            }
            compose.onNodeWithTag("task-quick-capture").performTextReplacement(draft)
            closeSoftKeyboard()
            val expected = geometry(fontScale)
            if (!restoredViews) {
                captureVisualCatalogSurface("task-capture.tasks.$suffix")
                compose.onNodeWithTag("task-destination-Today").performClick()
                assertEquals("Today must align with Tasks", expected, geometry(fontScale))
                captureVisualCatalogSurface("task-capture.today.$suffix")
                return@use
            }

            applySavedView(savedView.name)
            assertSavedFilters(savedView.textQuery)
            assertEquals("A saved query, filter and sort must not move capture", expected, geometry(fontScale))
            compose.onNodeWithTag("task-destination-Today").performClick()
            compose.onNodeWithTag("task-destination-Tasks").performClick()
            scenario.recreate()
            awaitWorkspace()
            assertSavedFilters(savedView.textQuery)
            assertEquals("Restored route and activity must retain capture geometry", expected, geometry(fontScale))
            captureVisualCatalogSurface("task-capture.restored.$suffix")

            applySavedView(emptyView.name)
            assertSavedFilters(emptyView.textQuery)
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText("No Matching Tasks"))
            compose.onNodeWithText("No Matching Tasks").assertIsDisplayed()
            assertEquals("Filtered empty state must retain capture geometry", expected, geometry(fontScale))
            captureVisualCatalogSurface("task-capture.filtered-empty.$suffix")
        }
    }

    private fun awaitWorkspace() = compose.waitUntil(10_000) {
        compose.onAllNodesWithTag("task-workspace-list").fetchSemanticsNodes().isNotEmpty()
    }

    private fun applySavedView(name: String) {
        compose.onNodeWithContentDescription("More task list actions").performClick()
        compose.onNodeWithText("Saved Views").performClick()
        compose.onNodeWithText(name).performScrollTo().performClick()
    }

    private fun assertSavedFilters(query: String) {
        val list = compose.onNodeWithTag("task-workspace-list")
        list.performScrollToNode(hasText("Query: $query"))
        compose.onNodeWithText("Query: $query").assertIsDisplayed()
        list.performScrollToNode(hasText("Pinned") and hasAnyAncestor(hasTestTag("active-filter-row")))
        compose.onNode(hasText("Pinned") and hasAnyAncestor(hasTestTag("active-filter-row"))).assertIsDisplayed()
        list.performScrollToNode(hasText("Sorted by Title · Descending"))
        compose.onNodeWithText("Sorted by Title · Descending").assertIsDisplayed()
        val action = compose.onNodeWithContentDescription("Filter & Sort Tasks, 2 active")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val badge = compose.onNodeWithTag("page-action-badge", useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Filter indicator must fit inside its stable action",
            badge.left >= action.left && badge.right <= action.right &&
                badge.top >= action.top && badge.bottom <= action.bottom)
    }

    private data class Geometry(val context: Rect, val capture: Rect)

    private fun geometry(expectedScale: Float, expectedText: String = draft): Geometry {
        // Scope and filter helpers intentionally scroll to controls below capture.
        // Compare the same viewport without changing production route scroll restoration.
        compose.onNodeWithTag("task-workspace-list").performScrollToIndex(0)
        compose.waitForIdle()
        val capture = compose.onNodeWithTag("task-quick-capture").assertIsDisplayed().assertTextContains(expectedText)
        capture.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
            val results = mutableListOf<TextLayoutResult>()
            getResults(results)
            assertTrue("Expected actual inline text layout", results.isNotEmpty())
            results.forEach { assertEquals(expectedScale, it.layoutInput.density.fontScale, 0.01f) }
        }
        val contextBounds = compose.onNodeWithTag("workspace-context-row").fetchSemanticsNode().boundsInRoot
        val captureBounds = capture.fetchSemanticsNode().boundsInRoot
        val fullBounds = capture.getUnclippedBoundsInRoot()
        with(compose.density) {
            assertEquals("Complete capture top must be visible", fullBounds.top.toPx(), captureBounds.top, 0.51f)
            assertEquals("Complete capture bottom must be visible", fullBounds.bottom.toPx(), captureBounds.bottom, 0.51f)
        }
        // Nonempty input retains the existing 8dp clearance for its floating label.
        val floatingLabelClearance = if (expectedText.isNotEmpty()) 8.dp else 0.dp
        val expectedGap = with(compose.density) {
            (WhipPageContentPadding.calculateTopPadding() + floatingLabelClearance).toPx()
        }
        assertEquals("Add Task must follow workspace context with shared page and floating-label padding",
            expectedGap, captureBounds.top - contextBounds.bottom, 0.51f)
        assertEquals("Capture must align with the context's left edge", contextBounds.left, captureBounds.left, 0.01f)
        assertEquals("Capture must align with the context's right edge", contextBounds.right, captureBounds.right, 0.01f)
        return Geometry(contextBounds, captureBounds)
    }
}
