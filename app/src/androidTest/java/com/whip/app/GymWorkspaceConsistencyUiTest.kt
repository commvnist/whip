package com.whip.app

import android.content.Intent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ExerciseDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class GymWorkspaceConsistencyUiTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app get() = ApplicationProvider.getApplicationContext<WhipApplication>()

    @Before fun prepare() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(setupCompleted = true, activeAreaScope = "all", themeMode = AppThemeMode.Dark,
                dynamicColor = false)
        }
    }

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test @AndroidFontScale(1f)
    fun libraryChildrenAndEmptyInsightsKeepContextAndReturnActions() = verifyWorkspace(1f)

    @Test @AndroidFontScale
    fun libraryChildrenAndEmptyInsightsKeepContextAndReturnActionsAtLargeText() = verifyWorkspace(2f)

    private fun verifyWorkspace(expectedScale: Float) {
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Library").performClick().assertIsSelected()
            await("gym-library-list")
            assertEquals(expectedScale, app.resources.configuration.fontScale, 0.01f)
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("workspace-context-summary")
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("The context summary must have a rendered text layout", layouts.isNotEmpty())
            layouts.forEach { assertEquals(expectedScale, it.layoutInput.density.fontScale, 0.01f) }

            val geometry = workspaceGeometry()
            val density = app.resources.displayMetrics.density
            assertTrue("Context must reserve the shared normal/enlarged height",
                geometry.getValue("workspace-context-row").height >= maxOf(72f, 40f * expectedScale + 24f) * density)
            val libraryContentTop = compose.onNodeWithTag("gym-library-list").fetchSemanticsNode().boundsInRoot.top
            assertTrue("Library content must follow the pinned context",
                libraryContentTop >= geometry.getValue("workspace-context-row").bottom)
            val profile = if (expectedScale == 1f) "normal" else "native200"
            for (destination in listOf("Routines", "Exercises", "Machines", "Categories", "Tools")) {
                compose.onNodeWithTag("gym-library-list")
                    .performScrollToNode(hasTestTag("gym-library-$destination"))
                compose.onNodeWithTag("gym-library-$destination").performClick()
                val back = compose.onNodeWithTag("gym-library-child-$destination")
                    .assertIsDisplayed().assertContentDescriptionEquals("Back to Gym Library")
                compose.onNodeWithTag("workspace-context-summary").assertTextEquals("Library")
                assertGeometry(geometry, destination)
                val context = geometry.getValue("workspace-context-row")
                val backBounds = back.fetchSemanticsNode().boundsInRoot
                assertTrue("Back must remain wholly inside the pinned context in $destination",
                    backBounds.top >= context.top && backBounds.bottom <= context.bottom)
                if (destination == "Exercises") {
                    captureVisualCatalogSurface("workspace-consistency.gym.library-child.$profile")
                }
                back.performClick()
                compose.onNodeWithTag("gym-destination-Library").assertIsSelected()
                compose.onNodeWithTag("gym-library-list").assertIsDisplayed()
                assertGeometry(geometry, "Back from $destination")
            }

            compose.onNodeWithTag("gym-destination-Insights").performClick().assertIsSelected()
            await("gym-progress-list")
            assertInsightsContext(geometry)
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("No Exercises to Track"))
            compose.onNodeWithText("No Exercises to Track").assertIsDisplayed()
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Open Exercise Library"))
            compose.onNodeWithText("Open Exercise Library").assertIsDisplayed()
            captureVisualCatalogSurface("workspace-consistency.gym.insights-no-exercises.$profile")
            compose.onNodeWithText("Open Exercise Library").performClick()
            compose.onNodeWithTag("gym-library-child-Exercises").assertIsDisplayed()
            assertGeometry(geometry, "Open Exercise Library")

            runBlocking { app.gymRepository.createExercise(ExerciseDraft("Workspace press")) }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Workspace press").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("gym-destination-Insights").performClick().assertIsSelected()
            assertInsightsContext(geometry)
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("No Progress Data Yet"))
            compose.onNodeWithText("No Progress Data Yet").assertIsDisplayed()
            compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Start a Workout"))
            compose.onNodeWithText("Start a Workout").assertIsDisplayed()
            captureVisualCatalogSurface("workspace-consistency.gym.insights-no-history.$profile")
            compose.onNodeWithText("Start a Workout").performClick()
            compose.onNodeWithTag("gym-destination-Workout").assertIsSelected()
            compose.onNodeWithTag("workspace-context-summary").assertTextEquals("Ready to train")
            assertGeometry(geometry, "Start a Workout")
        }
    }

    private fun assertInsightsContext(expected: Map<String, Rect>) {
        compose.onNodeWithTag("workspace-context-summary").assertTextEquals("Trends from completed workouts")
        compose.onNodeWithText("Progress", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Trends from completed workouts.", substring = false).assertDoesNotExist()
        assertGeometry(expected, "Insights")
    }

    private fun workspaceGeometry() = listOf(
        "workspace-top-app-bar", "gym-workspace-navigation", "workspace-context-row",
    ).associateWith { tag ->
        compose.onAllNodesWithTag(tag).assertCountEquals(1)
        compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    }

    private fun assertGeometry(expected: Map<String, Rect>, destination: String) {
        val actual = workspaceGeometry()
        expected.forEach { (tag, bounds) ->
            assertEquals("$tag moved in $destination", bounds, actual.getValue(tag))
        }
    }

    private fun await(tag: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}
