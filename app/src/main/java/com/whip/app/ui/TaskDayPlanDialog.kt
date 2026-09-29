package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskPriority

/** The workspace retains the draft and request owner; this surface only presents a plan. */
@Composable
internal fun TaskDayPlanDialog(
    candidates: List<ScheduledTask>,
    capacityText: String,
    onCapacityChange: (String) -> Unit,
    selectedKeys: Set<String>?,
    onSelectionChange: (Set<String>?) -> Unit,
    existingMinutes: Int,
    existingAssumptions: Int,
    saving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onApply: (List<ScheduledTask>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val capacity = capacityText.toIntOrNull()
    val validCapacity = capacity != null && capacity in 1..1440
    val selected = candidates.filter { it.stableKey in selectedKeys.orEmpty() }
    val totalMinutes = existingMinutes + selected.sumOf(ScheduledTask::estimatedDurationMinutes)
    val listState = rememberLazyListState()
    var revealPreview by remember { mutableStateOf(false) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) listState.scrollToItem(3)
    }
    LaunchedEffect(revealPreview, selectedKeys) {
        if (revealPreview && selectedKeys != null) {
            // Preview is enabled only when candidates exist and capacity remains.
            listState.scrollToItem(5)
            revealPreview = false
        }
    }
    PaneAwareAlertDialog(
        modifier = modifier,
        testTag = "task-day-plan-dialog",
        paneTitle = "Plan My Day",
        title = null,
        stableHeight = true,
        inputBlocked = saving,
        inputBlockedLabel = "Applying your day plan",
        onDismissRequest = onDismiss,
        text = {
            LazyColumn(
                Modifier.fillMaxWidth().testTag("task-day-plan-list"),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
            ) {
                item { WhipDialogHeading("Plan My Day") }
                item {
                    Text(
                        "Choose unscheduled tasks to do today. This list uses your current Area and Task filters, except Today’s fixed date scope. Capacity includes today’s tasks across all Areas. Tasks without a duration count as 30 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = onCapacityChange,
                        label = { Text("Daily Capacity in Minutes") },
                        enabled = !saving,
                        isError = !validCapacity,
                        supportingText = if (!validCapacity) { { Text("Enter 1–1440 minutes.") } } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("task-day-plan-capacity"),
                    )
                }
                item {
                    PersistenceFailureNotice(errorMessage, testTag = "task-day-plan-error")
                    Text(
                        "Already planned across all Areas: $existingMinutes minutes" +
                            if (existingAssumptions > 0) " · $existingAssumptions without estimates counted as 30 min" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                item {
                    WhipOutlinedButton(
                        enabled = !saving && validCapacity && capacity > existingMinutes && candidates.isNotEmpty(),
                        onClick = {
                            revealPreview = true
                            onSelectionChange(selectTasksForCapacity(candidates, (capacity!! - existingMinutes).coerceAtLeast(0))
                                .mapTo(linkedSetOf(), ScheduledTask::stableKey))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (selectedKeys == null) "Preview Plan" else "Rebuild Preview") }
                }
                if (candidates.isEmpty()) item {
                    Text("No unscheduled tasks match your Area and filters. Close the planner to adjust them or add a task.")
                }
                if (validCapacity && capacity <= existingMinutes) item {
                    Text("Today's existing plan fills this capacity. Increase it to add tasks, or review Tasks Today.")
                }
                if (selectedKeys != null) {
                    item {
                        Text("Proposed: ${selected.size} of ${candidates.size} new tasks · $totalMinutes of ${capacityText.ifBlank { "—" }} minutes total",
                            style = MaterialTheme.typography.titleSmall)
                        if (validCapacity && totalMinutes > capacity) Text(
                            "Remove ${totalMinutes - capacity} minutes before applying.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    items(candidates, key = ScheduledTask::stableKey) { candidate ->
                        val checked = candidate.stableKey in selectedKeys
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                                value = checked, enabled = !saving, role = Role.Checkbox,
                                onValueChange = {
                                    onSelectionChange(if (checked) selectedKeys - candidate.stableKey else selectedKeys + candidate.stableKey)
                                },
                            ).padding(vertical = WhipSpacing.micro),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(candidate.task.title)
                                Text(
                                    "${candidate.estimatedDurationMinutes()} min${if (candidate.task.durationMinutes == null) " assumed (no estimate)" else ""} · " +
                                        (if (candidate.task.priority == TaskPriority.None) "No priority" else "${candidate.task.priority.label} priority") +
                                        if (!checked && validCapacity && totalMinutes + candidate.estimatedDurationMinutes() > capacity) " · would exceed capacity" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Checkbox(checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
                        }
                    }
                }
            }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss, enabled = !saving) { Text("Close") } },
        confirmButton = {
            WhipButton(
                onClick = { onApply(selected, capacity!!) },
                enabled = !saving && selected.isNotEmpty() && validCapacity && totalMinutes <= capacity,
                modifier = Modifier.testTag("task-day-plan-apply"),
            ) { Text(if (saving) "Applying…" else "Apply Plan") }
        },
    )
}
