package com.whip.app.ui

import com.whip.app.core.ReviewSection
import org.junit.Assert.*
import org.junit.Test

class ReviewAvailabilityTest {
    @Test fun incompleteAndFailedSourcesCannotBecomeCurrentOutcomes() {
        val result = availability(ReviewSection.entries.toSet(), taskLoading = true, habitError = "Unavailable", gymLoading = true)
        assertFalse(result.complete)
        assertFalse(result.outcomesComplete)
        assertEquals(setOf(ReviewSection.Goals), result.readySections)
        assertEquals(listOf(ReviewSource.Tasks, ReviewSource.Gym), result.loading)
        assertEquals(listOf(ReviewSource.Habits), result.unavailable)
        assertTrue(result.tracksReady)
    }

    @Test fun hiddenOutcomeSourcesDoNotLimitTheReviewButGlobalTracksStillDo() {
        val result = availability(setOf(ReviewSection.Tasks), habitError = "Unavailable", gymLoading = true, trackError = "Unavailable")
        assertFalse(result.complete)
        assertTrue(result.outcomesComplete)
        assertEquals(setOf(ReviewSection.Tasks), result.readySections)
        assertEquals(listOf(ReviewSource.Tracks), result.unavailable)
        assertTrue(result.loading.isEmpty())
        assertFalse(result.tracksReady)
        val retried = mutableListOf<String>()
        result.retry(DomainRetryActions(tasks = { retried += "Tasks" }, habits = { retried += "Habits" },
            goals = { retried += "Goals" }, gym = { retried += "Gym" }, tracks = { retried += "Tracks" }))
        assertEquals(listOf("Tracks"), retried)
    }

    @Test fun failureTakesPrecedenceOverLoadingAndRecoveryRestoresReadySections() {
        val failed = availability(ReviewSection.entries.toSet(), taskLoading = true, taskError = "Unavailable")
        assertEquals(listOf(ReviewSource.Tasks), failed.unavailable)
        assertTrue(failed.loading.isEmpty())
        assertFalse(ReviewSection.Tasks in failed.readySections)
        val recovered = availability(ReviewSection.entries.toSet())
        assertTrue(recovered.complete)
        assertTrue(recovered.outcomesComplete)
        assertEquals(ReviewSection.entries.toSet(), recovered.readySections)
    }

    private fun availability(sections: Set<ReviewSection>, taskLoading: Boolean = false, taskError: String? = null,
        habitError: String? = null, gymLoading: Boolean = false, trackError: String? = null) = reviewAvailability(
        sections, TaskUiState(loading = taskLoading, errorMessage = taskError),
        HabitUiState(loading = false, errorMessage = habitError), GoalUiState(loading = false),
        GymUiState(loading = gymLoading), TrackUiState(loading = false, errorMessage = trackError),
    )
}
