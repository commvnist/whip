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

    private fun inspectorNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("goal-detail-surface")))
            .performScrollToNode(matcher)
        return compose.onNode(matcher and hasAnyAncestor(hasTestTag("goal-detail-surface")))
    }

    @Test fun backfilledGoalHistoryKeepsTheLaterObservationCurrentAfterRecreation() {
        val id = runBlocking {
            prepare()
            val today = app.clock.today()
            val id = app.goalRepository.create(GoalDraft("Observation order", type = GoalType.ReachValue,
                startDate = today.minusDays(2), targetMin = 100.0, precision = 1))
            app.goalRepository.recordMeasurement(id, 75.0, today, timestamp = app.clock.now().minusSeconds(60))
            app.goalRepository.recordMeasurement(id, 80.0, today.minusDays(1), timestamp = app.clock.now())
            id
        }
        val originalEntries = runBlocking { app.goalRepository.measurementEntries.first() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-$id").performScrollTo().performClick()
            inspectorNode(hasText("Current 75.0 → target 100.0")).assertIsDisplayed()
            inspectorNode(hasText("Trend Data Table")).performClick()
            inspectorNode(hasText("value 75.0, progress 75%", substring = true)).assertIsDisplayed()
            scenario.recreate()
            inspectorNode(hasText("value 75.0, progress 75%", substring = true)).assertIsDisplayed()
            captureVisualCatalogSurface("product-audit.goals.backfilled-current")
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            inspectorNode(hasText("80")).assertIsDisplayed()
            inspectorNode(hasText("75")).assertIsDisplayed()
        }
        assertEquals(originalEntries, runBlocking { app.goalRepository.measurementEntries.first() })
    }

    @Test fun windowedInsightsMatchCurrentProgressAndRetainExcludedHistory() {
        val id = runBlocking {
            prepare()
            val today = app.clock.today()
            val id = app.goalRepository.create(GoalDraft("Rolling distance", type = GoalType.MeetAverage,
                dimension = UnitDimension.Distance, unitId = "kilometre", precision = 1, targetMin = 50.0,
                startDate = today.minusDays(20), aggregationPeriod = GoalAggregationPeriod.RollingDays, rollingDays = 7))
            listOf(21L to 999.0, 10L to 1000.0, 2L to 10.0, 0L to 30.0).forEach { (days, value) ->
                app.goalRepository.recordMeasurement(id, value, today.minusDays(days))
            }
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-$id").performScrollTo().performClick()
            inspectorNode(hasText("Current 20.0 km → target 50.0 km")).assertIsDisplayed()
            inspectorNode(hasText("Trend Data Table")).performClick()
            inspectorNode(hasText("value 20.0 km, progress 40%", substring = true)).assertIsDisplayed()
            inspectorNode(hasText("value 10.0 km, progress 20%", substring = true)).assertIsDisplayed()
            inspectorNode(hasText("The trend shows observed days.", substring = true)).assertIsDisplayed()
            captureVisualCatalogSurface("ux-audit-2.goals.windowed-trend")
            scenario.recreate()
            inspectorNode(hasText("value 20.0 km, progress 40%", substring = true)).assertIsDisplayed()
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            inspectorNode(hasText("999 km")).assertIsDisplayed()
            captureVisualCatalogSurface("ux-audit-2.goals.excluded-history")
        }
        assertEquals(4, runBlocking { app.goalRepository.measurementEntries.first().count { it.measurementId == app.goalRepository.get(id)!!.measurementId } })
    }

    @Test fun datedTrendShowsItsMeasureAndEarlierDaysRemainReachable() {
        val id = runBlocking {
            prepare()
            val id = app.goalRepository.create(GoalDraft("Read across the season", startDate = app.clock.today().minusDays(90),
                type = GoalType.ReachValue, targetMin = 100.0, precision = 1))
            repeat(31) { index -> app.goalRepository.recordMeasurement(id, index + 1.0, app.clock.today().minusDays(if (index == 30) 0L else (90 - index).toLong())) }
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-status-$id", useUnmergedTree = true).assertTextContains("31 → 100", substring = true)
            runBlocking { app.goalRepository.setStatus(id, GoalStatus.Paused) }
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("goal-card-status-$id") and hasText("Paused", substring = true), useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("goal-card-status-$id", useUnmergedTree = true).assertTextContains("Paused", substring = true)
            compose.onNodeWithTag("goal-card-$id").performClick()
            inspectorNode(hasContentDescription("Spaced by date.", substring = true)).assertIsDisplayed()
            captureVisualCatalogSurface("ux-upgrades.goals.dated-trend")
            inspectorNode(hasText("Trend Data Table")).performClick()
            inspectorNode(hasTestTag("goal-trend-show-earlier")).performClick()
            inspectorNode(hasText("value 1.0, progress 1%", substring = true)).assertIsDisplayed()
            scenario.recreate()
            inspectorNode(hasText("value 1.0, progress 1%", substring = true)).assertIsDisplayed()
            captureVisualCatalogSurface("ux-upgrades.goals.earlier-trend-days")
        }
    }

    @Test fun poundGoalTrendValuesTargetsAndRatesKeepTheirSelectedUnit() {
        val id = runBlocking {
            prepare()
            val today = app.clock.today()
            val id = app.goalRepository.create(GoalDraft("Weight in pounds", type = GoalType.ReduceValue,
                dimension = UnitDimension.Mass, unitId = "pound", precision = 1,
                baseline = 180.0, targetMin = 150.0, startDate = today.minusDays(1)))
            app.goalRepository.recordMeasurement(id, 174.0, today.minusDays(1))
            app.goalRepository.recordMeasurement(id, 172.0, today)
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-$id").performScrollTo().performClick()
            compose.onNodeWithText("150.0 lb").performScrollTo().assertIsDisplayed()
            inspectorNode(hasText("-2.0 lb per day")).assertIsDisplayed()
            compose.onNodeWithText("Trend Data Table").performScrollTo().performClick()
            fun assertTrendRow(value: String) {
                val row = hasText("value $value lb", substring = true)
                compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("goal-detail-surface")))
                    .performScrollToNode(row)
                compose.onNode(row).assertIsDisplayed()
            }
            assertTrendRow("174.0")
            assertTrendRow("172.0")
            scenario.recreate()
            assertTrendRow("172.0")
            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNodeWithTag("goal-destination-Insights").performClick()
            compose.onNodeWithTag("goal-insights-list").performScrollToNode(hasText("Daily Rate"))
            compose.onNodeWithText("Daily Rate").assertIsDisplayed()
            compose.onNodeWithTag("goal-insights-list").performScrollToNode(hasText("-2.0 lb per day"))
            compose.onNodeWithText("-2.0 lb per day").assertIsDisplayed()
        }
        val saved = runBlocking { app.goalRepository.get(id) }!!
        assertEquals(68.0388555, saved.targetMin!!, 0.00000001)
    }

    @Test fun milestoneInsightsDescribeWeightedProgressAndRetainClosedOutcome() {
        val id = runBlocking {
            prepare()
            val id = app.goalRepository.create(GoalDraft("Ship the project", type = GoalType.WeightedMilestones,
                startDate = app.clock.today(), milestones = listOf(GoalMilestoneDraft("Build", 3.0), GoalMilestoneDraft("Release", 1.0))))
            val milestone = app.goalRepository.milestones.first().single { it.goalId == id && it.name == "Build" }
            app.goalRepository.toggleMilestone(milestone.id, true)
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-destination-Insights").performClick()
            compose.onNodeWithText("1 of 2 milestones complete", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithText("75% complete", useUnmergedTree = true).assertIsDisplayed()
            compose.onAllNodesWithText("Log on at least two days for a trend line.", useUnmergedTree = true).assertCountEquals(0)
            compose.onAllNodesWithText("Forecast confidence", substring = true, useUnmergedTree = true).assertCountEquals(0)
            captureVisualCatalogSurface("ux-upgrades.goals.milestone-insights")
            compose.onNodeWithTag("goal-insight-$id").performClick()
            inspectorNode(hasText("Milestone Progress")).assertIsDisplayed()
            inspectorNode(hasText("75% complete")).assertIsDisplayed()
            compose.onAllNodesWithText("Trend Data Table").assertCountEquals(0)
            compose.onNodeWithTag("entity-inspector-close").performClick()
            runBlocking { app.goalRepository.setStatus(id, GoalStatus.Completed) }
            scenario.recreate()
            compose.onNodeWithTag("goal-destination-History").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("goal-card-status-$id") and hasText("Completed", substring = true), useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("goal-card-$id").performClick()
            inspectorNode(hasText("Later milestone changes do not change this outcome.", substring = true)).assertIsDisplayed()
            scenario.recreate()
            inspectorNode(hasTestTag("goal-inspector-outcome")).assertTextEquals("1 of 2 milestones complete")
            inspectorNode(hasText("75% complete")).assertIsDisplayed()
            inspectorNode(hasText("Later milestone changes do not change this outcome.", substring = true)).assertIsDisplayed()
            captureVisualCatalogSurface("ux-upgrades.goals.milestone-closed")
        }
        val snapshot = runBlocking { app.goalRepository.closureSnapshots.first().single { it.goalId == id } }
        assertEquals(0.75, snapshot.progress!!, 0.0)
    }

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
            compose.onNodeWithTag("goal-detail-section-Overview").performClick()
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
            compose.onNodeWithTag("goal-detail-section-Overview").performClick()
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
            compose.openWorkspaceArchive("Goals")
            compose.onNodeWithTag("goal-card-$id").performScrollTo().performClick()
            compose.onNodeWithTag("goal-detail-section-Overview").performClick()
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
                goal("Not started", 0.0), goal("Target reached", 10.0),
                goal("Just beyond the target", 10.00001), goal("Past the target", 12.0))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            val expected = listOf("<0.1% complete", ">99.9% complete", "0% complete", "100% complete",
                ">100% complete", "120% complete")
            ids.zip(expected).forEachIndexed { index, (id, text) ->
                compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("goal-card-$id"))
                if (index == 0) captureVisualCatalogSurface("goals.progress.endpoints")
                assertCard(id, text, "overhaul.goals.endpoint-four-after-scroll".takeIf { index == 4 })
            }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("goal-card-${ids[1]}"))
            compose.onNodeWithTag("goal-card-${ids[1]}").performClick()
            compose.onNodeWithTag("goal-detail-section-Overview").performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals(">99.9% complete")
            scenario.recreate()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals(">99.9% complete")
            captureVisualCatalogSurface("goals.progress.near-target")
            assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(ids[1])?.status })

            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("goal-card-${ids[5]}"))
            compose.onNodeWithTag("goal-card-${ids[5]}").performClick()
            compose.onNodeWithTag("goal-detail-section-Overview").performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals("120% complete")
            compose.onNodeWithText("Trend Data Table").performScrollTo().performClick()
            inspectorNode(hasText("progress 120%", substring = true)).assertIsDisplayed()
            assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(ids[5])?.status })

            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNodeWithTag("goal-destination-Insights").performClick()
            compose.onNodeWithTag("goal-insights-list").performScrollToNode(hasTestTag("goal-insight-${ids[5]}"))
            compose.onNode(hasText("120% complete", substring = true) and hasAnyAncestor(hasTestTag("goal-insight-${ids[5]}")),
                useUnmergedTree = true).assertIsDisplayed()
            runBlocking { app.goalRepository.setStatus(ids[5], GoalStatus.Completed) }
            compose.onNodeWithTag("goal-destination-History").performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("goal-card-${ids[5]}"))
            compose.onNodeWithTag("goal-card-${ids[5]}").performClick()
            compose.onNodeWithTag("goal-detail-section-Overview").performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals("120% complete")
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            val snapshot = runBlocking { app.goalRepository.closureSnapshots.first().single { it.goalId == ids[5] } }
            assertEquals(1.2, requireNotNull(snapshot.progress), 0.0000001)
            compose.onNodeWithTag("goal-closure-history-${snapshot.id}").performScrollTo()
                .assertTextContains("120% progress", substring = true)
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
            val complete = inspectorNode(hasTestTag("entity-inspector-action-complete")).performScrollTo()
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
            compose.onNodeWithTag("goal-detail-section-Overview").performClick()
            compose.onNodeWithTag("goal-inspector-outcome").assertTextEquals("40% complete")
            compose.onNodeWithTag("entity-inspector-close").performClick()

            compose.onNodeWithTag("goal-destination-Goals").performClick()
            compose.onNodeWithTag("goal-card-${ids[1]}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-detail-section-Options").performClick()
            val secondComplete = inspectorNode(hasTestTag("entity-inspector-action-complete")).performScrollTo()
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
            compose.onNodeWithTag("goal-destination-Goals").performClick()
            compose.onNodeWithTag("goal-card-${ids[2]}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-detail-section-Options").performClick()
            inspectorNode(hasTestTag("entity-inspector-action-complete")).performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.get(ids[2])?.status == GoalStatus.Completed } }
            compose.onAllNodesWithTag("goal-celebration").assertCountEquals(0)
        }
    }

    private fun assertCard(id: Long, expected: String, captureSurfaceId: String? = null) {
        val label = compose.onNodeWithTag("goal-card-progress-label-$id", useUnmergedTree = true).performScrollTo()
        captureSurfaceId?.let { captureVisualCatalogSurface(it) }
        label.assertTextEquals(expected.removeSuffix(" complete")).assertIsDisplayed()
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
