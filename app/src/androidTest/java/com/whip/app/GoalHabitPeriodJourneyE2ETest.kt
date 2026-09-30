package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.AppSettings
import com.whip.app.domain.*
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GoalHabitPeriodJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun emptyCollectionAndTodayOfferDirectCreationWithoutLosingNavigation() {
        runBlocking { prepare() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.onNodeWithContentDescription("Habits tab").performClick()
            listOf("All", "Today").forEach { destination ->
                compose.onNodeWithTag("habit-destination-$destination").performClick()
                compose.onNodeWithText("Create Habit").performScrollTo().assertIsDisplayed()
                captureVisualCatalogSurface("overhaul.habits.empty-create-${destination.lowercase()}")
                compose.onNodeWithText("Create Habit").performClick()
                compose.onNodeWithText("Name *").assertIsDisplayed()
                compose.onNodeWithContentDescription("Cancel Habit editing").performClick()
                compose.onNodeWithTag("habit-destination-$destination").assertIsDisplayed()
            }
        }
        assertTrue(runBlocking { app.habitRepository.habits.first().isEmpty() })
    }

    @Test fun legacyReductionKeepsObservationsWithoutInvertedPercentAndOffersCompletionOnlyBelowTarget() {
        val id = runBlocking {
            prepare()
            val id = app.goalRepository.create(GoalDraft("Legacy weight", type = GoalType.ReduceValue,
                dimension = UnitDimension.Mass, unitId = "kilogram", baseline = 100.0,
                targetMin = 75.0, startDate = app.clock.today()))
            // A former ordinary template could persist this shape; preserve its data during the upgrade.
            val dao = app.database.goalDao()
            dao.updateGoal(requireNotNull(dao.getGoal(id)).copy(baseline = null))
            app.goalRepository.recordMeasurement(id, 90.0, app.clock.today())
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-status-$id", useUnmergedTree = true)
                .assertTextContains("90 → 75 kg", substring = true)
            compose.onAllNodes(hasTestTag("goal-card-status-$id") and hasText("Target reached", substring = true), useUnmergedTree = true)
                .assertCountEquals(0)
            compose.onAllNodesWithTag("goal-card-progress-$id").assertCountEquals(0)
            captureVisualCatalogSurface("overhaul.goals.legacy-reduce-above-target")
            runBlocking { app.goalRepository.recordMeasurement(id, 70.0, app.clock.today()) }
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasTestTag("goal-card-status-$id") and hasText("Target reached", substring = true), useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("goal-card-$id").performClick()
            compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("goal-detail-surface")))
                .performScrollToNode(hasTestTag("goal-completion-opportunity"))
            compose.onNodeWithTag("goal-completion-opportunity").assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.goals.legacy-reduce-below-target")
            scenario.recreate()
            compose.onNodeWithTag("goal-completion-opportunity").assertIsDisplayed()
        }
        assertEquals(2, runBlocking { app.goalRepository.measurementEntries.first().size })
        assertNull(runBlocking { app.goalRepository.get(id) }!!.baseline)
    }

    @Test fun finiteWeeklyAttainmentKeepsTwoWeeksAndOpenBoundedTargetRemainsPendingAfterRecreation() {
        val ids = runBlocking {
            prepare()
            val monday = app.clock.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val finite = app.habitRepository.create(HabitDraft("Two weekly outcomes", trackingMode = HabitTrackingMode.Count,
                targetPeriod = TargetPeriod.Week, targetMin = 1.0, startDate = monday.minusWeeks(2),
                endType = HabitEndType.AfterCompletions, endValue = 2.0))
            app.habitRepository.log(finite, 1.0, date = monday.minusWeeks(2).plusDays(2))
            app.habitRepository.log(finite, 1.0, date = monday.minusWeeks(1).plusDays(2))
            val bounded = app.habitRepository.create(HabitDraft("Weekly caffeine limit", trackingMode = HabitTrackingMode.Count,
                comparison = TargetComparison.AtMost, targetMin = null, targetMax = 2.0,
                targetPeriod = TargetPeriod.Week, startDate = monday))
            app.habitRepository.log(bounded, 2.0, date = app.clock.today())
            // Restored or cutoff-shifted evidence can have a later logical date; retain it without counting it today.
            val dao = app.database.habitDao()
            val recorded = dao.getLogsForHabit(bounded).single()
            dao.insertLog(recorded.copy(id = 0, uuid = "restored-future", value = 3.0, canonicalValue = 3.0,
                localEpochDay = app.clock.today().plusDays(1).toEpochDay(), measurementEntryId = null))
            val flexible = app.habitRepository.create(HabitDraft("Two flexible weekly outcomes",
                trackingMode = HabitTrackingMode.Count, scheduleType = HabitScheduleType.FlexibleTimesPerWeek,
                flexibleTimesPerWeek = 1, startDate = monday.minusWeeks(3),
                endType = HabitEndType.AfterCompletions, endValue = 2.0))
            app.habitRepository.log(flexible, 1.0, date = monday.minusWeeks(3).plusDays(2))
            app.habitRepository.log(flexible, 1.0, date = monday.minusWeeks(2).plusDays(2))
            Triple(finite, bounded, flexible)
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-card-${ids.first}").performScrollTo().performClick()
            compose.onNodeWithText("Not scheduled today").assertIsDisplayed()
            compose.onNodeWithTag("habit-detail-section-Insights").performClick()
            compose.onNodeWithText("2 weeks").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.habits.finite-weekly-streak")
            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNodeWithTag("habit-card-${ids.third}").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                runCatching {
                    compose.onNodeWithTag("entity-inspector-title")
                        .assertTextEquals("Two flexible weekly outcomes").assertIsDisplayed()
                }.isSuccess
            }
            compose.onNodeWithTag("habit-detail-section-Insights").performClick()
            compose.onNodeWithText("2 weeks").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.habits.finite-flexible-weekly-streak")
            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("habit-card-${ids.second}"))
            compose.onNodeWithTag("habit-card-title-${ids.second}", useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) {
                runCatching {
                    compose.onNodeWithTag("entity-inspector-title")
                        .assertTextEquals("Weekly caffeine limit").assertIsDisplayed()
                }.isSuccess
            }
            captureVisualCatalogSurface("overhaul.habits.bounded-inspector-diagnostic")
            compose.waitUntil(10_000) {
                runCatching {
                    compose.onNodeWithTag("entity-inspector-status")
                        .assert(hasAnyDescendant(hasText("Ready today"))).assertIsDisplayed()
                }.isSuccess
            }
            compose.onNodeWithTag("habit-detail-section-Today").performClick()
            compose.onNodeWithTag("entity-inspector-status").assert(hasAnyDescendant(hasText("Ready today"))).assertIsDisplayed()
            compose.onNodeWithTag("habit-today-state").assertTextContains("2 · at most 2", substring = true)
            compose.onAllNodesWithText("Complete today").assertCountEquals(0)
            captureVisualCatalogSurface("overhaul.habits.open-weekly-bound")
            scenario.recreate()
            compose.onNodeWithTag("entity-inspector-status").assert(hasAnyDescendant(hasText("Ready today"))).assertIsDisplayed()
            compose.onNodeWithTag("habit-today-state").assertTextContains("2 · at most 2", substring = true)
            compose.onAllNodesWithText("Complete today").assertCountEquals(0)
        }
        val logs = runBlocking { app.habitRepository.logs.first() }
        val finite = runBlocking { app.habitRepository.get(ids.first) }!!
        assertEquals(2, finite.successfulPeriodOutcomeDates(logs, finite.startDate, app.clock.today()).size)
        assertEquals(2, finite.currentStreak(logs, app.clock.today()))
        val flexible = runBlocking { app.habitRepository.get(ids.third) }!!
        assertEquals(2, flexible.currentStreak(logs, app.clock.today()))
        assertEquals(6, logs.size)
    }


    @Test fun finalCheckInRemainsInTodayAndHomeAfterRecreationAndLeavesOnTheNextLogicalDate() {
        val id = runBlocking {
            prepare()
            // App-owned fixture settings advance the logical date without changing the device clock.
            app.settingsRepository.update { it.copy(timeZoneId = "Etc/GMT+12", dayCutoffMinutes = 0) }
            app.habitRepository.create(HabitDraft("Final daily check-in", startDate = app.clock.today(),
                endType = HabitEndType.AfterCompletions, endValue = 1.0))
        }
        val earnedDate = app.clock.today()
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("home-habit-progress-record").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("home-habit-progress-record").performClick()
            compose.onNodeWithTag("habit-card-$id").performScrollTo().performClick()
            compose.onNodeWithTag("entity-inspector-primary-check-in").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Complete today").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Complete today").assertIsDisplayed()
            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNodeWithTag("habit-done-disclosure").performScrollTo().performClick()
            compose.onNodeWithTag("habit-card-$id").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.habits.final-outcome-today")
            compose.onNodeWithContentDescription("Go to Home").performClick()
            val finished = "Habit progress: 1 of 1 complete. Open Habits Today"
            compose.onNodeWithContentDescription(finished).assertIsDisplayed()
            captureVisualCatalogSurface("overhaul.habits.final-outcome-home")
            scenario.recreate()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription(finished).fetchSemanticsNodes().isNotEmpty()
            }
            runBlocking { app.settingsRepository.update { it.copy(timeZoneId = "Etc/GMT-14") } }
            assertTrue(app.clock.today() > earnedDate)
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Habit progress: No check-ins due. Open Habits Today")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("home-habit-progress-record").performClick()
            compose.onAllNodesWithTag("habit-card-$id").assertCountEquals(0)
            captureVisualCatalogSurface("overhaul.habits.final-outcome-next-date")
            compose.onNodeWithTag("habit-destination-All").performClick()
            compose.onNodeWithTag("habit-card-$id").performScrollTo().assertIsDisplayed()
        }
        val logs = runBlocking { app.habitRepository.logs.first() }
        assertEquals(1, logs.size)
        assertEquals(earnedDate, logs.single().localDate)
    }

    private suspend fun prepare() {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false) }
    }
}
