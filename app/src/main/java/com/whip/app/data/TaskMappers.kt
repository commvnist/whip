package com.whip.app.data

import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceEnd
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskProgressDisplay
import com.whip.app.domain.TaskStep
import com.whip.app.domain.TaskStepState
import com.whip.app.domain.TaskStepSnapshot
import com.whip.app.domain.WhipTask
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.LocationTrigger
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.TaskLocationReminder
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.MissedOccurrencePolicy
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.toWeekdayMask
import com.whip.app.domain.toWeekdays
import java.time.LocalDate
import java.util.UUID

fun TaskEntity.toDomain(): WhipTask {
    val kind = ScheduleKind.valueOf(scheduleKind)
    val startDate = dateEpochDay?.let(LocalDate::ofEpochDay)
    val recurrence = if (kind == ScheduleKind.Recurring) {
        RecurrenceRule(
            unit = RecurrenceUnit.valueOf(requireNotNull(recurrenceUnit)),
            interval = recurrenceInterval,
            weekdays = weekdaysMask.toWeekdays(),
            startDate = requireNotNull(startDate),
            end = RecurrenceEnd.valueOf(requireNotNull(recurrenceEnd)),
            endDate = recurrenceEndEpochDay?.let(LocalDate::ofEpochDay),
            occurrenceCount = recurrenceCount,
            anchor = runCatching { RecurrenceAnchor.valueOf(recurrenceAnchor) }
                .getOrDefault(RecurrenceAnchor.Schedule),
        )
    } else {
        null
    }

    return WhipTask(
        id = id,
        title = title,
        notes = notes,
        scheduleKind = kind,
        date = startDate,
        recurrence = recurrence,
        timeMinutes = timeMinutes,
        reminderEnabled = reminderEnabled,
        archived = archived,
        completedAtMillis = completedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        showSubtaskProgress = showSubtaskProgress,
        progressDisplay = TaskProgressDisplay.valueOf(progressDisplay),
        autoCompleteFromSteps = autoCompleteFromSteps,
        repeatStepPolicy = RepeatStepPolicy.valueOf(repeatStepPolicy),
        pinned = pinned,
        priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.None),
        areaId = areaId,
        area = area,
        tags = tagsCsv.split(',').map(String::trim).filter(String::isNotBlank).toSet(),
        deadline = deadlineEpochDay?.let(LocalDate::ofEpochDay),
        reminderOffsetsMinutes = reminderOffsetsMinutesCsv.split(',')
            .mapNotNull(String::toIntOrNull).distinct().sortedDescending(),
        locationReminder = if (
            locationReminderEnabled && locationLatitude != null && locationLongitude != null
        ) {
            TaskLocationReminder(
                name = locationName,
                latitude = locationLatitude,
                longitude = locationLongitude,
                radiusMeters = locationRadiusMeters,
                trigger = runCatching { LocationTrigger.valueOf(locationTrigger) }
                    .getOrDefault(LocationTrigger.Arrive),
            )
        } else null,
        missedOccurrencePolicy = runCatching { MissedOccurrencePolicy.valueOf(missedOccurrencePolicy) }
            .getOrDefault(MissedOccurrencePolicy.KeepLatest),
        inbox = inbox && kind == ScheduleKind.Anytime,
        durationMinutes = durationMinutes,
        effort = runCatching { TaskEffort.valueOf(effort) }.getOrDefault(TaskEffort.Moderate),
        manualPosition = manualPosition,
    )
}

fun TaskDraft.toEntity(
    id: Long = 0,
    uuid: String = UUID.randomUUID().toString(),
    createdAtMillis: Long,
    updatedAtMillis: Long = createdAtMillis,
    completedAtMillis: Long? = null,
    manualPosition: Int = 0,
): TaskEntity {
    val normalizedDate = when (scheduleKind) {
        ScheduleKind.Anytime -> null
        ScheduleKind.Once -> requireNotNull(date)
        ScheduleKind.Recurring -> requireNotNull(recurrence).startDate
    }
    val rule = recurrence.takeIf { scheduleKind == ScheduleKind.Recurring }

    return TaskEntity(
        id = id,
        uuid = uuid,
        title = title.trim(),
        notes = notes.trim(),
        scheduleKind = scheduleKind.name,
        dateEpochDay = normalizedDate?.toEpochDay(),
        recurrenceUnit = rule?.unit?.name,
        recurrenceInterval = rule?.interval ?: 1,
        weekdaysMask = rule?.weekdays?.toWeekdayMask() ?: 0,
        recurrenceEnd = rule?.end?.name,
        recurrenceEndEpochDay = rule?.endDate?.toEpochDay(),
        recurrenceCount = rule?.occurrenceCount,
        timeMinutes = timeMinutes.takeIf { scheduleKind != ScheduleKind.Anytime },
        reminderEnabled = reminderEnabled && scheduleKind != ScheduleKind.Anytime && timeMinutes != null,
        archived = false,
        completedAtMillis = completedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        showSubtaskProgress = showSubtaskProgress,
        progressDisplay = progressDisplay.name,
        autoCompleteFromSteps = autoCompleteFromSteps,
        repeatStepPolicy = repeatStepPolicy.name,
        pinned = false,
        priority = priority.name,
        areaId = areaId,
        area = area.trim(),
        tagsCsv = tags.map(String::trim).filter(String::isNotBlank).distinctBy(String::lowercase)
            .sortedBy(String::lowercase).joinToString(","),
        deadlineEpochDay = deadline?.toEpochDay(),
        recurrenceAnchor = rule?.anchor?.name ?: RecurrenceAnchor.Schedule.name,
        reminderOffsetsMinutesCsv = reminderOffsetsMinutes.filter { it >= 0 }.distinct()
            .sortedDescending().joinToString(","),
        locationReminderEnabled = locationReminder != null,
        locationName = locationReminder?.name?.trim().orEmpty(),
        locationLatitude = locationReminder?.latitude,
        locationLongitude = locationReminder?.longitude,
        locationRadiusMeters = locationReminder?.radiusMeters ?: 150f,
        locationTrigger = locationReminder?.trigger?.name ?: LocationTrigger.Arrive.name,
        missedOccurrencePolicy = missedOccurrencePolicy.name,
        inbox = inbox && scheduleKind == ScheduleKind.Anytime,
        durationMinutes = durationMinutes?.coerceIn(1, 1_440),
        effort = effort.name,
        manualPosition = manualPosition,
    )
}

fun TaskOccurrenceEntity.toDomain(): TaskOccurrence = TaskOccurrence(
    taskId = taskId,
    originalDate = LocalDate.ofEpochDay(originalEpochDay),
    scheduledDate = LocalDate.ofEpochDay(scheduledEpochDay),
    state = OccurrenceState.valueOf(state),
    completedAtMillis = completedAtMillis,
)

fun TaskOccurrence.toEntity(): TaskOccurrenceEntity = TaskOccurrenceEntity(
    taskId = taskId,
    originalEpochDay = originalDate.toEpochDay(),
    scheduledEpochDay = scheduledDate.toEpochDay(),
    state = state.name,
    completedAtMillis = completedAtMillis,
)

fun TaskStepEntity.toDomain(): TaskStep = TaskStep(
    id = id,
    taskId = taskId,
    title = title,
    position = position,
    notes = notes,
    archived = archived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun TaskStepStateEntity.toDomain(): TaskStepState = TaskStepState(
    stepId = stepId,
    taskId = taskId,
    occurrenceKey = occurrenceKey,
    completed = completed,
    completedAtMillis = completedAtMillis,
    titleSnapshot = titleSnapshot,
)

fun TaskStepSnapshotEntity.toDomain(): TaskStepSnapshot = TaskStepSnapshot(
    stepId = stepId,
    taskId = taskId,
    occurrenceKey = occurrenceKey,
    title = title,
    position = position,
    notes = notes,
    completed = completed,
    completedAtMillis = completedAtMillis,
)
