package com.whip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whip.app.core.ReviewSection
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.NumberFormat

@Composable
internal fun ReviewOutcomeDetails(
    section: ReviewSection,
    outcomes: List<ReviewOutcome>,
    rangeLabel: String,
    areaLabel: String?,
    availability: ReviewAvailability,
    retryActions: DomainRetryActions,
    locale: Locale,
    onBack: () -> Unit,
    onOpen: (ReviewOutcome) -> Unit,
) {
    val title = when (section) {
        ReviewSection.Tasks -> "Completed Tasks"
        ReviewSection.Habits -> "Habit Outcomes"
        ReviewSection.Goals -> "Goal Progress"
        ReviewSection.Gym -> "Finished Workouts"
    }
    val sourceAvailability = ReviewAvailability(availability.sources.filterKeys { it.section == section })
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(WhipSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {
            WhipBackAction("Back to Review & Trends", onBack)
            Text(title, modifier = Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("review-outcome-list"),
            contentPadding = PaddingValues(WhipSpacing.screenCompact),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
                    Text(
                        "$rangeLabel · ${if (section == ReviewSection.Gym) "All gym data" else areaLabel?.let { "Productivity: $it" } ?: "All Areas"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (sourceAvailability.complete) Text(
                        if (section == ReviewSection.Goals) "Progress score: ${formatReviewNumber(outcomes.sumOf { it.score }, locale)}"
                        else "${outcomes.size} ${if (outcomes.size == 1) "outcome" else "outcomes"}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (outcomes.isNotEmpty()) Text(
                        "Select an outcome to open its original ${when (section) {
                            ReviewSection.Tasks -> "Task"
                            ReviewSection.Habits -> "Habit"
                            ReviewSection.Goals -> "Goal"
                            ReviewSection.Gym -> "Workout"
                        }}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!sourceAvailability.complete) item {
                ReviewAvailabilityNotice(sourceAvailability, retryActions,
                    resultExplanation = "Outcomes from this source will appear here when it is available.")
            }
            if (sourceAvailability.complete && outcomes.isEmpty()) item {
                WhipEmptyState("No Outcomes in This View", "Change the period or included sections in Review & Trends to review other outcomes.")
            }
            items(outcomes, key = ReviewOutcome::stableKey) { outcome ->
                WhipRecordItem(
                    itemKey = outcome.stableKey,
                    itemType = when (section) {
                        ReviewSection.Tasks -> "Task"
                        ReviewSection.Habits -> "Habit"
                        ReviewSection.Goals -> "Goal"
                        ReviewSection.Gym -> "Workout"
                    },
                    title = outcome.title,
                    identityEmoji = outcome.icon,
                    onOpen = { onOpen(outcome) },
                    modifier = Modifier.testTag("review-outcome-${outcome.stableKey}"),
                ) {
                    context(outcome.date.format(DateTimeFormatter.ofPattern("EEE, MMM d, uuuu", locale)))
                    if (outcome.archived) context("Archived")
                    outcome.scheduledDate?.let { context("Scheduled: ${it.format(DateTimeFormatter.ofPattern("MMM d, uuuu", locale))}") }
                    outcome.originalDate?.takeIf { it != outcome.scheduledDate }?.let {
                        context("Originally scheduled: ${it.format(DateTimeFormatter.ofPattern("MMM d, uuuu", locale))}")
                    }
                    fact("", when (section) {
                        ReviewSection.Tasks -> "Completed"
                        ReviewSection.Habits -> "Habit target reached"
                        ReviewSection.Goals -> "Progress score: ${formatReviewNumber(outcome.score, locale)}"
                        ReviewSection.Gym -> "Finished"
                    })
                }
            }
        }
    }
}

internal fun formatReviewNumber(value: Double, locale: Locale): String {
    val format = NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        maximumFractionDigits = 3
    }
    return if (value > 0.0 && value < 0.001) "<${format.format(0.001)}" else format.format(value)
}
