package com.whip.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.whip.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal enum class TrackReviewRange(val label: String, val days: Long?) {
    All("All Dates", null), Week("7 Days", 7), Month("30 Days", 30), Quarter("90 Days", 90);

    fun includes(date: LocalDate, today: LocalDate): Boolean = days?.let {
        date >= today.minusDays(it - 1) && date <= today
    } ?: true
}

internal data class TrackReviewScope(
    val range: TrackReviewRange = TrackReviewRange.All,
    val conditions: List<TrackCondition> = emptyList(),
    val mode: TrackConditionMode = TrackConditionMode.MatchAll,
)

internal fun TrackProjection.reviewEntries(scope: TrackReviewScope, today: LocalDate): List<TrackEntryProjection> =
    matchingEntries(scope.conditions, scope.mode).filter { scope.range.includes(it.entry.entryDate, today) }

internal data class TrackReviewPoint(val entry: TrackEntryProjection, val value: Double)

internal fun trackReviewSeries(field: TrackField, entries: List<TrackEntryProjection>): List<TrackReviewPoint> =
    entries.mapNotNull { entry ->
        val value = entry.value(field.id)
        (if (field.type == TrackFieldType.Number) value?.canonicalNumber else value?.scaleValue)
            ?.takeIf(Double::isFinite)?.let { TrackReviewPoint(entry, it) }
    }.sortedWith(compareBy<TrackReviewPoint> { it.entry.entry.entryDate }
        .thenBy { it.entry.entry.createdAtMillis }.thenBy { it.entry.entry.id })

@Composable
internal fun TrackReviewRangeControl(
    scope: TrackReviewScope,
    today: LocalDate,
    count: Int,
    actions: @Composable RowScope.() -> Unit = {},
    onChange: (TrackReviewRange) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    @Composable fun RangeMenu(modifier: Modifier) {
        Box(modifier) {
            WhipOutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().testTag("track-review-range-menu")
                    .semantics {
                        contentDescription = "Entry Dates: ${scope.range.label}"
                        stateDescription = if (expanded) "Menu open" else "Menu closed"
                    },
            ) {
                Text(scope.range.label, Modifier.weight(1f))
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                TrackReviewRange.entries.forEach { range ->
                    DropdownMenuItem(
                        text = { Text(range.label) },
                        modifier = Modifier.testTag("track-review-range-${range.name}").semantics { selected = scope.range == range },
                        leadingIcon = if (scope.range == range) {{ Icon(Icons.Outlined.Check, contentDescription = null) }} else null,
                        onClick = { expanded = false; onChange(range) },
                    )
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.5f) {
                Column {
                    RangeMenu(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
                }
            } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RangeMenu(Modifier.weight(1f))
                actions()
            }
        }
        Text(scope.range.days?.let { "${today.minusDays(it - 1).format(format)} – ${today.format(format)} · ${quantityLabel(count, "matching Entry")}" }
            ?: "All dates, including future Entries · ${quantityLabel(count, "matching Entry")}",
            modifier = Modifier.testTag("track-review-scope"), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun TrackRecordedTrend(
    field: TrackField,
    entries: List<TrackEntryProjection>,
    units: List<UnitDefinition>,
    showData: Boolean,
    onShowData: (Boolean) -> Unit,
    page: Int,
    onPage: (Int) -> Unit,
    entryTitle: (TrackEntryProjection) -> String,
    onOpenEntry: (TrackEntryProjection) -> Unit,
) {
    val points = remember(field, entries) { trackReviewSeries(field, entries) }
    val format = remember(field, units) { TrackInsightNumberFormat(field, units) }
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val lastPage = (points.size - 1).coerceAtLeast(0) / 25
    val displayedPage = page.coerceAtMost(lastPage)
    WhipGroupedInformationCard(Modifier.testTag("track-trend-${field.uuid}")) {
        Text("${field.name} Over Time", style = MaterialTheme.typography.titleSmall)
        if (points.isEmpty()) Text("No recorded values in this scope.") else {
            val min = points.minOf { it.value }
            val max = points.maxOf { it.value }
            val first = points.first().entry.entry.entryDate
            val last = points.last().entry.entry.entryDate
            val scale = maxOf(kotlin.math.abs(min), kotlin.math.abs(max)).takeIf { it > 0.0 } ?: 1.0
            val span = (max / scale - min / scale).takeIf { it > 0.0 } ?: 1.0
            val days = (last.toEpochDay() - first.toEpochDay()).coerceAtLeast(1)
            val description = "${quantityLabel(points.size, "observation")}. ${first.format(dateFormat)} to ${last.format(dateFormat)}. Range ${format.format(min)} to ${format.format(max)}."
            Text(format.format(points.last().value), style = MaterialTheme.typography.headlineSmall)
            Text("Latest · ${last.format(dateFormat)} · ${quantityLabel(points.size, "observation")}", style = MaterialTheme.typography.bodySmall)
            if (points.size == 1) {
                Text("More observations are needed to show a trend.", style = MaterialTheme.typography.bodySmall)
                NavigationRow(
                    title = entryTitle(points.single().entry), preserveTitleCase = true,
                    supportingText = "${last.format(dateFormat)} · View source Entry",
                    onClick = { onOpenEntry(points.single().entry) },
                    modifier = Modifier.testTag("track-trend-latest-entry-${field.uuid}-${points.single().entry.entry.id}"),
                )
            } else {
            Text(description, style = MaterialTheme.typography.bodySmall)
            val color = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = description }) {
                val inset = 4.dp.toPx()
                fun position(point: TrackReviewPoint) = Offset(
                    inset + ((point.entry.entry.entryDate.toEpochDay() - first.toEpochDay()).toDouble() / days * (size.width - inset * 2)).toFloat(),
                    if (max == min) size.height / 2 else inset + ((max / scale - point.value / scale) / span * (size.height - inset * 2)).toFloat(),
                )
                points.zipWithNext().forEach { (start, end) -> drawLine(color, position(start), position(end), 3.dp.toPx()) }
                points.forEach { drawCircle(color, 3.dp.toPx(), position(it)) }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(first.format(dateFormat), style = MaterialTheme.typography.bodySmall)
                Text(last.format(dateFormat), style = MaterialTheme.typography.bodySmall)
            }
            }
            DisclosureButton("Recorded Values", showData, { onShowData(!showData) }, Modifier.testTag("track-trend-data-${field.uuid}"))
            if (showData) {
                Text("Recorded observations use Entry Date. Missing values are omitted; repeated dates stay separate.", style = MaterialTheme.typography.bodySmall)
                Text("Page ${displayedPage + 1} of ${lastPage + 1} · newest first", style = MaterialTheme.typography.labelSmall)
                points.asReversed().drop(displayedPage * 25).take(25).forEach { point ->
                    val original = point.entry.value(field.id)
                    val value = if (field.type == TrackFieldType.Number) {
                        val unit = units.firstOrNull { it.id == original?.enteredUnitId }
                        original?.enteredNumber?.let(::plainNumericValue).orEmpty() + " " + (unit?.symbol ?: original?.enteredUnitId).orEmpty()
                    } else original?.scaleValue?.let(::formatTrackScaleValue).orEmpty()
                    NavigationRow(
                        title = entryTitle(point.entry),
                        preserveTitleCase = true,
                        supportingText = "${point.entry.entry.entryDate.format(dateFormat)} · ${value.trim()}",
                        onClick = { onOpenEntry(point.entry) },
                        modifier = Modifier.testTag("track-trend-entry-${field.uuid}-${point.entry.entry.id}"),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WhipTextButton(enabled = displayedPage > 0, onClick = { onPage(displayedPage - 1) }) { Text("Newer Values") }
                    WhipTextButton(enabled = displayedPage < lastPage, onClick = { onPage(displayedPage + 1) }) { Text("Earlier Values") }
                }
            }
        }
    }
}
