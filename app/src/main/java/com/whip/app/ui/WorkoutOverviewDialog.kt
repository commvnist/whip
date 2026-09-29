package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutSet

internal fun selectedWorkoutExecutionSet(
    items: List<WorkoutExerciseUi>,
    requestedSetId: Long?,
    acceptedOptionalIds: Set<Long>,
): Pair<WorkoutExerciseUi, WorkoutSet>? = items.asSequence()
    .filter { it.workoutExercise.outcome == WorkoutExerciseOutcome.Active }
    .firstNotNullOfOrNull { item ->
        item.sets.firstOrNull { set ->
            set.id == requestedSetId && !set.completed && set.deletedAtMillis == null &&
                (set.optionalWorkKindSnapshot != RoutineOptionalWorkKind.Joker || set.id in acceptedOptionalIds)
        }?.let { item to it }
    }

@Composable
internal fun WorkoutOverviewDialog(
    items: List<WorkoutExerciseUi>,
    acceptedOptionalIds: Set<Long>,
    selectedSetId: Long?,
    saving: Boolean,
    onChoose: (Long, Long?) -> Unit,
    onFollowOrder: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit,
) {
    PaneAwareAlertDialog(
        paneTitle = "Workout Overview",
        testTag = "workout-overview",
        inputBlocked = saving,
        onDismissRequest = onDismiss,
        title = { Text("Workout Overview") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("workout-overview-list")) {
                item {
                    Text("Choose an exercise for your next Set. After completing it, the workout order resumes.",
                        style = MaterialTheme.typography.bodySmall)
                }
                items(items, key = { it.workoutExercise.id }) { item ->
                    val next = selectNextWorkoutSet(listOf(item), emptyMap(), acceptedOptionalIds)?.second
                    val kept = item.sets.filter { it.deletedAtMillis == null }
                    val completed = kept.count { it.completed }
                    WhipActionRow(
                        title = item.exercise.name,
                        supportingText = listOfNotNull(
                            "$completed/${kept.size} Sets complete",
                            item.group?.let { "${it.type.label} · ${it.name}" },
                            next?.let { "Next · Set ${it.position + 1}" }
                                ?: if (kept.any { !it.completed }) "Optional Sets await a decision" else "Review completed work",
                            "Selected".takeIf { next?.id == selectedSetId },
                        ).joinToString(" · "),
                        onClick = { if (!saving) onChoose(item.workoutExercise.id, next?.id) },
                        modifier = Modifier.testTag("workout-overview-exercise-${item.workoutExercise.id}"),
                    )
                }
            }
        },
        confirmButton = {
            WhipButton(onClick = onFinish, enabled = !saving, modifier = Modifier.testTag("workout-overview-finish")) {
                Text("Finish Workout")
            }
        },
        dismissButton = {
            WhipTextButton(onClick = onFollowOrder, enabled = !saving) { Text("Follow Workout Order") }
        },
    )
}
