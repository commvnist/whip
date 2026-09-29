package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
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

class ProductivityExperienceJourneyTest {
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

    @Test fun goalMilestonesWorkInsideDetailsAndCompleteOnlyOnRequest() {
        val id = runBlocking {
            prepare()
            app.goalRepository.create(GoalDraft("Publish the guide", type = GoalType.WeightedMilestones,
                startDate = app.clock.today(), milestones = listOf(GoalMilestoneDraft("Draft", 1.0), GoalMilestoneDraft("Publish", 3.0))))
        }
        val milestones = runBlocking { app.goalRepository.milestones.first().filter { it.goalId == id } }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-$id").performScrollTo().performClick()
            inside("goal-detail-surface", hasTestTag("goal-milestone-${milestones[0].id}")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.milestones.first().count { it.goalId == id && it.completed } == 1 } }
            inside("goal-detail-surface", hasText("25% complete")).assertIsDisplayed()
            inside("goal-detail-surface", hasTestTag("goal-milestone-${milestones[1].id}")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.milestones.first().count { it.goalId == id && it.completed } == 2 } }
            scenario.recreate()
            inside("goal-detail-surface", hasText("Target Reached")).assertIsDisplayed()
            assertEquals(GoalStatus.Active, runBlocking { app.goalRepository.get(id)?.status })
            captureVisualCatalogSurface("experience.goals.working-milestones")
            inside("goal-detail-surface", hasText("Complete Goal")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.get(id)?.status == GoalStatus.Completed } }
            compose.onNodeWithTag("goal-destination-History").performClick()
            compose.onNodeWithTag("goal-card-$id").performScrollTo().performClick()
            inside("goal-detail-surface", hasTestTag("goal-milestone-${milestones[0].id}")).assertIsNotEnabled()
            compose.onAllNodesWithTag("goal-completion-opportunity").assertCountEquals(0)
        }
    }

    @Test @AndroidFontScale fun habitChecklistWorksInsideTodayAndRetainsParentCompletion() {
        val id = runBlocking {
            prepare()
            app.habitRepository.create(HabitDraft("Evening routine", trackingMode = HabitTrackingMode.Checklist,
                startDate = app.clock.today(), checklistItems = listOf(HabitChecklistItemDraft("Prepare tomorrow", 0), HabitChecklistItemDraft("Read quietly", 1))))
        }
        val items = runBlocking { app.habitRepository.checklistItems.first().filter { it.habitId == id } }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-card-$id").performScrollTo().performClick()
            inside("habit-detail-surface", hasTestTag("habit-checklist-item-${items[0].id}")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.checklistStates.first().count { it.habitId == id && it.completed } == 1 } }
            inside("habit-detail-surface", hasTestTag("habit-checklist-item-${items[1].id}")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.checklistStates.first().count { it.habitId == id && it.completed } == 2 } }
            scenario.recreate()
            inside("habit-detail-surface", hasTestTag("habit-checklist-item-${items[0].id}")).assertIsOn()
            captureVisualCatalogSurface("experience.habits.working-checklist.large")
            assertTrue(runBlocking { app.habitRepository.logs.first().any { it.habitId == id && it.status == HabitLogStatus.Success } })
            inside("habit-detail-surface", hasTestTag("habit-checklist-item-${items[0].id}")).performClick()
            compose.waitUntil(10_000) { runBlocking { app.habitRepository.checklistStates.first().count { it.habitId == id && it.completed } == 1 } }
            runBlocking { app.habitRepository.setPaused(id, true) }
            inside("habit-detail-surface", hasTestTag("habit-checklist-item-${items[1].id}")).assertIsNotEnabled()
        }
    }

    @Test fun historySearchFindsOlderOriginalFactsAndRestoresItsQuery() {
        val ids = runBlocking {
            prepare()
            val today = app.clock.today()
            val habitId = app.habitRepository.create(HabitDraft("Reading notes", trackingMode = HabitTrackingMode.LogOnly,
                comparison = TargetComparison.None, startDate = today.minusDays(100)))
            val goalId = app.goalRepository.create(GoalDraft("Reading target", type = GoalType.ReachValue, startDate = today.minusDays(100), targetMin = 100.0))
            repeat(35) { index ->
                val note = if (index == 34) "Mountain chapter" else "Ordinary entry $index"
                app.habitRepository.log(habitId, null, date = today.minusDays(index.toLong()), note = note)
                app.goalRepository.recordMeasurement(goalId, index.toDouble(), today.minusDays(index.toLong()), note = note)
            }
            habitId to goalId
        }
        val logs = runBlocking { app.habitRepository.logs.first() }
        val entries = runBlocking { app.goalRepository.measurementEntries.first() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-card-${ids.first}").performScrollTo().performClick()
            compose.onNodeWithTag("habit-detail-section-History").performClick()
            inside("habit-detail-surface", hasTestTag("habit-history-search")).performTextInput("Mountain")
            closeSoftKeyboard()
            inside("habit-detail-surface", hasText("Mountain chapter", substring = true)).assertIsDisplayed()
            scenario.recreate()
            inside("habit-detail-surface", hasTestTag("habit-history-search")).assertTextContains("Mountain")
            inside("habit-detail-surface", hasTestTag("habit-history-result-count")).assertTextEquals("1 of 35 events")
            captureVisualCatalogSurface("experience.habits.history-search")
            inside("habit-detail-surface", hasText("Mountain chapter", substring = true)).performClick()
            compose.onNodeWithText("Mountain chapter").assertExists()
            pressBack()
            inside("habit-detail-surface", hasTestTag("habit-history-search")).assertTextContains("Mountain")
            pressBack()
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-${ids.second}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            inside("goal-detail-surface", hasTestTag("goal-history-search")).performTextInput("Mountain")
            closeSoftKeyboard()
            inside("goal-detail-surface", hasText("Mountain chapter", substring = true)).assertIsDisplayed()
            scenario.recreate()
            inside("goal-detail-surface", hasTestTag("goal-history-result-count")).assertTextEquals("1 of 35 updates")
            captureVisualCatalogSurface("experience.goals.history-search")
            inside("goal-detail-surface", hasTestTag("goal-history-search")).performTextReplacement("no-match")
            closeSoftKeyboard()
            inside("goal-detail-surface", hasText("No Matching Updates")).assertIsDisplayed()
            inside("goal-detail-surface", hasText("Clear Search")).performClick()
            inside("goal-detail-surface", hasTestTag("goal-history-result-count")).assertTextEquals("35 of 35 updates")
        }
        assertEquals(logs, runBlocking { app.habitRepository.logs.first() })
        assertEquals(entries, runBlocking { app.goalRepository.measurementEntries.first() })
    }

    @Test fun repeatedHistoryCleanupKeepsQueriesAndRecoversFromStaleGoalDeletion() {
        val ids = runBlocking {
            prepare()
            val today = app.clock.today()
            val habit = app.habitRepository.create(HabitDraft("Clean up notes", trackingMode = HabitTrackingMode.LogOnly,
                comparison = TargetComparison.None, startDate = today.minusDays(3)))
            repeat(3) { app.habitRepository.log(habit, null, date = today.minusDays(it.toLong()), note = "Cleanup $it") }
            val goal = app.goalRepository.create(GoalDraft("Clean up progress", type = GoalType.ReachValue,
                targetMin = 100.0, startDate = today.minusDays(3)))
            repeat(2) { app.goalRepository.recordMeasurement(goal, 10.0 + it, today.minusDays(it.toLong()), note = "Review $it") }
            habit to goal
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-card-${ids.first}").performScrollTo().performClick()
            compose.onNodeWithTag("habit-detail-section-History").performClick()
            inside("habit-detail-surface", hasTestTag("habit-history-search")).performTextInput("Cleanup")
            closeSoftKeyboard()
            repeat(2) { index ->
                inside("habit-detail-surface", hasText("Cleanup $index", substring = true)).performClick()
                compose.onNodeWithText("Delete", substring = false).performClick()
                compose.onNodeWithTag("habit-history-confirm-delete").performClick()
                compose.waitUntil(10_000) { runBlocking { app.habitRepository.logs.first().count { it.habitId == ids.first } == 2 - index } }
                compose.waitUntil(10_000) { compose.onAllNodesWithTag("habit-history-dialog").fetchSemanticsNodes().isEmpty() }
                inside("habit-detail-surface", hasTestTag("habit-history-search")).assertTextContains("Cleanup")
            }
            scenario.recreate()
            inside("habit-detail-surface", hasTestTag("habit-history-result-count")).assertTextEquals("1 of 1 event")
            captureVisualCatalogSurface("experience.habits.retained-history")
            pressBack()
            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithTag("goal-card-${ids.second}").performScrollTo().performClick()
            compose.onNodeWithTag("goal-detail-section-History").performClick()
            inside("goal-detail-surface", hasTestTag("goal-history-search")).performTextInput("Review")
            closeSoftKeyboard()
            inside("goal-detail-surface", hasText("Review 0", substring = true)).performClick()
            // A concurrent correction must fail locally, then a reopened fresh entry can be deleted.
            runBlocking {
                val entry = app.goalRepository.measurementEntries.first().single { it.note == "Review 0" }
                app.goalRepository.updateMeasurement(ids.second, entry.id, 12.0, entry.localDate, "Review refreshed")
            }
            compose.onNodeWithTag("goal-measurement-delete").performClick()
            compose.onNodeWithTag("goal-measurement-confirm-delete").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("goal-measurement-save-problem").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("goal-measurement-save-problem").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("goal-measurement-cancel").performClick()
            inside("goal-detail-surface", hasTestTag("goal-history-search")).assertTextContains("Review")
            inside("goal-detail-surface", hasText("Review refreshed", substring = true)).performClick()
            compose.onNodeWithTag("goal-measurement-delete").performClick()
            compose.onNodeWithTag("goal-measurement-confirm-delete").performClick()
            compose.waitUntil(10_000) { runBlocking { app.goalRepository.measurementEntries.first().size == 1 } }
            inside("goal-detail-surface", hasText("Review 1", substring = true)).performClick()
            compose.onNodeWithTag("goal-measurement-note").performScrollTo().performTextReplacement("Review corrected")
            closeSoftKeyboard()
            compose.onNodeWithTag("goal-measurement-save").performClick()
            inside("goal-detail-surface", hasText("Review corrected", substring = true)).assertIsDisplayed()
            scenario.recreate()
            inside("goal-detail-surface", hasTestTag("goal-history-search")).assertTextContains("Review")
            captureVisualCatalogSurface("experience.goals.retained-history")
        }
        assertEquals(listOf("Cleanup 2"), runBlocking { app.habitRepository.logs.first().map { it.note } })
        assertEquals(listOf("Review corrected"), runBlocking { app.goalRepository.measurementEntries.first().map { it.note } })
    }

    @Test fun trackHistoryChangesKeepLoadedOlderWindowAndSearchAcrossRecreation() {
        val id = runBlocking {
            prepare()
            val id = app.trackRepository.create(TrackDraft("Cleanup log", fields = listOf(
                TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true))))
            val field = requireNotNull(app.trackRepository.projection(id)).primaryField
            repeat(105) { app.trackRepository.addEntry(id, TrackEntryDraft(app.clock.today().minusDays(it.toLong()),
                mapOf(field.uuid to TrackValueDraft(textValue = "Retain $it")))) }
            id
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-card-$id").performScrollTo().performClick()
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Show 5 More · 5 Remaining"))
            compose.onNodeWithText("Show 5 More · 5 Remaining").performClick()
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Retain 104"))
            compose.onNodeWithText("Retain 104").assertIsDisplayed()
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasContentDescription("More Actions for Retain 103"))
            compose.onNodeWithContentDescription("More Actions for Retain 103").performClick()
            compose.onNodeWithText("Delete Entry").performClick()
            compose.onNodeWithTag("track-entry-row-delete-confirmation").assertIsDisplayed()
            compose.onNodeWithText("Delete Entry").performClick()
            compose.waitUntil(10_000) { runBlocking { app.trackRepository.projection(id)?.entries?.size == 104 } }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("track-entry-row-delete-confirmation").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("Retain 104").assertIsDisplayed()
            // Correct an old persisted row; the changed-content refill must still include its page.
            runBlocking {
                val projection = requireNotNull(app.trackRepository.projection(id))
                val entry = projection.entries.single { projection.primaryText(it) == "Retain 104" }
                app.trackRepository.updateEntry(entry.entry.id, TrackEntryDraft(entry.entry.entryDate,
                    mapOf(projection.primaryField.uuid to TrackValueDraft(textValue = "Retain corrected"))))
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Retain corrected").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Retain corrected").assertIsDisplayed()
            scenario.recreate()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Retain corrected").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Retain corrected").assertIsDisplayed()
            captureVisualCatalogSurface("experience.tracks.retained-history")
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasContentDescription("Search Entries in Cleanup log"))
            compose.onNodeWithContentDescription("Search Entries in Cleanup log").performClick()
            compose.onNodeWithTag("track-entry-search").performTextInput("corrected")
            closeSoftKeyboard()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Retain corrected").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("track-entry-list").performScrollToNode(hasText("Retain corrected"))
            scenario.recreate()
            compose.onNodeWithTag("track-detail-title").assertTextEquals("Cleanup log")
            compose.onNodeWithTag("track-entry-search").assertTextContains("corrected")
        }
        assertEquals(104, runBlocking { app.trackRepository.projection(id)?.entries?.size })
    }

    @Test @AndroidFontScale fun trackStarterPreviewUsesRealFieldsWithoutSavingSampleValues() {
        runBlocking { prepare() }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-list").performScrollToNode(hasText("Create Track"))
            compose.onNodeWithText("Create Track").performClick()
            compose.onNodeWithTag("track-editor-list").performScrollToNode(hasTestTag("track-choose-starter"))
            compose.onNodeWithTag("track-choose-starter").performClick()
            compose.onNodeWithTag("track-starter-Spending").performScrollTo().performClick()
            compose.onNodeWithTag("track-editor-list").performScrollToNode(hasTestTag("track-preview-entry"))
            compose.onNodeWithTag("track-preview-entry").performClick()
            compose.onNodeWithTag("track-preview-check").performClick()
            compose.onNodeWithTag("track-preview-fields").performScrollToNode(hasTestTag("track-preview-validation"))
            compose.onNodeWithTag("track-preview-validation").assertTextEquals("Item is required")
            compose.onNodeWithTag("track-preview-fields").performScrollToNode(hasTestTag("track-entry-short-text-preview-field-0"))
            compose.onNodeWithTag("track-entry-short-text-preview-field-0").performTextInput("Train ticket")
            closeSoftKeyboard()
            compose.onNodeWithTag("track-preview-fields").performScrollToNode(hasTestTag("track-entry-number-preview-field-1"))
            compose.onNodeWithTag("track-entry-number-preview-field-1").performTextInput("4.25")
            closeSoftKeyboard()
            compose.onNodeWithTag("track-preview-check").performClick()
            compose.onNodeWithTag("track-preview-fields").performScrollToNode(hasTestTag("track-preview-validation"))
            compose.onNodeWithTag("track-preview-validation").assertTextEquals("Sample entry is valid. Nothing has been saved.")
            captureVisualCatalogSurface("experience.tracks.entry-preview.large")
            compose.onNodeWithText("Close Preview").performClick()
            scenario.recreate()
            compose.onNodeWithTag("track-editor-list").performScrollToNode(hasTestTag("track-editor-name"))
            compose.onNodeWithTag("track-editor-name").assertTextContains("Spending Log")
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(10_000) { runBlocking { app.trackRepository.tracks.first().size == 1 } }
            val track = runBlocking { app.trackRepository.tracks.first().single() }
            val projection = runBlocking { app.trackRepository.projection(track.id) }
            assertEquals(listOf("Item", "Amount", "Notes"), requireNotNull(projection).fields.map { it.name })
            assertTrue(projection.entries.isEmpty())
        }
    }
}
