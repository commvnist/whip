package com.whip.app.ui

import com.whip.app.domain.ScheduledTask

/** Preserve collection precedence for inspector requests and occurrence-key lookup. */
internal class TaskNavigationIndex(state: TaskUiState) {
    private val projected = state.inbox + state.today + state.upcoming + state.planning + state.completed + state.archived
    private val projectedKeys = projected.mapTo(hashSetOf(), ScheduledTask::stableKey)
    val items: List<ScheduledTask> = projected + state.collectionTasks().filter { it.stableKey !in projectedKeys }
    val byKey: Map<String, ScheduledTask> = items.associateBy(ScheduledTask::stableKey)
}
