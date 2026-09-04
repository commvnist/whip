package com.whip.app.ui

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhipDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    firstDayOfWeek: DayOfWeek? = null,
    saving: Boolean = false,
    persistenceError: String? = null,
    savingLabel: String = "Saving Date",
    preferWheelSelector: Boolean? = null,
) {
    val resolvedFirstDayOfWeek = firstDayOfWeek ?: LocalWhipFirstDayOfWeek.current
    val today = LocalWhipToday.current
    val density = LocalDensity.current
    val preferAccessibleWheels = preferWheelSelector
        ?: (LocalConfiguration.current.screenWidthDp < 384 || density.fontScale >= 1.5f)
    var selectedEpochDay by rememberSaveable(initialDate) { mutableLongStateOf(initialDate.toEpochDay()) }
    var monthStartEpochDay by rememberSaveable(initialDate) { mutableLongStateOf(initialDate.withDayOfMonth(1).toEpochDay()) }
    var choosingDateWithWheels by rememberSaveable(initialDate, preferAccessibleWheels) { mutableStateOf(preferAccessibleWheels) }
    var jumpYear by rememberSaveable(initialDate) { mutableIntStateOf(initialDate.year) }
    var jumpMonth by rememberSaveable(initialDate) { mutableIntStateOf(initialDate.monthValue) }
    var jumpDay by rememberSaveable(initialDate) { mutableIntStateOf(initialDate.dayOfMonth) }
    var wheelResetToken by rememberSaveable(initialDate) { mutableIntStateOf(0) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val monthStart = LocalDate.ofEpochDay(monthStartEpochDay).withDayOfMonth(1)
    val jumpDate = clampedDate(jumpYear, jumpMonth, jumpDay)
    val displayedDate = if (choosingDateWithWheels) jumpDate else selectedDate
    val leadingEmptyDays = (monthStart.dayOfWeek.value - resolvedFirstDayOfWeek.value + 7) % 7
    val calendarCells = buildList<LocalDate?> {
        repeat(leadingEmptyDays) { add(null) }
        repeat(monthStart.lengthOfMonth()) { dayOffset -> add(monthStart.plusDays(dayOffset.toLong())) }
        while (size % 7 != 0) add(null)
    }
    ProductivityEditorDialog(
        modifier = Modifier.widthIn(min = 280.dp, max = 560.dp).then(modifier),
        testTag = "date-picker-dialog",
        paneTitle = "Choose Date",
        onDismissRequest = { if (!saving) onDismiss() },
        inputBlocked = saving,
        inputBlockedLabel = savingLabel,
        title = { Text("Choose Date") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                PersistenceFailureNotice(persistenceError, testTag = "date-picker-save-problem")
                Text(displayedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)), Modifier.testTag("date-picker-selected-date"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                HorizontalDivider()
                if (choosingDateWithWheels) {
                    Text("Swipe each column to choose a year, month, and day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    key(wheelResetToken) {
                        Row(Modifier.fillMaxWidth().testTag("date-picker-wheel-selector"), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                            DateWheelColumn("Year", 1..9999, jumpYear, Int::toString, { year -> jumpYear = year; jumpDay = jumpDay.coerceAtMost(YearMonth.of(year, jumpMonth).lengthOfMonth()) }, Modifier.weight(1f))
                            DateWheelColumn("Month", 1..12, jumpMonth, { month -> Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault()) }, { month -> jumpMonth = month; jumpDay = jumpDay.coerceAtMost(YearMonth.of(jumpYear, month).lengthOfMonth()) }, Modifier.weight(1.35f))
                            DateWheelColumn("Day", 1..YearMonth.of(jumpYear, jumpMonth).lengthOfMonth(), jumpDay, Int::toString, { jumpDay = it }, Modifier.weight(0.85f))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        WhipTextButton(onClick = { jumpYear = today.year; jumpMonth = today.monthValue; jumpDay = today.dayOfMonth; wheelResetToken += 1 }, modifier = Modifier.testTag("date-picker-today")) { Text("Today") }
                        WhipOutlinedButton(onClick = { selectedEpochDay = jumpDate.toEpochDay(); monthStartEpochDay = jumpDate.withDayOfMonth(1).toEpochDay(); choosingDateWithWheels = false }, modifier = Modifier.testTag("date-picker-show-calendar")) { Text("Show Calendar") }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { monthStartEpochDay = monthStart.minusMonths(1).toEpochDay() }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.ChevronLeft, "Previous Month") }
                        WhipTextButton(onClick = { jumpYear = selectedDate.year; jumpMonth = selectedDate.monthValue; jumpDay = selectedDate.dayOfMonth; choosingDateWithWheels = true }, modifier = Modifier.weight(1f).testTag("date-picker-month-year").semantics { contentDescription = "${monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}. Jump to Date" }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy")), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Jump to Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { monthStartEpochDay = monthStart.plusMonths(1).toEpochDay() }, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.ChevronRight, "Next Month") }
                    }
                    Row(Modifier.fillMaxWidth()) { datePickerOrderedWeekdays(resolvedFirstDayOfWeek).forEach { day -> Text(day.shortLabel, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    calendarCells.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth()) { week.forEach { date ->
                        if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1f)) else {
                            val selected = date == selectedDate
                            Surface(onClick = { selectedEpochDay = date.toEpochDay() }, modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).semantics { contentDescription = "Select ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))}"; stateDescription = if (selected) "Selected" else "Not Selected" }, shape = MaterialTheme.shapes.small, color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                                Box(contentAlignment = Alignment.Center) { Text(date.dayOfMonth.toString(), color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold.takeIf { selected }) }
                            }
                        }
                    } } }
                }
            }
        },
        confirmButton = { WhipTextButton(enabled = !saving, onClick = { onDateSelected(if (choosingDateWithWheels) jumpDate else selectedDate) }) { Text("Set") } },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun clampedDate(year: Int, month: Int, day: Int): LocalDate {
    val yearMonth = YearMonth.of(year, month)
    return yearMonth.atDay(day.coerceIn(1, yearMonth.lengthOfMonth()))
}

@Composable
private fun DateWheelColumn(label: String, values: IntRange, selectedValue: Int, valueLabel: (Int) -> String, onValueSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selectedIndex = (selectedValue - values.first).coerceIn(0, values.count() - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val coroutineScope = rememberCoroutineScope()
    val currentSelectedValue by rememberUpdatedState(selectedValue)
    val currentOnValueSelected by rememberUpdatedState(onValueSelected)
    LaunchedEffect(listState, values.first, values.last) { snapshotFlow { listState.centeredItemIndex()?.minus(1) }.filterNotNull().distinctUntilChanged().collect { index -> val value = values.first + index; if (value in values && value != currentSelectedValue) currentOnValueSelected(value) } }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().height(144.dp).testTag("date-picker-${label.lowercase(Locale.ROOT)}-wheel").semantics { contentDescription = "$label picker"; stateDescription = valueLabel(selectedValue) }, flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center)) {
            items(count = values.count() + 2, key = { index -> "$label-wheel-$index" }) { index ->
                if (index == 0 || index == values.count() + 1) { Spacer(Modifier.height(48.dp)); return@items }
                val value = values.first + index - 1
                val selected = value == selectedValue
                Surface(onClick = { coroutineScope.launch { listState.animateScrollToItem(value - values.first) } }, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("date-picker-${label.lowercase(Locale.ROOT)}-$value").semantics { contentDescription = "$label ${valueLabel(value)}"; stateDescription = if (selected) "Selected" else "Not selected" }, shape = MaterialTheme.shapes.small, color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Box(contentAlignment = Alignment.Center) { Text(valueLabel(value), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold.takeIf { selected }) }
                }
            }
        }
    }
}

private fun LazyListState.centeredItemIndex(): Int? {
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return layoutInfo.visibleItemsInfo.minByOrNull { item -> abs(item.offset + item.size / 2 - viewportCenter) }?.index
}

private val DayOfWeek.shortLabel: String
    get() = name.take(2).lowercase().replaceFirstChar(Char::uppercase)

private fun datePickerOrderedWeekdays(first: DayOfWeek): List<DayOfWeek> =
    (0..6).map { offset -> DayOfWeek.of((first.value - 1 + offset) % 7 + 1) }
