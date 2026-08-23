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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import com.whip.app.domain.RecurrenceEnd
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskProgressDisplay
import com.whip.app.domain.WhipTask
import com.whip.app.data.TaskDeletionImpact
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun TaskRow(
    item: ScheduledTask,
    completed: Boolean,
    onComplete: (() -> Unit)?,
    onOpenActions: (() -> Unit)?,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                completed -> MaterialTheme.colorScheme.surfaceContainerLow
                item.isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        selectionMode && onSelectionToggle != null -> Modifier.clickable(onClickLabel = "Select ${item.task.title}", onClick = onSelectionToggle)
                        onOpenActions != null -> Modifier.clickable(onClickLabel = "Open ${item.task.title} actions", onClick = onOpenActions)
                        else -> Modifier
                    },
                )
                .padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = if (selectionMode) selected else completed,
                onCheckedChange = {
                    if (selectionMode) onSelectionToggle?.invoke() else onComplete?.invoke()
                },
                enabled = if (selectionMode) onSelectionToggle != null else onComplete != null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.task.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.LineThrough.takeIf { completed },
                    )
                    if (item.isOverdue) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                "Overdue",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                if (item.task.areaId != null) {
                    Spacer(Modifier.height(4.dp))
                    AreaBadge(item.task.areaId, item.task.area)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    item.detailLabel(completed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.task.notes.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                if (item.task.showSubtaskProgress && item.totalSubtasks > 0) {
                    Spacer(Modifier.height(9.dp))
                    LinearProgressIndicator(
                        progress = { item.subtaskProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.progressLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (onOpenActions != null && !selectionMode) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Task actions", modifier = Modifier.size(28.dp))
                }
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
    onDuplicate: () -> Unit = {},
    onToggleInbox: () -> Unit = {},
    onStartFocus: (Int) -> Unit = {},
    onToggleSubtask: (Long, Boolean) -> Unit,
    onPromoteSubtask: (Long) -> Unit,
    occurrenceHistory: List<TaskOccurrence> = emptyList(),
    onReopenOccurrence: (TaskOccurrence) -> Unit = {},
    onResetOccurrence: (TaskOccurrence) -> Unit = {},
    dialogModifier: Modifier = Modifier,
) {
    var section by rememberSaveable(item.stableKey) { mutableStateOf(TaskDetailSection.Overview) }
    ProductivityEditorDialog(
        modifier = dialogModifier.widthIn(min = 280.dp, max = 560.dp),
        testTag = "task-actions-surface",
        onDismissRequest = onDismiss,
        title = { Text(item.task.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TaskDetailSection.entries.forEach { value ->
                        androidx.compose.material3.FilterChip(
                            selected = section == value,
                            onClick = { section = value },
                            label = { Text(value.label) },
                        )
                    }
                }
                if (section == TaskDetailSection.Overview) {
                    if (item.subtasks.isEmpty()) Text("No subtasks. Edit this task to add steps.")
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
                            TextButton(enabled = !item.task.archived, onClick = { onPromoteSubtask(subtask.step.id) }) {
                                Text("Promote")
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    }
                    if (!item.task.archived) {
                    TextButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                        Text("Complete", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (item.task.scheduleKind == ScheduleKind.Recurring) {
                            "Edit this and future"
                        } else {
                            "Edit task"
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    }
                    }
                }
                if (section == TaskDetailSection.Schedule) {
                    if (item.task.scheduleKind == ScheduleKind.Recurring) {
                        SeriesHistory(
                            occurrences = occurrenceHistory,
                            scheduleExplanation = item.task.seriesScheduleExplanation(),
                            actionsEnabled = !item.task.archived,
                            onReopenOccurrence = onReopenOccurrence,
                            onResetOccurrence = onResetOccurrence,
                        )
                    } else Text(item.task.seriesScheduleExplanation())
                    if (!item.task.archived && item.scheduledDate != null) {
                        TextButton(onClick = onReschedule, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (item.task.scheduleKind == ScheduleKind.Recurring) "Move this occurrence" else "Change due date",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (!item.task.archived && item.task.scheduleKind == ScheduleKind.Recurring) {
                        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                            Text("Skip this occurrence", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                if (section == TaskDetailSection.More) {
                    if (!item.task.archived) {
                        TextButton(onClick = onPin, modifier = Modifier.fillMaxWidth()) {
                            Text(if (item.task.pinned) "Unpin from Home" else "Pin to Home", modifier = Modifier.fillMaxWidth())
                        }
                        TextButton(onClick = onDuplicate, modifier = Modifier.fillMaxWidth()) {
                            Text("Duplicate to Inbox", modifier = Modifier.fillMaxWidth())
                        }
                        if (item.task.scheduleKind == ScheduleKind.Anytime) {
                            TextButton(onClick = onToggleInbox, modifier = Modifier.fillMaxWidth()) {
                                Text(if (item.task.inbox) "Mark triaged" else "Move to Inbox", modifier = Modifier.fillMaxWidth())
                            }
                        }
                        Text("Focus timer", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 25, 45, 60).forEach { minutes ->
                                OutlinedButton(onClick = { onStartFocus(minutes) }) { Text("$minutes min") }
                            }
                        }
                    }
                    TextButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (item.task.archived) "Restore task" else if (item.task.scheduleKind == ScheduleKind.Recurring) "Stop series (archive)" else "Archive task",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    TextButton(onClick = onDeletePermanently, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (item.task.scheduleKind == ScheduleKind.Recurring) "Delete entire series permanently" else "Delete permanently",
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {},
    )
}

private enum class TaskDetailSection(val label: String) {
    Overview("Overview"),
    Schedule("Schedule"),
    More("More"),
}

@Composable
fun CompletedTaskDialog(
    item: ScheduledTask,
    onDismiss: () -> Unit,
    onReopen: () -> Unit,
    onDeletePermanently: () -> Unit,
    occurrenceHistory: List<TaskOccurrence> = emptyList(),
    onReopenOccurrence: (TaskOccurrence) -> Unit = {},
    onResetOccurrence: (TaskOccurrence) -> Unit = {},
    dialogModifier: Modifier = Modifier,
) {
    ProductivityEditorDialog(
        modifier = dialogModifier.widthIn(min = 280.dp, max = 560.dp),
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
                if (item.task.scheduleKind == ScheduleKind.Recurring) {
                    SeriesHistory(
                        occurrences = occurrenceHistory,
                        scheduleExplanation = item.task.seriesScheduleExplanation(),
                        actionsEnabled = true,
                        onReopenOccurrence = onReopenOccurrence,
                        onResetOccurrence = onResetOccurrence,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onReopen) {
                Text(
                    if (item.task.scheduleKind == ScheduleKind.Recurring) {
                        "Reopen occurrence"
                    } else {
                        "Reopen"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDeletePermanently) {
                Text(
                    if (item.task.scheduleKind == ScheduleKind.Recurring) {
                        "Delete entire series"
                    } else {
                        "Delete permanently"
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
        "Series history",
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
                    OccurrenceState.Completed -> TextButton(
                        onClick = { onReopenOccurrence(occurrence) },
                    ) { Text("Reopen") }
                    OccurrenceState.Skipped -> TextButton(
                        onClick = { onResetOccurrence(occurrence) },
                    ) { Text("Undo skip") }
                    OccurrenceState.Open -> if (occurrence.scheduledDate != occurrence.originalDate) {
                        TextButton(onClick = { onResetOccurrence(occurrence) }) { Text("Reset date") }
                    }
                }
            }
        }
    }
    if (visibleCount < occurrences.size) {
        TextButton(
            onClick = { visibleCount = (visibleCount + SERIES_HISTORY_PAGE_SIZE).coerceAtMost(occurrences.size) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Show ${minOf(SERIES_HISTORY_PAGE_SIZE, occurrences.size - visibleCount)} more · ${occurrences.size - visibleCount} remaining")
        }
    }
}

private const val SERIES_HISTORY_PAGE_SIZE = 20

@Composable
fun PermanentTaskDeleteDialog(
    item: ScheduledTask,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    impact: TaskDeletionImpact? = null,
) {
    val recurring = item.task.scheduleKind == ScheduleKind.Recurring
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (recurring) "Delete “${item.task.title}” series permanently?"
                else "Delete “${item.task.title}” permanently?",
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
                    Text("${impact.stepCount} step${if (impact.stepCount == 1) "" else "s"}")
                    Text("${impact.linkRuleCount} goal link${if (impact.linkRuleCount == 1) "" else "s"}")
                    Text("${impact.automationRuleCount} automation${if (impact.automationRuleCount == 1) "" else "s"}")
                    Text(
                        "This cannot be undone. Export a backup first if you may need this history.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = impact?.taskId == item.task.id && impact.exists,
                onClick = onConfirm,
            ) {
                Text("Delete permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun ScheduledTask.detailLabel(completed: Boolean): String {
    val parts = mutableListOf<String>()
    if (completed) {
        parts += "Completed"
    } else if (isOverdue) {
        parts += "Due ${(task.deadline ?: scheduledDate)?.format(shortDateFormatter)}"
    } else {
        scheduledDate?.let { parts += it.format(shortDateFormatter) }
    }
    task.timeMinutes?.let { total ->
        val hour = total / 60
        val minute = total % 60
        parts += java.time.LocalTime.of(hour, minute)
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
    if (task.reminderEnabled && !completed) parts += "Reminder"
    if (!completed && task.deadline != null) parts += "Deadline ${task.deadline.format(shortDateFormatter)}"
    if (task.priority != TaskPriority.None) parts += task.priority.name
    task.durationMinutes?.let { parts += "$it min" }
    if (task.durationMinutes != null) parts += task.effort.label
    if (task.tags.isNotEmpty()) parts += task.tags.joinToString(prefix = "#", separator = " #")
    task.locationReminder?.let { parts += "${it.trigger.name} ${it.name}" }
    if (task.scheduleKind == ScheduleKind.Recurring) parts += task.repeatLabel()
    if (task.inbox) parts += "Inbox"
    else if (task.scheduleKind == ScheduleKind.Anytime && parts.isEmpty()) parts += "Anytime"
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

private fun WhipTask.seriesScheduleExplanation(): String {
    val rule = requireNotNull(recurrence)
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
