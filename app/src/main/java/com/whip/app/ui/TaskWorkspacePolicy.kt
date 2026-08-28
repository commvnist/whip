package com.whip.app.ui

import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.normalizedNavigation
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskQuickCaptureParser
import com.whip.app.domain.TaskStepDraft
import java.time.LocalDate

/** Primary destinations shown in the Tasks workspace. */
internal enum class TaskWorkspaceDestination(val label: String) {
    Inbox("Inbox"),
    Today("Today"),
    Upcoming("Upcoming"),
    History("History"),
}

internal val primaryTaskWorkspaceDestinations = listOf(
    TaskWorkspaceDestination.Today,
    TaskWorkspaceDestination.Inbox,
    TaskWorkspaceDestination.Upcoming,
)

internal val allTaskWorkspaceDestinations = primaryTaskWorkspaceDestinations + TaskWorkspaceDestination.History

/** History keeps completion and archival behavior distinct inside one route. */
internal enum class TaskHistorySection(val label: String) {
    Completed("Completed"),
    Archived("Archived"),
}

internal enum class TaskPlanningView { List, Agenda, Calendar }

internal data class TaskWorkspaceRoute(
    val destination: TaskWorkspaceDestination,
    val historySection: TaskHistorySection = TaskHistorySection.Completed,
)

internal fun TaskWorkspaceDestination.allowedPlanningViews(): List<TaskPlanningView> =
    if (this == TaskWorkspaceDestination.Upcoming) {
        TaskPlanningView.entries
    } else {
        listOf(TaskPlanningView.List)
    }

internal fun TaskWorkspaceDestination.normalizePlanningView(view: TaskPlanningView): TaskPlanningView =
    view.takeIf { it in allowedPlanningViews() } ?: TaskPlanningView.List

internal fun TaskDestination.toWorkspaceRoute(): TaskWorkspaceRoute = when (this) {
    TaskDestination.Inbox -> TaskWorkspaceRoute(TaskWorkspaceDestination.Inbox)
    TaskDestination.Today -> TaskWorkspaceRoute(TaskWorkspaceDestination.Today)
    TaskDestination.Upcoming -> TaskWorkspaceRoute(TaskWorkspaceDestination.Upcoming)
    TaskDestination.Completed -> TaskWorkspaceRoute(
        TaskWorkspaceDestination.History,
        TaskHistorySection.Completed,
    )
    TaskDestination.Archived -> TaskWorkspaceRoute(
        TaskWorkspaceDestination.History,
        TaskHistorySection.Archived,
    )
}

internal fun TaskWorkspaceRoute.dataDestination(): TaskDestination = when (destination) {
    TaskWorkspaceDestination.Inbox -> TaskDestination.Inbox
    TaskWorkspaceDestination.Today -> TaskDestination.Today
    TaskWorkspaceDestination.Upcoming -> TaskDestination.Upcoming
    TaskWorkspaceDestination.History -> when (historySection) {
        TaskHistorySection.Completed -> TaskDestination.Completed
        TaskHistorySection.Archived -> TaskDestination.Archived
    }
}

/** Keeps every saved filter inside the views supported by its destination. */
internal fun SavedTaskFilter.normalizedForWorkspace(): SavedTaskFilter {
    return normalizedNavigation()
}

/** User-selected task sorting. Missing dates remain last in either direction. */
internal fun List<ScheduledTask>.sortedForWorkspace(
    sortMode: String,
    direction: SortDirection,
): List<ScheduledTask> {
    val ascending = direction == SortDirection.Ascending
    return when (sortMode) {
        "Title" -> sortedWith(
            (if (ascending) compareBy<ScheduledTask> { it.task.title.lowercase() }
            else compareByDescending { it.task.title.lowercase() })
                .thenBy { it.task.createdAtMillis },
        )
        "Scheduled Date" -> sortedWith(
            compareBy<ScheduledTask, LocalDate?>(nullsLast(if (ascending) naturalOrder() else reverseOrder())) { it.scheduledDate }
                .thenBy { it.task.title.lowercase() },
        )
        "Deadline" -> sortedWith(
            compareBy<ScheduledTask, LocalDate?>(nullsLast(if (ascending) naturalOrder() else reverseOrder())) { it.task.deadline }
                .thenBy { it.task.title.lowercase() },
        )
        "Completion Date" -> sortedWith(
            compareBy<ScheduledTask, Long?>(nullsLast(if (ascending) naturalOrder() else reverseOrder())) { it.completedAtMillis }
                .thenBy { it.task.title.lowercase() },
        )
        "Archived Date" -> sortedWith(
            (if (ascending) compareBy<ScheduledTask> { it.task.updatedAtMillis }
            else compareByDescending { it.task.updatedAtMillis })
                .thenBy { it.task.title.lowercase() },
        )
        "Priority" -> sortedWith(
            (if (ascending) compareBy<ScheduledTask> { it.task.priority.ordinal }
            else compareByDescending { it.task.priority.ordinal })
                .thenBy { it.task.title.lowercase() },
        )
        // Manual is an authored sequence, not a scalar field. Reversing it while
        // drag-reordering would make every move appear to undo itself.
        "Manual" -> sortedWith(
            compareByDescending<ScheduledTask> { it.task.pinned }
                .thenBy { it.task.manualPosition }
                .thenBy { it.task.createdAtMillis },
        )
        else -> this
    }
}

internal fun buildQuickAddTaskDraft(
    capture: String,
    defaultDate: LocalDate?,
    areaId: String?,
    smartCaptureToday: LocalDate? = null,
): TaskDraft? {
    val lines = capture.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
    if (lines.isEmpty()) return null
    val parsed = smartCaptureToday
        ?.let { today -> TaskQuickCaptureParser.parse(lines.first(), today) }
        ?.takeIf { it.assumptions.isNotEmpty() }
    val scheduleKind = parsed?.scheduleKind
        ?: if (defaultDate == null) ScheduleKind.Anytime else ScheduleKind.Once
    return TaskDraft(
        title = parsed?.title ?: lines.first(),
        scheduleKind = scheduleKind,
        date = if (parsed == null) {
            defaultDate
        } else {
            parsed.date.takeIf { scheduleKind == ScheduleKind.Once }
        },
        recurrence = parsed?.recurrence,
        deadline = parsed?.deadline,
        timeMinutes = parsed?.timeMinutes,
        reminderEnabled = parsed?.reminderEnabled == true,
        reminderOffsetsMinutes = parsed?.reminderOffsetsMinutes.orEmpty(),
        priority = parsed?.priority ?: TaskPriority.None,
        durationMinutes = parsed?.durationMinutes,
        effort = parsed?.effort ?: TaskEffort.Unspecified,
        tags = parsed?.tags.orEmpty(),
        areaId = areaId,
        inbox = scheduleKind == ScheduleKind.Anytime,
        steps = lines.drop(1).mapIndexed { index, title ->
            TaskStepDraft(title = title, position = index)
        },
    )
}
