package com.whip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import com.whip.app.domain.RecurrenceEnd
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskProgressDisplay
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.WhipTask
import com.whip.app.data.TaskDeletionImpact
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.whip.app.ui.theme.whipColors

@Composable
fun SectionHeading(title: String, count: Int, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClickLabel = "Open $title", onClick = onClick))
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun TaskRow(
    item: ScheduledTask,
    completed: Boolean,
    onComplete: (() -> Unit)?,
    onOpenActions: (() -> Unit)?,
    onEdit: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null,
) {
    val compact = LocalCompactItemLayout.current
    val disclosure = rememberCompactItemDisclosure("task:${item.stableKey}")
    val compactSummary = item.detailLabel(completed)
    ProductivityItemCard(
        modifier = Modifier.then(
            when {
                selectionMode && onSelectionToggle != null -> Modifier.clickable(
                    onClickLabel = "Select ${item.task.title}",
                    onClick = onSelectionToggle,
                )
                onOpenActions != null -> Modifier
                    .clickable(
                        onClickLabel = "Open task details for ${item.task.title}",
                        onClick = onOpenActions,
                    )
                    .semantics { contentDescription = "Open task details for ${item.task.title}" }
                else -> Modifier
            },
        ),
        containerColor = when {
            completed -> MaterialTheme.colorScheme.surfaceContainerLow
            item.isDeadlineOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        ProductivityItemHeader(
            itemType = "task",
            itemName = item.task.title,
            emoji = item.task.icon,
            areaId = item.task.areaId,
            areaName = item.task.area,
            onEdit = onEdit.takeUnless { selectionMode },
            identityModifier = Modifier.testTag("task-icon-${item.task.id}"),
            primaryActionModifier = Modifier.testTag("task-primary-action-${item.task.id}"),
            editModifier = Modifier.testTag("task-edit-action-${item.task.id}"),
            titleTextDecoration = TextDecoration.LineThrough.takeIf { completed },
            headlineAccessory = if (item.isDeadlineOverdue || item.isPastScheduledDate) {
                {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (item.isDeadlineOverdue) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ) {
                        Text(
                            if (item.isDeadlineOverdue) "Deadline Overdue" else "Past Scheduled Date",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isDeadlineOverdue) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            } else null,
            supportingContent = {
                Text(
                    item.detailLabel(completed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compact) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.task.notes.isNotBlank()) Text(
                    item.task.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            },
            compactSummaryContent = {
                if (compactSummary.isNotBlank()) {
                    Text(
                        compactSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            compactExpanded = disclosure.expanded,
            onCompactExpansionToggle = disclosure.toggle.takeIf { compact },
            compactExpansionTag = "task-expand-${item.task.id}",
            primaryAction = {
                Checkbox(
                    checked = if (selectionMode) selected else completed,
                    onCheckedChange = {
                        if (selectionMode) onSelectionToggle?.invoke() else onComplete?.invoke()
                    },
                    enabled = if (selectionMode) onSelectionToggle != null else onComplete != null,
                    modifier = Modifier.semantics {
                        contentDescription = if (selectionMode) {
                            if (selected) "Deselect task ${item.task.title}" else "Select task ${item.task.title}"
                        } else if (completed) {
                            "Task ${item.task.title} completed"
                        } else "Complete task ${item.task.title}"
                    },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.whipColors.success),
                )
            },
        )
        if ((!compact || disclosure.expanded) && item.task.showSubtaskProgress && item.totalSubtasks > 0) {
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinearProgressIndicator(progress = { item.subtaskProgress }, modifier = Modifier.weight(1f))
                    Text(
                        item.progressLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                LinearProgressIndicator(progress = { item.subtaskProgress }, modifier = Modifier.fillMaxWidth())
                Text(
                    item.progressLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun TaskActionsDialog(
    item: ScheduledTask,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onReschedule: () -> Unit,
    onSkip: () -> Unit,
    onArchive: () -> Unit,
    onDeletePermanently: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier,
    onDuplicate: () -> Unit,
    onStartFocus: (Int) -> Unit,
    onToggleSubtask: (Long, Boolean) -> Unit,
    onPromoteSubtask: (Long) -> Unit,
    occurrenceHistory: List<TaskOccurrence> = emptyList(),
    onReopenOccurrence: (TaskOccurrence) -> Unit,
    onResetOccurrence: (TaskOccurrence) -> Unit,
) {
    var section by rememberSaveable(item.stableKey) { mutableStateOf(TaskDetailSection.Overview) }
    var pendingMoveStepId by rememberSaveable(item.stableKey) { mutableStateOf<Long?>(null) }
    ProductivityEditorDialog(
        modifier = modifier.widthIn(min = 280.dp, max = 560.dp),
        testTag = "task-actions-surface",
        onDismissRequest = onDismiss,
        title = { Text(item.task.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DetailSectionBar(
                    labels = TaskDetailSection.entries.map(TaskDetailSection::label),
                    selected = section.label,
                    onSelect = { label -> section = TaskDetailSection.entries.first { it.label == label } },
                    testTagPrefix = "task-detail-section",
                )
                if (section == TaskDetailSection.Overview) {
                    if (item.subtasks.isEmpty()) Text("No subtasks. Edit this task to add them.")
                    else {
                    Text(
                        "Subtasks · ${item.completedSubtasks}/${item.totalSubtasks}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    item.subtasks.forEach { subtask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (item.task.archived) Modifier else Modifier.clickable(onClickLabel = "Toggle ${subtask.step.title}") {
                                        onToggleSubtask(subtask.step.id, !subtask.completed)
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = subtask.completed,
                                enabled = !item.task.archived,
                                onCheckedChange = { checked ->
                                    onToggleSubtask(subtask.step.id, checked)
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = if (subtask.completed) {
                                        "Mark Subtask ${subtask.step.title} incomplete"
                                    } else "Complete Subtask ${subtask.step.title}"
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    subtask.title,
                                    textDecoration = TextDecoration.LineThrough.takeIf {
                                        subtask.completed
                                    },
                                )
                                if (subtask.notes.isNotBlank()) {
                                    Text(
                                        subtask.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                    )
                                }
                            }
                            WhipTextButton(enabled = !item.task.archived, onClick = { pendingMoveStepId = subtask.step.id }) {
                                Text("Move to New Task")
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    }
                    if (!item.task.archived) {
                    WhipButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                        Text("Complete")
                    }
                    }
                }
                if (section == TaskDetailSection.Schedule) {
                    if (item.task.scheduleKind == ScheduleKind.Recurring) {
                        SeriesHistory(
                            occurrences = occurrenceHistory,
                            scheduleExplanation = item.task.scheduleExplanation(),
                            actionsEnabled = !item.task.archived,
                            onReopenOccurrence = onReopenOccurrence,
                            onResetOccurrence = onResetOccurrence,
                        )
                    } else Text(item.task.scheduleExplanation())
                    if (!item.task.archived) {
                        WhipTextButton(onClick = onReschedule, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                when (item.task.scheduleKind) {
                                    ScheduleKind.Anytime -> "Choose a Date"
                                    ScheduleKind.Once -> "Change Scheduled Date"
                                    ScheduleKind.Recurring -> "Move This Occurrence"
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (!item.task.archived && item.task.scheduleKind == ScheduleKind.Recurring) {
                        WhipTextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                            Text("Skip This Occurrence")
                        }
                    }
                }
                if (section == TaskDetailSection.More) {
                    if (!item.task.archived) {
                        WhipTextButton(onClick = onPin, modifier = Modifier.fillMaxWidth()) {
                            Text(if (item.task.pinned) "Unpin from Home" else "Pin to Home")
                        }
                        WhipTextButton(onClick = onDuplicate, modifier = Modifier.fillMaxWidth()) {
                            Text("Duplicate to Inbox")
                        }
                        Text("Focus Timer", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 25, 45, 60).forEach { minutes ->
                                WhipOutlinedButton(onClick = { onStartFocus(minutes) }) { Text("$minutes min") }
                            }
                        }
                    }
                    WhipTextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (item.task.archived) "Restore Task" else if (item.task.scheduleKind == ScheduleKind.Recurring) "Stop Series (Archive)" else "Archive Task",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    WhipTextButton(onClick = onDeletePermanently, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (item.task.scheduleKind == ScheduleKind.Recurring) "Delete Entire Series Permanently" else "Delete Permanently",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {
            DetailEditButton(
                if (item.task.scheduleKind == ScheduleKind.Recurring) "Edit This and Future" else "Edit Task",
                onEdit,
            )
        },
    )
    pendingMoveStepId?.let { stepId ->
        val step = item.subtasks.firstOrNull { it.step.id == stepId }
        PaneAwareAlertDialog(
            modifier = modifier,
            onDismissRequest = { pendingMoveStepId = null },
            title = { Text("Move Subtask to a New Task?") },
            text = {
                Text(
                    "“${step?.title.orEmpty()}” will become a new Inbox task and will be removed from “${item.task.title}”. You can undo this from the confirmation.",
                )
            },
            confirmButton = {
                WhipButton(
                    enabled = step != null,
                    onClick = {
                        pendingMoveStepId = null
                        onPromoteSubtask(stepId)
                    },
                ) { Text("Move to New Task") }
            },
            dismissButton = {
                WhipTextButton(onClick = { pendingMoveStepId = null }) { Text("Cancel") }
            },
        )
    }
}

private enum class TaskDetailSection(val label: String) {
    Overview("Overview"),
    Schedule("Schedule"),
    More("Options"),
}

@Composable
fun CompletedTaskDialog(
    item: ScheduledTask,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onReopen: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier,
    occurrenceHistory: List<TaskOccurrence> = emptyList(),
    onReopenOccurrence: (TaskOccurrence) -> Unit,
    onResetOccurrence: (TaskOccurrence) -> Unit,
) {
    ProductivityEditorDialog(
        modifier = modifier.widthIn(min = 280.dp, max = 560.dp),
        testTag = "completed-task-surface",
        onDismissRequest = onDismiss,
        title = { Text(item.task.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (item.task.scheduleKind == ScheduleKind.Recurring) {
                        "This completed occurrence is kept in your history."
                    } else {
                        "This task is complete. You can put it back on your list."
                    },
                )
                WhipTextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (item.task.scheduleKind == ScheduleKind.Recurring) "Edit This and Future" else "Edit Task",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (item.task.scheduleKind == ScheduleKind.Recurring) {
                    SeriesHistory(
                        occurrences = occurrenceHistory,
                        scheduleExplanation = item.task.scheduleExplanation(),
                        actionsEnabled = true,
                        onReopenOccurrence = onReopenOccurrence,
                        onResetOccurrence = onResetOccurrence,
                    )
                }
            }
        },
        confirmButton = {
            WhipTextButton(onClick = onReopen) {
                Text(
                    if (item.task.scheduleKind == ScheduleKind.Recurring) {
                        "Reopen Occurrence"
                    } else {
                        "Reopen"
                    },
                )
            }
        },
        dismissButton = {
            WhipTextButton(onClick = onDeletePermanently) {
                Text(
                    if (item.task.scheduleKind == ScheduleKind.Recurring) {
                        "Delete Entire Series"
                    } else {
                        "Delete Permanently"
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun SeriesHistory(
    occurrences: List<TaskOccurrence>,
    scheduleExplanation: String,
    actionsEnabled: Boolean,
    onReopenOccurrence: (TaskOccurrence) -> Unit,
    onResetOccurrence: (TaskOccurrence) -> Unit,
) {
    var visibleCount by rememberSaveable { mutableIntStateOf(SERIES_HISTORY_PAGE_SIZE) }
    Text(
        "Series History",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp),
    )
    Text(
        scheduleExplanation,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (occurrences.isEmpty()) {
        Text(
            "Completed, skipped, and moved occurrences will appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    occurrences.take(visibleCount).forEach { occurrence ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (occurrence.state) {
                        OccurrenceState.Completed -> "Completed ${occurrence.scheduledDate.format(shortDateFormatter)}"
                        OccurrenceState.Skipped -> "Skipped ${occurrence.originalDate.format(shortDateFormatter)}"
                        OccurrenceState.Open -> "Moved to ${occurrence.scheduledDate.format(shortDateFormatter)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (occurrence.scheduledDate != occurrence.originalDate) {
                    Text(
                        "Originally ${occurrence.originalDate.format(shortDateFormatter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (actionsEnabled) {
                when (occurrence.state) {
                    OccurrenceState.Completed -> WhipTextButton(
                        onClick = { onReopenOccurrence(occurrence) },
                    ) { Text("Reopen") }
                    OccurrenceState.Skipped -> WhipTextButton(
                        onClick = { onResetOccurrence(occurrence) },
                    ) { Text("Undo Skip") }
                    OccurrenceState.Open -> if (occurrence.scheduledDate != occurrence.originalDate) {
                        WhipTextButton(onClick = { onResetOccurrence(occurrence) }) { Text("Reset Date") }
                    }
                }
            }
        }
    }
    if (visibleCount < occurrences.size) {
        WhipTextButton(
            onClick = { visibleCount = (visibleCount + SERIES_HISTORY_PAGE_SIZE).coerceAtMost(occurrences.size) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Show ${minOf(SERIES_HISTORY_PAGE_SIZE, occurrences.size - visibleCount)} More · ${occurrences.size - visibleCount} Remaining")
        }
    }
}

private const val SERIES_HISTORY_PAGE_SIZE = 20

@Composable
fun PermanentTaskDeleteDialog(
    item: ScheduledTask,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    impact: TaskDeletionImpact? = null,
) {
    val recurring = item.task.scheduleKind == ScheduleKind.Recurring
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (recurring) "Delete “${item.task.title}” Series Permanently?"
                else "Delete “${item.task.title}” Permanently?",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (impact == null || impact.taskId != item.task.id) {
                    Text("Calculating the exact deletion impact…")
                } else {
                    Text("Removed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${impact.recordedOccurrenceCount} recorded occurrence${if (impact.recordedOccurrenceCount == 1) "" else "s"} " +
                            "(${impact.completedOccurrenceCount} completed, ${impact.skippedOccurrenceCount} skipped, ${impact.openOccurrenceCount} open)",
                    )
                    Text("${impact.stepCount} subtask${if (impact.stepCount == 1) "" else "s"}")
                    Text("${impact.linkRuleCount} goal progress source${if (impact.linkRuleCount == 1) "" else "s"}")
                    Text("${impact.automationRuleCount} automation${if (impact.automationRuleCount == 1) "" else "s"}")
                    Text(
                        "This cannot be undone. Export a backup first if you may need this history.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = impact?.taskId == item.task.id && impact.exists,
                onClick = onConfirm,
            ) {
                Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            WhipTextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun ScheduledTask.detailLabel(completed: Boolean): String {
    val parts = mutableListOf<String>()
    if (completed) {
        parts += "Completed"
    } else {
        scheduledDate?.let {
            parts += if (isPastScheduledDate) {
                "Past Scheduled Date ${it.format(shortDateFormatter)}"
            } else {
                "Scheduled ${it.format(shortDateFormatter)}"
            }
        }
    }
    task.timeMinutes?.let { total ->
        val hour = total / 60
        val minute = total % 60
        parts += java.time.LocalTime.of(hour, minute)
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
    if (task.reminderEnabled && !completed) parts += "Reminder"
    if (!completed && task.deadline != null) {
        parts += if (isDeadlineOverdue) {
            "Deadline Overdue ${task.deadline.format(shortDateFormatter)}"
        } else {
            "Deadline ${task.deadline.format(shortDateFormatter)}"
        }
    }
    if (task.priority != TaskPriority.None) parts += "Priority: ${task.priority.name}"
    task.durationMinutes?.let { parts += "$it min" }
    if (task.effort != TaskEffort.Unspecified) parts += "Effort: ${task.effort.label}"
    if (task.tags.isNotEmpty()) parts += task.tags.joinToString(prefix = "#", separator = " #")
    if (task.scheduleKind == ScheduleKind.Recurring) parts += task.repeatLabel()
    if (task.scheduleKind == ScheduleKind.Anytime) parts += "Inbox"
    return parts.joinToString(" • ")
}

private fun ScheduledTask.progressLabel(): String = when (task.progressDisplay) {
    TaskProgressDisplay.Percent -> "${(subtaskProgress * 100).toInt()}%"
    TaskProgressDisplay.Fraction -> "$completedSubtasks/$totalSubtasks"
    TaskProgressDisplay.Both -> {
        "$completedSubtasks/$totalSubtasks · ${(subtaskProgress * 100).toInt()}%"
    }
}

private fun WhipTask.repeatLabel(): String {
    val rule = requireNotNull(recurrence)
    val base = when {
        rule.unit == RecurrenceUnit.Days && rule.interval == 1 -> "Daily"
        rule.unit == RecurrenceUnit.Days -> "Every ${rule.interval} days"
        rule.unit == RecurrenceUnit.Months -> "Every ${rule.interval} month${if (rule.interval == 1) "" else "s"}"
        rule.unit == RecurrenceUnit.Years -> "Every ${rule.interval} year${if (rule.interval == 1) "" else "s"}"
        rule.interval == 1 && rule.weekdays.isNotEmpty() -> {
            rule.weekdays.sorted().joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
        }
        rule.weekdays.isNotEmpty() -> "Every ${rule.interval} weeks"
        else -> "Weekly"
    }
    return when (rule.end) {
        RecurrenceEnd.Never -> base
        RecurrenceEnd.OnDate -> "$base until ${rule.endDate?.format(shortDateFormatter)}"
        RecurrenceEnd.AfterCount -> "$base • ${rule.occurrenceCount} times"
    }
}

private fun WhipTask.scheduleExplanation(): String {
    if (scheduleKind == ScheduleKind.Anytime) {
        return "This task is in Inbox without a scheduled date. Choose one when you are ready to schedule it."
    }
    if (scheduleKind == ScheduleKind.Once) {
        return date?.let { "Scheduled for ${it.format(shortDateFormatter)}." }
            ?: "This task does not currently have a scheduled date."
    }
    val rule = requireNotNull(recurrence) { "Recurring tasks require a recurrence rule" }
    val cadence = when {
        rule.unit == RecurrenceUnit.Days && rule.interval == 1 -> "each day"
        rule.unit == RecurrenceUnit.Days -> "every ${rule.interval} days from ${rule.startDate.format(shortDateFormatter)}"
        rule.unit == RecurrenceUnit.Months -> "every ${rule.interval} month${if (rule.interval == 1) "" else "s"}"
        rule.unit == RecurrenceUnit.Years -> "every ${rule.interval} year${if (rule.interval == 1) "" else "s"}"
        rule.weekdays.isNotEmpty() -> "on ${rule.weekdays.sorted().joinToString(", ") { it.name.take(3).lowercase().replaceFirstChar(Char::uppercase) }}"
        else -> "every ${rule.interval} week${if (rule.interval == 1) "" else "s"} from ${rule.startDate.format(shortDateFormatter)}"
    }
    return if (rule.anchor == RecurrenceAnchor.Completion) {
        "The next date is $cadence after you complete the current occurrence. Only one future occurrence can be known at a time."
    } else {
        "The next date is the next scheduled slot $cadence. Completing, skipping, or moving one occurrence does not shift the series."
    }
}

private val shortDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
