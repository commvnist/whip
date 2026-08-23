package com.whip.app.ui

import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.normalizedNavigation
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskQuickCaptureParser
import com.whip.app.domain.TaskStepDraft
import java.time.LocalDate

/** Primary destinations shown in the Tasks workspace. */
internal enum class TaskWorkspaceDestination(val label: String) {
    Inbox("Inbox"),
    Today("Today"),
    Upcoming("Upcoming"),
    Anytime("Anytime"),
    History("History"),
}

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
    TaskDestination.Anytime -> TaskWorkspaceRoute(TaskWorkspaceDestination.Anytime)
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
    TaskWorkspaceDestination.Anytime -> TaskDestination.Anytime
    TaskWorkspaceDestination.History -> when (historySection) {
        TaskHistorySection.Completed -> TaskDestination.Completed
        TaskHistorySection.Archived -> TaskDestination.Archived
    }
}

/** Keeps every saved filter inside the views supported by its destination. */
internal fun SavedTaskFilter.normalizedForWorkspace(): SavedTaskFilter {
    return normalizedNavigation()
}

internal fun buildQuickAddTaskDraft(
    capture: String,
    today: LocalDate,
    defaultDate: LocalDate?,
    inbox: Boolean,
    areaId: String?,
): TaskDraft? {
    val lines = capture.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
    if (lines.isEmpty()) return null
    val parsed = TaskQuickCaptureParser.parse(lines.first(), today)
    val usesDestinationDate = parsed.scheduleKind == ScheduleKind.Anytime && defaultDate != null
    val scheduleKind = if (usesDestinationDate) ScheduleKind.Once else parsed.scheduleKind
    return TaskDraft(
        title = parsed.title,
        scheduleKind = scheduleKind,
        date = if (usesDestinationDate) defaultDate else parsed.date,
        recurrence = parsed.recurrence,
        deadline = parsed.deadline,
        areaId = areaId,
        inbox = inbox && scheduleKind == ScheduleKind.Anytime,
        steps = lines.drop(1).mapIndexed { index, title ->
            TaskStepDraft(title = title, position = index)
        },
    )
}
