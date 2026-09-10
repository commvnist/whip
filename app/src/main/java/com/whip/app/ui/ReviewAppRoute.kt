package com.whip.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.whip.app.core.ReviewSection
import com.whip.app.core.zoneId
import com.whip.app.domain.AreaScope

/** References the shell's existing saved navigation state; it owns no duplicate destination state. */
@Stable
internal class ReviewNavigationState(
    val destination: MutableState<AppDestination>,
    val taskDestination: MutableState<TaskDestination>,
    val taskActions: MutableState<String?>,
    val completedTask: MutableState<String?>,
    val habit: MutableState<Long?>,
    val goal: MutableState<Long?>,
    val gymDomain: MutableState<SearchDomain?>,
    val gymId: MutableState<Long?>,
) {
    fun open(outcome: ReviewOutcome, tasks: TaskUiState) {
        when (outcome.section) {
            ReviewSection.Tasks -> {
                if (outcome.archived) {
                    taskDestination.value = TaskDestination.Archived
                    taskActions.value = tasks.archived.firstOrNull { it.task.id == outcome.sourceId }?.stableKey
                } else {
                    taskDestination.value = TaskDestination.Completed
                    completedTask.value = outcome.sourceKey
                }
                destination.value = AppDestination.Tasks
            }
            ReviewSection.Habits -> { habit.value = outcome.sourceId; destination.value = AppDestination.Habits }
            ReviewSection.Goals -> { goal.value = outcome.sourceId; destination.value = AppDestination.Goals }
            ReviewSection.Gym -> {
                gymDomain.value = SearchDomain.Workout
                gymId.value = outcome.sourceId
                destination.value = AppDestination.Gym
            }
        }
    }

    fun openSection(section: ReviewSection) {
        destination.value = when (section) {
            ReviewSection.Tasks -> { taskDestination.value = TaskDestination.Completed; AppDestination.Tasks }
            ReviewSection.Habits -> AppDestination.Habits
            ReviewSection.Goals -> AppDestination.Goals
            ReviewSection.Gym -> AppDestination.Gym
        }
    }
}

@Composable
internal fun ReviewAppRoute(
    tasks: TaskUiState,
    habits: HabitUiState,
    goals: GoalUiState,
    gym: GymUiState,
    tracks: TrackUiState,
    settingsState: SettingsUiState,
    settingsViewModel: SettingsViewModel?,
    areaScope: AreaScope,
    leadingPaneWidth: Dp,
    hingeWidth: Dp,
    retryActions: DomainRetryActions,
    navigation: ReviewNavigationState,
    onTemporarilySelectAreaScope: (AreaScope) -> Unit,
    onDismiss: () -> Unit,
) {
    ReviewDialog(
        taskState = tasks, habitState = habits, goalState = goals, gymState = gym,
        period = settingsState.settings.reviewPeriod,
        modifier = Modifier.fillMaxSize(), wideLeadingPaneWidth = leadingPaneWidth, wideHingeWidth = hingeWidth,
        zone = settingsState.settings.zoneId(),
        onPeriodChange = { value -> settingsViewModel?.update { it.copy(reviewPeriod = value) } },
        onDismiss = onDismiss,
        sections = settingsState.settings.reviewSections,
        onSectionsChange = { settingsViewModel?.setReviewSections(it) },
        onOpenOutcome = { navigation.open(it, tasks); onDismiss() },
        onDrillDown = { navigation.openSection(it); onDismiss() },
        productivityAreaLabel = reviewProductivityAreaLabel(areaScope, settingsState.areas),
        trackState = tracks, retryActions = retryActions,
        onOpenTracks = {
            if (areaScope != AreaScope.All) onTemporarilySelectAreaScope(AreaScope.All)
            navigation.destination.value = AppDestination.Tracks
            onDismiss()
        },
    )
}
