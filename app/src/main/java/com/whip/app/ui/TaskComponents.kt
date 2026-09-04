package com.whip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
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
import java.time.format.TextStyle
import java.util.Locale
import com.whip.app.ui.theme.whipColors

@Composable
fun SectionHeading(title: String, count: Int, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClickLabel = "Open $title", onClick = onClick))
            .semantics(mergeDescendants = true) {
                heading()
                if (onClick != null) role = Role.Button
            }
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (count > 0) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        count.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (onClick != null) Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
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
    reorderMode: Boolean = false,
    showCompletionControl: Boolean = true,
) {
    val disclosure = rememberCompactItemDisclosure("task:${item.stableKey}")
    val metadata = item.detailSegments(completed)
    ProductivityItemCard(
        modifier = Modifier.then(
            when {
                selectionMode && onSelectionToggle != null -> Modifier
                    .toggleable(
                        value = selected,
                        role = Role.Checkbox,
                        onValueChange = { onSelectionToggle() },
                    )
                    .semantics {
                        contentDescription = if (selected) {
                            "Deselect task ${item.task.title}"
                        } else "Select task ${item.task.title}"
                        stateDescription = if (selected) "Selected" else "Not selected"
                    }
                !reorderMode && onOpenActions != null -> Modifier
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
            onEdit = onEdit.takeUnless { selectionMode || reorderMode },
            identityModifier = Modifier.testTag("task-icon-${item.task.id}"),
            primaryActionModifier = Modifier.testTag("task-primary-action-${item.task.id}"),
            editModifier = Modifier.testTag("task-edit-action-${item.task.id}"),
            titleCompleted = completed,
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
            expanded = disclosure.expanded,
            onExpansionToggle = disclosure.toggle.takeUnless { reorderMode },
            expansionTag = "task-expand-${item.task.id}",
            primaryActionWidth = 48.dp,
            primaryAction = if (reorderMode || (!selectionMode && !showCompletionControl)) null else ({
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                        enabled = onSelectionToggle != null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                } else {
                    WhipCompletionCheckbox(
                        checked = completed,
                        onCheckedChange = { onComplete?.invoke() },
                        enabled = onComplete != null,
                        modifier = Modifier.semantics {
                            contentDescription = if (completed) {
                                "Task ${item.task.title} completed"
                            } else "Complete task ${item.task.title}"
                        },
                    )
                }
            }),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().testTag("task-metadata-${item.task.id}"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                metadata.forEach { label ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (!reorderMode && disclosure.expanded && item.task.notes.isNotBlank()) {
                Text(
                    item.task.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!reorderMode && disclosure.expanded && item.task.showSubtaskProgress && item.totalSubtasks > 0) {
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
        }
    }
}

@Composable
private fun TaskSubtaskCompletionRow(
    subtask: com.whip.app.domain.ScheduledSubtask,
    archived: Boolean,
    onToggle: () -> Unit,
    onConvert: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("task-subtask-row-${subtask.step.id}"),
    ) {
        val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
        if (stacked) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TaskSubtaskText(subtask, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    TaskSubtaskCheckbox(subtask, archived, onToggle)
                }
                WhipTextButton(
                    enabled = !archived,
                    onClick = onConvert,
                    modifier = Modifier.fillMaxWidth().testTag("task-subtask-convert-${subtask.step.id}"),
                ) {
                    Text("Convert to Task")
                    Spacer(Modifier.weight(1f))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TaskSubtaskText(subtask, Modifier.weight(1f))
                WhipTextButton(
                    enabled = !archived,
                    onClick = onConvert,
                    modifier = Modifier.testTag("task-subtask-convert-${subtask.step.id}"),
                ) { Text("Convert to Task") }
                Spacer(Modifier.width(8.dp))
                TaskSubtaskCheckbox(subtask, archived, onToggle)
            }
        }
    }
}

@Composable
private fun TaskSubtaskText(
    subtask: com.whip.app.domain.ScheduledSubtask,
    modifier: Modifier,
) {
    Column(modifier = modifier.testTag("task-subtask-text-${subtask.step.id}")) {
        Text(
            subtask.title,
            color = completionTextColor(subtask.completed),
            textDecoration = completionTextDecoration(subtask.completed),
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
}

@Composable
private fun TaskSubtaskCheckbox(
    subtask: com.whip.app.domain.ScheduledSubtask,
    archived: Boolean,
    onToggle: () -> Unit,
) {
    val actionDescription = if (subtask.completed) {
        "Mark Subtask ${subtask.step.title} incomplete"
    } else "Complete Subtask ${subtask.step.title}"
    Box(
        modifier = Modifier
            .size(48.dp)
            .testTag("task-subtask-check-${subtask.step.id}")
            .toggleable(
                value = subtask.completed,
                enabled = !archived,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics { contentDescription = actionDescription },
        contentAlignment = Alignment.Center,
    ) {
        WhipCompletionCheckbox(
            checked = subtask.completed,
            onCheckedChange = null,
            enabled = !archived,
            modifier = Modifier.clearAndSetSemantics { },
        )
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
    EntityInspector(
        entityType = "Task",
        title = item.task.title,
        emoji = item.task.icon,
        context = item.inspectorContext(),
        status = item.inspectorStatus(completed = false),
        statusTone = item.inspectorStatusTone(completed = false),
        sections = TaskDetailSection.entries.map { it.inspectorSection },
        selectedSectionId = section.id,
        onSelectSection = { id -> section = TaskDetailSection.entries.first { it.id == id } },
        onDismiss = onDismiss,
        onEdit = onEdit,
        editLabel = if (
            item.task.scheduleKind == ScheduleKind.Recurring &&
            !item.task.archived &&
            item.completedAtMillis == null &&
            (item.occurrenceState == null || item.occurrenceState == OccurrenceState.Open)
        ) "Edit This and Future" else if (item.task.scheduleKind == ScheduleKind.Recurring) "Edit Series" else "Edit Task",
        modifier = modifier,
        connectedSurfaceTag = "task-actions-surface",
        connectedSectionTagPrefix = "task-detail-section",
        primaryAction = EntityInspectorPrimaryAction(
            id = "complete",
            label = "Complete Task",
            onClick = onComplete,
            enabled = !item.task.archived,
        ).takeUnless { item.task.archived },
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (section == TaskDetailSection.Overview) {
                    EntityInspectorGroup("Context") {
                        EntityInspectorFact("Timing", item.task.scheduleExplanation())
                        item.task.notes.takeIf(String::isNotBlank)?.let { EntityInspectorFact("Notes", it) }
                        item.task.deadline?.let { deadline ->
                            EntityInspectorFact(
                                "Deadline",
                                deadline.format(shortDateFormatter) + if (item.isDeadlineOverdue) " · overdue" else "",
                            )
                        }
                    }
                    EntityInspectorGroup(
                        title = "Subtasks",
                        supportingText = if (item.subtasks.isEmpty()) {
                            "No subtasks yet. Use Edit to break this task into smaller steps."
                        } else "${item.completedSubtasks} of ${item.totalSubtasks} complete",
                    ) {
                        item.subtasks.forEach { subtask ->
                            TaskSubtaskCompletionRow(
                                subtask = subtask,
                                archived = item.task.archived,
                                onToggle = { onToggleSubtask(subtask.step.id, !subtask.completed) },
                                onConvert = { pendingMoveStepId = subtask.step.id },
                            )
                        }
                    }
                }
                if (section == TaskDetailSection.Activity) {
                    EntityInspectorGroup(
                        "Schedule",
                        supportingText = item.task.scheduleExplanation().takeUnless {
                            item.task.scheduleKind == ScheduleKind.Recurring
                        },
                    ) {
                        if (!item.task.archived) EntityInspectorAction(
                            id = "reschedule",
                            label =
                                when (item.task.scheduleKind) {
                                    ScheduleKind.Anytime -> "Choose a Date"
                                    ScheduleKind.Once -> "Change Scheduled Date"
                                    ScheduleKind.Recurring -> "Move This Occurrence"
                                },
                            onClick = onReschedule,
                        )
                        if (!item.task.archived && item.task.scheduleKind == ScheduleKind.Recurring) {
                            EntityInspectorAction("skip-occurrence", "Skip This Occurrence", onSkip)
                        }
                    }
                    if (item.task.scheduleKind == ScheduleKind.Recurring) {
                        EntityInspectorGroup("Activity") {
                            SeriesHistory(
                                occurrences = occurrenceHistory,
                                scheduleExplanation = item.task.scheduleExplanation(),
                                actionsEnabled = !item.task.archived,
                                onReopenOccurrence = onReopenOccurrence,
                                onResetOccurrence = onResetOccurrence,
                            )
                        }
                    } else {
                        EntityInspectorFact(
                            "Recorded activity",
                            item.completedAtMillis?.let { "Completed" } ?: "No completed activity yet",
                        )
                    }
                }
                if (section == TaskDetailSection.More) {
                    EntityInspectorGroup("Actions") {
                        if (!item.task.archived) {
                            EntityInspectorAction("duplicate", "Duplicate to Inbox", onDuplicate)
                            Text("Start a Focus Timer", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(15, 25, 45, 60).forEach { minutes ->
                                    WhipOutlinedButton(
                                        onClick = { onStartFocus(minutes) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) { Text("$minutes min") }
                                }
                            }
                        }
                    }
                    EntityInspectorGroup("Availability") {
                        if (!item.task.archived) EntityInspectorAction(
                            "pin",
                            if (item.task.pinned) "Unpin from Whip Home" else "Pin to Whip Home",
                            onPin,
                            supportingText = if (item.task.pinned) {
                                "The Task keeps its schedule and remains available in Tasks."
                            } else {
                                "When this Task is due today, it stays first in Whip Home's Tasks section."
                            },
                        )
                        EntityInspectorAction(
                            "archive",
                            if (item.task.archived) "Restore Task" else if (item.task.scheduleKind == ScheduleKind.Recurring) "Stop Series (Archive)" else "Archive Task",
                            onArchive,
                        )
                    }
                    EntityInspectorDangerZone {
                        EntityInspectorAction(
                            id = "delete",
                            label =
                            if (item.task.scheduleKind == ScheduleKind.Recurring) "Delete Entire Series Permanently" else "Delete Permanently",
                            onClick = onDeletePermanently,
                            modifier = Modifier.testTag("entity-inspector-delete"),
                            danger = true,
                        )
                    }
                }
            }
        },
    )
    pendingMoveStepId?.let { stepId ->
        val step = item.subtasks.firstOrNull { it.step.id == stepId }
        PaneAwareAlertDialog(
            modifier = modifier,
            onDismissRequest = { pendingMoveStepId = null },
            title = { Text("Convert Subtask to a Task?") },
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
                ) { Text("Convert to Inbox Task") }
            },
            dismissButton = {
                WhipTextButton(onClick = { pendingMoveStepId = null }) { Text("Cancel") }
            },
        )
    }
}

private enum class TaskDetailSection(
    val id: String,
    val label: String,
    val connectedLabel: String = label,
) {
    Overview("overview", "Overview"),
    Activity("activity", "Activity", "Schedule"),
    More("options", "Options"),
    ;

    val inspectorSection: EntityInspectorSection
        get() = EntityInspectorSection(
            id = id,
            label = label,
            connectedLabel = connectedLabel,
        )
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
    var section by rememberSaveable(item.stableKey) { mutableStateOf(TaskDetailSection.Overview) }
    EntityInspector(
        entityType = "Task",
        title = item.task.title,
        emoji = item.task.icon,
        context = item.inspectorContext(),
        status = item.inspectorStatus(completed = true),
        statusTone = item.inspectorStatusTone(completed = true),
        sections = TaskDetailSection.entries.map { it.inspectorSection },
        selectedSectionId = section.id,
        onSelectSection = { id -> section = TaskDetailSection.entries.first { it.id == id } },
        onDismiss = onDismiss,
        onEdit = onEdit,
        editLabel = if (item.task.scheduleKind == ScheduleKind.Recurring) "Edit Series" else "Edit Task",
        modifier = modifier,
        connectedSurfaceTag = "completed-task-surface",
        connectedSectionTagPrefix = "completed-task-detail-section",
        primaryAction = EntityInspectorPrimaryAction(
            id = "reopen",
            label = if (item.task.scheduleKind == ScheduleKind.Recurring) "Reopen Occurrence" else "Reopen Task",
            onClick = onReopen,
        ),
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (section) {
                    TaskDetailSection.Overview -> {
                        EntityInspectorGroup("Outcome") {
                            Text(
                                if (item.task.scheduleKind == ScheduleKind.Recurring) {
                                    "This occurrence is complete and remains available in the series history."
                                } else {
                                    "This task is complete. Reopen it when more work is needed."
                                },
                            )
                        }
                        EntityInspectorGroup("Context") {
                            EntityInspectorFact("Timing", item.task.scheduleExplanation())
                            item.task.notes.takeIf(String::isNotBlank)?.let { EntityInspectorFact("Notes", it) }
                            if (item.totalSubtasks > 0) {
                                EntityInspectorFact("Subtasks", "${item.completedSubtasks} of ${item.totalSubtasks} complete")
                            }
                        }
                    }
                    TaskDetailSection.Activity -> {
                        if (item.task.scheduleKind == ScheduleKind.Recurring) {
                            SeriesHistory(
                                occurrences = occurrenceHistory,
                                scheduleExplanation = item.task.scheduleExplanation(),
                                actionsEnabled = true,
                                onReopenOccurrence = onReopenOccurrence,
                                onResetOccurrence = onResetOccurrence,
                            )
                        } else {
                            EntityInspectorFact("Recorded activity", "Completed")
                        }
                    }
                    TaskDetailSection.More -> {
                        EntityInspectorDangerZone {
                            EntityInspectorAction(
                                id = "delete",
                                label = if (item.task.scheduleKind == ScheduleKind.Recurring) {
                                    "Delete Entire Series Permanently"
                                } else "Delete Permanently",
                                onClick = onDeletePermanently,
                                modifier = Modifier.testTag("entity-inspector-delete"),
                                danger = true,
                            )
                        }
                    }
                }
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

internal fun ScheduledTask.inspectorStatus(completed: Boolean): String = when {
    task.archived -> "Archived"
    completed -> "Completed"
    isDeadlineOverdue -> "Deadline overdue"
    isPastScheduledDate -> "Past scheduled date"
    task.scheduleKind == ScheduleKind.Anytime -> "Ready in Inbox"
    task.scheduleKind == ScheduleKind.Recurring -> "Active series"
    else -> "Scheduled"
}

internal fun ScheduledTask.inspectorStatusTone(completed: Boolean): WhipStatusTone = when {
    task.archived -> WhipStatusTone.Neutral
    completed -> WhipStatusTone.Success
    isDeadlineOverdue -> WhipStatusTone.Destructive
    isPastScheduledDate -> WhipStatusTone.Warning
    task.scheduleKind == ScheduleKind.Anytime -> WhipStatusTone.Neutral
    task.scheduleKind == ScheduleKind.Recurring -> WhipStatusTone.Info
    else -> WhipStatusTone.Info
}

private fun ScheduledTask.inspectorContext(): String = task.area.ifBlank {
    when (task.scheduleKind) {
        ScheduleKind.Anytime -> "Inbox"
        ScheduleKind.Once -> scheduledDate?.format(shortDateFormatter) ?: "Scheduled task"
        ScheduleKind.Recurring -> "Recurring task"
    }
}

@Composable
fun PermanentTaskDeleteDialog(
    item: ScheduledTask? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    impact: TaskDeletionImpact? = null,
    saving: Boolean = false,
    persistenceError: String? = null,
    taskId: Long? = null,
    taskTitle: String = "",
    recurringSeries: Boolean = false,
) {
    val targetTaskId = requireNotNull(item?.task?.id ?: taskId)
    val targetTitle = item?.task?.title ?: taskTitle
    val recurring = item?.task?.scheduleKind == ScheduleKind.Recurring || recurringSeries
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!saving) onDismiss() },
        inputBlocked = saving,
        inputBlockedLabel = "Deleting Task Permanently",
        title = {
            Text(
                if (recurring) "Delete “$targetTitle” Series Permanently?"
                else "Delete “$targetTitle” Permanently?",
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersistenceFailureNotice(
                    persistenceError,
                    testTag = "task-delete-save-problem",
                )
                if (impact == null || impact.taskId != targetTaskId) {
                    Text("Calculating the exact deletion impact…")
                } else {
                    Text("Removed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${impact.recordedOccurrenceCount} recorded occurrence${if (impact.recordedOccurrenceCount == 1) "" else "s"} " +
                            "(${impact.completedOccurrenceCount} completed, ${impact.skippedOccurrenceCount} skipped, ${impact.openOccurrenceCount} open)",
                    )
                    Text("${impact.stepCount} subtask${if (impact.stepCount == 1) "" else "s"}")
                    Text(
                        "This cannot be undone. Export a backup first if you may need this history.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !saving && impact?.taskId == targetTaskId && impact.exists,
                onClick = onConfirm,
            ) {
                Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun ScheduledTask.detailSegments(completed: Boolean): List<String> {
    val parts = mutableListOf<String>()
    if (completed) {
        parts += "Completed"
    } else {
        scheduledDate?.let {
            parts += if (isPastScheduledDate) {
                "Past date · ${it.format(shortDateFormatter)}"
            } else {
                "Scheduled · ${it.format(shortDateFormatter)}"
            }
        }
    }
    task.timeMinutes?.let { total ->
        val hour = total / 60
        val minute = total % 60
        parts += java.time.LocalTime.of(hour, minute)
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
    if (task.reminderEnabled && !completed) parts += "Reminder on"
    if (!completed && task.deadline != null) {
        parts += if (isDeadlineOverdue) {
            "Deadline overdue · ${task.deadline.format(shortDateFormatter)}"
        } else {
            "Deadline · ${task.deadline.format(shortDateFormatter)}"
        }
    }
    if (task.priority != TaskPriority.None) parts += "${task.priority.label} priority"
    task.durationMinutes?.let { parts += "$it min" }
    if (task.effort != TaskEffort.Unspecified) parts += "${task.effort.label} effort"
    task.tags.sorted().forEach { parts += "#$it" }
    if (task.scheduleKind == ScheduleKind.Recurring) parts += "Repeats · ${task.repeatLabel()}"
    if (task.scheduleKind == ScheduleKind.Anytime) parts += "Inbox"
    return parts
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
            rule.weekdays.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
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
        rule.weekdays.isNotEmpty() -> "on ${rule.weekdays.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }}"
        else -> "every ${rule.interval} week${if (rule.interval == 1) "" else "s"} from ${rule.startDate.format(shortDateFormatter)}"
    }
    return if (rule.anchor == RecurrenceAnchor.Completion) {
        "The next date is $cadence after you complete the current occurrence. Only one future occurrence can be known at a time."
    } else {
        "The next date is the next scheduled slot $cadence. Completing, skipping, or moving one occurrence does not shift the series."
    }
}

private val shortDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
