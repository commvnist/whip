package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.HomeSection
import com.whip.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GoalProgressJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun smallProgressStaysVisibleThroughHomeDetailsAndArchivedHistory() {
        val id = runBlocking { prepare(); goal("Read a little every day", 0.05) }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.waitUntil(15_000) {
                compose.onAllNodesWithContentDescription("Open goal details for Read a little every day")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("goal-card-$id"))
            captureVisualCatalogSurface("goals.progress.home")
            assertCard(id, "0.5% complete")
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-$id").performScrollTo()
            assertCard(id, "0.5% complete")
            compose.onNodeWithTag("goal-expand-$id").performClick()
            compose.onNodeWithContentDescription("Collapse goal Read a little every day").assertIsDisplayed()
            compose.onAllNodes(hasText("0.5% complete") and hasAnyAncestor(hasTestTag("goal-card-$id")),
                useUnmergedTree = true).assertCountEquals(1)
            captureVisualCatalogSurface("goals.progress.expanded")
            compose.onNodeWithTag("goal-card-title-$id", useUnmergedTree = true).performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals("0.5% complete")
            captureVisualCatalogSurface("goals.progress.overview")
            compose.onNodeWithText("Trend Data Table").performScrollTo().performClick()
            compose.onNodeWithText("progress 0.5%", substring = true).performScrollTo().assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("progress 0.5%", substring = true).performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("goals.progress.trend-table")
            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNodeWithTag("goal-destination-Insights").performClick()
            compose.onNodeWithTag("goal-insight-$id").performScrollTo()
            compose.onNode(hasText("0.5% complete", substring = true) and hasAnyAncestor(hasTestTag("goal-insight-$id")),
                useUnmergedTree = true).assertIsDisplayed()
            captureVisualCatalogSurface("goals.progress.insights")
            compose.onNodeWithTag("goal-insight-$id").performClick()
            compose.onNodeWithTag("goal-detail-section-Options").performClick()
            compose.onNodeWithText("Abandon Goal").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("entity-inspector-close").fetchSemanticsNodes().isEmpty() }
            assertEquals(GoalStatus.Abandoned, runBlocking { app.goalRepository.get(id)?.status })
            compose.onAllNodesWithTag("goal-celebration").assertCountEquals(0)
            compose.onNodeWithTag("goal-destination-History").performClick()
            compose.onNodeWithTag("goal-card-$id").performScrollTo().performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals("0.5% complete")
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            val snapshot = runBlocking { app.goalRepository.closureSnapshots.first().single { it.goalId == id } }
            assertEquals(0.005, requireNotNull(snapshot.progress), 0.00000001)
            compose.onNodeWithTag("goal-closure-history-${snapshot.id}").performScrollTo()
                .assertTextContains("0.5% progress", substring = true)
            captureVisualCatalogSurface("goals.progress.closure")
            compose.onNodeWithTag("goal-detail-section-Options").performClick()
            compose.onNodeWithText("Archive Goal").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("entity-inspector-close").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("goal-destination-Archived").performClick()
            compose.onNodeWithTag("goal-card-$id").performScrollTo().performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals("0.5% complete")
            scenario.recreate()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals("0.5% complete")
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            compose.onNodeWithTag("goal-closure-history-${snapshot.id}").performScrollTo()
                .assertTextContains("0.5% progress", substring = true)
            captureVisualCatalogSurface("goals.progress.archived-history")
            assertEquals(snapshot, runBlocking { app.goalRepository.closureSnapshots.first().single { it.id == snapshot.id } })
        }
    }

    @Test fun tinyAndNearlyCompleteGoalsRemainDistinctFromTheirEndpoints() {
        val ids = runBlocking {
            prepare()
            listOf(goal("A small beginning", 0.0005), goal("Almost at the target", 9.99999),
                goal("Not started", 0.0), goal("Target reached", 10.0))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            val expected = listOf("<0.1% complete", ">99.9% complete", "0% complete", "100% complete")
            ids.zip(expected).forEachIndexed { index, (id, text) ->
                compose.onNodeWithTag("goal-card-$id").performScrollTo()
                if (index == 0) captureVisualCatalogSurface("goals.progress.endpoints")
                assertCard(id, text)
            }
            compose.onNodeWithTag("goal-card-${ids[1]}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals(">99.9% complete")
            scenario.recreate()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals(">99.9% complete")
            captureVisualCatalogSurface("goals.progress.near-target")
            assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(ids[1])?.status })
        }
    }

    @Test fun savedGoalCompletionKeepsCardFourSecondsAndTapOnlyDismissesCard() {
        val ids = runBlocking {
            prepare()
            listOf(goal("Finish the chapter", 4.0), goal("Make the appointment", 2.0),
                goal("Pack the bag", 3.0))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-${ids[0]}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-detail-section-Options").performClick()
            val complete = compose.onNodeWithText("Complete Goal").performScrollTo()
            compose.mainClock.autoAdvance = false
            try {
                complete.performClick()
                compose.waitUntil(10_000) {
                    compose.mainClock.advanceTimeByFrame()
                    compose.onAllNodesWithTag("goal-celebration").fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("goal-celebration-card").assertIsDisplayed().assertHasClickAction()
                compose.mainClock.advanceTimeBy(3_500)
                compose.onNodeWithTag("goal-celebration-card").assertIsDisplayed()
                compose.mainClock.advanceTimeBy(650)
                compose.waitUntil(5_000) {
                    compose.mainClock.advanceTimeByFrame()
                    compose.onAllNodesWithTag("goal-celebration").fetchSemanticsNodes().isEmpty()
                }
            } finally {
                compose.mainClock.autoAdvance = true
            }
            assertEquals(GoalStatus.Completed, runBlocking { app.goalRepository.get(ids[0])?.status })
            compose.onNodeWithTag("goal-destination-History").performClick()
            compose.onAllNodesWithTag("goal-celebration").assertCountEquals(0)
            compose.onNodeWithTag("goal-card-${ids[0]}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals("40% complete")
            compose.onNodeWithTag("entity-inspector-close").performClick()

            compose.onNodeWithTag("goal-destination-Active").performClick()
            compose.onNodeWithTag("goal-card-${ids[1]}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-detail-section-Options").performClick()
            val secondComplete = compose.onNodeWithText("Complete Goal").performScrollTo()
            compose.mainClock.autoAdvance = false
            try {
                secondComplete.performClick()
                compose.waitUntil(10_000) {
                    compose.mainClock.advanceTimeByFrame()
                    compose.onAllNodesWithTag("goal-celebration").fetchSemanticsNodes().isNotEmpty()
                }
                compose.mainClock.advanceTimeBy(500)
                compose.onNodeWithTag("goal-celebration").performTouchInput {
                    click(androidx.compose.ui.geometry.Offset(40f, 500f))
                }
                compose.waitUntil(2_000) {
                    compose.mainClock.advanceTimeByFrame()
                    compose.onAllNodesWithTag("goal-celebration-card").fetchSemanticsNodes().isEmpty()
                }
                compose.onNodeWithTag("goal-destination-History").performClick()
                compose.onNodeWithTag("goal-celebration").assertIsDisplayed()
                compose.mainClock.advanceTimeBy(3_000)
                compose.onNodeWithTag("goal-celebration").assertIsDisplayed()
                compose.mainClock.advanceTimeBy(700)
                compose.waitUntil(5_000) {
                    compose.mainClock.advanceTimeByFrame()
                    compose.onAllNodesWithTag("goal-celebration").fetchSemanticsNodes().isEmpty()
                }
            } finally {
                compose.mainClock.autoAdvance = true
            }
            assertEquals(GoalStatus.Completed, runBlocking { app.goalRepository.get(ids[1])?.status })

            app.settingsRepository.update { current -> current.copy(goalCelebrationEnabled = false) }
            compose.waitForIdle()
            compose.onNodeWithTag("goal-destination-Active").performClick()
            compose.onNodeWithTag("goal-card-${ids[2]}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-detail-section-Options").performClick()
            compose.onNodeWithText("Complete Goal").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.get(ids[2])?.status == GoalStatus.Completed } }
            compose.onAllNodesWithTag("goal-celebration").assertCountEquals(0)
        }
    }

    private fun assertCard(id: Long, expected: String) {
        compose.onNodeWithTag("goal-card-status-$id", useUnmergedTree = true).assertTextEquals(expected)
    }

    private suspend fun prepare() {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark,
            dynamicColor = false, homeSections = listOf(HomeSection.Goals) + HomeSection.entries.filterNot { it == HomeSection.Goals }) }
    }

    private suspend fun goal(name: String, value: Double): Long {
        val id = app.goalRepository.create(GoalDraft(name, type = GoalType.ReachValue, startDate = app.clock.today(),
            targetMin = 10.0, precision = 6))
        app.goalRepository.recordMeasurement(id, value, app.clock.today())
        return id
    }
}
