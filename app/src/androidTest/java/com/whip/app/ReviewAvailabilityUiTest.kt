package com.whip.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.whip.app.core.AppSettings
import com.whip.app.core.ReviewSection
import com.whip.app.core.ReviewPeriod
import com.whip.app.domain.*
import com.whip.app.ui.*
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Controlled source states through the production Home → Review host and retry boundary. */
class ReviewAvailabilityUiTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 9, 10)

    @Test fun partialReviewPreservesAvailableOutcomesAndRetriesOnlyUnavailableSources() {
        val habits = mutableStateOf(HabitUiState(loading = true))
        val goals = mutableStateOf(GoalUiState(loading = false, errorMessage = "Goal storage unavailable"))
        val tracks = mutableStateOf(savedTracks().copy(errorMessage = "Track storage unavailable"))
        val settings = mutableStateOf(AppSettings(setupCompleted = true))
        val retried = mutableListOf<String>()
        val restore = StateRestorationTester(compose)
        restore.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                WhipScreen(
                    state = completedTasks(), habitState = habits.value, goalState = goals.value,
                    trackState = tracks.value, gymState = GymUiState(loading = false),
                    settingsState = SettingsUiState(settings = settings.value),
                    domainRetryActions = DomainRetryActions(
                        tasks = { retried += "Tasks" }, habits = { retried += "Habits" },
                        goals = { retried += "Goals"; goals.value = GoalUiState(loading = true) },
                        tracks = { retried += "Tracks"; tracks.value = TrackUiState(loading = true) },
                        gym = { retried += "Gym" },
                    ),
                    onSaveTask = { _, _, _ -> }, onComplete = {}, onSkip = {},
                    onReschedule = { _, _ -> }, onArchive = {}, onReopen = {},
                )
            }
        }
        openReview()
        compose.waitForIdle()
        captureVisualCatalogSurface("shared.review.partial-sources")
        compose.onNodeWithTag("review-data-status").assertExists()
        compose.onNodeWithTag("review-total-Tasks", useUnmergedTree = true).assertTextEquals("1")
        compose.onAllNodesWithTag("review-total-Habits", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("review-total-Goals", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("review-track-evidence").assertCountEquals(0)
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("review-data-status").performScrollTo().assertIsDisplayed()
        // A hidden outcome source must not be retried, while Tracks remain global evidence.
        compose.runOnIdle { settings.value = settings.value.copy(reviewSections = setOf(ReviewSection.Tasks, ReviewSection.Gym)) }
        compose.onNodeWithText("Retry Unavailable Sources").performScrollTo().performClick()
        assertEquals(listOf("Tracks"), retried)
        compose.runOnIdle {
            tracks.value = savedTracks()
            settings.value = settings.value.copy(reviewSections = ReviewSection.entries.toSet())
        }
        compose.onNodeWithText("Retry Unavailable Sources").performScrollTo().performClick()
        assertEquals(listOf("Tracks", "Goals"), retried)
        compose.onAllNodesWithText("Retry Unavailable Sources").assertCountEquals(0)
        compose.runOnIdle {
            habits.value = HabitUiState(loading = false)
            goals.value = GoalUiState(loading = false)
        }
        compose.onAllNodesWithTag("review-data-status").assertCountEquals(0)
        compose.onNodeWithTag("review-total-Tasks", useUnmergedTree = true).assertTextEquals("1")
        compose.onNodeWithTag("review-total-Habits", useUnmergedTree = true).assertTextEquals("0")
        compose.onNodeWithTag("review-total-Goals", useUnmergedTree = true).assertTextEquals("0")
        compose.onNodeWithTag("review-track-evidence").performScrollTo().assertIsDisplayed()
        captureVisualCatalogSurface("shared.review.sources-recovered")
    }

    @Test fun incompleteSourcesNeverClaimThereAreNoOutcomes() {
        val ready = mutableStateOf(false)
        val failed = mutableStateOf(false)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                WhipScreen(
                    state = when {
                        ready.value -> TaskUiState(loading = false)
                        failed.value -> TaskUiState(loading = false, errorMessage = "Task storage unavailable")
                        else -> completedTasks()
                    },
                    habitState = HabitUiState(loading = !ready.value), goalState = GoalUiState(loading = !ready.value),
                    gymState = GymUiState(loading = !ready.value), trackState = TrackUiState(loading = !ready.value),
                    settingsState = SettingsUiState(settings = AppSettings(setupCompleted = true)),
                    onSaveTask = { _, _, _ -> }, onComplete = {}, onSkip = {},
                    onReschedule = { _, _ -> }, onArchive = {}, onReopen = {},
                )
            }
        }
        openReview()
        compose.runOnIdle { failed.value = true }
        compose.waitForIdle()
        captureVisualCatalogSurface("shared.review.sources-unavailable")
        compose.onNodeWithTag("review-data-status").assertExists()
        compose.onAllNodesWithText("No Outcomes in This View").assertCountEquals(0)
        compose.onAllNodesWithTag("review-total-Tasks", useUnmergedTree = true).assertCountEquals(0)
        compose.runOnIdle { ready.value = true }
        compose.onAllNodesWithTag("review-data-status").assertCountEquals(0)
        compose.onNodeWithText("No Outcomes in This View").performScrollTo().assertIsDisplayed()
        captureVisualCatalogSurface("shared.review.sources-ready-empty")
    }

    @Test fun correlationsUseReadySourcesEvenWhenOtherEvidenceIsUnavailable() {
        val gymFailed = mutableStateOf(false)
        val task = completedTasks().completed.single()
        val tasks = completedTasks().copy(completed = (0L until 10L).map { offset ->
            val date = today.minusDays(offset)
            task.copy(originalDate = date, scheduledDate = date, completedAtMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        })
        val history = (0L until 10L).map { offset ->
            val date = today.minusDays(offset)
            val instant = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
            WorkoutSession(offset, "workout-$offset", "Workout", "", instant, instant, date, "UTC",
                WorkoutSessionState.Finished, false, null, null, false, 1, 1)
        }
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ReviewDialog(
                    taskState = tasks, habitState = HabitUiState(loading = false), goalState = GoalUiState(loading = false),
                    gymState = GymUiState(history = history, loading = false, errorMessage = "Gym unavailable".takeIf { gymFailed.value }),
                    trackState = TrackUiState(loading = false, errorMessage = "Tracks unavailable"),
                    period = ReviewPeriod.Weekly, sections = setOf(ReviewSection.Tasks, ReviewSection.Gym),
                    onPeriodChange = {}, onDismiss = {},
                )
            }
        }
        val comparison = hasText("Tasks ↔ Workouts: 1.00 (n=30)")
        compose.onNode(comparison).performScrollTo().assertIsDisplayed()
        captureVisualCatalogSurface("shared.review.available-correlation")
        compose.runOnIdle { gymFailed.value = true }
        compose.onAllNodes(comparison).assertCountEquals(0)
        compose.onAllNodesWithTag("review-total-Gym", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("review-data-status").performScrollTo().assertIsDisplayed()
        captureVisualCatalogSurface("shared.review.unavailable-correlation")
        compose.runOnIdle { gymFailed.value = false }
        compose.onNode(comparison).performScrollTo().assertIsDisplayed()
    }

    private fun openReview() {
        val action = (hasText("Review & Trends") or hasText("Review Progress")) and hasClickAction()
        compose.onAllNodes(action)[0].performScrollTo().performClick()
        compose.onNodeWithContentDescription("Close Review & Trends").assertIsDisplayed()
    }

    private fun completedTasks(): TaskUiState {
        val task = WhipTask(1, "Reviewed work", "", ScheduleKind.Once, today, null, null,
            false, false, 1, 1, 1)
        return TaskUiState(completed = listOf(ScheduledTask(task, today, today,
            today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())), currentDate = today, loading = false)
    }

    private fun savedTracks() = TrackUiState(loading = false, projections = listOf(TrackProjection(
        track = Track(1, "track-1", "Observations", "", "📌", "", "", emptyList(), false, false, 0, 0, 0),
        fields = emptyList(), options = emptyList(), entries = listOf(TrackEntryProjection(
            TrackEntry(1, "entry-1", 1, today, 0, 0), emptyMap(),
        )),
    )))
}
