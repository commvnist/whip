package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Neutral outcomes are visible context, never a scored failure or an unfinished check-in. */
internal data class HomeHabitSummary(
    val completed: Int = 0,
    val total: Int = 0,
    val skipped: Int = 0,
    val timersToReview: Int = 0,
) {
    val attentionCount: Int get() = total - completed + timersToReview
    val hasContent: Boolean get() = total > 0 || skipped > 0 || timersToReview > 0
}

internal fun List<HabitDayProgress>.homeHabitSummary(): HomeHabitSummary {
    val scored = filter {
        it.scheduled && !it.habit.paused && !it.habit.archived &&
            it.dayState !in setOf(HabitDayState.Skipped, HabitDayState.Paused, HabitDayState.NotScheduled)
    }
    return HomeHabitSummary(
        completed = scored.count { it.isDoneForToday() },
        total = scored.size,
        skipped = count { it.dayState == HabitDayState.Skipped },
        // A timer can remain after completion or after restored data makes its Habit unavailable.
        // It still needs attention, but must not add a check-in to the completion denominator.
        timersToReview = count {
            it.habit.timerSessionId != null && (it !in scored || it.isDoneForToday())
        },
    )
}

@Composable
internal fun TodayHeader(
    date: LocalDate,
    taskTotal: Int,
    habitSummary: HomeHabitSummary,
    onOpenTasks: () -> Unit,
    onOpenHabits: () -> Unit,
    showFullHeader: Boolean = true,
    onOpenReview: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = WhipSpacing.compact),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {
        if (showFullHeader) {
            Text(
                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (showFullHeader || onOpenReview != null) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
            ) {
                if (showFullHeader) {
                    Text(
                        "Home",
                        modifier = Modifier.align(Alignment.CenterVertically).semantics { heading() },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                onOpenReview?.let { onReview ->
                    WhipTextButton(onClick = onReview) { Text("Review & Trends") }
                }
            }
        }
        if (taskTotal > 0 || habitSummary.hasContent) {
            val taskCard: @Composable (Modifier) -> Unit = { modifier ->
                HomeDailySummaryCard(
                    title = "Tasks today",
                    value = "$taskTotal remaining",
                    accessibilityLabel = "Tasks today: $taskTotal remaining. Open Tasks Today",
                    onClick = onOpenTasks,
                    modifier = modifier.testTag("home-tasks-today-record"),
                )
            }
            val habitCard: @Composable (Modifier) -> Unit = { modifier ->
                val value = if (habitSummary.total > 0) {
                    "${habitSummary.completed} of ${habitSummary.total} complete"
                } else {
                    "No check-ins due"
                }
                val context = listOfNotNull(
                    "${habitSummary.skipped} skipped".takeIf { habitSummary.skipped > 0 },
                    "${habitSummary.timersToReview} ${if (habitSummary.timersToReview == 1) "timer" else "timers"} to review"
                        .takeIf { habitSummary.timersToReview > 0 },
                )
                HomeDailySummaryCard(
                    title = "Habit progress",
                    value = value,
                    context = context.joinToString(" · ").takeIf { it.isNotEmpty() },
                    accessibilityLabel = (listOf("Habit progress: $value") + context + "Open Habits Today")
                        .joinToString(". "),
                    progress = if (habitSummary.total > 0) {
                        habitSummary.completed.toFloat() / habitSummary.total
                    } else null,
                    onClick = onOpenHabits,
                    modifier = modifier.testTag("home-habit-progress-record"),
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stack = maxWidth < 320.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                if (taskTotal > 0 && habitSummary.hasContent && !stack) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                    ) {
                        taskCard(Modifier.weight(1f).fillMaxHeight())
                        habitCard(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                        if (taskTotal > 0) taskCard(Modifier.fillMaxWidth())
                        if (habitSummary.hasContent) habitCard(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDailySummaryCard(
    title: String,
    value: String,
    accessibilityLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    context: String? = null,
    progress: Float? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 80.dp).semantics(mergeDescendants = true) {
            contentDescription = accessibilityLabel
        },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(WhipSpacing.compact),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
            }
            Text(value, style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content))
            context?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            progress?.let {
                LinearProgressIndicator(
                    progress = { it.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clearAndSetSemantics {},
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}
