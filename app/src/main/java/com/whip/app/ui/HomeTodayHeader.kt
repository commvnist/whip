package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.TargetComparison
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Neutral outcomes are visible context, never a scored failure or an unfinished check-in. */
internal data class HomeHabitSummary(
    val completed: Int = 0,
    val total: Int = 0,
    val skipped: Int = 0,
    val timersToReview: Int = 0,
    val recordedCheckIns: Int = 0,
    val targetlessCheckIns: Int = 0,
) {
    val attentionCount: Int get() = total - completed + targetlessCheckIns - recordedCheckIns + timersToReview
    val hasContent: Boolean get() = total > 0 || targetlessCheckIns > 0 || skipped > 0 || timersToReview > 0
}

internal enum class HomeSummaryAvailability { Ready, Loading, Unavailable }

internal fun List<HabitDayProgress>.homeHabitSummary(): HomeHabitSummary {
    val expected = filter {
        it.scheduled && !it.habit.paused && !it.habit.archived &&
            it.dayState !in setOf(HabitDayState.Skipped, HabitDayState.Paused, HabitDayState.NotScheduled)
    }
    val scored = expected.filter { it.habit.comparison != TargetComparison.None }
    val targetless = expected.filter { it.habit.comparison == TargetComparison.None }
    return HomeHabitSummary(
        completed = scored.count { it.isDoneForToday() },
        total = scored.size,
        skipped = count { it.dayState == HabitDayState.Skipped },
        recordedCheckIns = targetless.count { it.status != null },
        targetlessCheckIns = targetless.size,
        // A timer can remain after completion or after restored data makes its Habit unavailable.
        // It still needs attention, but must not add a check-in to the completion denominator.
        timersToReview = count {
            it.habit.timerSessionId != null && (it !in expected || it.isDoneForToday() || it in targetless && it.status != null)
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
    onPlanDay: (() -> Unit)? = null,
    showTasks: Boolean = true,
    showHabits: Boolean = true,
    taskAvailability: HomeSummaryAvailability = HomeSummaryAvailability.Ready,
    habitAvailability: HomeSummaryAvailability = HomeSummaryAvailability.Ready,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = WhipSpacing.micro),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {
        if (showFullHeader) {
            Text(
                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (showFullHeader || onOpenReview != null || onPlanDay != null) {
            val actions: @Composable RowScope.() -> Unit = {
                if (showTasks) {
                    WhipTextButton(
                        onClick = { onPlanDay?.invoke() },
                        enabled = onPlanDay != null,
                        modifier = Modifier.semantics {
                            if (onPlanDay == null) stateDescription = when (taskAvailability) {
                                HomeSummaryAvailability.Ready -> "No unscheduled tasks to plan"
                                HomeSummaryAvailability.Loading -> "Tasks are loading"
                                HomeSummaryAvailability.Unavailable -> "Tasks are unavailable; open Tasks to retry"
                            }
                        },
                    ) { Text("Plan My Day") }
                }
                onOpenReview?.let { onReview ->
                    WhipTextButton(onClick = onReview) { Text("Review & Trends") }
                }
            }
            if (showFullHeader) WhipPageHeader(title = "Home", actions = actions)
            else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, content = actions)
            if (showTasks && onPlanDay == null) Text(
                when (taskAvailability) {
                    HomeSummaryAvailability.Ready -> "No unscheduled Tasks to plan"
                    HomeSummaryAvailability.Loading -> "Tasks are loading"
                    HomeSummaryAvailability.Unavailable -> "Tasks are unavailable; open Tasks to retry"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("home-planning-availability"),
            )
        }
        if (showTasks || showHabits) {
            val taskCard: @Composable (Modifier) -> Unit = { modifier ->
                val value = when (taskAvailability) {
                    HomeSummaryAvailability.Ready -> "$taskTotal remaining"
                    HomeSummaryAvailability.Loading -> "Loading…"
                    HomeSummaryAvailability.Unavailable -> "Unavailable"
                }
                HomeDailySummaryCard(
                    title = "Tasks today",
                    value = value,
                    accessibilityLabel = "Tasks today: $value. Open Tasks Today",
                    onClick = onOpenTasks,
                    modifier = modifier.testTag("home-tasks-today-record"),
                )
            }
            val habitCard: @Composable (Modifier) -> Unit = { modifier ->
                val value = when {
                    habitAvailability == HomeSummaryAvailability.Loading -> "Loading…"
                    habitAvailability == HomeSummaryAvailability.Unavailable -> "Unavailable"
                    habitSummary.total > 0 -> "${habitSummary.completed} of ${habitSummary.total} complete"
                    habitSummary.targetlessCheckIns > 0 -> "${habitSummary.recordedCheckIns} of ${habitSummary.targetlessCheckIns} check-ins recorded"
                    else -> "No check-ins due"
                }
                val context = if (habitAvailability != HomeSummaryAvailability.Ready) emptyList() else listOfNotNull(
                    "${habitSummary.recordedCheckIns} of ${habitSummary.targetlessCheckIns} check-ins without targets recorded"
                        .takeIf { habitSummary.total > 0 && habitSummary.targetlessCheckIns > 0 },
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
                    progress = if (habitAvailability == HomeSummaryAvailability.Ready && habitSummary.total > 0) {
                        habitSummary.completed.toFloat() / habitSummary.total
                    } else null,
                    onClick = onOpenHabits,
                    modifier = modifier.testTag("home-habit-progress-record"),
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stack = maxWidth < 320.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                if (showTasks && showHabits && !stack) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                    ) {
                        taskCard(Modifier.weight(1f).fillMaxHeight())
                        habitCard(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                        if (showTasks) taskCard(Modifier.fillMaxWidth())
                        if (showHabits) habitCard(Modifier.fillMaxWidth())
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
                    drawStopIndicator = {},
                )
            }
        }
    }
}
