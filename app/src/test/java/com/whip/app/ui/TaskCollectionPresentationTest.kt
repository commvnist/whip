package com.whip.app.ui

import com.whip.app.domain.*
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class TaskCollectionPresentationTest {
    private val today = LocalDate.of(2026, 9, 29)

    @Test fun reopenedOccurrenceDoesNotInventAReschedule() {
        val reopened = TaskOccurrence(1, today, today, OccurrenceState.Open, null)
        assertTrue(reopened.historyLabel().startsWith("Open occurrence"))
        assertFalse(reopened.historyLabel().contains("Moved"))
        assertTrue(reopened.copy(scheduledDate = today.plusDays(1)).historyLabel().startsWith("Moved to"))
    }

    @Test fun compactTaskPrioritizesDeadlineAndRelativeDateOverConfiguration() {
        val task = WhipTask(1, "Review", "Notes", ScheduleKind.Once, today.plusDays(1), null, null,
            true, false, null, 1, 1).copy(deadline = today.minusDays(1),
                priority = TaskPriority.High, tags = setOf("secondary-tag"), durationMinutes = 90)
        val item = ScheduledTask(task, today.plusDays(1), today.plusDays(1), isDeadlineOverdue = true)
        val label = item.collectionSummary(false, today, ZoneOffset.UTC)
        assertTrue(label, label.startsWith("Deadline overdue · Yesterday"))
        assertTrue(label, "Tomorrow" in label && "High priority" in label)
        assertFalse(label, "secondary-tag" in label || "90 min" in label || "Reminder" in label)
    }

    @Test fun undatedRepeatingDefinitionDoesNotBecomeAnUnscheduledTask() {
        val task = WhipTask(1, "Annual review", "", ScheduleKind.Recurring, null,
            RecurrenceRule(unit = RecurrenceUnit.Years, startDate = today), null, false, false, null, 1, 1)
        assertEquals("Repeating task", ScheduledTask(task, null, null).collectionSummary(false, today, ZoneOffset.UTC))
    }
}
