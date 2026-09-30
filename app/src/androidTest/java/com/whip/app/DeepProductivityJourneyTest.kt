package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class DeepProductivityJourneyTest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    private suspend fun prepare() {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark,
            dynamicColor = false, goalCelebrationEnabled = false) }
    }
    private fun inside(surface: String, matcher: SemanticsMatcher): SemanticsNodeInteraction {
        val list = hasScrollToIndexAction() and hasAnyAncestor(hasTestTag(surface))
        compose.waitUntil(10_000) { compose.onAllNodes(list).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(list).performScrollToNode(matcher)
        return compose.onNode(matcher and hasAnyAncestor(hasTestTag(surface)))
    }

    private fun awaitHabitLogs(count: Int) {
        compose.waitUntil(10_000) { runBlocking { app.habitRepository.logs.first().size == count } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("habit-value-dialog").fetchSemanticsNodes().isEmpty() }
    }

    @Test fun numericHabitActionsStayInDetailsAndPreserveFacts() {
        val id = runBlocking {
            prepare()
            app.habitRepository.create(HabitDraft("Water", trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Volume, unitId = "litre", precision = 2,
                targetMin = 2.0, quickIncrement = 0.25, quickActions = listOf(0.5), startDate = app.clock.today()))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.openProductivityCardDetails("habit", id, "Water")
            inside("habit-detail-surface", hasText("+0.5 L")).performClick()
            awaitHabitLogs(1)
            inside("habit-detail-surface", hasText("Add Amount")).performClick()
            compose.onNodeWithTag("habit-value-input").performTextInput("0.25")
            closeSoftKeyboard()
            compose.onNodeWithTag("habit-value-save").performClick()
            awaitHabitLogs(2)
            inside("habit-detail-surface", hasText("Set Total")).performClick()
            compose.onNodeWithTag("habit-value-input").performTextReplacement("1")
            closeSoftKeyboard()
            compose.onNodeWithText("Save", substring = false).performClick()
            awaitHabitLogs(3)
            inside("habit-detail-surface", hasText("Undo Last Entry")).performClick()
            awaitHabitLogs(2)
            inside("habit-detail-surface", hasText("Add Amount")).performClick()
            compose.onNodeWithText("Cancel", substring = false).performClick()
            scenario.recreate()
            inside("habit-detail-surface", hasText("Set Total")).assertIsDisplayed()
            captureVisualCatalogSurface("deep.habits.numeric-details")
        }
        val logs = runBlocking { app.habitRepository.logs.first() }
        assertEquals(listOf(0.25, 0.5), logs.mapNotNull { it.value }.sorted())
        assertTrue(logs.all { it.enteredUnitId == "litre" })
    }

    @Test @AndroidFontScale fun habitFocusedInsightsAndChecklistFitLargeText() {
        val id = runBlocking {
            prepare()
            val id = app.habitRepository.create(HabitDraft("Evening reset", trackingMode = HabitTrackingMode.Checklist,
                startDate = app.clock.today().minusDays(20), checklistItems = listOf(
                    HabitChecklistItemDraft("Prepare tomorrow", 0), HabitChecklistItemDraft("Read quietly", 1))))
            repeat(8) { app.habitRepository.setCheckOff(id, app.clock.today().minusDays(it.toLong() + 1), true) }
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.openProductivityCardDetails("habit", id, "Evening reset")
            inside("habit-detail-surface", hasText("Today's Checklist")).assertIsDisplayed()
            captureVisualCatalogSurface("deep.habits.execution.large")
            compose.onNodeWithTag("habit-detail-section-Insights").performClick()
            inside("habit-detail-surface", hasText("Eight-Week Consistency")).assertIsDisplayed()
            captureVisualCatalogSurface("deep.habits.focused-insights.large")
            scenario.recreate()
            inside("habit-detail-surface", hasText("View History")).performClick()
            inside("habit-detail-surface", hasTestTag("habit-history-search")).assertIsDisplayed()
        }
    }

    @Test fun goalCompletionEventsKeepTheirOriginalMeaning() {
        val id = runBlocking {
            prepare()
            val id = app.goalRepository.create(GoalDraft("Practice regularly", type = GoalType.Consistency,
                dimension = UnitDimension.Count, unitId = "count", targetMin = 3.0,
                aggregation = GoalAggregation.CompletionCount, consistencyRequiredPeriods = 4,
                startDate = app.clock.today().minusDays(2)))
            app.goalRepository.recordMeasurement(id, 5.0, app.clock.today(), note = "Original five")
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.openProductivityCardDetails("goal", id, "Practice regularly")
            compose.onNodeWithText("Record Completion", substring = false).performClick()
            compose.onAllNodesWithTag("goal-measurement-value").assertCountEquals(0)
            compose.onNodeWithTag("goal-new-completion").assertTextEquals("One completion")
            captureVisualCatalogSurface("deep.goals.record-completion")
            compose.onNodeWithTag("goal-measurement-save").performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.measurementEntries.first().size == 2 } }
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            inside("goal-detail-surface", hasText("Original five", substring = true)).performClick()
            compose.onNodeWithTag("goal-measurement-note").performScrollTo().performTextReplacement("Original five corrected")
            closeSoftKeyboard()
            compose.onNodeWithTag("goal-measurement-save").performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.measurementEntries.first().any { it.note == "Original five corrected" } } }
            scenario.recreate()
            inside("goal-detail-surface", hasText("Original five corrected", substring = true)).assertIsDisplayed()
        }
        val entries = runBlocking { app.goalRepository.measurementEntries.first() }
        assertEquals(5.0, entries.single { it.note == "Original five corrected" }.enteredValue!!, 0.0)
        assertEquals(1.0, entries.single { it.note.isBlank() }.enteredValue!!, 0.0)
        val goal = runBlocking { requireNotNull(app.goalRepository.get(id)) }
        assertEquals(2.0, calculateConsistencyProgress(goal, entries, app.clock.today()).currentPeriodValue, 0.0)
    }
    private fun inList(tag: String, matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag(tag).performScrollToNode(matcher)
        return compose.onNode(matcher)
    }

    @Test fun habitAndGoalSearchUsesTheWorkspaceActionAndKeepsCollectionsClear() {
        val ids = runBlocking {
            prepare()
            repeat(10) { app.habitRepository.create(HabitDraft("Routine $it", startDate = app.clock.today())) }
            val finished = app.habitRepository.create(HabitDraft("Read a chapter", tags = listOf("quiet"), startDate = app.clock.today()))
            app.habitRepository.setCheckOff(finished, app.clock.today(), true)
            val archived = app.habitRepository.create(HabitDraft("Quiet archive", startDate = app.clock.today()))
            app.habitRepository.setArchived(archived, true)
            repeat(8) { app.goalRepository.create(GoalDraft("Project $it", type = GoalType.ReachValue, targetMin = 10.0, startDate = app.clock.today())) }
            val goal = app.goalRepository.create(GoalDraft("Finish research", type = GoalType.ReachValue, tags = listOf("quiet"), targetMin = 10.0, startDate = app.clock.today()))
            Triple(finished, archived, goal)
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-collection-search").assertDoesNotExist()
            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").performTextInput("quiet")
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithTag("unified-search-query").assertTextContains("quiet")
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-result-Habit-${ids.first}").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("unified-search-result-Habit-${ids.first}").performScrollTo().performClick()
            pressBack()
            compose.onNodeWithTag("habit-collection-search").assertDoesNotExist()
            inList("habit-list-Habits", hasTestTag("habit-card-${ids.first}")).assertIsDisplayed()
            captureVisualCatalogSurface("search-consistency.habits.collection")
            compose.openWorkspaceArchive("Habits")
            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").performTextInput("status:archived quiet")
            closeSoftKeyboard()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-result-Habit-${ids.second}").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("unified-search-result-Habit-${ids.second}").performScrollTo().performClick()
            pressBack()
            compose.onNodeWithText("Quiet archive").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-collection-search").assertDoesNotExist()
            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").performTextInput("quiet")
            closeSoftKeyboard()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-result-Goal-${ids.third}").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("unified-search-result-Goal-${ids.third}").performScrollTo().performClick()
            pressBack()
            scenario.recreate()
            compose.onNodeWithTag("goal-collection-search").assertDoesNotExist()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("goal-card-${ids.third}"))
            compose.onNodeWithTag("goal-card-${ids.third}").assertIsDisplayed()
            captureVisualCatalogSurface("search-consistency.goals.collection")
            compose.onNodeWithTag("goal-insights-list").assertDoesNotExist()
            compose.onNodeWithTag("goal-destination-Goals").performClick()
            compose.onNodeWithContentDescription("More Goal Actions").performClick()
            compose.onNodeWithText("Reorder Goals").performClick()
            compose.onNodeWithTag("goal-collection-search").assertDoesNotExist()
            compose.onNode(hasScrollToIndexAction())
                .performScrollToNode(hasContentDescription("Reorder Project 0", substring = true))
            compose.onNodeWithContentDescription("Reorder Project 0", substring = true).assertIsDisplayed()
        }
        assertEquals(9, runBlocking { app.goalRepository.goals.first().size })
    }

    @Test @AndroidFontScale fun trackWorkspaceSearchAndSelectionShowConfiguredDetails() {
        val ids = runBlocking {
            prepare()
            val fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true)) +
                (1..4).map { TrackFieldDraft("Detail $it", TrackFieldType.ShortText, showInList = true) }
            val first = app.trackRepository.create(TrackDraft("Alpha journal", tags = listOf("quiet"), fields = fields))
            val second = app.trackRepository.create(TrackDraft("Beta journal", fields = fields))
            repeat(6) { app.trackRepository.create(TrackDraft("Other journal $it", fields = fields)) }
            val projection = requireNotNull(app.trackRepository.projection(first))
            app.trackRepository.addEntry(first, TrackEntryDraft(app.clock.today(), projection.fields.associate { field ->
                field.uuid to TrackValueDraft(textValue = if (field.primary) "Four facts" else "Original ${field.name}") }))
            first to second
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-collection-search").assertDoesNotExist()
            compose.onNodeWithTag("workspace-search-action").performClick()
            compose.onNodeWithTag("unified-search-query").performTextInput("quiet domain:track")
            closeSoftKeyboard()
            scenario.recreate()
            compose.onNodeWithTag("unified-search-query").assertTextContains("quiet domain:track")
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-result-Track-${ids.first}").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("unified-search-result-Track-${ids.first}").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Back to Tracks").performClick()
            compose.onNodeWithContentDescription("More Track Options").performClick()
            compose.onNodeWithText("Select Tracks").performClick()
            inList("track-list", hasText("0 Tracks selected")).assertIsDisplayed()
            inList("track-list", hasText("Pin to Whip Home")).assertIsNotEnabled()
            inList("track-list", hasTestTag("track-card-${ids.second}")).performClick()
            inList("track-list", hasText("Pin to Whip Home")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.trackRepository.projection(ids.second)?.track?.pinned == true } }
            assertFalse(runBlocking { requireNotNull(app.trackRepository.projection(ids.first)).track.pinned })
            scenario.recreate()
            compose.onNodeWithTag("track-collection-search").assertDoesNotExist()
            captureVisualCatalogSurface("search-consistency.tracks.collection")
            inList("track-list", hasTestTag("track-card-${ids.first}")).performClick()
            inList("track-entry-list", hasContentDescription("View All 4 Details for Four facts")).assertIsDisplayed()
            inList("track-entry-list", hasText("2 more configured details · View all 4")).performScrollTo()
            captureVisualCatalogSurface("deep.tracks.configured-details")
            compose.onNodeWithContentDescription("View All 4 Details for Four facts").performScrollTo().performClick()
            inside("track-entry-detail-surface", hasText("Original Detail 4")).assertIsDisplayed()
            scenario.recreate()
            inside("track-entry-detail-surface", hasText("Original Detail 4")).assertIsDisplayed()
            pressBack()
            compose.onNodeWithContentDescription("Back to Tracks").performClick()
            compose.onNodeWithTag("track-collection-search").assertDoesNotExist()
            compose.onNodeWithTag("track-workspace-destination-Activity").performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("View All 4 Details for Four facts"))
            compose.onNodeWithContentDescription("View All 4 Details for Four facts").assertIsDisplayed().performClick()
            inside("track-entry-detail-surface", hasText("Original Detail 4")).assertIsDisplayed()
        }
    }

    @Test @AndroidFontScale fun milestoneEditorKeepsOptionalDetailsAndIdentity() {
        val id = runBlocking {
            prepare()
            app.goalRepository.create(GoalDraft("Publish research", type = GoalType.WeightedMilestones,
                startDate = app.clock.today(), milestones = (1..8).map { GoalMilestoneDraft("Chapter $it", 1.0) }))
        }
        val originals = runBlocking { app.goalRepository.milestones.first().filter { it.goalId == id } }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.openProductivityCardDetails("goal", id, "Publish research")
            compose.onNodeWithContentDescription("Edit Goal").performClick()
            inList("goal-editor-fields", hasTestTag("goal-milestone-name-0")).assertIsDisplayed()
            compose.onAllNodesWithTag("goal-milestone-weight-0").assertCountEquals(0)
            inList("goal-editor-fields", hasTestTag("goal-milestone-details-1")).performScrollTo()
            captureVisualCatalogSurface("deep.goals.compact-milestones.large")
            inList("goal-editor-fields", hasTestTag("goal-milestone-details-0")).performClick()
            inList("goal-editor-fields", hasTestTag("goal-milestone-weight-0")).performTextReplacement("")
            closeSoftKeyboard()
            inList("goal-editor-fields", hasTestTag("goal-milestone-details-0")).performClick()
            compose.onNodeWithText("Save", substring = false).performClick()
            inList("goal-editor-fields", hasTestTag("goal-milestone-weight-0")).assertExists()
            inList("goal-editor-fields", hasTestTag("goal-milestone-details-0")).performClick()
            compose.onNodeWithText("Save", substring = false).performClick()
            inList("goal-editor-fields", hasTestTag("goal-milestone-weight-0")).assertExists()
            compose.onNodeWithTag("goal-milestone-weight-0").performTextReplacement("2.5")
            closeSoftKeyboard()
            inList("goal-editor-fields", hasText("Reward (optional)")).performTextInput("A quiet break")
            closeSoftKeyboard()
            val move = inList("goal-editor-fields", hasContentDescription("Reorder Chapter 1", substring = true))
                .fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == "Move Chapter 1 down" }
            compose.runOnIdle { assertTrue(move.action()) }
            scenario.recreate()
            inList("goal-editor-fields", hasTestTag("goal-milestone-weight-1")).assertTextContains("2.5")
            inList("goal-editor-fields", hasText("Reward (optional)")).assertTextContains("A quiet break")
            captureVisualCatalogSurface("deep.goals.milestone-details.large")
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.milestones.first().any { it.goalId == id && it.weight == 2.5 } } }
        }
        val saved = runBlocking { app.goalRepository.milestones.first().filter { it.goalId == id }.sortedBy { it.position } }
        assertEquals(originals.map { it.id }.toSet(), saved.map { it.id }.toSet())
        assertEquals("Chapter 1", saved[1].name)
        assertEquals("A quiet break", saved[1].reward)
        assertEquals(2.5, saved[1].weight, 0.0)
    }

    @Test fun trackInsightRangeLeadsToTheSameCorrectableEvidence() {
        val id = runBlocking {
            prepare()
            val id = app.trackRepository.create(TrackDraft("Water observations", fields = listOf(
                TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true),
                TrackFieldDraft("Amount", TrackFieldType.Number, showInList = true, dimension = UnitDimension.Volume, unitId = "litre", precision = 2),
                TrackFieldDraft("Optional date", TrackFieldType.Date))))
            val fields = requireNotNull(app.trackRepository.projection(id)).fields
            val days = listOf(40L, 29L, 4L, 0L, -1L)
            days.forEachIndexed { index, day ->
                app.trackRepository.addEntry(id, TrackEntryDraft(app.clock.today().minusDays(day), mapOf(
                    fields[0].uuid to TrackValueDraft(textValue = "Observation $index"),
                    fields[1].uuid to TrackValueDraft(enteredNumber = if (index == 2) 2000.0 else 0.5,
                        enteredUnitId = if (index == 2) "millilitre" else "litre"))))
            }
            id
        }
        val projection = runBlocking { requireNotNull(app.trackRepository.projection(id)) }
        val amount = projection.fields[1]
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-card-$id").performScrollTo().performClick()
            inList("track-entry-list", hasContentDescription("Search Entries in Water observations")).performClick()
            compose.onNodeWithTag("track-entry-search").performTextInput("unrelated")
            closeSoftKeyboard()
            compose.onNodeWithTag("track-destination-Track Insights").performClick()
            inList("track-insights-list", hasTestTag("track-review-range-Month")).performClick()
            compose.onNodeWithTag("track-review-scope").assertTextContains("3 matching Entries", substring = true)
            inList("track-insights-list", hasTestTag("track-trend-${amount.uuid}")).assertIsDisplayed()
            captureVisualCatalogSurface("deep.tracks.dated-trend")
            inList("track-insights-list", hasTestTag("track-trend-data-${amount.uuid}")).performClick()
            inList("track-insights-list", hasText("2000 mL", substring = true)).assertIsDisplayed()
            inList("track-insights-list", hasTestTag("track-review-matching-entries")).performClick()
            compose.onNodeWithTag("track-review-range-Month").assertIsSelected()
            inList("track-entry-list", hasText("Observation 2")).performClick()
            compose.onNodeWithContentDescription("Edit Entry").performClick()
            inList("track-entry-editor-list", hasTestTag("track-entry-number-${amount.uuid}")).performTextReplacement("1500")
            closeSoftKeyboard()
            compose.onNodeWithText("Save", substring = false).performClick()
            compose.waitUntil(10_000) { runBlocking { app.trackRepository.projection(id)?.entries?.any { it.value(amount.id)?.enteredNumber == 1500.0 } == true } }
            assertEquals("millilitre", runBlocking { app.trackRepository.projection(id)!!.entries.single { it.value(amount.id)?.enteredNumber == 1500.0 }.value(amount.id)!!.enteredUnitId })
            scenario.recreate()
            inList("track-entry-list", hasTestTag("track-review-range-Month")).assertIsSelected()
            compose.onNodeWithTag("track-review-scope").assertTextContains("3 matching Entries", substring = true)
            captureVisualCatalogSurface("deep.tracks.matching-evidence")
            compose.onNodeWithTag("track-destination-Track Insights").performClick()
            inList("track-insights-list", hasText("1500 mL", substring = true)).assertIsDisplayed()
        }
        assertEquals(5, runBlocking { app.trackRepository.projection(id)?.entries?.size })
    }

}
