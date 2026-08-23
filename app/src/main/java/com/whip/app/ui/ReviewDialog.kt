package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.HomeSection
import com.whip.app.core.SavedReviewFilter
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.successfulPeriodOutcomeDates
import com.whip.app.domain.goalOutcomeScoreOnDate
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.pearsonCorrelation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private data class ReviewSignal(val name: String, val values: List<Double>)

@Composable
fun ReviewDialog(
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    gymState: GymUiState,
    period: ReviewPeriod,
    zone: ZoneId = ZoneId.systemDefault(),
    onPeriodChange: (ReviewPeriod) -> Unit,
    onDismiss: () -> Unit,
    sections: Set<HomeSection> = HomeSection.entries.toSet(),
    savedFilters: List<SavedReviewFilter> = emptyList(),
    selectedFilterName: String? = null,
    onSectionsChange: (Set<HomeSection>) -> Unit = {},
    onSaveFilter: (SavedReviewFilter) -> Unit = {},
    onSelectFilter: (String?) -> Unit = {},
    onDeleteFilter: (String) -> Unit = {},
    onDrillDown: (HomeSection) -> Unit = {},
    dialogModifier: Modifier = Modifier,
    productivityAreaLabel: String? = null,
) {
    var saveFilterOpen by rememberSaveable { mutableStateOf(false) }
    var filterName by rememberSaveable { mutableStateOf("") }
    val locale = LocalConfiguration.current.locales[0]
    val through = taskState.currentDate
    val start = when (period) {
        ReviewPeriod.Weekly -> through.minusDays(6)
        ReviewPeriod.Monthly -> through.withDayOfMonth(1)
    }
    val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(through) }.toList()
    val completedTasks = taskState.completed.groupingBy { item ->
        item.completedAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: item.scheduledDate
    }.eachCount()
    // Emit one outcome when the target period succeeds. Six water increments
    // remain one 6-of-8 partial result, and a 3x/week habit remains one weekly
    // result rather than three unrelated check-ins.
    val habitOutcomeDates = habitState.all.associate { item ->
        item.habit.id to item.habit.successfulPeriodOutcomeDates(
            habitState.logs,
            minOf(start, through.minusDays(29)),
            through,
            habitState.pauses,
        )
    }
    val successfulHabitPeriods = dates.associateWith { date -> habitOutcomeDates.values.count { date in it } }
    val workouts = gymState.history.filter { it.state == WorkoutSessionState.Finished }.groupingBy { it.localDate }.eachCount()
    val goalProjections = goalState.active + goalState.completed + goalState.archived
    val goalOutcomes = (dates + (0L until 30L).map { through.minusDays(it) }).distinct().associateWith { date ->
        goalProjections.sumOf { projection ->
            goalOutcomeScoreOnDate(projection.goal, projection.entries, projection.milestones, date)
        }
    }
    val allSignals = listOf(
        HomeSection.Tasks to ReviewSignal("Tasks", dates.map { completedTasks[it]?.toDouble() ?: 0.0 }),
        HomeSection.Habits to ReviewSignal("Habit outcomes", dates.map { successfulHabitPeriods[it]?.toDouble() ?: 0.0 }),
        HomeSection.Gym to ReviewSignal(if (productivityAreaLabel == null) "Workouts" else "Workouts · All gym data", dates.map { workouts[it]?.toDouble() ?: 0.0 }),
        HomeSection.Goals to ReviewSignal("Goal progress", dates.map { goalOutcomes[it] ?: 0.0 }),
    )
    val signals = allSignals.filter { it.first in sections }.map { it.second }
    val correlationSignals = if (productivityAreaLabel == null) signals else signals.filterNot { it.name.startsWith("Workouts") }
    val correlationDates = (0L until 30L).map { through.minusDays(29L - it) }
    fun valuesFor(name: String): List<Double> = when (name) {
        "Tasks" -> correlationDates.map { completedTasks[it]?.toDouble() ?: 0.0 }
        "Habit outcomes" -> correlationDates.map { date -> habitOutcomeDates.values.count { date in it }.toDouble() }
        "Workouts", "Workouts · All gym data" -> correlationDates.map { workouts[it]?.toDouble() ?: 0.0 }
        else -> correlationDates.map { goalOutcomes[it] ?: 0.0 }
    }
    val correlations = buildList {
        correlationSignals.indices.forEach { left ->
            (left + 1 until correlationSignals.size).forEach { right ->
                pearsonCorrelation(valuesFor(correlationSignals[left].name), valuesFor(correlationSignals[right].name))
                    ?.let { add(Triple(correlationSignals[left].name, correlationSignals[right].name, it)) }
            }
        }
    }
    val hasReviewData = signals.any { signal -> signal.values.any { it != 0.0 } }

    BackHandler(onBack = onDismiss)
    WhipFullScreenSurface(title = "Review & Trends") {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = "Close Review & Trends" }) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
                WhipPageHeader(
                    title = "Review & Trends",
                    supportingText = "Understand progress across your selected period and open any section for details.",
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 16.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                productivityAreaLabel?.let { label -> item {
                    Text("Productivity: $label · Gym: All data", style = MaterialTheme.typography.labelLarge)
                    Text("Gym is shown for context but excluded from scoped cross-domain correlations.", style = MaterialTheme.typography.bodySmall)
                } }
                if (!hasReviewData) item {
                    WhipEmptyState(
                        title = "Nothing Recorded Yet",
                        supportingText = "Complete a task, check in a habit, record goal progress, or finish a workout. Review & Trends will build from those entries.",
                    )
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ReviewPeriod.entries.forEach { value ->
                            WhipFilterChip(value == period, { onPeriodChange(value) }, { Text(value.name) })
                        }
                    }
                    Text("$start – $through", style = MaterialTheme.typography.bodySmall)
                }
                if (hasReviewData) item {
                    Text("Included Sections", fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        HomeSection.entries.forEach { section ->
                            WhipFilterChip(
                                selected = section in sections,
                                onClick = {
                                    val changed = if (section in sections) sections - section else sections + section
                                    if (changed.isNotEmpty()) onSectionsChange(changed)
                                },
                                label = { Text(section.name) },
                            )
                        }
                    }
                    if (savedFilters.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            WhipFilterChip(selectedFilterName == null, { onSelectFilter(null) }, { Text("Custom") })
                            savedFilters.forEach { filter ->
                                WhipFilterChip(selectedFilterName == filter.name, { onSelectFilter(filter.name) }, { Text(filter.name) })
                            }
                        }
                        selectedFilterName?.let { name ->
                            WhipTextButton(onClick = { onDeleteFilter(name) }) { Text("Delete “$name”") }
                        }
                    }
                    WhipTextButton(onClick = { saveFilterOpen = true }) { Text("Save Review Filter") }
                }
                if (hasReviewData) allSignals.filter { it.first in sections }.forEach { (section, signal) ->
                    item {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClickLabel = "Open ${signal.name} details") { onDrillDown(section) }.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(signal.name, fontWeight = FontWeight.SemiBold)
                            val total = signal.values.sum()
                            Text(if (total % 1.0 == 0.0) total.toInt().toString() else String.format(locale, "%.1f", total))
                        }
                        if (signal.name == "Goal progress") {
                            Text("Goal progress is a normalized outcome score; partial forward movement counts fractionally, not as raw log volume.", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            sparkline(signal.values),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics {
                                contentDescription = "${signal.name} daily values: ${signal.values.joinToString(", ") { value ->
                                    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(locale, "%.1f", value)
                                }}"
                            },
                        )
                    }
                }
                if (hasReviewData) item {
                    Text("30-Day Correlations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Correlation is an association, not evidence that one activity caused another. At least 7 observed days per series are required.", style = MaterialTheme.typography.bodySmall)
                    if (correlations.isEmpty()) Text("Not enough observations yet.", modifier = Modifier.padding(top = 6.dp))
                    correlations.forEach { (left, right, result) ->
                        Text("$left ↔ $right: ${String.format(locale, "%.2f", result.coefficient)} (n=${result.sampleSize})")
                    }
                }
            }
        }
    }
    if (saveFilterOpen) {
        AlertDialog(
            onDismissRequest = { saveFilterOpen = false },
            title = { Text("Save Review Filter") },
            text = { OutlinedTextField(filterName, { filterName = it }, label = { Text("Filter name") }) },
            confirmButton = {
                WhipTextButton(
                    enabled = filterName.isNotBlank(),
                    onClick = {
                        onSaveFilter(SavedReviewFilter(filterName.trim(), sections))
                        filterName = ""
                        saveFilterOpen = false
                    },
                ) { Text("Save") }
            },
            dismissButton = { WhipTextButton(onClick = { saveFilterOpen = false }) { Text("Cancel") } },
        )
    }
}

private fun sparkline(values: List<Double>): String {
    if (values.isEmpty()) return ""
    val levels = "▁▂▃▄▅▆▇█"
    val max = values.maxOrNull()?.takeIf { it > 0.0 } ?: return "▁".repeat(values.size)
    return values.joinToString("") { value -> levels[((value / max) * levels.lastIndex).toInt().coerceIn(0, levels.lastIndex)].toString() }
}
