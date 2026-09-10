package com.whip.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.whip.app.core.ReviewSection

internal enum class ReviewSource(val label: String, val section: ReviewSection? = null) {
    Tasks("Tasks", ReviewSection.Tasks),
    Habits("Habits", ReviewSection.Habits),
    Goals("Goals", ReviewSection.Goals),
    Gym("Gym", ReviewSection.Gym),
    Tracks("Tracks"),
}

internal enum class ReviewSourceStatus { Ready, Loading, Unavailable }

internal data class ReviewAvailability(val sources: Map<ReviewSource, ReviewSourceStatus>) {
    val complete: Boolean get() = sources.values.all { it == ReviewSourceStatus.Ready }
    val outcomesComplete: Boolean get() = sources.filterKeys { it.section != null }.values.all { it == ReviewSourceStatus.Ready }
    val readySections: Set<ReviewSection> get() = sources.filterValues { it == ReviewSourceStatus.Ready }.keys.mapNotNull { it.section }.toSet()
    val tracksReady: Boolean get() = sources[ReviewSource.Tracks] == ReviewSourceStatus.Ready
    val loading: List<ReviewSource> get() = sources.filterValues { it == ReviewSourceStatus.Loading }.keys.toList()
    val unavailable: List<ReviewSource> get() = sources.filterValues { it == ReviewSourceStatus.Unavailable }.keys.toList()

    fun retry(actions: DomainRetryActions) {
        unavailable.forEach { source ->
            when (source) {
                ReviewSource.Tasks -> actions.tasks
                ReviewSource.Habits -> actions.habits
                ReviewSource.Goals -> actions.goals
                ReviewSource.Gym -> actions.gym
                ReviewSource.Tracks -> actions.tracks
            }.invoke()
        }
    }
}

internal fun reviewAvailability(
    sections: Set<ReviewSection>,
    tasks: TaskUiState,
    habits: HabitUiState,
    goals: GoalUiState,
    gym: GymUiState,
    tracks: TrackUiState,
): ReviewAvailability {
    fun status(loading: Boolean, error: String?) = when {
        error != null -> ReviewSourceStatus.Unavailable
        loading -> ReviewSourceStatus.Loading
        else -> ReviewSourceStatus.Ready
    }
    return ReviewAvailability(linkedMapOf(
        ReviewSource.Tasks to status(tasks.loading, tasks.errorMessage),
        ReviewSource.Habits to status(habits.loading, habits.errorMessage),
        ReviewSource.Goals to status(goals.loading, goals.errorMessage),
        ReviewSource.Gym to status(gym.loading, gym.errorMessage),
        ReviewSource.Tracks to status(tracks.loading, tracks.errorMessage),
    ).filterKeys { it.section == null || it.section in sections })
}

@Composable
internal fun ReviewAvailabilityNotice(availability: ReviewAvailability, retryActions: DomainRetryActions) {
    if (availability.complete) return
    WhipNoticeCard(
        title = "Review Is Incomplete",
        message = buildList {
            if (availability.loading.isNotEmpty()) add("Loading: ${availability.loading.joinToString { it.label }}.")
            if (availability.unavailable.isNotEmpty()) add("Unavailable: ${availability.unavailable.joinToString { it.label }}.")
            add("These sources are excluded from the results and comparisons below.")
        }.joinToString("\n"),
        tone = if (availability.unavailable.isEmpty()) WhipNoticeTone.Informative else WhipNoticeTone.Error,
        actionLabel = "Retry Unavailable Sources".takeIf { availability.unavailable.isNotEmpty() },
        onAction = { availability.retry(retryActions) },
        modifier = Modifier.testTag("review-data-status"),
    )
}
