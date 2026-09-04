package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.ReviewSection
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.successfulPeriodOutcomeDates
import com.whip.app.domain.goalOutcomeScoreOnDate
import com.whip.app.domain.MeasurementEntryStatus
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.pearsonCorrelation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private data class ReviewSignal(val name: String, val values: List<Double>)

private data class ReviewCorrelation(
    val left: String,
    val right: String,
    val coefficient: Double,
    val sampleSize: Int,
)

internal data class TrackReviewEvidence(
    val entryCount: Int,
    val touchedTrackCount: Int,
)

internal fun trackReviewEvidence(
    trackState: TrackUiState,
    start: LocalDate,
    through: LocalDate,
): TrackReviewEvidence? {
    val entries = trackState.projections.flatMap { projection ->
        projection.entries.filter { it.entry.entryDate in start..through }
            .map { projection.track.id }
    }
    return entries.takeIf { it.isNotEmpty() }?.let {
        TrackReviewEvidence(entryCount = it.size, touchedTrackCount = it.distinct().size)
    }
}

internal fun reviewStartDate(period: ReviewPeriod, through: LocalDate): LocalDate = when (period) {
    ReviewPeriod.Weekly -> through.minusDays(6)
    ReviewPeriod.Monthly -> through.withDayOfMonth(1)
}

internal fun reviewSectionsInDisplayOrder(sections: Set<ReviewSection>): List<ReviewSection> =
    ReviewSection.entries.filter(sections::contains)

@Composable
fun ReviewDialog(
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    gymState: GymUiState,
    period: ReviewPeriod,
    modifier: Modifier = Modifier,
    wideLeadingPaneWidth: Dp = 0.dp,
    wideHingeWidth: Dp = 0.dp,
    zone: ZoneId = ZoneId.systemDefault(),
    onPeriodChange: (ReviewPeriod) -> Unit,
    onDismiss: () -> Unit,
    sections: Set<ReviewSection> = ReviewSection.entries.toSet(),
    onSectionsChange: (Set<ReviewSection>) -> Unit = {},
    onDrillDown: (ReviewSection) -> Unit = {},
    productivityAreaLabel: String? = null,
    trackState: TrackUiState = TrackUiState(loading = false),
    onOpenTracks: () -> Unit = {},
) {
    var compactOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val through = taskState.currentDate
    val start = reviewStartDate(period, through)
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
            habitState.customUnits,
            habitState.skips,
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
        ReviewSection.Tasks to ReviewSignal("Tasks", dates.map { completedTasks[it]?.toDouble() ?: 0.0 }),
        ReviewSection.Habits to ReviewSignal("Habit outcomes", dates.map { successfulHabitPeriods[it]?.toDouble() ?: 0.0 }),
        ReviewSection.Goals to ReviewSignal("Goal progress", dates.map { goalOutcomes[it] ?: 0.0 }),
        ReviewSection.Gym to ReviewSignal(if (productivityAreaLabel == null) "Workouts" else "Workouts · All gym data", dates.map { workouts[it]?.toDouble() ?: 0.0 }),
    )
    val includedSections = reviewSectionsInDisplayOrder(sections).toSet()
    val signals = allSignals.filter { it.first in includedSections }.map { it.second }
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
                    ?.let { result ->
                        add(
                            ReviewCorrelation(
                                left = correlationSignals[left].name,
                                right = correlationSignals[right].name,
                                coefficient = result.coefficient,
                                sampleSize = result.sampleSize,
                            ),
                        )
                    }
            }
        }
    }
    val hasReviewData = signals.any { signal -> signal.values.any { it != 0.0 } }
    val hasTrackEvidence = trackState.projections.any { it.entries.isNotEmpty() }
    val trackEvidence = trackReviewEvidence(trackState, start, through)
    val rangeLabel = formatReviewRange(start, through, locale)
    val controls: @Composable () -> Unit = {
        ReviewControlPanel(
            period = period,
            rangeLabel = rangeLabel,
            sections = sections,
            productivityAreaLabel = productivityAreaLabel,
            onPeriodChange = onPeriodChange,
            onSectionsChange = onSectionsChange,
        )
    }
    val overview: @Composable () -> Unit = {
        ReviewOverview(
            hasReviewData = hasReviewData,
            hasTrackEvidence = hasTrackEvidence,
            trackEvidence = trackEvidence,
            allSignals = allSignals,
            includedSections = includedSections,
            correlations = correlations,
            rangeLabel = rangeLabel,
            locale = locale,
            onDrillDown = onDrillDown,
            onOpenTracks = onOpenTracks,
        )
    }

    BackHandler(onBack = onDismiss)
    WhipFullScreenSurface(title = "Review & Trends", modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val useWideDashboard = maxWidth >= 720.dp && maxHeight >= 440.dp
            if (useWideDashboard) {
                ReviewWideDashboard(
                    availableWidth = maxWidth,
                    leadingPaneWidth = wideLeadingPaneWidth,
                    hingeWidth = wideHingeWidth,
                    onDismiss = onDismiss,
                    controls = controls,
                    overview = overview,
                )
            } else {
                ReviewCompactDashboard(
                    onDismiss = onDismiss,
                    hasReviewData = hasReviewData,
                    optionsExpanded = compactOptionsExpanded,
                    onOptionsExpandedChange = { compactOptionsExpanded = it },
                    controls = controls,
                    overview = overview,
                )
            }
        }
    }
}

@Composable
private fun ReviewWideDashboard(
    availableWidth: Dp,
    leadingPaneWidth: Dp,
    hingeWidth: Dp,
    onDismiss: () -> Unit,
    controls: @Composable () -> Unit,
    overview: @Composable () -> Unit,
) {
    val gutterWidth = hingeWidth.coerceIn(0.dp, 72.dp)
    val usableWidth = (availableWidth - gutterWidth).coerceAtLeast(640.dp)
    val maximumControlWidth = (usableWidth - 360.dp).coerceAtLeast(248.dp)
    val controlWidth = if (leadingPaneWidth > 0.dp) {
        (leadingPaneWidth - gutterWidth).coerceIn(248.dp, maximumControlWidth)
    } else {
        (usableWidth * 0.28f).coerceIn(280.dp, minOf(360.dp, maximumControlWidth))
    }
    Row(Modifier.fillMaxSize().testTag("review-wide-dashboard")) {
        Surface(
            modifier = Modifier.width(controlWidth).fillMaxHeight().testTag("review-control-pane"),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.fillMaxSize()) {
                ReviewDestinationHeader(onDismiss = onDismiss, sidebar = true)
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(WhipSpacing.screenExpanded),
                    verticalArrangement = Arrangement.spacedBy(WhipSpacing.major),
                ) {
                    item { controls() }
                }
            }
        }
        if (gutterWidth > 0.dp) {
            Spacer(
                Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .semantics { contentDescription = "Device hinge gutter" }
                    .testTag("review-hinge-gutter"),
            )
        } else {
            Spacer(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("review-overview-pane"),
            contentPadding = PaddingValues(
                start = WhipSpacing.screenExpanded,
                top = WhipSpacing.screenExpanded,
                end = WhipSpacing.screenExpanded,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.major),
        ) {
            item { overview() }
        }
    }
}

@Composable
private fun ReviewCompactDashboard(
    onDismiss: () -> Unit,
    hasReviewData: Boolean,
    optionsExpanded: Boolean,
    onOptionsExpandedChange: (Boolean) -> Unit,
    controls: @Composable () -> Unit,
    overview: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().testTag("review-compact-dashboard")) {
        ReviewDestinationHeader(onDismiss = onDismiss, sidebar = false)
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = WhipSpacing.screenCompact,
                top = WhipSpacing.standard,
                end = WhipSpacing.screenCompact,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.major),
        ) {
            if (hasReviewData) {
                item { controls() }
                item { overview() }
            } else {
                item { overview() }
                item {
                    DisclosureRow(
                        title = "Review Options",
                        supportingText = "Change the period or included sections.",
                        expanded = optionsExpanded,
                        onClick = { onOptionsExpandedChange(!optionsExpanded) },
                    )
                }
                if (optionsExpanded) item { controls() }
            }
        }
    }
}

@Composable
private fun ReviewDestinationHeader(
    onDismiss: () -> Unit,
    sidebar: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = if (sidebar) WhipSpacing.standard else WhipSpacing.compact,
            vertical = WhipSpacing.compact,
        ),
        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).testTag("review-destination-title"),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
        ) {
            Text(
                "Review & Trends",
                modifier = Modifier.semantics { heading() },
                style = if (sidebar) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (sidebar) "Progress dashboard" else "See outcomes, patterns, and progress in one place.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WhipTrailingCloseAction(
            label = "Close Review & Trends",
            onClick = onDismiss,
            modifier = Modifier.testTag("review-close-action"),
        )
    }
}

@Composable
private fun ReviewControlPanel(
    period: ReviewPeriod,
    rangeLabel: String,
    sections: Set<ReviewSection>,
    productivityAreaLabel: String?,
    onPeriodChange: (ReviewPeriod) -> Unit,
    onSectionsChange: (Set<ReviewSection>) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("review-controls"),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.standard),
    ) {
        Text("View", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Period", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {
            ReviewPeriod.entries.forEach { value ->
                WhipFilterChip(value == period, { onPeriodChange(value) }, { Text(value.label) })
            }
        }
        Text(
            rangeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        Text("Included Sections", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {
            ReviewSection.entries.forEach { section ->
                WhipFilterChip(
                    selected = section in sections,
                    onClick = {
                        val changed = if (section in sections) sections - section else sections + section
                        if (changed.isNotEmpty()) onSectionsChange(changed)
                    },
                    label = { Text(section.label) },
                )
            }
        }
        productivityAreaLabel?.let { label ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(WhipSpacing.compact),
                    verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
                ) {
                    Text("Area Context", style = MaterialTheme.typography.labelLarge)
                    Text("Productivity: $label", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Gym uses all data and is excluded from Area-scoped correlations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewOverview(
    hasReviewData: Boolean,
    hasTrackEvidence: Boolean,
    trackEvidence: TrackReviewEvidence?,
    allSignals: List<Pair<ReviewSection, ReviewSignal>>,
    includedSections: Set<ReviewSection>,
    correlations: List<ReviewCorrelation>,
    rangeLabel: String,
    locale: java.util.Locale,
    onDrillDown: (ReviewSection) -> Unit,
    onOpenTracks: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("review-overview"),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.standard),
    ) {
        WhipPageHeader(
            title = "Overview",
            supportingText = "$rangeLabel · Select any card to open its source.",
        )
        trackEvidence?.let { evidence ->
            TrackEvidenceCard(evidence = evidence, rangeLabel = rangeLabel, onOpenTracks = onOpenTracks)
        }
        if (!hasReviewData) {
            WhipEmptyState(
                title = "No Reviewable Outcomes Yet",
                supportingText = if (hasTrackEvidence) {
                    "Track entries are evidence, not outcomes by themselves. Connect them to a Goal or complete another outcome to build this dashboard."
                } else {
                    "Complete a Task, check in a Habit, record Goal progress, or finish a Workout to build this dashboard."
                },
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
            ) {
                WhipTextButton(onClick = { onDrillDown(ReviewSection.Tasks) }) { Text("Open Tasks") }
                WhipTextButton(onClick = { onDrillDown(ReviewSection.Habits) }) { Text("Open Habits") }
                WhipTextButton(onClick = { onDrillDown(ReviewSection.Goals) }) { Text("Open Goals") }
                if (trackEvidence == null) {
                    WhipTextButton(onClick = onOpenTracks) { Text("Open Tracks") }
                }
                WhipTextButton(onClick = { onDrillDown(ReviewSection.Gym) }) { Text("Open Gym") }
            }
            return@Column
        }
        BoxWithConstraints(Modifier.fillMaxWidth().testTag("review-signal-grid")) {
            val visibleSignals = allSignals.filter { it.first in includedSections }
            val columns = when {
                maxWidth >= 1_240.dp && visibleSignals.size >= 4 -> 4
                maxWidth >= 620.dp && visibleSignals.size >= 2 -> 2
                else -> 1
            }
            val cardWidth = (maxWidth - WhipSpacing.compact * (columns - 1)) / columns
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
            ) {
                visibleSignals.forEach { (section, signal) ->
                    ReviewSignalCard(
                        section = section,
                        signal = signal,
                        locale = locale,
                        onOpen = onDrillDown,
                        modifier = Modifier.width(cardWidth),
                    )
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(WhipSpacing.standard),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
            ) {
                Text("30-Day Correlations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Correlations show association, not causation. Each comparison needs at least seven observed days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (correlations.isEmpty()) {
                    Text("Not enough observations yet.")
                } else {
                    correlations.forEach { result ->
                        Text(
                            "${result.left} ↔ ${result.right}: ${String.format(locale, "%.2f", result.coefficient)} (n=${result.sampleSize})",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackEvidenceCard(
    evidence: TrackReviewEvidence,
    rangeLabel: String,
    onOpenTracks: () -> Unit,
) {
    WhipGroupedInformationCard(
        modifier = Modifier.testTag("review-track-evidence"),
    ) {
            Text("Track Evidence · All Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "$rangeLabel · ${formatTrackEvidenceSummary(evidence)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Track values are evidence, not productivity outcomes; they are excluded from scores and correlations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WhipTextButton(
                onClick = onOpenTracks,
                modifier = Modifier.testTag("review-track-evidence-open"),
            ) { Text("Open Tracks") }
    }
}

internal fun formatTrackEvidenceSummary(evidence: TrackReviewEvidence): String =
    "${evidence.entryCount} ${if (evidence.entryCount == 1) "entry" else "entries"} across " +
        "${evidence.touchedTrackCount} touched ${if (evidence.touchedTrackCount == 1) "Track" else "Tracks"}."

internal fun formatReviewRange(start: LocalDate, through: LocalDate, locale: java.util.Locale): String {
    val sameYear = start.year == through.year
    val sameMonth = sameYear && start.month == through.month
    val startPattern = if (sameYear) "MMM d" else "MMM d, uuuu"
    val endPattern = if (sameMonth) "d, uuuu" else "MMM d, uuuu"
    val startText = start.format(java.time.format.DateTimeFormatter.ofPattern(startPattern, locale))
    val endText = through.format(java.time.format.DateTimeFormatter.ofPattern(endPattern, locale))
    return "$startText–$endText"
}

@Composable
private fun ReviewSignalCard(
    section: ReviewSection,
    signal: ReviewSignal,
    locale: java.util.Locale,
    onOpen: (ReviewSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = signal.values.sum()
    val totalText = if (total % 1.0 == 0.0) total.toInt().toString() else String.format(locale, "%.1f", total)
    val chartDescription = "${signal.name} daily values: ${signal.values.joinToString(", ") { value ->
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format(locale, "%.1f", value)
    }}"
    Card(
        modifier = modifier
            .heightIn(min = 148.dp)
            .clickable(onClickLabel = "Open ${signal.name} details") { onOpen(section) },
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(signal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(totalText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (signal.name == "Goal progress") {
                Text(
                    "Normalized progress score; partial progress counts proportionally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ReviewLineChart(
                values = signal.values,
                modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = chartDescription },
            )
        }
    }
}

@Composable
private fun ReviewLineChart(values: List<Double>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        drawLine(baselineColor, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height))
        if (values.isEmpty()) return@Canvas
        val max = values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val step = if (values.size <= 1) 0f else size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * step
            val y = size.height - ((value / max).toFloat() * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}
