package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.RectangleShape
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
internal fun EditorSectionHeader(
    title: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(top = 3.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

internal data class WhipDialogPlacement(
    val offsetX: Dp = 0.dp,
    val maxWidth: Dp = WhipContentWidth.compactDialog,
)

internal val LocalWhipDialogPlacement = staticCompositionLocalOf { WhipDialogPlacement() }

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
 * Keeps the consequence of a choice next to the control that caused it.
 * Use this before conditionally revealed settings when the relationship is not
 * already obvious from a single adjacent field label.
 */
@Composable
internal fun DependentSettingsNotice(
    message: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    WhipNoticeCard(
        message = message,
        modifier = modifier
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .semantics { contentDescription = "Dependent settings. $message" },
        tone = WhipNoticeTone.Neutral,
    )
}

/** Keeps a rejected save actionable instead of leaving an enabled Save button
 * that appears to do nothing. The summary is suitable for any editor dialog;
 * individual required fields should still carry an asterisk and inline error. */
@Composable
internal fun FormValidationSummary(
    messages: List<String>,
    visible: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "form-save-problem",
) {
    if (!visible || messages.isEmpty()) return
    val distinctMessages = messages.distinct()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Save blocked. ${distinctMessages.joinToString(". ")}"
            },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                "Review Required Fields",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            distinctMessages.take(5).forEach { message ->
                Text(
                    "• $message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (distinctMessages.size > 5) Text(
                "• ${distinctMessages.size - 5} more fields need attention",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
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
    primary: Boolean = false,
    paneTitle: String = "Editor",
    stableHeight: Boolean = false,
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
                    .then(
                        when {
                            primary -> Modifier.fillMaxHeight()
                            stableHeight -> Modifier.height(maxHeight * 0.92f)
                            else -> Modifier.heightIn(max = maxHeight * 0.92f)
                        },
                    )
                    .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
                    .semantics { this.paneTitle = paneTitle },
                shape = if (primary) RectangleShape else MaterialTheme.shapes.extraLarge,
                tonalElevation = if (primary) 0.dp else 6.dp,
            ) {
                if (primary) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            dismissButton()
                            Box(Modifier.weight(1f).semantics { heading() }) { title() }
                            confirmButton()
                        }
                        HorizontalDivider()
                        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                            text()
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.semantics { heading() }) { title() }
                        Box(Modifier.weight(1f, fill = stableHeight)) { text() }
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
    paneTitle: String = "Dialog",
    stableHeight: Boolean = false,
) {
    val placement = LocalWhipDialogPlacement.current
    val resolvedModifier = if (modifier == Modifier) {
        Modifier.absoluteOffset(x = placement.offsetX).width(placement.maxWidth)
    } else modifier
    ProductivityEditorDialog(
        modifier = resolvedModifier.widthIn(min = 280.dp, max = WhipContentWidth.compactDialog),
        testTag = null,
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        paneTitle = paneTitle,
        stableHeight = stableHeight,
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
    WhipOutlinedButton(onClick = { pickerOpen = true }, modifier = modifier.fillMaxWidth()) {
        Text(if (minutes == null) "$label · Not set" else "$label · ${formatClockMinutes(minutes)}")
    }
    if (pickerOpen) {
        val picker = rememberTimePickerState(
            initialHour = (minutes ?: 8 * 60) / 60,
            initialMinute = (minutes ?: 0) % 60,
            is24Hour = true,
        )
        PaneAwareAlertDialog(
            onDismissRequest = { pickerOpen = false },
            paneTitle = label,
            title = { Text(label) },
            text = { TimePicker(state = picker) },
            confirmButton = {
                WhipTextButton(onClick = {
                    onChange(picker.hour * 60 + picker.minute)
                    pickerOpen = false
                }) { Text("Set Time") }
            },
            dismissButton = {
                Row {
                    if (minutes != null) WhipTextButton(onClick = { onChange(null); pickerOpen = false }) { Text("Clear") }
                    WhipTextButton(onClick = { pickerOpen = false }) { Text("Cancel") }
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
                    WhipFilterChip(
                        selected = true,
                        onClick = { onChange(values - value) },
                        label = { Text(formatClockMinutes(value)) },
                        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                        modifier = Modifier.semantics { contentDescription = "Remove $label ${formatClockMinutes(value)}" },
                    )
                }
            }
        }
        WhipOutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Reminder Time")
        }
    }
    if (adding) {
        ClockPickerDialog(
            title = "Add Reminder Time",
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
        Text("Weekday-Specific Reminders", style = MaterialTheme.typography.labelLarge)
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
                    times.sorted().forEach { value -> WhipFilterChip(
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
        WhipOutlinedButton(onClick = { choosingDay = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Add Weekday Reminder")
        }
    }
    if (choosingDay) {
        PaneAwareAlertDialog(
            onDismissRequest = { choosingDay = false },
            paneTitle = "Choose Weekday",
            title = { Text("Choose Weekday") },
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
                        orderedWeekdays(firstDayOfWeek).forEach { day -> WhipFilterChip(
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
            dismissButton = { WhipTextButton(onClick = { choosingDay = false }) { Text("Cancel") } },
        )
    }
    addingDayName?.let { dayName ->
        val selectedDay = DayOfWeek.valueOf(dayName)
        ClockPickerDialog(
            title = "${selectedDay.weekdayLabel()} Reminder",
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
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        paneTitle = title,
        title = { Text(title) },
        text = { TimePicker(state = picker) },
        confirmButton = { WhipTextButton(onClick = { onSet(picker.hour * 60 + picker.minute) }) { Text("Add") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
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
        Text("Quick Buttons", style = MaterialTheme.typography.labelLarge)
        Text("Add the values you use most often. They are amounts to add, not units of measurement.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (values.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value ->
                WhipFilterChip(
                    selected = true,
                    onClick = { write(values - value) },
                    label = { Text(editableNumericValue(value)) },
                    trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nextValue, { nextValue = it }, label = { Text("Value") }, singleLine = true, modifier = Modifier.weight(1f))
            WhipOutlinedButton(
                enabled = nextValue.toDoubleOrNull()?.let { it.isFinite() && it > 0.0 } == true,
                onClick = { nextValue.toDoubleOrNull()?.let { write(values + it) }; nextValue = "" },
            ) { Text("Add") }
        }
        Text("Build a Range", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(rangeStart, { rangeStart = it }, label = { Text("From") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(rangeEnd, { rangeEnd = it }, label = { Text("To") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        WhipOutlinedButton(
            enabled = rangeStart.toDoubleOrNull() != null && rangeEnd.toDoubleOrNull() != null && increment.toDoubleOrNull()?.let { it > 0.0 } == true,
            onClick = {
                val start = rangeStart.toDoubleOrNull() ?: return@WhipOutlinedButton
                val end = rangeEnd.toDoubleOrNull() ?: return@WhipOutlinedButton
                val step = increment.toDoubleOrNull() ?: return@WhipOutlinedButton
                if (end >= start && step > 0.0) {
                    val generated = generateSequence(start) { previous -> (previous + step).takeIf { it <= end + step / 1000.0 } }.take(24).toList()
                    write(generated)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Use Range with Increment $increment") }
        WhipTextButton(onClick = { showExpertEntry = !showExpertEntry }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showExpertEntry) "Hide Expert Text Entry" else "Expert Text Entry")
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

/**
 * Shared recovery guard for commands that make a configured Automation
 * disappear immediately. Naming the Automation and its consequence keeps the
 * destructive contract consistent across Habits, Goals, and Tracks.
 */
@Composable
internal fun RemoveAutomationConfirmationDialog(
    automationName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    PaneAwareAlertDialog(
        onDismissRequest = onDismiss,
        paneTitle = "Remove Automation",
        title = { Text("Remove Automation?") },
        text = {
            Text(
                "“$automationName” will stop running and its configuration will be removed. " +
                    "Source history remains, but results generated only by this Automation—such as Goal contributions " +
                    "or automatic Habit check-ins—will be removed and progress recalculated.",
            )
        },
        confirmButton = {
            WhipTextButton(onClick = onConfirm) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
