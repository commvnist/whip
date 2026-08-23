package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whip.app.domain.editableNumericValue
import java.time.DayOfWeek

/**
 * Models a dependent control without allowing an unexplained disabled state.
 * Callers must provide the reason and the action that makes the control available.
 */
internal data class ControlAvailability(
    val enabled: Boolean,
    val unavailableExplanation: String? = null,
) {
    init {
        require(enabled || !unavailableExplanation.isNullOrBlank()) {
            "Disabled controls must explain why they are unavailable and how to enable them"
        }
    }
}

@Composable
internal fun AvailabilityNotice(
    label: String,
    availability: ControlAvailability,
    modifier: Modifier = Modifier,
) {
    if (!availability.enabled) {
        Text(
            "$label unavailable. ${availability.unavailableExplanation}",
            modifier = modifier.semantics {
                contentDescription = "$label unavailable. ${availability.unavailableExplanation}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A full-window dialog whose card can be positioned wholly inside the active fold pane.
 * Material's AlertDialog sizes its platform window around the card, which prevents an
 * offset applied to the card from moving it out of the hinge area.
 */
@Composable
internal fun ProductivityEditorDialog(
    modifier: Modifier,
    testTag: String?,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = modifier
                    .heightIn(max = maxHeight * 0.92f)
                    .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    title()
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f, fill = false)) { text() }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        dismissButton()
                        confirmButton()
                    }
                }
            }
        }
    }
}

/**
 * Alert-style content hosted in a full-window dialog so a pane offset is never
 * clipped by a platform window that was sized around the unshifted card.
 */
@Composable
internal fun PaneAwareAlertDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit = {},
) {
    ProductivityEditorDialog(
        modifier = modifier.widthIn(min = 280.dp, max = 560.dp),
        testTag = null,
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

internal fun formatClockMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClockPickerButton(
    label: String,
    minutes: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by rememberSaveable(label) { mutableStateOf(false) }
    OutlinedButton(onClick = { pickerOpen = true }, modifier = modifier.fillMaxWidth()) {
        Text(if (minutes == null) "$label · Not set" else "$label · ${formatClockMinutes(minutes)}")
    }
    if (pickerOpen) {
        val picker = rememberTimePickerState(
            initialHour = (minutes ?: 8 * 60) / 60,
            initialMinute = (minutes ?: 0) % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = { Text(label) },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(picker.hour * 60 + picker.minute)
                    pickerOpen = false
                }) { Text("Set time") }
            },
            dismissButton = {
                Row {
                    if (minutes != null) TextButton(onClick = { onChange(null); pickerOpen = false }) { Text("Clear") }
                    TextButton(onClick = { pickerOpen = false }) { Text("Cancel") }
                }
            },
        )
    }
}

@Composable
internal fun ReminderTimesEditor(
    label: String,
    values: List<Int>,
    onChange: (List<Int>) -> Unit,
) {
    var adding by rememberSaveable(label) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (values.isEmpty()) {
            Text("No reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                values.sorted().forEach { value ->
                    FilterChip(
                        selected = true,
                        onClick = { onChange(values - value) },
                        label = { Text(formatClockMinutes(value)) },
                        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                        modifier = Modifier.semantics { contentDescription = "Remove $label ${formatClockMinutes(value)}" },
                    )
                }
            }
        }
        OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add reminder time")
        }
    }
    if (adding) {
        ClockPickerDialog(
            title = "Add reminder time",
            initialMinutes = values.lastOrNull() ?: 8 * 60,
            onDismiss = { adding = false },
            onSet = { value -> onChange((values + value).distinct().sorted()); adding = false },
        )
    }
}

@Composable
internal fun WeekdayReminderEditor(
    values: Map<DayOfWeek, List<Int>>,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    onChange: (Map<DayOfWeek, List<Int>>) -> Unit,
) {
    var choosingDay by rememberSaveable { mutableStateOf(false) }
    var addingDayName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeValues = values.mapValues { (_, times) -> times.distinct().sorted() }
        .filterValues { it.isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Weekday-specific reminders", style = MaterialTheme.typography.labelLarge)
        Text(
            if (activeValues.isEmpty()) {
                "No weekday-specific reminders. Days without one use Default reminders."
            } else {
                "Days without a weekday-specific time use Default reminders."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        activeValues.toSortedMap(compareBy { it.value }).forEach { (day, times) ->
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${day.weekdayLabel()} reminders", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    times.sorted().forEach { value -> FilterChip(
                        selected = true,
                        onClick = {
                            val remaining = times - value
                            onChange(if (remaining.isEmpty()) activeValues - day else activeValues + (day to remaining))
                        },
                        label = { Text(formatClockMinutes(value)) },
                        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                    ) }
                }
            }
        }
        OutlinedButton(onClick = { choosingDay = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add weekday reminder")
        }
    }
    if (choosingDay) {
        AlertDialog(
            onDismissRequest = { choosingDay = false },
            title = { Text("Choose weekday") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Nothing is scheduled until you choose a time and tap Add.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        orderedWeekdays(firstDayOfWeek).forEach { day -> FilterChip(
                            selected = false,
                            onClick = {
                                addingDayName = day.name
                                choosingDay = false
                            },
                            label = { Text(day.weekdayLabel()) },
                        ) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosingDay = false }) { Text("Cancel") } },
        )
    }
    addingDayName?.let { dayName ->
        val selectedDay = DayOfWeek.valueOf(dayName)
        ClockPickerDialog(
            title = "${selectedDay.weekdayLabel()} reminder",
            initialMinutes = activeValues[selectedDay]?.lastOrNull() ?: 8 * 60,
            onDismiss = { addingDayName = null },
            onSet = { value ->
                onChange(activeValues + (selectedDay to (activeValues[selectedDay].orEmpty() + value).distinct().sorted()))
                addingDayName = null
            },
        )
    }
}

private fun DayOfWeek.weekdayLabel(): String = name.lowercase().replaceFirstChar(Char::uppercase)

private fun orderedWeekdays(first: DayOfWeek): List<DayOfWeek> =
    (0..6).map { offset -> DayOfWeek.of((first.value - 1 + offset) % 7 + 1) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockPickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSet: (Int) -> Unit,
) {
    val picker = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = picker) },
        confirmButton = { TextButton(onClick = { onSet(picker.hour * 60 + picker.minute) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun NumericQuickActionBuilder(
    values: List<Double>,
    increment: String,
    rawSpecification: String,
    onSpecificationChange: (String) -> Unit,
) {
    var nextValue by rememberSaveable { mutableStateOf("") }
    var rangeStart by rememberSaveable { mutableStateOf("") }
    var rangeEnd by rememberSaveable { mutableStateOf("") }
    var showExpertEntry by rememberSaveable { mutableStateOf(false) }
    fun write(newValues: List<Double>) {
        onSpecificationChange(newValues.filter(Double::isFinite).distinct().sorted().joinToString(",") { editableNumericValue(it) })
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quick buttons", style = MaterialTheme.typography.labelLarge)
        Text("Add the values you use most often. They are amounts to add, not units of measurement.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (values.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value ->
                FilterChip(
                    selected = true,
                    onClick = { write(values - value) },
                    label = { Text(editableNumericValue(value)) },
                    trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nextValue, { nextValue = it }, label = { Text("Value") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedButton(
                enabled = nextValue.toDoubleOrNull()?.let { it.isFinite() && it > 0.0 } == true,
                onClick = { nextValue.toDoubleOrNull()?.let { write(values + it) }; nextValue = "" },
            ) { Text("Add") }
        }
        Text("Build a range", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(rangeStart, { rangeStart = it }, label = { Text("From") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(rangeEnd, { rangeEnd = it }, label = { Text("To") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedButton(
            enabled = rangeStart.toDoubleOrNull() != null && rangeEnd.toDoubleOrNull() != null && increment.toDoubleOrNull()?.let { it > 0.0 } == true,
            onClick = {
                val start = rangeStart.toDoubleOrNull() ?: return@OutlinedButton
                val end = rangeEnd.toDoubleOrNull() ?: return@OutlinedButton
                val step = increment.toDoubleOrNull() ?: return@OutlinedButton
                if (end >= start && step > 0.0) {
                    val generated = generateSequence(start) { previous -> (previous + step).takeIf { it <= end + step / 1000.0 } }.take(24).toList()
                    write(generated)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Use range with increment $increment") }
        TextButton(onClick = { showExpertEntry = !showExpertEntry }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showExpertEntry) "Hide expert text entry" else "Expert text entry")
        }
        if (showExpertEntry) {
            OutlinedTextField(
                rawSpecification,
                onSpecificationChange,
                label = { Text("Comma-separated values or range") },
                supportingText = { Text("Examples: 1, 2.5, 8 or 1-10") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
