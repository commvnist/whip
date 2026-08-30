package com.whip.app.ui

import com.whip.app.core.HomeSection
import com.whip.app.core.WhipLaunchActions
import com.whip.app.domain.Track
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.WhipTask
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchAndHomeLoadPolicyTest {
    @Test
    fun launchResolutionWaitsOnlyForTheApplicableDomainIncludingTracks() {
        val pending = resolveLaunchTarget(
            action = WhipLaunchActions.ACTION_OPEN_TRACK,
            entityId = 42,
            occurrenceEpochDay = null,
            taskState = TaskUiState(),
            habitState = HabitUiState(loading = false),
            goalState = GoalUiState(loading = false),
            trackState = TrackUiState(),
        )
        assertEquals(LaunchTargetResolution.Pending, pending)

        val missing = resolveLaunchTarget(
            action = WhipLaunchActions.ACTION_OPEN_TRACK,
            entityId = 42,
            occurrenceEpochDay = null,
            taskState = TaskUiState(), // Other domains may still be loading.
            habitState = HabitUiState(),
            goalState = GoalUiState(),
            trackState = TrackUiState(loading = false),
        )
        assertEquals(
            LaunchTargetResolution.Unavailable(AppDestination.Tracks, "This Track is no longer available."),
            missing,
        )
    }

    @Test
    fun failedApplicableDomainRemainsRetryableInsteadOfLookingDeleted() {
        val resolution = resolveLaunchTarget(
            action = WhipLaunchActions.ACTION_OPEN_TASK,
            entityId = 7,
            occurrenceEpochDay = null,
            taskState = TaskUiState(loading = false, errorMessage = "database unavailable"),
            habitState = HabitUiState(),
            goalState = GoalUiState(),
            trackState = TrackUiState(),
        )

        assertEquals(LaunchTargetResolution.LoadFailed(AppDestination.Tasks), resolution)
    }

    @Test
    fun existingTrackDeepLinkOpensTheTrack() {
        val projection = TrackProjection(
            track = Track(42, "track-42", "Mood", "", "🙂", "health", "Health", emptyList(), false, false, 0, 1, 1),
            fields = emptyList(),
            options = emptyList(),
            entries = emptyList(),
        )

        val resolution = resolveLaunchTarget(
            action = WhipLaunchActions.ACTION_OPEN_TRACK,
            entityId = 42,
            occurrenceEpochDay = null,
            taskState = TaskUiState(),
            habitState = HabitUiState(),
            goalState = GoalUiState(),
            trackState = TrackUiState(projections = listOf(projection), loading = false),
        )

        assertEquals(
            LaunchTargetResolution.Available("health"),
            resolution,
        )
    }

    @Test
    fun recurringTaskOccurrenceOutsideTheProjectionFallsBackToTheExistingTask() {
        val task = WhipTask(
            id = 7,
            title = "Long-running routine",
            notes = "",
            scheduleKind = ScheduleKind.Recurring,
            date = LocalDate.of(2026, 8, 29),
            recurrence = null,
            timeMinutes = null,
            reminderEnabled = false,
            archived = false,
            completedAtMillis = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            areaId = "health",
        )

        val resolution = resolveLaunchTarget(
            action = WhipLaunchActions.ACTION_OPEN_TASK,
            entityId = task.id,
            occurrenceEpochDay = LocalDate.of(2028, 9, 1).toEpochDay(),
            taskState = TaskUiState(taskEntities = listOf(task), loading = false),
            habitState = HabitUiState(),
            goalState = GoalUiState(),
            trackState = TrackUiState(),
        )

        assertEquals(
            LaunchTargetResolution.Available(
                "health",
                "This Task occurrence is no longer available. Showing Tasks instead.",
            ),
            resolution,
        )
    }

    @Test
    fun homeEmptyStatesRequireEveryVisibleDomainToSettleSuccessfully() {
        val visible = listOf(HomeSection.Tasks, HomeSection.Tracks)
        val settledTasks = TaskUiState(loading = false)
        val otherHabit = HabitUiState()
        val otherGoal = GoalUiState()
        val otherGym = GymUiState()

        assertFalse(
            homeEmptyStateEligible(visible, settledTasks, otherHabit, otherGoal, TrackUiState(), otherGym),
        )
        assertFalse(
            homeEmptyStateEligible(
                visible,
                settledTasks,
                otherHabit,
                otherGoal,
                TrackUiState(loading = false, errorMessage = "failed"),
                otherGym,
            ),
        )
        assertTrue(
            homeEmptyStateEligible(
                visible,
                settledTasks,
                otherHabit,
                otherGoal,
                TrackUiState(loading = false),
                otherGym,
            ),
        )
    }
}
