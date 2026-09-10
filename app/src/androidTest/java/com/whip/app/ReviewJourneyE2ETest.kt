package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.ReviewSection
import com.whip.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReviewJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun allTrackEvidenceKeepsItsScopeAcrossAreaReviewAndRecreation() {
        val (work, trackIds) = runBlocking {
            app.backupRepository.deleteAllData()
            val work = app.areaRepository.create("Work")
            val personal = app.areaRepository.create("Personal")
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false,
                    activeAreaScope = AreaScope.One(work).storageKey)
            }
            val task = app.taskRepository.create(TaskDraft("Reviewed work", areaId = work))
            app.taskRepository.completeOccurrence(task, null)
            val ids = listOf(work, personal).mapIndexed { index, area ->
                val id = app.trackRepository.create(TrackDraft("Evidence ${index + 1}", areaId = area,
                    fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true))))
                val field = requireNotNull(app.trackRepository.projection(id)).primaryField
                app.trackRepository.addEntry(id, TrackEntryDraft(app.clock.today(), values = mapOf(
                    field.uuid to TrackValueDraft(textValue = "Saved observation ${index + 1}"),
                )))
                id
            }
            work to ids
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openReview()
            compose.onNodeWithTag("review-track-evidence").performScrollTo()
            captureVisualCatalogSurface("shared.review.area-evidence")
            assertBothTracks()
            assertWideReviewColumns()
            scenario.recreate()
            compose.onNodeWithTag("review-track-evidence").performScrollTo()
            assertBothTracks()
            if (compose.onAllNodesWithTag("review-options-toggle").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("review-options-toggle").performScrollTo().performClick()
            }
            compose.onNodeWithText("Monthly").performScrollTo().performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().reviewPeriod == ReviewPeriod.Monthly }
            val gymChoice = hasText("Gym") and hasAnyAncestor(hasTestTag("review-controls"))
            compose.onNode(gymChoice).performScrollTo().performClick()
            compose.waitUntil(10_000) { ReviewSection.Gym !in app.settingsRepository.current().reviewSections }
            scenario.recreate()
            compose.onNodeWithText("Monthly").performScrollTo().assertIsSelected()
            compose.onNode(gymChoice).assertIsNotSelected()
            captureVisualCatalogSurface("shared.review.options")
            assertWideReviewColumns()
            compose.onNodeWithTag("review-track-evidence-open").performScrollTo().performClick()
            trackIds.forEach { id -> compose.onNodeWithTag("track-card-$id").performScrollTo().assertIsDisplayed() }
            assertEquals(AreaScope.One(work).storageKey, app.settingsRepository.current().activeAreaScope)
            captureVisualCatalogSurface("shared.review.all-tracks")
        }
    }

    @Test fun archivedTaskAndHabitOutcomesRemainInReviewHistory() {
        runBlocking {
            app.backupRepository.deleteAllData()
            val work = app.areaRepository.create("Work")
            val personal = app.areaRepository.create("Personal")
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false,
                    activeAreaScope = AreaScope.One(work).storageKey)
            }
            val today = app.clock.today()
            listOf(work, personal).forEach { area ->
                val task = app.taskRepository.create(TaskDraft("Finished design review", areaId = area, scheduleKind = ScheduleKind.Once, date = today))
                app.taskRepository.completeOccurrence(task, today)
                app.taskRepository.archive(task)
                val habit = app.habitRepository.create(HabitDraft("Daily reflection", areaId = area, startDate = today))
                app.habitRepository.setCheckOff(habit, today, true)
                app.habitRepository.setArchived(habit, true)
            }
            val recurring = app.taskRepository.create(TaskDraft("Daily design notes", areaId = work,
                scheduleKind = ScheduleKind.Recurring,
                recurrence = RecurrenceRule(RecurrenceUnit.Days, startDate = today.minusDays(1))))
            app.taskRepository.completeOccurrence(recurring, today.minusDays(1))
            app.taskRepository.completeOccurrence(recurring, today)
            app.taskRepository.archive(recurring)
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openReview()
            assertArchivedTotals()
            captureVisualCatalogSurface("shared.review.archived-outcomes")
            scenario.recreate()
            assertArchivedTotals()
            compose.onAllNodesWithText("No Outcomes in This View").assertCountEquals(0)
        }
    }

    @Test fun trackOnlyHistoryOffersReviewWithoutInventingOutcomes() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Dark) }
            val id = app.trackRepository.create(TrackDraft("Observations", fields = listOf(
                TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true),
            )))
            val field = requireNotNull(app.trackRepository.projection(id)).primaryField
            app.trackRepository.addEntry(id, TrackEntryDraft(app.clock.today(), values = mapOf(
                field.uuid to TrackValueDraft(textValue = "A useful observation"),
            )))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use {
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Review Progress").fetchSemanticsNodes().isNotEmpty() }
            captureVisualCatalogSurface("shared.review.track-only-home")
            openReview()
            compose.onNodeWithTag("review-track-evidence").assertIsDisplayed()
            compose.onAllNodesWithTag("review-signal-Tasks").assertCountEquals(0)
            compose.onNodeWithText("No Outcomes in This View").assertExists()
            captureVisualCatalogSurface("shared.review.track-only-evidence")
        }
    }

    @Test fun changingReviewOptionsRevealsSavedOutcomesWithoutLosingTheView() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false,
                    reviewPeriod = ReviewPeriod.Weekly, reviewSections = setOf(ReviewSection.Habits))
            }
            val task = app.taskRepository.create(TaskDraft("Recorded design progress"))
            app.taskRepository.completeOccurrence(task, null)
            val olderDate = app.clock.today().minusDays(40)
            val habit = app.habitRepository.create(HabitDraft("Earlier reflection", startDate = olderDate))
            app.habitRepository.setCheckOff(habit, olderDate, true)
            app.habitRepository.setArchived(habit, true)
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openReview()
            compose.waitForIdle()
            captureVisualCatalogSurface("shared.review.empty-selection")
            compose.onNodeWithText("No Outcomes in This View").assertIsDisplayed()
            compose.onAllNodesWithTag("review-signal-Tasks").assertCountEquals(0)
            if (compose.onAllNodesWithTag("review-options-toggle").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("review-options-toggle").performScrollTo().performClick()
            }
            val tasksChoice = hasText("Tasks") and hasAnyAncestor(hasTestTag("review-controls"))
            compose.onNode(tasksChoice).performScrollTo().performClick()
            compose.waitUntil(10_000) { ReviewSection.Tasks in app.settingsRepository.current().reviewSections }
            compose.onNodeWithTag("review-signal-Tasks").performScrollTo()
            compose.onNodeWithTag("review-total-Tasks", useUnmergedTree = true).assertTextEquals("1")
            captureVisualCatalogSurface("shared.review.selection-outcomes")
            scenario.recreate()
            compose.onNode(tasksChoice).performScrollTo().assertIsSelected()
            compose.onNodeWithTag("review-total-Tasks", useUnmergedTree = true).assertTextEquals("1")
            compose.onNode(tasksChoice).performScrollTo().performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().reviewSections == setOf(ReviewSection.Habits) }
            scenario.recreate()
            compose.onNodeWithText("No Outcomes in This View").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("review-close-action").assertIsDisplayed().performClick()
            openReview()
            compose.onNodeWithText("No Outcomes in This View").performScrollTo().assertIsDisplayed()
            captureVisualCatalogSurface("shared.review.empty-selection-reopened")
        }
    }

    private fun openReview() {
        val action = (hasText("Review & Trends") or hasText("Review Progress")) and hasClickAction()
        compose.waitUntil(15_000) { compose.onAllNodes(action).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(action)[0].performScrollTo().performClick()
        compose.onNodeWithContentDescription("Close Review & Trends").assertIsDisplayed()
    }

    private fun assertBothTracks() {
        compose.onNode(hasText("2 entries across 2 touched Tracks.", substring = true) and
            hasAnyAncestor(hasTestTag("review-track-evidence"))).assertIsDisplayed()
    }

    private fun assertArchivedTotals() {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("review-total-Tasks") and hasText("3"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodes(hasTestTag("review-total-Habits") and hasText("1"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("review-total-Tasks", useUnmergedTree = true).assertTextEquals("3")
        compose.onNodeWithTag("review-total-Habits", useUnmergedTree = true).assertTextEquals("1")
    }

    private fun assertWideReviewColumns() {
        if (compose.onAllNodesWithTag("review-wide-dashboard").fetchSemanticsNodes().isEmpty()) return
        val grid = compose.onNodeWithTag("review-signal-grid").fetchSemanticsNode().boundsInRoot
        val widthDp = grid.width / compose.density.density
        if (widthDp < 620f || widthDp >= 1_240f) return
        val tasks = compose.onNodeWithTag("review-signal-Tasks").fetchSemanticsNode().boundsInRoot
        val habits = compose.onNodeWithTag("review-signal-Habits").fetchSemanticsNode().boundsInRoot
        val goals = compose.onNodeWithTag("review-signal-Goals").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("The first two outcome cards must share a row: $tasks / $habits", kotlin.math.abs(tasks.top - habits.top) <= 1f)
        org.junit.Assert.assertTrue("The two columns must use the available content width", kotlin.math.abs(tasks.left - grid.left) <= 1f && kotlin.math.abs(habits.right - grid.right) <= 1f)
        org.junit.Assert.assertTrue("The next row must preserve the column width even with only three sections", goals.top >= maxOf(tasks.bottom, habits.bottom) && kotlin.math.abs(goals.width - tasks.width) <= 1f)
    }
}
