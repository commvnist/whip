package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
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
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.DEFAULT_TASK_EMOJI
import com.whip.app.domain.toDraft
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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

internal val LocalWhipFirstDayOfWeek = staticCompositionLocalOf { DayOfWeek.MONDAY }
internal val LocalWhipToday = staticCompositionLocalOf { LocalDate.now() }
internal val LocalWhipZone = staticCompositionLocalOf { ZoneId.systemDefault() }

data class TaskEditorRequest(
    val task: WhipTask? = null,
    val fromOccurrence: LocalDate? = null,
    val initialCapture: String = "",
    val initialScheduleDate: LocalDate? = null,
    val initialPlacement: TaskPlacement? = null,
    val sessionId: Long = 0L,
)

enum class TaskPlacement(val label: String) {
    Inbox("Inbox"),
    Scheduled("Scheduled"),
}

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
    naturalLanguageCapture: Boolean = true,
    powerMode: Boolean = false,
    areas: List<Area> = emptyList(),
    defaultAreaId: String? = null,
    inheritedAreaFromScope: Boolean = false,
    onCreateArea: (String, Long?, (Result<String>) -> Unit) -> Unit = { _, _, _ -> },
    knownTags: List<String> = emptyList(),
    customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveIdentityEmoji: (CustomIdentityEmoji) -> Unit = {},
    onRemoveSavedIdentityEmoji: (String) -> Unit = {},
    paneOffsetX: Dp = 0.dp,
    paneMaxWidth: Dp = 720.dp,
    saving: Boolean = false,
    persistenceError: String? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }
    val captureLines = request.initialCapture.lines().map(String::trim).filter(String::isNotBlank)
    val requestedPlacement = request.initialPlacement
        ?: TaskPlacement.Scheduled.takeIf { request.initialScheduleDate != null }
        ?: TaskPlacement.Inbox
    val initial = request.task?.toDraft() ?: TaskDraft(
        title = captureLines.firstOrNull().orEmpty(),
        steps = captureLines.drop(1).mapIndexed { index, line -> TaskStepDraft(title = line, position = index) },
        showSubtaskProgress = captureLines.size > 1,
        repeatStepPolicy = defaultRepeatStepPolicy,
        scheduleKind = when (requestedPlacement) {
            TaskPlacement.Inbox -> ScheduleKind.Anytime
            TaskPlacement.Scheduled -> ScheduleKind.Once
        },
        date = request.initialScheduleDate,
        recurrence = null,
        inbox = requestedPlacement == TaskPlacement.Inbox,
        areaId = defaultAreaId,
        area = areas.firstOrNull { it.id == defaultAreaId }?.name.orEmpty(),
    )
    val initialRule = initial.recurrence
    val editStartDate = request.fromOccurrence ?: initialRule?.startDate
    val editorKey = "${request.task?.id ?: "new"}:${request.fromOccurrence?.toEpochDay() ?: "base"}:${request.sessionId}"

    var title by rememberSaveable(editorKey) { mutableStateOf(initial.title) }
    var icon by rememberSaveable(editorKey) { mutableStateOf(initial.icon) }
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
    val smartCapturePreview = remember(title, today, naturalLanguageCapture, request.task) {
        if (naturalLanguageCapture && request.task == null) {
            TaskQuickCaptureParser.parse(title, today)
        } else {
            null
        }
    }
    val smartCaptureAssumptions = smartCapturePreview?.assumptions.orEmpty()
    val smartCaptureStateDescription = smartCaptureAssumptions.smartCaptureStateDescription(
        "These highlighted phrases are a preview and are only applied when Apply Highlighted Details is selected",
    )
    val stepDraftSaver = listSaver<List<TaskStepDraft>, Any>(
        save = { drafts -> drafts.flatMap { listOf(it.id ?: Long.MIN_VALUE, it.title, it.position, it.notes, it.uiKey) } },
        restore = { saved -> saved.chunked(5).map { values ->
            TaskStepDraft(
                id = (values[0] as Long).takeUnless { it == Long.MIN_VALUE },
                title = values[1] as String,
                position = values[2] as Int,
                notes = values[3] as String,
                uiKey = values[4] as String,
            )
        } },
    )
    val initialStepDrafts = remember(editorKey, initial.steps) {
        initial.steps.sortedBy(TaskStepDraft::position).map { draft ->
            if (draft.uiKey.isNotBlank()) draft else draft.copy(
                uiKey = draft.id?.let { "saved-task-step-$it" }
                    ?: java.util.UUID.randomUUID().toString(),
            )
        }
    }
    var stepDrafts by rememberSaveable(editorKey, stateSaver = stepDraftSaver) {
        mutableStateOf(initialStepDrafts)
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
                initial.durationMinutes != null || initial.effort != TaskEffort.Unspecified,
        )
    }
    var recipesOpen by rememberSaveable(editorKey) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(editorKey) { mutableStateOf(false) }
    var pendingUndatedPlacement by rememberSaveable(editorKey) { mutableStateOf<TaskPlacement?>(null) }
    var pendingRepeatEnable by rememberSaveable(editorKey) { mutableStateOf(false) }
    var validationRequested by rememberSaveable(editorKey) { mutableStateOf(false) }

    LaunchedEffect(editorKey) {
        if (request.task == null) {
            titleFocusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val initialOffsets = initial.reminderOffsetsMinutes.toSet().ifEmpty {
        setOf(0).takeIf { initial.reminderEnabled }.orEmpty()
    }
    val isDirty = request.initialCapture.isNotBlank() ||
        title != initial.title || icon != initial.icon || notes != initial.notes ||
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
        priority != initial.priority ||
        durationMinutes != initial.durationMinutes?.toString().orEmpty() || effort != initial.effort ||
        areaId != initial.areaId || area != initial.area || tagsText != initial.tags.joinToString(", ") ||
        hasDeadline != (initial.deadline != null) ||
        (hasDeadline && deadline != (initial.deadline ?: (editStartDate ?: initial.date ?: today).plusDays(7))) ||
        stepDrafts.map { it.copy(uiKey = "") } != initial.steps.sortedBy(TaskStepDraft::position).map { it.copy(uiKey = "") } ||
        showSubtaskProgress != initial.showSubtaskProgress ||
        progressDisplay != initial.progressDisplay ||
        autoCompleteFromSteps != initial.autoCompleteFromSteps ||
        repeatStepPolicy != initial.repeatStepPolicy ||
        missedOccurrencePolicy != initial.missedOccurrencePolicy
    val requestDismiss = { if (isDirty) confirmDiscard = true else onDismiss() }
    BackHandler(enabled = !confirmDiscard && !saving, onBack = requestDismiss)

    val interval = intervalText.toIntOrNull()
    val count = occurrenceCountText.toIntOrNull()
    val recurrenceValid = scheduleKind != ScheduleKind.Recurring || (
        (repeatPreset in setOf(RepeatPreset.Daily, RepeatPreset.Weekdays) || (interval ?: 0) > 0) &&
            (repeatPreset != RepeatPreset.Weekdays || weekdays.isNotEmpty()) &&
            (recurrenceEnd != RecurrenceEnd.OnDate || !endDate.isBefore(mainDate)) &&
            (recurrenceEnd != RecurrenceEnd.AfterCount || (count ?: 0) > 0)
        )
    val deadlineValid = !hasDeadline || scheduleKind == ScheduleKind.Anytime || !deadline.isBefore(mainDate)
    val areaSelectionValid = request.task != null || areas.count { !it.archived } <= 1 || areaId != null
    val canSave = title.isNotBlank() && recurrenceValid && deadlineValid && areaSelectionValid &&
        (!reminderEnabled || reminderOffsets.isNotEmpty())
    val saveProblem = when {
        title.isBlank() -> "Enter a Task title to save."
        !recurrenceValid -> "Finish the Repeat settings to save."
        !deadlineValid -> "Deadline cannot be before the Task's scheduled start."
        reminderEnabled && reminderOffsets.isEmpty() -> "Choose at least one reminder time."
        !areaSelectionValid -> "Choose an Area for this Task."
        else -> null
    }
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
        title = title.trim(), icon = icon, notes = notes.trim(), scheduleKind = scheduleKind,
        date = mainDate.takeIf { scheduleKind == ScheduleKind.Once }, recurrence = recurrence,
        timeMinutes = timeMinutes.takeIf { hasTime && scheduleKind != ScheduleKind.Anytime },
        reminderEnabled = hasTime && reminderEnabled && scheduleKind != ScheduleKind.Anytime,
        steps = stepDrafts.filter { it.title.isNotBlank() }.mapIndexed { position, step ->
            step.copy(title = step.title.trim(), position = position)
        },
        showSubtaskProgress = showSubtaskProgress && stepDrafts.isNotEmpty(),
        progressDisplay = progressDisplay, autoCompleteFromSteps = autoCompleteFromSteps,
        repeatStepPolicy = repeatStepPolicy, priority = priority, areaId = areaId, area = area.trim(),
        tags = tagsText.split(',').map(String::trim).filter(String::isNotBlank).toSet(),
        deadline = deadline.takeIf { scheduleKind != ScheduleKind.Anytime && hasDeadline },
        reminderOffsetsMinutes = reminderOffsets.toList().takeIf { scheduleKind != ScheduleKind.Anytime }.orEmpty(),
        missedOccurrencePolicy = missedOccurrencePolicy,
        inbox = scheduleKind == ScheduleKind.Anytime,
        durationMinutes = durationMinutes.toIntOrNull()?.coerceIn(1, 1_440), effort = effort,
    )
    val nestedDialogModifier = Modifier
        .absoluteOffset(x = paneOffsetX)
        .width(minOf(paneMaxWidth * 0.9f, 560.dp))
    val placement = when {
        scheduleKind != ScheduleKind.Anytime -> TaskPlacement.Scheduled
        else -> TaskPlacement.Inbox
    }
    fun applyPlacement(next: TaskPlacement) {
        scheduleKind = when (next) {
            TaskPlacement.Inbox -> ScheduleKind.Anytime
            TaskPlacement.Scheduled -> ScheduleKind.Once
        }
        if (next == TaskPlacement.Inbox) {
            hasTime = false
            reminderEnabled = false
            reminderOffsets = emptySet()
            hasDeadline = false
        }
    }
    fun setRepeatEnabled(enabled: Boolean) {
        scheduleKind = if (enabled) ScheduleKind.Recurring else ScheduleKind.Once
    }
    val editorScrollState = rememberScrollState()
    LaunchedEffect(persistenceError) {
        if (!persistenceError.isNullOrBlank()) editorScrollState.scrollTo(0)
    }

    Dialog(
        onDismissRequest = { if (!saving) requestDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val editorWidth = minOf(maxWidth, paneMaxWidth)
            Surface(
                modifier = Modifier
                    .absoluteOffset(x = paneOffsetX)
                    .width(editorWidth)
                    .fillMaxHeight()
                    .testTag("task-editor-surface"),
                shape = RectangleShape,
                tonalElevation = 0.dp,
            ) {
            Box {
            Column(
                modifier = if (saving) Modifier.clearAndSetSemantics {} else Modifier,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = requestDismiss, enabled = !saving) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancel Task editing")
                    }
                    Text(
                        text = when {
                            request.task == null -> "Create Task"
                            request.fromOccurrence != null -> "Edit This and Future"
                            else -> "Edit Task"
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (request.task == null && onSaveAndNew != null && powerMode) {
                        WhipTextButton(
                            enabled = !saving,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            onClick = {
                                if (canSave) {
                                    onSaveAndNew(request.task?.id, currentDraft, request.fromOccurrence)
                                } else {
                                    validationRequested = true
                                }
                            },
                        ) {
                            Text(
                                text = "Save & New",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    WhipButton(
                        enabled = !saving,
                        onClick = {
                            if (canSave) {
                                onSave(request.task?.id, currentDraft, request.fromOccurrence)
                            } else {
                                validationRequested = true
                            }
                        },
                    ) {
                        Text(
                            when {
                                saving -> "Saving…"
                                request.fromOccurrence != null -> "Save Future"
                                else -> "Save"
                            },
                        )
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .verticalScroll(editorScrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("* Required field", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PersistenceFailureNotice(
                        message = persistenceError,
                        testTag = "task-persistence-save-problem",
                    )
                    saveProblem?.takeIf { validationRequested }?.let { problem ->
                        FormValidationSummary(
                            messages = listOf(problem),
                            visible = true,
                            testTag = "task-save-problem",
                        )
                    }
                    if (request.fromOccurrence != null) {
                        DependentSettingsNotice(
                            message = "Changes apply from ${request.fromOccurrence.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} forward. Earlier occurrences and history stay unchanged.",
                            testTag = "task-edit-scope",
                        )
                    }
                    EditorSectionHeader("Basics", "Name this Task and choose the emoji used across Whip.")
                    OutlinedTextField(
                        value = title,
                        onValueChange = { entered ->
                            val lines = entered.lines()
                            // Smart Capture accepts a compact command, not just a final title. Leave
                            // enough room for recurrence, date, priority, tag, and reminder phrases;
                            // applying the preview reduces this back to the parsed Task title.
                            title = lines.firstOrNull().orEmpty().replace('\r', ' ').take(200)
                            smartCaptureSummary = null
                            val pastedSteps = lines.drop(1).map(String::trim).filter(String::isNotBlank)
                            if (pastedSteps.isNotEmpty()) {
                                stepDrafts = (stepDrafts + pastedSteps.mapIndexed { index, step ->
                                    TaskStepDraft(
                                        title = step,
                                        position = stepDrafts.size + index,
                                        uiKey = java.util.UUID.randomUUID().toString(),
                                    )
                                }).mapIndexed { index, step -> step.copy(position = index) }
                                showSubtaskProgress = true
                                showAdvanced = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester)
                            .semantics {
                                smartCaptureStateDescription?.let { stateDescription = it }
                            }
                            .testTag("task-editor-title"),
                        label = { Text("Task *") },
                        isError = validationRequested && title.isBlank(),
                        supportingText = {
                            Text(
                                if (validationRequested && title.isBlank()) "Task title is required"
                                else "${title.length}/200",
                            )
                        },
                        visualTransformation = SmartTaskCaptureVisualTransformation(
                            assumptions = smartCaptureAssumptions,
                            highlightColor = MaterialTheme.colorScheme.primaryContainer,
                            highlightedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        singleLine = true,
                    )
                    SmartTaskCapturePreview(
                        assumptions = smartCaptureAssumptions,
                        actionText = "Highlighted phrases are only a preview here. Apply them to update this Task's details.",
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "smart-task-editor-preview",
                    )
                    if (smartCaptureAssumptions.isNotEmpty()) {
                        WhipOutlinedButton(
                            onClick = {
                                val parsed = requireNotNull(smartCapturePreview)
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
                                parsed.timeMinutes?.let {
                                    hasTime = true
                                    timeMinutes = it
                                }
                                if (parsed.reminderEnabled) {
                                    reminderEnabled = true
                                    reminderOffsets = parsed.reminderOffsetsMinutes.toSet()
                                }
                                parsed.priority?.let { priority = it }
                                parsed.durationMinutes?.let { durationMinutes = it.toString() }
                                parsed.effort?.let { effort = it }
                                if (parsed.tags.isNotEmpty()) {
                                    tagsText = (tagsText.split(',').map(String::trim).filter(String::isNotBlank) + parsed.tags)
                                        .distinctBy { it.lowercase() }
                                        .joinToString(", ")
                                }
                                if (
                                    parsed.timeMinutes != null || parsed.reminderEnabled || parsed.priority != null ||
                                    parsed.durationMinutes != null || parsed.effort != null || parsed.tags.isNotEmpty()
                                ) {
                                    showAdvanced = true
                                }
                                smartCaptureSummary = parsed.recognized.joinToString(
                                    prefix = "Applied · ",
                                    separator = " · ",
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("smart-task-capture-apply"),
                        ) { Text("Apply Highlighted Details") }
                    }
                    smartCaptureSummary?.let {
                        Text(
                            it,
                            modifier = Modifier.testTag("smart-task-capture-applied"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    WhipEmojiPicker(
                        value = icon,
                        defaultEmoji = DEFAULT_TASK_EMOJI,
                        onValueChange = { icon = it },
                        modifier = Modifier.fillMaxWidth(),
                        customEmojis = customIdentityEmojis,
                        onSaveEmoji = onSaveIdentityEmoji,
                        onRemoveSavedEmoji = onRemoveSavedIdentityEmoji,
                    )
                    if (request.task == null) {
                        WhipTextButton(onClick = { recipesOpen = true }) { Text("Use a Template") }
                    }
                    EditorSectionHeader("Schedule", "Choose when this Task belongs; related repeat, date, time, and reminder controls stay together.")
                    FieldLabel("When")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TaskPlacement.entries.forEach { value ->
                            WhipFilterChip(
                                selected = placement == value,
                                onClick = {
                                    if (
                                        value == TaskPlacement.Inbox &&
                                        placement == TaskPlacement.Scheduled
                                    ) {
                                        pendingUndatedPlacement = value
                                    } else {
                                        applyPlacement(value)
                                    }
                                },
                                label = { Text(value.label) },
                            )
                        }
                    }
                    DependentSettingsNotice(
                        message = when (scheduleKind) {
                            ScheduleKind.Anytime ->
                                "Inbox keeps this Task unscheduled until you decide when it belongs."
                            ScheduleKind.Once -> "Scheduled Date places this Task on one day. Time, reminders, and an optional Deadline appear below."
                            ScheduleKind.Recurring -> "This Task repeats from its start date. Configure the pattern directly below."
                        },
                        testTag = "task-schedule-consequence",
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            FieldLabel("Repeat")
                            Text(
                                if (scheduleKind == ScheduleKind.Recurring) "Repeating schedule is on."
                                else "Create future occurrences from a schedule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = scheduleKind == ScheduleKind.Recurring,
                            modifier = Modifier.testTag("task-repeat-toggle"),
                            onCheckedChange = { enabled ->
                                if (enabled && scheduleKind == ScheduleKind.Anytime) pendingRepeatEnable = true
                                else setRepeatEnabled(enabled)
                            },
                        )
                    }

                    if (scheduleKind != ScheduleKind.Anytime) {
                        ValueButton(
                            label = if (scheduleKind == ScheduleKind.Once) "Scheduled Date" else "Starts",
                            value = mainDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            onClick = { dateTarget = DateTarget.Main },
                        )
                    }

                    if (scheduleKind == ScheduleKind.Anytime) {
                        Text(
                            "Schedule this Task to add a time, reminder, or Deadline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (scheduleKind != ScheduleKind.Anytime) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Separate Deadline")
                                Text(
                                    if (scheduleKind == ScheduleKind.Once) {
                                        "Plan work on one date and keep the final Deadline visible."
                                    } else {
                                        "Keep one final Deadline visible across this repeating series."
                                    },
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
                            FieldLabel("On These Days")
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
                                                    MissedOccurrencePolicy.KeepOldest -> "Show oldest missed"
                                                    MissedOccurrencePolicy.KeepLatest -> "Show latest missed"
                                                    MissedOccurrencePolicy.CurrentOnly -> "Show only today's occurrence"
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                            Text(
                                when (missedOccurrencePolicy) {
                                    MissedOccurrencePolicy.KeepOldest -> "Work through the backlog in calendar order."
                                    MissedOccurrencePolicy.KeepLatest -> "Show one current missed occurrence without filling the screen."
                                    MissedOccurrencePolicy.CurrentOnly -> "Past cadence stays out of the queue without creating skipped history."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FieldLabel("Repeat Ends")
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
                                label = "End Date",
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
                                label = { Text("Number of Occurrences") },
                                singleLine = true,
                                isError = (count ?: 0) <= 0,
                            )
                        }
                        if (stepDrafts.isNotEmpty()) {
                            FieldLabel("For Repeating Tasks")
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

                    if (scheduleKind != ScheduleKind.Anytime) {
                        TaskTimeSettings(
                            hasTime = hasTime,
                            timeMinutes = timeMinutes,
                            reminderEnabled = reminderEnabled,
                            reminderOffsets = reminderOffsets,
                            customReminderText = customReminderText,
                            onHasTimeChange = {
                                hasTime = it
                                if (!it) {
                                    reminderEnabled = false
                                    reminderOffsets = emptySet()
                                }
                            },
                            onChangeTime = { showTimePicker = true },
                            onReminderEnabledChange = {
                                reminderEnabled = it
                                if (it) {
                                    if (reminderOffsets.isEmpty()) reminderOffsets = setOf(0)
                                    onRequestNotificationPermission()
                                } else {
                                    reminderOffsets = emptySet()
                                }
                            },
                            onReminderOffsetsChange = { reminderOffsets = it },
                            onCustomReminderTextChange = { customReminderText = it },
                        )
                    }

                    EditorSectionHeader("Organization", "Choose the Area that owns this Task.")
                    AreaPicker(
                        areas = areas,
                        selectedAreaId = areaId,
                        selectedAreaName = area,
                        onSelect = { id, name -> areaId = id; area = name },
                        onCreateArea = onCreateArea,
                        modifier = Modifier.fillMaxWidth(),
                        dialogModifier = Modifier.absoluteOffset(x = paneOffsetX).width(paneMaxWidth),
                        inheritedFromScope = request.task == null && inheritedAreaFromScope,
                    )
                    if (validationRequested && !areaSelectionValid) {
                        Text(
                            "Choose an Area before saving.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    EditorSectionHeader("Planning", "Set urgency now and reveal estimates or subtasks only when useful.")
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
                        label = "Planning Details",
                        expanded = showAdvanced,
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.fillMaxWidth().testTag("task-editor-more-details"),
                    )
                    if (showAdvanced) {
                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Duration in Minutes (Optional)") },
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
                            "Effort describes the energy this Task needs: Light, Medium, or High. It does not change Priority.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (showAdvanced) {
                    EditorSectionHeader("Subtasks", "Break the Task into steps and choose how progress appears.")
                    WhipReorderLayout(itemSpacing = 14.dp) {
                    stepDrafts.forEachIndexed { index, step ->
                        key(step.uiKey.ifBlank { "task-step-${step.id ?: index}" }) {
                        val reorderInteraction = rememberWhipReorderInteractionState()
                        Card(
                            modifier = Modifier.fillMaxWidth().whipReorderItem(
                                reorderInteraction,
                                layoutPosition = index + 1,
                                layoutScope = "task-editor-subtasks",
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WhipReorderHandle(
                                    label = step.title.ifBlank { "subtask ${index + 1}" },
                                    canMovePrevious = index > 0,
                                    canMoveNext = index < stepDrafts.lastIndex,
                                    position = index + 1,
                                    total = stepDrafts.size,
                                    interactionState = reorderInteraction,
                                    moveWholeItem = true,
                                    layoutScope = "task-editor-subtasks",
                                    onMove = { delta ->
                                        stepDrafts = moveListItem(stepDrafts, index, delta)
                                            .mapIndexed { position, item -> item.copy(position = position) }
                                    },
                                )
                                OutlinedTextField(
                                    value = step.title,
                                    onValueChange = { changed ->
                                        stepDrafts = stepDrafts.mapIndexed { stepIndex, existing ->
                                            if (stepIndex == index) existing.copy(title = changed) else existing
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Subtask ${index + 1}") },
                                    singleLine = true,
                                )
                            }
                            OutlinedTextField(
                                value = step.notes,
                                onValueChange = { changed ->
                                    stepDrafts = stepDrafts.mapIndexed { stepIndex, existing ->
                                        if (stepIndex == index) existing.copy(notes = changed) else existing
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notes (Optional)") },
                                minLines = 2,
                                maxLines = 3,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
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
                            label = { Text("New Subtask") },
                            singleLine = true,
                        )
                        IconButton(
                            enabled = newStepTitle.isNotBlank(),
                            onClick = {
                                stepDrafts = stepDrafts + TaskStepDraft(
                                    title = newStepTitle.trim(),
                                    position = stepDrafts.size,
                                    uiKey = java.util.UUID.randomUUID().toString(),
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
                                FieldLabel("Show Progress on Task Card")
                                Text(
                                    "Display completion based on these Subtasks.",
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
                                FieldLabel("Complete Task With Final Subtask")
                                Text(
                                    "Automatically finish the parent Task at 100%.",
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

                    if (showAdvanced) {
                        EditorSectionHeader("Notes & Tags", "Keep reference material and reusable labels with the Task.")
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            modifier = Modifier.fillMaxWidth().testTag("task-editor-notes"),
                            label = { Text("Notes (Optional)") },
                            minLines = 2,
                            maxLines = 4,
                        )
                        OutlinedTextField(
                            value = tagsText,
                            onValueChange = { tagsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tags, Comma-Separated") },
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
                }
                PersistenceSavingOverlay(active = saving, label = "Saving Task")
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
            modifier = nestedDialogModifier,
            firstDayOfWeek = firstDayOfWeek,
        )
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = timeMinutes / 60,
            initialMinute = timeMinutes % 60,
            is24Hour = false,
        )
        PaneAwareAlertDialog(
            modifier = nestedDialogModifier,
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
    pendingUndatedPlacement?.let { target ->
        PaneAwareAlertDialog(
            modifier = nestedDialogModifier,
            onDismissRequest = { pendingUndatedPlacement = null },
            title = { Text("Remove Scheduling Details?") },
            text = {
                Text(
                    buildList {
                        add(
                            if (scheduleKind == ScheduleKind.Recurring) {
                                "The Repeat schedule and Start Date will be removed."
                            } else {
                                "The Scheduled Date will be removed."
                            },
                        )
                        if (hasTime) add("The time will be removed.")
                        if (reminderEnabled) add("All reminders will be removed.")
                        if (hasDeadline) add("The Deadline will be removed.")
                    }.joinToString(" ") + " The Task will move to ${target.label}.",
                )
            },
            confirmButton = {
                WhipButton(onClick = {
                    applyPlacement(target)
                    pendingUndatedPlacement = null
                }) { Text("Move to ${target.label}") }
            },
            dismissButton = {
                WhipTextButton(onClick = { pendingUndatedPlacement = null }) { Text("Keep Scheduled") }
            },
        )
    }
    if (pendingRepeatEnable) {
        PaneAwareAlertDialog(
            modifier = nestedDialogModifier,
            onDismissRequest = { pendingRepeatEnable = false },
            title = { Text("Schedule This Repeating Task?") },
            text = { Text("Repeat needs a Start Date. This Task will move from ${placement.label} to Scheduled, using ${mainDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} as its Start Date.") },
            confirmButton = {
                WhipButton(onClick = { setRepeatEnabled(true); pendingRepeatEnable = false }) { Text("Schedule and Repeat") }
            },
            dismissButton = { WhipTextButton(onClick = { pendingRepeatEnable = false }) { Text("Cancel") } },
        )
    }
    if (recipesOpen) {
        TaskRecipeDialog(
            today = today,
            modifier = nestedDialogModifier,
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
        PaneAwareAlertDialog(
            modifier = nestedDialogModifier,
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
            "Specific-weekday repeats stay tied to the calendar. Under Repeats, choose Daily or a custom interval to use completion-based timing."
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
private fun TaskTimeSettings(
    hasTime: Boolean,
    timeMinutes: Int,
    reminderEnabled: Boolean,
    reminderOffsets: Set<Int>,
    customReminderText: String,
    onHasTimeChange: (Boolean) -> Unit,
    onChangeTime: () -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderOffsetsChange: (Set<Int>) -> Unit,
    onCustomReminderTextChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            FieldLabel("Time")
            Text(
                if (hasTime) {
                    LocalTime.of(timeMinutes / 60, timeMinutes % 60)
                        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                } else {
                    "No time"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = hasTime,
            onCheckedChange = onHasTimeChange,
            modifier = Modifier.testTag("task-time-toggle"),
        )
    }
    if (!hasTime) return
    WhipTextButton(onClick = onChangeTime) { Text("Change Time") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            FieldLabel("Reminder")
            Text(
                "Notify me relative to the Task time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = reminderEnabled,
            onCheckedChange = onReminderEnabledChange,
            modifier = Modifier.testTag("task-reminder-toggle"),
        )
    }
    if (!reminderEnabled) return
    FieldLabel("Reminder Times")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        reminderOffsetOptions.forEach { (minutes, label) ->
            WhipFilterChip(
                selected = minutes in reminderOffsets,
                onClick = {
                    onReminderOffsetsChange(
                        if (minutes in reminderOffsets) reminderOffsets - minutes else reminderOffsets + minutes,
                    )
                },
                label = { Text(label) },
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = customReminderText,
            onValueChange = { onCustomReminderTextChange(it.filter(Char::isDigit).take(5)) },
            modifier = Modifier.weight(1f),
            label = { Text("Custom Minutes Before") },
            singleLine = true,
        )
        WhipTextButton(
            enabled = (customReminderText.toIntOrNull() ?: -1) in 0..43_200,
            onClick = {
                onReminderOffsetsChange(reminderOffsets + requireNotNull(customReminderText.toIntOrNull()))
                onCustomReminderTextChange("")
            },
        ) { Text("Add") }
    }
    Text(
        reminderOffsets.sortedDescending().joinToString(", ") { reminderOffsetLabel(it) }
            .ifBlank { "Choose at least one reminder." },
        style = MaterialTheme.typography.bodySmall,
        color = if (reminderOffsets.isEmpty()) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
internal fun TaskRecipeDialog(
    today: LocalDate,
    modifier: Modifier,
    onDismiss: () -> Unit,
    onChoose: (TaskDraft) -> Unit,
) {
    val recipes = listOf(
        "Capture Something for Later" to TaskDraft(title = "New Task", inbox = true),
        "Do Something on a Date" to TaskDraft(
            title = "Dated Task", scheduleKind = ScheduleKind.Once, date = today,
            inbox = false, durationMinutes = 30,
        ),
        "Repeat on Chosen Weekdays" to TaskDraft(
            title = "Weekly Task",
            scheduleKind = ScheduleKind.Recurring,
            recurrence = RecurrenceRule(
                unit = RecurrenceUnit.Weeks,
                weekdays = setOf(today.dayOfWeek),
                startDate = today,
            ),
            inbox = false,
        ),
        "Break Complex Work into Subtasks" to TaskDraft(
            title = "Complex Task",
            steps = listOf(
                TaskStepDraft(title = "Plan", position = 0),
                TaskStepDraft(title = "Do the work", position = 1),
                TaskStepDraft(title = "Review", position = 2),
            ),
            showSubtaskProgress = true,
            durationMinutes = 90,
            effort = TaskEffort.High,
            inbox = true,
        ),
    )
    val useCompactTemplateRows = LocalDensity.current.fontScale >= 2f
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Task Templates") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .testTag("task-template-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text("Choose the closest shape. Whip fills the editor so you can review and change everything before saving.")
                }
                items(recipes, key = { it.first }) { (label, draft) ->
                    val description = when (label) {
                        "Capture Something for Later" -> "An unscheduled Inbox Task."
                        "Do Something on a Date" -> "A one-time Task scheduled today with a 30-minute estimate."
                        "Repeat on Chosen Weekdays" -> "A weekly series starting on today’s weekday."
                        else -> "A three-step Task with progress and a High Effort estimate."
                    }
                    WhipOutlinedButton(
                        onClick = { onChoose(draft) },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "${label.uiTitleCase()}. $description"
                        },
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                label.uiTitleCase(),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = if (useCompactTemplateRows) 2 else 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!useCompactTemplateRows) Text(description, style = MaterialTheme.typography.bodySmall)
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
    modifier: Modifier = Modifier,
    firstDayOfWeek: DayOfWeek? = null,
) {
    val resolvedFirstDayOfWeek = firstDayOfWeek ?: LocalWhipFirstDayOfWeek.current
    val today = LocalWhipToday.current
    var selectedEpochDay by rememberSaveable(initialDate) { mutableLongStateOf(initialDate.toEpochDay()) }
    var monthStartEpochDay by rememberSaveable(initialDate) {
        mutableLongStateOf(initialDate.withDayOfMonth(1).toEpochDay())
    }
    var choosingDateWithWheels by rememberSaveable(initialDate) { mutableStateOf(false) }
    var jumpYear by rememberSaveable(initialDate) { mutableIntStateOf(initialDate.year) }
    var jumpMonth by rememberSaveable(initialDate) { mutableIntStateOf(initialDate.monthValue) }
    var jumpDay by rememberSaveable(initialDate) { mutableIntStateOf(initialDate.dayOfMonth) }
    var wheelResetToken by rememberSaveable(initialDate) { mutableIntStateOf(0) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val monthStart = LocalDate.ofEpochDay(monthStartEpochDay).withDayOfMonth(1)
    val jumpDate = clampedDate(jumpYear, jumpMonth, jumpDay)
    val displayedDate = if (choosingDateWithWheels) jumpDate else selectedDate
    val leadingEmptyDays =
        (monthStart.dayOfWeek.value - resolvedFirstDayOfWeek.value + 7) % 7
    val calendarCells = buildList<LocalDate?> {
        repeat(leadingEmptyDays) { add(null) }
        repeat(monthStart.lengthOfMonth()) { dayOffset -> add(monthStart.plusDays(dayOffset.toLong())) }
        while (size % 7 != 0) add(null)
    }
    ProductivityEditorDialog(
        modifier = Modifier.widthIn(min = 280.dp, max = 560.dp).then(modifier),
        testTag = "date-picker-dialog",
        paneTitle = "Choose Date",
        onDismissRequest = onDismiss,
        title = { Text("Choose Date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    displayedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                    modifier = Modifier.testTag("date-picker-selected-date"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                HorizontalDivider()
                if (choosingDateWithWheels) {
                    Text(
                        "Swipe each column to choose a year, month, and day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    key(wheelResetToken) {
                        Row(
                            modifier = Modifier.fillMaxWidth().testTag("date-picker-wheel-selector"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            DateWheelColumn(
                                label = "Year",
                                values = 1..9999,
                                selectedValue = jumpYear,
                                valueLabel = Int::toString,
                                onValueSelected = { year ->
                                    jumpYear = year
                                    jumpDay = jumpDay.coerceAtMost(YearMonth.of(year, jumpMonth).lengthOfMonth())
                                },
                                modifier = Modifier.weight(1f),
                            )
                            DateWheelColumn(
                                label = "Month",
                                values = 1..12,
                                selectedValue = jumpMonth,
                                valueLabel = { month ->
                                    Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
                                },
                                onValueSelected = { month ->
                                    jumpMonth = month
                                    jumpDay = jumpDay.coerceAtMost(YearMonth.of(jumpYear, month).lengthOfMonth())
                                },
                                modifier = Modifier.weight(1.35f),
                            )
                            DateWheelColumn(
                                label = "Day",
                                values = 1..YearMonth.of(jumpYear, jumpMonth).lengthOfMonth(),
                                selectedValue = jumpDay,
                                valueLabel = Int::toString,
                                onValueSelected = { jumpDay = it },
                                modifier = Modifier.weight(0.85f),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WhipTextButton(
                            onClick = {
                                jumpYear = today.year
                                jumpMonth = today.monthValue
                                jumpDay = today.dayOfMonth
                                wheelResetToken += 1
                            },
                            modifier = Modifier.testTag("date-picker-today"),
                        ) { Text("Today") }
                        WhipOutlinedButton(
                            onClick = {
                                selectedEpochDay = jumpDate.toEpochDay()
                                monthStartEpochDay = jumpDate.withDayOfMonth(1).toEpochDay()
                                choosingDateWithWheels = false
                            },
                            modifier = Modifier.testTag("date-picker-show-calendar"),
                        ) { Text("Show Calendar") }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { monthStartEpochDay = monthStart.minusMonths(1).toEpochDay() },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous Month")
                        }
                        WhipTextButton(
                            onClick = {
                                jumpYear = selectedDate.year
                                jumpMonth = selectedDate.monthValue
                                jumpDay = selectedDate.dayOfMonth
                                choosingDateWithWheels = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("date-picker-month-year")
                                .semantics {
                                    contentDescription =
                                        "${monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}. Jump to Date"
                                },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Jump to Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            onClick = { monthStartEpochDay = monthStart.plusMonths(1).toEpochDay() },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        orderedWeekdays(resolvedFirstDayOfWeek).forEach { day ->
                            Text(
                                day.shortLabel,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    calendarCells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                if (date == null) {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val selected = date == selectedDate
                                    Surface(
                                        onClick = { selectedEpochDay = date.toEpochDay() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .semantics {
                                                contentDescription = "Select ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))}"
                                                stateDescription = if (selected) "Selected" else "Not Selected"
                                            },
                                        shape = MaterialTheme.shapes.small,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                date.dayOfMonth.toString(),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                fontWeight = FontWeight.SemiBold.takeIf { selected },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                onClick = { onDateSelected(if (choosingDateWithWheels) jumpDate else selectedDate) },
            ) { Text("Set") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun clampedDate(year: Int, month: Int, day: Int): LocalDate {
    val yearMonth = YearMonth.of(year, month)
    return yearMonth.atDay(day.coerceIn(1, yearMonth.lengthOfMonth()))
}

@Composable
private fun DateWheelColumn(
    label: String,
    values: IntRange,
    selectedValue: Int,
    valueLabel: (Int) -> String,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = (selectedValue - values.first).coerceIn(0, values.count() - 1)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex,
    )
    val coroutineScope = rememberCoroutineScope()
    val currentSelectedValue by rememberUpdatedState(selectedValue)
    val currentOnValueSelected by rememberUpdatedState(onValueSelected)

    LaunchedEffect(listState, values.first, values.last) {
        snapshotFlow { listState.centeredItemIndex()?.minus(1) }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { index ->
                val value = values.first + index
                if (value in values && value != currentSelectedValue) currentOnValueSelected(value)
            }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .testTag("date-picker-${label.lowercase(Locale.ROOT)}-wheel")
                .semantics {
                    contentDescription = "$label picker"
                    stateDescription = valueLabel(selectedValue)
                },
            flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center),
        ) {
            items(
                count = values.count() + 2,
                key = { index -> "$label-wheel-$index" },
            ) { index ->
                if (index == 0 || index == values.count() + 1) {
                    Spacer(Modifier.height(48.dp))
                    return@items
                }
                val value = values.first + index - 1
                val selected = value == selectedValue
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(value - values.first)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("date-picker-${label.lowercase(Locale.ROOT)}-$value")
                        .semantics {
                            contentDescription = "$label ${valueLabel(value)}"
                            stateDescription = if (selected) "Selected" else "Not selected"
                        },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            valueLabel(value),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = if (selected) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.SemiBold.takeIf { selected },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListState.centeredItemIndex(): Int? {
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return layoutInfo.visibleItemsInfo.minByOrNull { item ->
        abs(item.offset + item.size / 2 - viewportCenter)
    }?.index
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
        RepeatPreset.EveryDays -> "Custom Day Interval"
        RepeatPreset.Weekdays -> "Specific Weekdays"
        RepeatPreset.EveryWeeks -> "Custom Week Interval"
        RepeatPreset.EveryMonths -> "Custom Month Interval"
        RepeatPreset.EveryYears -> "Custom Year Interval"
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
