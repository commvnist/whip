package com.whip.app.domain

import java.io.Serializable
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.LocalDate

enum class ScheduleKind {
    Anytime,
    Once,
    Recurring,
}

enum class RecurrenceUnit {
    Days,
    Weeks,
    Months,
    Years,
}

enum class RecurrenceAnchor {
    Schedule,
    Completion,
}

enum class RecurrenceEnd {
    Never,
    OnDate,
    AfterCount,
}

enum class OccurrenceState {
    Open,
    Completed,
    Skipped,
}

enum class TaskProgressDisplay {
    Percent,
    Fraction,
    Both,
}

enum class RepeatStepPolicy {
    Reset,
    CarryUnfinished,
}

enum class MissedOccurrencePolicy {
    KeepOldest,
    KeepLatest,
    CurrentOnly,
}

enum class TaskPriority(val label: String) {
    None("None"),
    Low("Low"),
    Medium("Medium"),
    High("High"),
    Urgent("Urgent"),
}

enum class TaskEffort(val label: String) {
    Unspecified("Unspecified"),
    Light("Light"),
    Medium("Medium"),
    High("High"),
}

data class RecurrenceRule(
    val unit: RecurrenceUnit,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val startDate: LocalDate,
    val end: RecurrenceEnd = RecurrenceEnd.Never,
    val endDate: LocalDate? = null,
    val occurrenceCount: Int? = null,
    val anchor: RecurrenceAnchor = RecurrenceAnchor.Schedule,
) {
    init {
        require(interval > 0) { "Recurrence interval must be positive" }
        if (end == RecurrenceEnd.OnDate) {
            requireNotNull(endDate) { "An end date is required" }
            require(!endDate.isBefore(startDate)) { "End date cannot precede the start date" }
        }
        if (end == RecurrenceEnd.AfterCount) {
            require((occurrenceCount ?: 0) > 0) { "Occurrence count must be positive" }
        }
    }
}

data class WhipTask(
    val id: Long,
    val title: String,
    val notes: String,
    val scheduleKind: ScheduleKind,
    val date: LocalDate?,
    val recurrence: RecurrenceRule?,
    val timeMinutes: Int?,
    val reminderEnabled: Boolean,
    val archived: Boolean,
    val completedAtMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val showSubtaskProgress: Boolean = false,
    val progressDisplay: TaskProgressDisplay = TaskProgressDisplay.Percent,
    val autoCompleteFromSteps: Boolean = true,
    val repeatStepPolicy: RepeatStepPolicy = RepeatStepPolicy.Reset,
    val steps: List<TaskStep> = emptyList(),
    val pinned: Boolean = false,
    val priority: TaskPriority = TaskPriority.None,
    val areaId: String? = null,
    val area: String = "",
    val tags: Set<String> = emptySet(),
    val deadline: LocalDate? = null,
    val reminderOffsetsMinutes: List<Int> = emptyList(),
    val missedOccurrencePolicy: MissedOccurrencePolicy = MissedOccurrencePolicy.KeepLatest,
    /** Compatibility field; every undated Task is normalized into Inbox. */
    val inbox: Boolean = false,
    val durationMinutes: Int? = null,
    val effort: TaskEffort = TaskEffort.Unspecified,
    val manualPosition: Int = 0,
    val icon: String = DEFAULT_TASK_EMOJI,
    /** Stable persistence identity used to reject stale numeric-id aliases after restore/import. */
    val uuid: String = "",
)

data class TaskStep(
    val id: Long,
    val taskId: Long,
    val title: String,
    val position: Int,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class TaskStepDraft(
    val id: Long? = null,
    val title: String,
    val position: Int,
    val notes: String = "",
    /** Editor-only identity keeps focus and reorder announcements attached to
     * the logical draft before it has a database id. Repositories ignore it. */
    val uiKey: String = "",
)

data class TaskStepState(
    val stepId: Long,
    val taskId: Long,
    val occurrenceKey: Long,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val titleSnapshot: String,
)

data class TaskStepSnapshot(
    val stepId: Long,
    val taskId: Long,
    val occurrenceKey: Long,
    val title: String,
    val position: Int,
    val notes: String,
    val completed: Boolean,
    val completedAtMillis: Long?,
)

fun visibleTaskStepsForOccurrence(
    steps: List<TaskStep>,
    snapshots: List<TaskStepSnapshot>,
    occurrenceKey: Long,
    policy: RepeatStepPolicy,
): List<TaskStep> {
    val activeSteps = steps.filterNot(TaskStep::archived)
    if (policy == RepeatStepPolicy.Reset) return activeSteps
    val prior = snapshots
        .asSequence()
        .filter { it.occurrenceKey < occurrenceKey }
        .groupBy(TaskStepSnapshot::stepId)
        .mapValues { (_, stepSnapshots) -> stepSnapshots.maxBy(TaskStepSnapshot::occurrenceKey) }
    if (prior.isEmpty()) return activeSteps
    return activeSteps.filter { prior[it.id]?.completed != true }
}

data class ScheduledSubtask(
    val step: TaskStep,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val title: String,
    val notes: String = "",
)

data class TaskOccurrence(
    val taskId: Long,
    val originalDate: LocalDate,
    val scheduledDate: LocalDate,
    val state: OccurrenceState,
    val completedAtMillis: Long?,
)

data class TaskDraft(
    val title: String,
    val notes: String = "",
    val scheduleKind: ScheduleKind = ScheduleKind.Anytime,
    val date: LocalDate? = null,
    val recurrence: RecurrenceRule? = null,
    val timeMinutes: Int? = null,
    val reminderEnabled: Boolean = false,
    val steps: List<TaskStepDraft> = emptyList(),
    val showSubtaskProgress: Boolean = false,
    val progressDisplay: TaskProgressDisplay = TaskProgressDisplay.Percent,
    val autoCompleteFromSteps: Boolean = true,
    val repeatStepPolicy: RepeatStepPolicy = RepeatStepPolicy.Reset,
    val priority: TaskPriority = TaskPriority.None,
    val areaId: String? = null,
    val area: String = "",
    val tags: Set<String> = emptySet(),
    val deadline: LocalDate? = null,
    val reminderOffsetsMinutes: List<Int> = emptyList(),
    val missedOccurrencePolicy: MissedOccurrencePolicy = MissedOccurrencePolicy.KeepLatest,
    val inbox: Boolean = true,
    val durationMinutes: Int? = null,
    val effort: TaskEffort = TaskEffort.Unspecified,
    val icon: String = DEFAULT_TASK_EMOJI,
)

data class ScheduledTask(
    val task: WhipTask,
    val originalDate: LocalDate?,
    val scheduledDate: LocalDate?,
    val completedAtMillis: Long? = null,
    /** Persisted state for a materialized recurring occurrence. Null means the
     * occurrence is virtual (derived from the current recurrence rule). */
    val occurrenceState: OccurrenceState? = null,
    val isPastScheduledDate: Boolean = false,
    val isDeadlineOverdue: Boolean = false,
    val subtasks: List<ScheduledSubtask> = emptyList(),
) {
    val stableKey: String = "${task.id}:${originalDate?.toEpochDay() ?: "task"}"
    val completedSubtasks: Int = subtasks.count(ScheduledSubtask::completed)
    val totalSubtasks: Int = subtasks.size
    val subtaskProgress: Float = if (totalSubtasks == 0) {
        0f
    } else {
        completedSubtasks.toFloat() / totalSubtasks
    }

    val occurrenceKey: Long = originalDate?.toEpochDay()
        ?: task.date?.toEpochDay()
        ?: ANYTIME_TASK_OCCURRENCE_KEY
}

data class TaskEditSubtaskBoundary(
    val stepId: Long,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val title: String,
) : Serializable

/** Saveable optimistic-concurrency boundary for an editor opened from a concrete projection. */
data class TaskEditBoundary(
    val taskId: Long,
    val taskUuid: String,
    val taskCreatedAtMillis: Long,
    val taskRevision: String,
    val originalEpochDay: Long?,
    val scheduledEpochDay: Long?,
    val completedAtMillis: Long?,
    val occurrenceState: OccurrenceState?,
    val subtasks: List<TaskEditSubtaskBoundary>,
) : Serializable

fun ScheduledTask.toEditBoundary(): TaskEditBoundary = TaskEditBoundary(
    taskId = task.id,
    taskUuid = task.uuid,
    taskCreatedAtMillis = task.createdAtMillis,
    taskRevision = task.semanticRevisionToken(),
    originalEpochDay = originalDate?.toEpochDay(),
    scheduledEpochDay = scheduledDate?.toEpochDay(),
    completedAtMillis = completedAtMillis,
    occurrenceState = occurrenceState,
    subtasks = subtasks.map { subtask ->
        TaskEditSubtaskBoundary(
            stepId = subtask.step.id,
            completed = subtask.completed,
            completedAtMillis = subtask.completedAtMillis,
            title = subtask.title,
        )
    },
)

fun WhipTask.semanticRevisionToken(): String = MessageDigest.getInstance("SHA-256")
    .digest(toString().toByteArray(Charsets.UTF_8))
    .joinToString("") { byte -> "%02x".format(byte) }

fun WhipTask.toDraft(): TaskDraft = TaskDraft(
    title = title,
    icon = icon,
    notes = notes,
    scheduleKind = scheduleKind,
    date = date,
    recurrence = recurrence,
    timeMinutes = timeMinutes,
    reminderEnabled = reminderEnabled,
    steps = steps.filterNot(TaskStep::archived).map { step ->
        TaskStepDraft(
            id = step.id,
            title = step.title,
            position = step.position,
            notes = step.notes,
        )
    },
    showSubtaskProgress = showSubtaskProgress,
    progressDisplay = progressDisplay,
    autoCompleteFromSteps = autoCompleteFromSteps,
    repeatStepPolicy = repeatStepPolicy,
    priority = priority,
    areaId = areaId,
    area = area,
    tags = tags,
    deadline = deadline,
    reminderOffsetsMinutes = reminderOffsetsMinutes,
    missedOccurrencePolicy = missedOccurrencePolicy,
    inbox = inbox,
    durationMinutes = durationMinutes,
    effort = effort,
)

const val ANYTIME_TASK_OCCURRENCE_KEY: Long = Long.MIN_VALUE
