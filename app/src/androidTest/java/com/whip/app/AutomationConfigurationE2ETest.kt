package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.TrackAggregation
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.projectGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomationConfigurationE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    private var trackId: Long = 0
    private var goalId: Long = 0

    @Before
    fun setUp() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true, powerMode = false) }
        trackId = app.trackRepository.create(
            TrackDraft(
                name = "Movies Watched",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
                    TrackFieldDraft("Notes", TrackFieldType.LongText),
                ),
            ),
        )
        goalId = app.goalRepository.create(
            GoalDraft(
                name = "Watch 50 Movies",
                type = GoalType.ReachValue,
                dimension = UnitDimension.Count,
                unitId = "count",
                precision = 0,
                targetMin = 50.0,
                startDate = LocalDate.now(),
                aggregation = GoalAggregation.Latest,
            ),
        )
    }

    @After
    fun cleanUp() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun userCanConnectMovieEntryCountToAnExistingGoalAndObserveTheEffect() {
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithContentDescription("Movies Watched, 0 Entries. Open Track").performClick()
            compose.onNodeWithTag("track-destination-Rules").performClick()
            compose.onNodeWithTag("track-automation-connect-goal").performScrollTo().performClick()

            compose.onNodeWithText("Connect Entries to a Goal").assertIsDisplayed()
            compose.onNodeWithText("Watch 50 Movies").assertIsDisplayed()
            compose.onNodeWithText("Count Entries").assertIsDisplayed()
            compose.onNodeWithText("Each eligible Entry adds 1 to Goal progress.").assertIsDisplayed()
            compose.onNodeWithText("This Automation will change it to Total", substring = true).assertIsDisplayed()
            compose.onNodeWithTag("track-existing-goal-confirm").performClick()

            runBlocking {
                withTimeout(5_000) {
                    app.linkRepository.rules.first { rules ->
                        rules.any { rule ->
                            rule.sourceType == LinkSourceType.Track &&
                                rule.sourceEntityId == trackId &&
                                rule.targetGoalId == goalId &&
                                rule.trackAggregation == TrackAggregation.CountEntries
                        }
                    }
                }
                val projection = requireNotNull(app.trackRepository.projection(trackId))
                listOf("Arrival", "Moonlight").forEach { title ->
                    app.trackRepository.addEntry(
                        trackId,
                        TrackEntryDraft(
                            LocalDate.now(),
                            mapOf(projection.primaryField.uuid to TrackValueDraft(textValue = title)),
                        ),
                    )
                }
                app.linkRepository.rebuildSources(setOf(LinkSourceType.Track))
                val contributions = withTimeout(5_000) {
                    app.linkRepository.contributions.first { values -> values.count { it.targetGoalId == goalId } == 2 }
                }.filter { it.targetGoalId == goalId }
                val goal = app.goalRepository.goals.first().single { it.id == goalId }
                val entries = app.measurementRepository.entries.first().filter { it.metricId == goal.metricId }

                assertEquals(2, contributions.size)
                assertTrue(contributions.all { it.canonicalValue == 1.0 })
                assertEquals(2, entries.size)
                assertEquals(GoalAggregation.Sum, goal.aggregation)
            }
        }
    }

    @Test
    fun allTrackHistoryMovesGoalWindowSoEligibleCountMatchesCurrentProgress() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        runBlocking {
            val projection = requireNotNull(app.trackRepository.projection(trackId))
            listOf(yesterday to "Sicario", today to "Arrival").forEach { (date, title) ->
                app.trackRepository.addEntry(
                    trackId,
                    TrackEntryDraft(
                        date,
                        mapOf(projection.primaryField.uuid to TrackValueDraft(textValue = title)),
                    ),
                )
            }
        }

        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithContentDescription("Movies Watched, 2 Entries", substring = true).performClick()
            compose.onNodeWithTag("track-destination-Rules").performClick()
            compose.onNodeWithTag("track-automation-connect-goal").performScrollTo().performClick()

            compose.onNodeWithTag("track-existing-goal-content")
                .performScrollToNode(hasText("New Entries Only"))
            compose.onNodeWithText("New Entries Only").performClick()
            compose.onNodeWithText("Include All Track History").performClick()
            compose.onNodeWithTag("track-existing-goal-content")
                .performScrollToNode(hasText("2 scanned · 2 eligible · 0 skipped"))
            compose.onNodeWithText("2 scanned · 2 eligible · 0 skipped").assertIsDisplayed()
            compose.onNodeWithText("Current Goal result from these Entries: 2").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("To make every included contribution count", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("track-existing-goal-confirm").performClick()

            runBlocking {
                val contributions = withTimeout(5_000) {
                    app.linkRepository.contributions.first { values -> values.count { it.targetGoalId == goalId } == 2 }
                }.filter { it.targetGoalId == goalId }
                val goal = app.goalRepository.goals.first().single { it.id == goalId }
                val entries = app.measurementRepository.entries.first().filter { it.metricId == goal.metricId }
                val current = projectGoal(goal, entries, emptyList(), today).currentValue

                assertEquals(2, contributions.size)
                assertEquals(yesterday, goal.startDate)
                assertEquals(2.0, current ?: Double.NaN, 0.0)
            }
        }
    }

    @Test
    fun trackSortMenuOffersIdentityAndLongTextFields() {
        val intent = Intent(app, MainActivity::class.java)
            .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        launchMainActivity(intent).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithContentDescription("Movies Watched, 0 Entries. Open Track").performClick()
            compose.onNodeWithContentDescription("Sort Entries by Entry Date, Descending").performClick()

            compose.onNodeWithText("Fields").assertIsDisplayed()
            compose.onNodeWithText("Title").assertIsDisplayed()
            compose.onNodeWithText("Notes").assertIsDisplayed()
            compose.onNodeWithText("Ascending").assertIsDisplayed()
            compose.onNodeWithText("Descending").assertIsDisplayed()
        }
    }
}
