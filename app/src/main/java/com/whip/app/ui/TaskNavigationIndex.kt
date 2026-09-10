package com.whip.app.ui

import com.whip.app.domain.ScheduledTask

/** Preserve collection precedence for inspector requests and occurrence-key lookup. */
internal class TaskNavigationIndex(state: TaskUiState) {
    val items: List<ScheduledTask> = state.inbox + state.today + state.upcoming +
        state.planning + state.completed + state.archived
    val byKey: Map<String, ScheduledTask> = items.associateBy(ScheduledTask::stableKey)
}
