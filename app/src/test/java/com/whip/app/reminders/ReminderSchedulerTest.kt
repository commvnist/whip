package com.whip.app.reminders

import com.whip.app.core.AppSettings
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.WhipTask
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSchedulerTest {
    @Test
    fun farMovedRecurringOccurrenceStillProducesReminder() {
        val zone = ZoneId.of("UTC")
        val original = LocalDate.of(2026, 7, 1)
        val movedTo = LocalDate.of(2026, 8, 20)
        val task = WhipTask(
            id = 7,
            title = "Moved",
            notes = "",
            scheduleKind = ScheduleKind.Recurring,
            date = original,
            recurrence = RecurrenceRule(RecurrenceUnit.Years, startDate = original),
            timeMinutes = 9 * 60,
            reminderEnabled = true,
            archived = false,
            completedAtMillis = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val occurrence = TaskOccurrence(7, original, movedTo, OccurrenceState.Open, null)
        val afterMillis = LocalDate.of(2026, 8, 19).atStartOfDay(zone).toInstant().toEpochMilli()

        val reminder = nextTaskReminder(task, listOf(occurrence), afterMillis, 0, zone)

        assertEquals(original, reminder?.originalDate)
        assertEquals(movedTo, reminder?.scheduledDate)
        assertEquals(
            movedTo.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            reminder?.triggerAtMillis,
        )
    }

    @Test
    fun malformedOffsetsFailClosedWhileEmptyKeepsAtTimeCompatibility() {
        assertEquals(listOf(0), parseTaskReminderOffsetsOrNull(""))
        assertEquals(listOf(60, 15, 0), parseTaskReminderOffsetsOrNull("15,60,0,15"))
        assertNull(parseTaskReminderOffsetsOrNull("15,,60"))
        assertNull(parseTaskReminderOffsetsOrNull("soon"))
        assertNull(parseTaskReminderOffsetsOrNull("-10"))
        assertNull(parseTaskReminderOffsetsOrNull("43201"))
    }

    @Test
    fun fingerprintIgnoresPresentationButCoversDeliverySemantics() {
        val date = LocalDate.of(2026, 8, 20)
        val task = oneTimeTask(date)
        val settings = AppSettings(timeZoneId = "UTC", quietStartMinutes = 1_320, quietEndMinutes = 420)
        fun fingerprint(
            value: WhipTask = task,
            scheduled: LocalDate = date,
            offset: Int = 0,
            appSettings: AppSettings = settings,
        ) = taskReminderFingerprint(
            task = value,
            stableEntityId = "task-stable-id",
            originalDate = date,
            scheduledDate = scheduled,
            occurrenceState = OccurrenceState.Open,
            offsetMinutes = offset,
            normalizedOffsets = listOf(0, 15),
            settings = appSettings,
        )

        val baseline = fingerprint()
        assertEquals(
            baseline,
            fingerprint(task.copy(title = "Renamed", notes = "Presentation only", icon = "📌")),
        )
        assertNotEquals(baseline, fingerprint(task.copy(timeMinutes = 10 * 60)))
        assertNotEquals(baseline, fingerprint(scheduled = date.plusDays(1)))
        assertNotEquals(baseline, fingerprint(offset = 15))
        assertNotEquals(baseline, fingerprint(appSettings = settings.copy(timeZoneId = "America/Toronto")))
        assertNotEquals(baseline, fingerprint(appSettings = settings.copy(quietStartMinutes = 1_260)))
    }

    @Test
    fun impossibleTimeAndOffsetNeverProduceQueuedReminder() {
        val date = LocalDate.of(2026, 8, 20)
        val zone = ZoneId.of("UTC")
        val after = date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        assertNull(nextTaskReminder(oneTimeTask(date).copy(timeMinutes = -1), emptyList(), after, 0, zone))
        assertNull(nextTaskReminder(oneTimeTask(date), emptyList(), after, -1, zone))
        assertNull(nextTaskReminder(oneTimeTask(date), emptyList(), after, 43_201, zone))
    }

    @Test
    fun oneMillisecondSuccessorBoundaryKeepsAdjacentMinuteReminder() {
        val date = LocalDate.of(2026, 8, 20)
        val zone = ZoneId.of("UTC")
        val nineAm = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val reminder = nextTaskReminder(
            task = oneTimeTask(date).copy(timeMinutes = 9 * 60 + 1),
            occurrences = emptyList(),
            afterMillis = nineAm + 1L,
            offsetMinutes = 0,
            zone = zone,
        )

        assertEquals(nineAm + 60_000L, reminder?.triggerAtMillis)
    }

    private fun oneTimeTask(date: LocalDate) = WhipTask(
        id = 9,
        title = "Task",
        notes = "",
        scheduleKind = ScheduleKind.Once,
        date = date,
        recurrence = null,
        timeMinutes = 9 * 60,
        reminderEnabled = true,
        archived = false,
        completedAtMillis = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        reminderOffsetsMinutes = listOf(0, 15),
    )
}
