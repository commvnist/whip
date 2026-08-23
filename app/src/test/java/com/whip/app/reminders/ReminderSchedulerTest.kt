package com.whip.app.reminders

import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.WhipTask
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
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
        assertEquals(
            movedTo.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            reminder?.triggerAtMillis,
        )
    }
}
