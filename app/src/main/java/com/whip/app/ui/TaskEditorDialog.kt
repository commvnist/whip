package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import com.whip.app.domain.RecurrenceEnd
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskProgressDisplay
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.TaskQuickCaptureParser
import com.whip.app.domain.WhipTask
import com.whip.app.domain.Area
import com.whip.app.domain.toDraft
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class TaskEditorRequest(
    val task: WhipTask? = null,
    val fromOccurrence: LocalDate? = null,
    val initialCapture: String = "",
    val initialScheduleDate: LocalDate? = null,
    val sessionId: Long = 0L,
)

private enum class RepeatPreset {
    Daily,
    EveryDays,
    Weekdays,
    EveryWeeks,
    EveryMonths,
    EveryYears,
}

private enum class DateTarget {
    Main,
    End,
    Deadline,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(
    request: TaskEditorRequest,
    onDismiss: () -> Unit,
    onSave: (Long?, TaskDraft, LocalDate?) -> Unit,
    onSaveAndNew: ((Long?, TaskDraft, LocalDate?) -> Unit)? = null,
    onRequestNotificationPermission: () -> Unit,
    defaultRepeatStepPolicy: RepeatStepPolicy = RepeatStepPolicy.Reset,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    today: LocalDate = LocalDate.now(),
    naturalLanguageCapture: Boolean = false,
    powerMode: Boolean = false,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    knownTags: List<String> = emptyList(),
    paneOffsetX: Dp = 0.dp,
    paneMaxWidth: Dp = 720.dp,
    saving: Boolean = false,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }
    val captureLines = request.initialCapture.lines().map(String::trim).filter(String::isNotBlank)
    val initial = request.task?.toDraft() ?: TaskDraft(
        title = captureLines.firstOrNull().orEmpty(),
        steps = captureLines.drop(1).mapIndexed { index, line -> TaskStepDraft(title = line, position = index) },
        showSubtaskProgress = captureLines.size > 1,
        repeatStepPolicy = defaultRepeatStepPolicy,
        scheduleKind = if (request.initialScheduleDate == null) ScheduleKind.Anytime else ScheduleKind.Once,
        date = request.initialScheduleDate,
        inbox = request.initialScheduleDate == null,
        areaId = defaultAreaId,
        area = areas.firstOrNull { it.id == defaultAreaId }?.name.orEmpty(),
    )
    val initialRule = initial.recurrence
    val editStartDate = request.fromOccurrence ?: initialRule?.startDate
    val editorKey = "${request.task?.id ?: "new"}:${request.fromOccurrence?.toEpochDay() ?: "base"}:${request.sessionId}"

    var title by rememberSaveable(editorKey) { mutableStateOf(initial.title) }
    var notes by rememberSaveable(editorKey) { mutableStateOf(initial.notes) }
    var scheduleKind by rememberSaveable(editorKey) { mutableStateOf(initial.scheduleKind) }
    var mainDate by rememberSaveable(editorKey) {
        mutableStateOf(editStartDate ?: initial.date ?: today)
    }
    var repeatPreset by rememberSaveable(editorKey) { mutableStateOf(initialRule.toPreset()) }
    var intervalText by rememberSaveable(editorKey) {
        mutableStateOf((initialRule?.interval ?: 2).toString())
    }
    var weekdays by rememberSaveable(editorKey) {
        mutableStateOf(
            initialRule?.weekdays?.takeIf(Set<DayOfWeek>::isNotEmpty)
                ?: setOf(mainDate.dayOfWeek),
        )
    }
    var recurrenceEnd by rememberSaveable(editorKey) {
        mutableStateOf(initialRule?.end ?: RecurrenceEnd.Never)
    }
    var endDate by rememberSaveable(editorKey) {
        mutableStateOf(initialRule?.endDate ?: mainDate.plusMonths(1))
    }
    var occurrenceCountText by rememberSaveable(editorKey) {
        mutableStateOf((initialRule?.occurrenceCount ?: 10).toString())
    }
    var hasTime by rememberSaveable(editorKey) { mutableStateOf(initial.timeMinutes != null) }
    var timeMinutes by rememberSaveable(editorKey) { mutableIntStateOf(initial.timeMinutes ?: 9 * 60) }
    var reminderEnabled by rememberSaveable(editorKey) { mutableStateOf(initial.reminderEnabled) }
    var reminderOffsets by rememberSaveable(editorKey) {
        mutableStateOf(initial.reminderOffsetsMinutes.toSet().ifEmpty { setOf(0).takeIf { initial.reminderEnabled }.orEmpty() })
    }
    var customReminderText by rememberSaveable(editorKey) { mutableStateOf("") }
    var priority by rememberSaveable(editorKey) { mutableStateOf(initial.priority) }
    var inbox by rememberSaveable(editorKey) { mutableStateOf(initial.inbox) }
    var durationMinutes by rememberSaveable(editorKey) { mutableStateOf(initial.durationMinutes?.toString().orEmpty()) }
    var effort by rememberSaveable(editorKey) { mutableStateOf(initial.effort) }
    var areaId by rememberSaveable(editorKey) { mutableStateOf(initial.areaId) }
    var area by rememberSaveable(editorKey) { mutableStateOf(initial.area) }
    var tagsText by rememberSaveable(editorKey) { mutableStateOf(initial.tags.joinToString(", ")) }
    var hasDeadline by rememberSaveable(editorKey) { mutableStateOf(initial.deadline != null) }
    var deadline by rememberSaveable(editorKey) { mutableStateOf(initial.deadline ?: mainDate.plusDays(7)) }
    var recurrenceAnchor by rememberSaveable(editorKey) {
        mutableStateOf(initialRule?.anchor ?: RecurrenceAnchor.Schedule)
    }
    var smartCaptureSummary by rememberSaveable(editorKey) { mutableStateOf<String?>(null) }
    val stepDraftSaver = listSaver<List<TaskStepDraft>, Any>(
        save = { drafts -> drafts.flatMap { listOf(it.id ?: Long.MIN_VALUE, it.title, it.position, it.notes) } },
        restore = { saved -> saved.chunked(4).map { values ->
            TaskStepDraft(
                id = (values[0] as Long).takeUnless { it == Long.MIN_VALUE },
                title = values[1] as String,
                position = values[2] as Int,
                notes = values[3] as String,
            )
        } },
    )
    var stepDrafts by rememberSaveable(editorKey, stateSaver = stepDraftSaver) {
        mutableStateOf(initial.steps.sortedBy(TaskStepDraft::position))
    }
    var newStepTitle by rememberSaveable(editorKey) { mutableStateOf("") }
    var showSubtaskProgress by rememberSaveable(editorKey) {
        mutableStateOf(initial.showSubtaskProgress)
    }
    var progressDisplay by rememberSaveable(editorKey) { mutableStateOf(initial.progressDisplay) }
    var autoCompleteFromSteps by rememberSaveable(editorKey) {
        mutableStateOf(initial.autoCompleteFromSteps)
    }
    var repeatStepPolicy by rememberSaveable(editorKey) { mutableStateOf(initial.repeatStepPolicy) }
    var missedOccurrencePolicy by rememberSaveable(editorKey) {
        mutableStateOf(initial.missedOccurrencePolicy)
    }
    var dateTarget by rememberSaveable(editorKey) { mutableStateOf<DateTarget?>(null) }
    var showTimePicker by rememberSaveable(editorKey) { mutableStateOf(false) }
    var showAdvanced by rememberSaveable(editorKey) {
        mutableStateOf(
            powerMode || initial.notes.isNotBlank() || initial.steps.isNotEmpty() ||
                initialRule?.end != null && initialRule.end != RecurrenceEnd.Never ||
                initial.priority != TaskPriority.None || initial.tags.isNotEmpty() ||
                initial.deadline != null ||
                initialRule?.anchor == RecurrenceAnchor.Completion ||
                initial.missedOccurrencePolicy != MissedOccurrencePolicy.KeepLatest ||
                initial.durationMinutes != null || initial.effort != TaskEffort.Moderate,
        )
    }
    var recipesOpen by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(editorKey) { mutableStateOf(false) }

    LaunchedEffect(editorKey) {
        if (request.task == null) {
            titleFocusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val initialOffsets = initial.reminderOffsetsMinutes.toSet().ifEmpty {
        setOf(0).takeIf { initial.reminderEnabled }.orEmpty()
    }
    val isDirty = title != initial.title || notes != initial.notes ||
        scheduleKind != initial.scheduleKind ||
        (scheduleKind == ScheduleKind.Once && mainDate != (initial.date ?: today)) ||
        (scheduleKind == ScheduleKind.Recurring && (
            mainDate != (editStartDate ?: today) || repeatPreset != initialRule.toPreset() ||
                intervalText != (initialRule?.interval ?: 2).toString() ||
                weekdays != (initialRule?.weekdays?.takeIf(Set<DayOfWeek>::isNotEmpty) ?: setOf((editStartDate ?: today).dayOfWeek)) ||
                recurrenceEnd != (initialRule?.end ?: RecurrenceEnd.Never) ||
                endDate != (initialRule?.endDate ?: (editStartDate ?: today).plusMonths(1)) ||
                occurrenceCountText != (initialRule?.occurrenceCount ?: 10).toString() ||
                recurrenceAnchor != (initialRule?.anchor ?: RecurrenceAnchor.Schedule)
            )) ||
        hasTime != (initial.timeMinutes != null) ||
        (hasTime && timeMinutes != (initial.timeMinutes ?: 9 * 60)) ||
        reminderEnabled != initial.reminderEnabled || reminderOffsets != initialOffsets ||
        priority != initial.priority || inbox != initial.inbox ||
        durationMinutes != initial.durationMinutes?.toString().orEmpty() || effort != initial.effort ||
        areaId != initial.areaId || area != initial.area || tagsText != initial.tags.joinToString(", ") ||
        hasDeadline != (initial.deadline != null) ||
        (hasDeadline && deadline != (initial.deadline ?: (editStartDate ?: initial.date ?: today).plusDays(7))) ||
        stepDrafts != initial.steps.sortedBy(TaskStepDraft::position) ||
        showSubtaskProgress != initial.showSubtaskProgress ||
        progressDisplay != initial.progressDisplay ||
        autoCompleteFromSteps != initial.autoCompleteFromSteps ||
        repeatStepPolicy != initial.repeatStepPolicy ||
        missedOccurrencePolicy != initial.missedOccurrencePolicy
    val requestDismiss = { if (isDirty) confirmDiscard = true else onDismiss() }
    BackHandler(enabled = !confirmDiscard, onBack = requestDismiss)

    val interval = intervalText.toIntOrNull()
    val count = occurrenceCountText.toIntOrNull()
    val recurrenceValid = scheduleKind != ScheduleKind.Recurring || (
        (repeatPreset in setOf(RepeatPreset.Daily, RepeatPreset.Weekdays) || (interval ?: 0) > 0) &&
            (repeatPreset != RepeatPreset.Weekdays || weekdays.isNotEmpty()) &&
            (recurrenceEnd != RecurrenceEnd.OnDate || !endDate.isBefore(mainDate)) &&
            (recurrenceEnd != RecurrenceEnd.AfterCount || (count ?: 0) > 0)
        )
    val deadlineValid = !hasDeadline || scheduleKind != ScheduleKind.Once || !deadline.isBefore(mainDate)
    val canSave = title.isNotBlank() && recurrenceValid && deadlineValid &&
        (!reminderEnabled || reminderOffsets.isNotEmpty())
    val recurrence = if (scheduleKind == ScheduleKind.Recurring && recurrenceValid) {
        RecurrenceRule(
            unit = when (repeatPreset) {
                RepeatPreset.Daily, RepeatPreset.EveryDays -> RecurrenceUnit.Days
                RepeatPreset.Weekdays, RepeatPreset.EveryWeeks -> RecurrenceUnit.Weeks
                RepeatPreset.EveryMonths -> RecurrenceUnit.Months
                RepeatPreset.EveryYears -> RecurrenceUnit.Years
            },
            interval = when (repeatPreset) {
                RepeatPreset.Daily, RepeatPreset.Weekdays -> 1
                else -> requireNotNull(interval)
            },
            weekdays = weekdays.takeIf { repeatPreset == RepeatPreset.Weekdays }.orEmpty(),
            startDate = mainDate,
            end = recurrenceEnd,
            endDate = endDate.takeIf { recurrenceEnd == RecurrenceEnd.OnDate },
            occurrenceCount = count.takeIf { recurrenceEnd == RecurrenceEnd.AfterCount },
            anchor = recurrenceAnchor,
        )
    } else null
    val currentDraft = TaskDraft(
        title = title.trim(), notes = notes.trim(), scheduleKind = scheduleKind,
        date = mainDate.takeIf { scheduleKind == ScheduleKind.Once }, recurrence = recurrence,
        timeMinutes = timeMinutes.takeIf { hasTime }, reminderEnabled = hasTime && reminderEnabled,
        steps = stepDrafts.filter { it.title.isNotBlank() }.mapIndexed { position, step ->
            step.copy(title = step.title.trim(), position = position)
        },
        showSubtaskProgress = showSubtaskProgress && stepDrafts.isNotEmpty(),
        progressDisplay = progressDisplay, autoCompleteFromSteps = autoCompleteFromSteps,
        repeatStepPolicy = repeatStepPolicy, priority = priority, areaId = areaId, area = area.trim(),
        tags = tagsText.split(',').map(String::trim).filter(String::isNotBlank).toSet(),
        deadline = deadline.takeIf { scheduleKind == ScheduleKind.Once && hasDeadline },
        reminderOffsetsMinutes = reminderOffsets.toList(),
        missedOccurrencePolicy = missedOccurrencePolicy,
        inbox = inbox && scheduleKind == ScheduleKind.Anytime,
        durationMinutes = durationMinutes.toIntOrNull()?.coerceIn(1, 1_440), effort = effort,
    )

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val editorWidth = minOf(maxWidth * 0.94f, paneMaxWidth)
            Surface(
                modifier = Modifier
                    .absoluteOffset(x = paneOffsetX)
                    .width(editorWidth)
                    .heightIn(max = maxHeight * 0.92f)
                    .testTag("task-editor-surface"),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = if (request.task == null) "Create Task" else "Edit Task",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (request.task == null) {
                    WhipTextButton(onClick = { recipesOpen = true }) { Text("Start from a Plain-Language Recipe") }
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { entered ->
                            val lines = entered.lines()
                            title = lines.firstOrNull().orEmpty()
                            val pastedSteps = lines.drop(1).map(String::trim).filter(String::isNotBlank)
                            if (pastedSteps.isNotEmpty()) {
                                stepDrafts = (stepDrafts + pastedSteps.mapIndexed { index, step ->
                                    TaskStepDraft(title = step, position = stepDrafts.size + index)
                                }).mapIndexed { index, step -> step.copy(position = index) }
                                showSubtaskProgress = true
                                showAdvanced = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(titleFocusRequester).testTag("task-editor-title"),
                        label = { Text("Task") },
                        singleLine = true,
                        supportingText = { Text("Paste multiple lines to turn the extra lines into steps.") },
                    )
                    if (naturalLanguageCapture && request.task == null) {
                        WhipTextButton(
                            enabled = title.isNotBlank(),
                            onClick = {
                                val parsed = TaskQuickCaptureParser.parse(title, today)
                                title = parsed.title
                                scheduleKind = parsed.scheduleKind
                                parsed.date?.let { mainDate = it }
                                parsed.recurrence?.let { rule ->
                                    repeatPreset = rule.toPreset()
                                    intervalText = rule.interval.toString()
                                    weekdays = rule.weekdays
                                }
                                parsed.deadline?.let {
                                    hasDeadline = true
                                    deadline = it
                                }
                                smartCaptureSummary = parsed.recognized.takeIf { it.isNotEmpty() }
                                    ?.joinToString(prefix = "Applied: ")
                                    ?: "No supported date or repeat phrase found"
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Apply Smart Date and Repeat") }
                        smartCaptureSummary?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    AreaPicker(
                        areas = areas,
                        selectedAreaId = areaId,
                        selectedAreaName = area,
                        onSelect = { id, name -> areaId = id; area = name },
                        onCreateArea = onCreateArea,
                        modifier = Modifier.fillMaxWidth(),
                        dialogModifier = Modifier.absoluteOffset(x = paneOffsetX).width(paneMaxWidth),
                        inheritedFromScope = request.task == null && defaultAreaId != null,
                    )
                    FieldLabel("Schedule")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ScheduleKind.entries.forEach { kind ->
                            WhipFilterChip(
                                selected = scheduleKind == kind,
                                onClick = { scheduleKind = kind },
                                label = {
                                    Text(
                                        when (kind) {
                                            ScheduleKind.Anytime -> "Anytime"
                                            ScheduleKind.Once -> "Work date"
                                            ScheduleKind.Recurring -> "Repeat"
                                        },
                                    )
                                },
                            )
                        }
                    }
                    DependentSettingsNotice(
                        message = when (scheduleKind) {
                            ScheduleKind.Anytime -> "Anytime keeps this task unscheduled. Inbox controls appear next."
                            ScheduleKind.Once -> "Work date schedules one occurrence. Its date and optional deadline appear next."
                            ScheduleKind.Recurring -> "Repeat creates future occurrences. Start date and repeat rules appear next."
                        },
                        testTag = "task-schedule-consequence",
                    )

                    if (scheduleKind == ScheduleKind.Anytime) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Inbox")
                                Text(
                                    if (inbox) "Untriaged capture; decide when and where it belongs later." else "Triaged anytime task.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = inbox, onCheckedChange = { inbox = it })
                        }
                    }

                    if (scheduleKind != ScheduleKind.Anytime) {
                        ValueButton(
                            label = if (scheduleKind == ScheduleKind.Once) "Work date" else "Starts",
                            value = mainDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            onClick = { dateTarget = DateTarget.Main },
                        )
                    }

                    if (scheduleKind == ScheduleKind.Once) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Separate deadline")
                                Text(
                                    "Plan work on one date and keep the final due date visible.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = hasDeadline, onCheckedChange = { hasDeadline = it })
                        }
                        if (hasDeadline) {
                            ValueButton(
                                label = "Deadline",
                                value = deadline.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                onClick = { dateTarget = DateTarget.Deadline },
                                isError = deadline.isBefore(mainDate),
                            )
                        }
                    }

                    if (scheduleKind == ScheduleKind.Recurring) {
                        FieldLabel("Repeats")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RepeatPreset.entries.forEach { preset ->
                                WhipFilterChip(
                                    selected = repeatPreset == preset,
                                    onClick = {
                                        repeatPreset = preset
                                        if (preset == RepeatPreset.Weekdays && weekdays.isEmpty()) {
                                            weekdays = setOf(mainDate.dayOfWeek)
                                        }
                                        if (preset == RepeatPreset.Weekdays) recurrenceAnchor = RecurrenceAnchor.Schedule
                                    },
                                    label = { Text(preset.label) },
                                )
                            }
                        }

                        if (repeatPreset !in setOf(RepeatPreset.Daily, RepeatPreset.Weekdays)) {
                            OutlinedTextField(
                                value = intervalText,
                                onValueChange = { intervalText = it.filter(Char::isDigit).take(3) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Every how many ${repeatPreset.unitLabel}?") },
                                singleLine = true,
                                isError = (interval ?: 0) <= 0,
                            )
                        }

                        if (repeatPreset == RepeatPreset.Weekdays) {
                            FieldLabel("On these days")
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                orderedWeekdays(firstDayOfWeek).forEach { day ->
                                    WhipFilterChip(
                                        selected = day in weekdays,
                                        onClick = {
                                            weekdays = if (day in weekdays) weekdays - day else weekdays + day
                                        },
                                        label = { Text(day.shortLabel) },
                                    )
                                }
                            }
                        }

                        RecurrenceAnchorSelector(
                            selected = recurrenceAnchor,
                            usesSelectedWeekdays = repeatPreset == RepeatPreset.Weekdays,
                            onSelect = { recurrenceAnchor = it },
                        )
                        if (recurrenceAnchor == RecurrenceAnchor.Schedule) {
                            FieldLabel("If occurrences are missed")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MissedOccurrencePolicy.entries.forEach { policy ->
                                    WhipFilterChip(
                                        selected = missedOccurrencePolicy == policy,
                                        onClick = { missedOccurrencePolicy = policy },
                                        label = {
                                            Text(
                                                when (policy) {
                                                    MissedOccurrencePolicy.KeepOldest -> "Keep oldest overdue"
                                                    MissedOccurrencePolicy.KeepLatest -> "Show latest overdue"
                                                    MissedOccurrencePolicy.AutoSkip -> "Auto-skip past occurrences"
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                            Text(
                                when (missedOccurrencePolicy) {
                                    MissedOccurrencePolicy.KeepOldest -> "Work through the backlog in calendar order."
                                    MissedOccurrencePolicy.KeepLatest -> "Show one current overdue occurrence without filling the screen."
                                    MissedOccurrencePolicy.AutoSkip -> "Mark elapsed occurrences skipped and keep only today's cadence."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FieldLabel("Ends")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RecurrenceEnd.entries.forEach { end ->
                                WhipFilterChip(
                                    selected = recurrenceEnd == end,
                                    onClick = { recurrenceEnd = end },
                                    label = {
                                        Text(
                                            when (end) {
                                                RecurrenceEnd.Never -> "Never"
                                                RecurrenceEnd.OnDate -> "On date"
                                                RecurrenceEnd.AfterCount -> "After count"
                                            },
                                        )
                                    },
                                )
                            }
                        }
                        if (recurrenceEnd == RecurrenceEnd.OnDate) {
                            ValueButton(
                                label = "End date",
                                value = endDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                onClick = { dateTarget = DateTarget.End },
                                isError = endDate.isBefore(mainDate),
                            )
                        }
                        if (recurrenceEnd == RecurrenceEnd.AfterCount) {
                            OutlinedTextField(
                                value = occurrenceCountText,
                                onValueChange = {
                                    occurrenceCountText = it.filter(Char::isDigit).take(4)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Number of occurrences") },
                                singleLine = true,
                                isError = (count ?: 0) <= 0,
                            )
                        }
                        if (stepDrafts.isNotEmpty()) {
                            FieldLabel("When the task repeats")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RepeatStepPolicy.entries.forEach { policy ->
                                    WhipFilterChip(
                                        selected = repeatStepPolicy == policy,
                                        onClick = { repeatStepPolicy = policy },
                                        label = { Text(policy.uiLabel()) },
                                    )
                                }
                            }
                        }
                    }

                    FieldLabel("Priority")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TaskPriority.entries.forEach { value ->
                            WhipFilterChip(
                                selected = priority == value,
                                onClick = { priority = value },
                                label = { Text(value.name) },
                            )
                        }
                    }
                    DisclosureButton(
                        label = "Optional details",
                        expanded = showAdvanced,
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (showAdvanced) {
                        FieldLabel("Planning estimate")
                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Duration in minutes (optional)") },
                            supportingText = { Text("Used by Plan My Day; unknown tasks use 30 minutes.") },
                            singleLine = true,
                        )
                        FieldLabel("Effort")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TaskEffort.entries.forEach { value ->
                                WhipFilterChip(
                                    selected = effort == value,
                                    onClick = { effort = value },
                                    modifier = Modifier.testTag("task-effort-${value.name}"),
                                    label = { Text(value.label) },
                                )
                            }
                        }
                        Text(
                            "Effort helps planning when time is unknown: Light is low energy, Medium needs ordinary attention, and High is sustained physical or mental work.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (showAdvanced) {
                    FieldLabel("Subtasks")
                    stepDrafts.forEachIndexed { index, step ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                            OutlinedTextField(
                                value = step.title,
                                onValueChange = { changed ->
                                    stepDrafts = stepDrafts.mapIndexed { stepIndex, existing ->
                                        if (stepIndex == index) existing.copy(title = changed) else existing
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Subtask ${index + 1}") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = step.notes,
                                onValueChange = { changed ->
                                    stepDrafts = stepDrafts.mapIndexed { stepIndex, existing ->
                                        if (stepIndex == index) existing.copy(notes = changed) else existing
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notes (optional)") },
                                minLines = 2,
                                maxLines = 3,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = {
                                        stepDrafts = stepDrafts.toMutableList().apply {
                                            add(index - 1, removeAt(index))
                                        }.mapIndexed { position, item -> item.copy(position = position) }
                                    },
                                ) {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move subtask up", modifier = Modifier.size(26.dp))
                                }
                                IconButton(
                                    enabled = index < stepDrafts.lastIndex,
                                    onClick = {
                                        stepDrafts = stepDrafts.toMutableList().apply {
                                            add(index + 1, removeAt(index))
                                        }.mapIndexed { position, item -> item.copy(position = position) }
                                    },
                                ) {
                                    Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move subtask down", modifier = Modifier.size(26.dp))
                                }
                                IconButton(
                                    onClick = {
                                        stepDrafts = stepDrafts
                                            .filterIndexed { stepIndex, _ -> stepIndex != index }
                                            .mapIndexed { position, item -> item.copy(position = position) }
                                    },
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove subtask", modifier = Modifier.size(26.dp))
                                }
                            }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newStepTitle,
                            onValueChange = { newStepTitle = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("New subtask") },
                            singleLine = true,
                        )
                        IconButton(
                            enabled = newStepTitle.isNotBlank(),
                            onClick = {
                                stepDrafts = stepDrafts + TaskStepDraft(
                                    title = newStepTitle.trim(),
                                    position = stepDrafts.size,
                                )
                                newStepTitle = ""
                            },
                            modifier = Modifier.size(52.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add subtask", modifier = Modifier.size(28.dp))
                        }
                    }

                    if (stepDrafts.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("Show progress on task card")
                                Text(
                                    "Display completion based on these subtasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = showSubtaskProgress,
                                onCheckedChange = { showSubtaskProgress = it },
                            )
                        }
                        if (showSubtaskProgress) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TaskProgressDisplay.entries.forEach { display ->
                                    WhipFilterChip(
                                        selected = progressDisplay == display,
                                        onClick = { progressDisplay = display },
                                        label = { Text(display.label) },
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("Complete task with final subtask")
                                Text(
                                    "Automatically finish the parent task at 100%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = autoCompleteFromSteps,
                                onCheckedChange = { autoCompleteFromSteps = it },
                            )
                        }
                    }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            FieldLabel("Set a time")
                            if (hasTime) {
                                Text(
                                    LocalTime.of(timeMinutes / 60, timeMinutes % 60)
                                        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = hasTime,
                            onCheckedChange = {
                                hasTime = it
                                if (!it) reminderEnabled = false
                            },
                        )
                    }
                    if (hasTime) {
                        WhipTextButton(onClick = { showTimePicker = true }) {
                            Text("Change Time")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldLabel("Notification")
                                Text(
                                    "Remind me at the task time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = {
                                    reminderEnabled = it
                                    if (it) {
                                        if (reminderOffsets.isEmpty()) reminderOffsets = setOf(0)
                                        onRequestNotificationPermission()
                                    } else {
                                        reminderOffsets = emptySet()
                                    }
                                },
                            )
                        }
                        if (reminderEnabled) {
                            FieldLabel("Reminder times")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                reminderOffsetOptions.forEach { (minutes, label) ->
                                    WhipFilterChip(
                                        selected = minutes in reminderOffsets,
                                        onClick = {
                                            reminderOffsets = if (minutes in reminderOffsets) {
                                                reminderOffsets - minutes
                                            } else {
                                                reminderOffsets + minutes
                                            }
                                        },
                                        label = { Text(label) },
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = customReminderText,
                                    onValueChange = { customReminderText = it.filter(Char::isDigit).take(5) },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Custom minutes before") },
                                    singleLine = true,
                                )
                                WhipTextButton(
                                    enabled = (customReminderText.toIntOrNull() ?: -1) in 0..43_200,
                                    onClick = {
                                        reminderOffsets = reminderOffsets + requireNotNull(customReminderText.toIntOrNull())
                                        customReminderText = ""
                                    },
                                ) { Text("Add") }
                            }
                            Text(
                                reminderOffsets.sortedDescending().joinToString(", ") { reminderOffsetLabel(it) }
                                    .ifBlank { "Choose at least one reminder." },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (reminderOffsets.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (showAdvanced) {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Notes (optional)") },
                            minLines = 2,
                            maxLines = 4,
                        )
                        OutlinedTextField(
                            value = tagsText,
                            onValueChange = { tagsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tags, comma-separated") },
                            singleLine = true,
                        )
                        if (knownTags.isNotEmpty()) {
                            val selectedTags = tagsText.split(',').map(String::trim).filter(String::isNotBlank)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                knownTags.forEach { value ->
                                    WhipFilterChip(
                                        selected = selectedTags.any { it.equals(value, true) },
                                        onClick = {
                                            val updated = if (selectedTags.any { it.equals(value, true) }) {
                                                selectedTags.filterNot { it.equals(value, true) }
                                            } else selectedTags + value
                                            tagsText = updated.joinToString(", ")
                                        },
                                        label = { Text("#$value") },
                                    )
                                }
                            }
                        }
                    }

                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    WhipTextButton(onClick = requestDismiss, enabled = !saving) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    if (request.task == null && onSaveAndNew != null) {
                        WhipOutlinedButton(
                            enabled = canSave && !saving,
                            onClick = { onSaveAndNew(request.task?.id, currentDraft, request.fromOccurrence) },
                        ) { Text("Save + New") }
                        Spacer(Modifier.width(8.dp))
                    }
                    WhipTextButton(
                        enabled = canSave && !saving,
                        onClick = { onSave(request.task?.id, currentDraft, request.fromOccurrence) },
                    ) { Text(if (saving) "Saving…" else "Save") }
                }
            }
            }
        }
    }

    dateTarget?.let { target ->
        WhipDatePickerDialog(
            initialDate = when (target) {
                DateTarget.Main -> mainDate
                DateTarget.End -> endDate
                DateTarget.Deadline -> deadline
            },
            onDismiss = { dateTarget = null },
            onDateSelected = { selected ->
                when (target) {
                    DateTarget.Main -> mainDate = selected
                    DateTarget.End -> endDate = selected
                    DateTarget.Deadline -> deadline = selected
                }
                dateTarget = null
            },
        )
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = timeMinutes / 60,
            initialMinute = timeMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Task Time") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                WhipTextButton(
                    onClick = {
                        timeMinutes = pickerState.hour * 60 + pickerState.minute
                        showTimePicker = false
                    },
                ) { Text("Set") }
            },
            dismissButton = {
                WhipTextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
    if (recipesOpen) {
        TaskRecipeDialog(
            today = today,
            onDismiss = { recipesOpen = false },
            onChoose = { draft ->
                title = draft.title
                notes = draft.notes
                scheduleKind = draft.scheduleKind
                mainDate = draft.recurrence?.startDate ?: draft.date ?: today
                repeatPreset = draft.recurrence.toPreset()
                intervalText = (draft.recurrence?.interval ?: 2).toString()
                weekdays = draft.recurrence?.weekdays?.takeIf(Set<DayOfWeek>::isNotEmpty)
                    ?: setOf(mainDate.dayOfWeek)
                recurrenceEnd = draft.recurrence?.end ?: RecurrenceEnd.Never
                endDate = draft.recurrence?.endDate ?: mainDate.plusMonths(1)
                occurrenceCountText = (draft.recurrence?.occurrenceCount ?: 10).toString()
                recurrenceAnchor = draft.recurrence?.anchor ?: RecurrenceAnchor.Schedule
                hasTime = draft.timeMinutes != null
                timeMinutes = draft.timeMinutes ?: 9 * 60
                reminderEnabled = draft.reminderEnabled
                reminderOffsets = draft.reminderOffsetsMinutes.toSet()
                priority = draft.priority
                inbox = draft.inbox
                durationMinutes = draft.durationMinutes?.toString().orEmpty()
                effort = draft.effort
                if (draft.areaId != null || draft.area.isNotBlank()) {
                    areaId = draft.areaId
                    area = draft.area
                }
                tagsText = draft.tags.joinToString(", ")
                hasDeadline = draft.deadline != null
                deadline = draft.deadline ?: mainDate.plusDays(7)
                stepDrafts = draft.steps.sortedBy(TaskStepDraft::position)
                showSubtaskProgress = draft.showSubtaskProgress
                progressDisplay = draft.progressDisplay
                autoCompleteFromSteps = draft.autoCompleteFromSteps
                repeatStepPolicy = draft.repeatStepPolicy
                missedOccurrencePolicy = draft.missedOccurrencePolicy
                showAdvanced = draft.notes.isNotBlank() || draft.steps.isNotEmpty() ||
                    draft.durationMinutes != null || draft.priority != TaskPriority.None
                recipesOpen = false
            },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("Your edits to this task have not been saved.") },
            confirmButton = {
                WhipTextButton(onClick = onDismiss) { Text("Discard Changes") }
            },
            dismissButton = {
                WhipTextButton(onClick = { confirmDiscard = false }) { Text("Keep Editing") }
            },
        )
    }
}

internal fun completionAnchorAvailability(usesSelectedWeekdays: Boolean): ControlAvailability =
    ControlAvailability(
        enabled = !usesSelectedWeekdays,
        unavailableExplanation = if (usesSelectedWeekdays) {
            "Selected-day repeats stay tied to the calendar. Under Repeats, choose Daily or an Every X option to use completion-based timing."
        } else {
            null
        },
    )

@Composable
internal fun RecurrenceAnchorSelector(
    selected: RecurrenceAnchor,
    usesSelectedWeekdays: Boolean,
    onSelect: (RecurrenceAnchor) -> Unit,
) {
    val completionAvailability = completionAnchorAvailability(usesSelectedWeekdays)
    FieldLabel("Next repeat is based on")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RecurrenceAnchor.entries.forEach { anchor ->
            val availability = if (anchor == RecurrenceAnchor.Schedule) {
                ControlAvailability(enabled = true)
            } else {
                completionAvailability
            }
            WhipFilterChip(
                selected = selected == anchor,
                enabled = availability.enabled,
                onClick = { onSelect(anchor) },
                label = { Text(if (anchor == RecurrenceAnchor.Schedule) "Scheduled Date" else "Completion Date") },
            )
        }
    }
    AvailabilityNotice("Completion date", completionAvailability)
    Text(
        if (selected == RecurrenceAnchor.Schedule) {
            "Scheduled date keeps the calendar cadence fixed when an occurrence is completed late."
        } else {
            "Completion date creates only the next occurrence, measured from when you complete this one."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TaskRecipeDialog(
    today: LocalDate,
    onDismiss: () -> Unit,
    onChoose: (TaskDraft) -> Unit,
) {
    val recipes = listOf(
        "Capture something for later" to TaskDraft(title = "New task", inbox = true),
        "Do something on a date" to TaskDraft(
            title = "Dated task", scheduleKind = ScheduleKind.Once, date = today,
            inbox = false, durationMinutes = 30,
        ),
        "Repeat on chosen weekdays" to TaskDraft(
            title = "Weekly task",
            scheduleKind = ScheduleKind.Recurring,
            recurrence = RecurrenceRule(
                unit = RecurrenceUnit.Weeks,
                weekdays = setOf(today.dayOfWeek),
                startDate = today,
            ),
            inbox = false,
        ),
        "Break a project into steps" to TaskDraft(
            title = "Project",
            steps = listOf(
                TaskStepDraft(title = "Plan", position = 0),
                TaskStepDraft(title = "Do the work", position = 1),
                TaskStepDraft(title = "Review", position = 2),
            ),
            showSubtaskProgress = true,
            durationMinutes = 90,
            effort = TaskEffort.Deep,
            inbox = true,
        ),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Task Recipes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose the closest shape. Whip fills the editor so you can review and change everything before saving.")
                recipes.forEach { (label, draft) ->
                    WhipOutlinedButton(onClick = { onChoose(draft) }, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(label.uiTitleCase(), fontWeight = FontWeight.SemiBold)
                            Text(
                                when (label) {
                                    "Capture something for later" -> "An unscheduled Inbox task."
                                    "Do something on a date" -> "A one-time task dated today with a 30-minute estimate."
                                    "Repeat on chosen weekdays" -> "A weekly series starting on today’s weekday."
                                    else -> "A three-step project with progress and a deep-work estimate."
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhipDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            WhipTextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate(),
                        )
                    }
                },
            ) { Text("Set") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ValueButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    isError: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            FieldLabel(label)
            Text(
                value,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        WhipTextButton(onClick = onClick) { Text("Change") }
    }
}

private fun RecurrenceRule?.toPreset(): RepeatPreset = when {
    this == null -> RepeatPreset.Daily
    unit == RecurrenceUnit.Days && interval == 1 -> RepeatPreset.Daily
    unit == RecurrenceUnit.Days -> RepeatPreset.EveryDays
    unit == RecurrenceUnit.Weeks && weekdays.isNotEmpty() -> RepeatPreset.Weekdays
    unit == RecurrenceUnit.Weeks -> RepeatPreset.EveryWeeks
    unit == RecurrenceUnit.Months -> RepeatPreset.EveryMonths
    else -> RepeatPreset.EveryYears
}

private val RepeatPreset.label: String
    get() = when (this) {
        RepeatPreset.Daily -> "Daily"
        RepeatPreset.EveryDays -> "Every X Days"
        RepeatPreset.Weekdays -> "Selected Days"
        RepeatPreset.EveryWeeks -> "Every X Weeks"
        RepeatPreset.EveryMonths -> "Every X Months"
        RepeatPreset.EveryYears -> "Every X Years"
    }

private val RepeatPreset.unitLabel: String
    get() = when (this) {
        RepeatPreset.EveryWeeks -> "weeks"
        RepeatPreset.EveryMonths -> "months"
        RepeatPreset.EveryYears -> "years"
        else -> "days"
    }

private val reminderOffsetOptions = listOf(
    0 to "At time",
    10 to "10 min before",
    30 to "30 min before",
    60 to "1 hour before",
    1_440 to "1 day before",
)

private fun reminderOffsetLabel(minutes: Int): String = when {
    minutes == 0 -> "At time"
    minutes % 1_440 == 0 -> "${minutes / 1_440}d before"
    minutes % 60 == 0 -> "${minutes / 60}h before"
    else -> "${minutes}m before"
}

private fun decimalInput(value: String): String = value.filterIndexed { index, char ->
    char.isDigit() || char == '.' || char == ',' || (char == '-' && index == 0)
}.take(12)

private val DayOfWeek.shortLabel: String
    get() = name.take(2).lowercase().replaceFirstChar(Char::uppercase)

private fun orderedWeekdays(first: DayOfWeek): List<DayOfWeek> =
    (0..6).map { offset -> DayOfWeek.of((first.value - 1 + offset) % 7 + 1) }

private val TaskProgressDisplay.label: String
    get() = when (this) {
        TaskProgressDisplay.Percent -> "Percentage"
        TaskProgressDisplay.Fraction -> "Fraction"
        TaskProgressDisplay.Both -> "Both"
    }
