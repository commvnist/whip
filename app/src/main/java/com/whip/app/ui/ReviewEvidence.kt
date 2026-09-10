package com.whip.app.ui

import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope

internal fun reviewProductivityAreaLabel(scope: AreaScope, areas: List<Area>): String? = when (scope) {
    AreaScope.All -> null
    AreaScope.Unassigned -> "Main"
    is AreaScope.One -> areas.firstOrNull { it.id == scope.areaId }?.name ?: "Selected Area"
}

/** Archiving removes a Task from working collections, not from its completed history. */
internal fun reviewCompletedTasks(state: TaskUiState): List<ScheduledTask> {
    val archived = state.archived.associateBy { it.task.id }
    val archivedOccurrences = state.occurrences.mapNotNull { occurrence ->
        val item = archived[occurrence.taskId] ?: return@mapNotNull null
        if (occurrence.state != OccurrenceState.Completed) return@mapNotNull null
        ScheduledTask(
            task = item.task,
            originalDate = occurrence.originalDate,
            scheduledDate = occurrence.scheduledDate,
            completedAtMillis = occurrence.completedAtMillis,
            occurrenceState = occurrence.state,
        )
    }
    return (state.completed + archived.values.filter { it.completedAtMillis != null } + archivedOccurrences)
        .distinctBy(ScheduledTask::stableKey)
}
