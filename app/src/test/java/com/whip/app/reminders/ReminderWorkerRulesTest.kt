package com.whip.app.reminders

import com.whip.app.domain.WorkoutSessionState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderWorkerRulesTest {
    @Test
    fun quietHourShiftKeepsTheOriginalHabitScheduleDate() {
        val monday = LocalDate.of(2026, 8, 17)
        val tuesday = monday.plusDays(1)

        assertEquals(monday, logicalHabitReminderDate(monday.toEpochDay(), tuesday))
        assertEquals(tuesday, logicalHabitReminderDate(Long.MIN_VALUE, tuesday))
    }

    @Test
    fun shiftedGoalReminderCannotCrossItsDeadline() {
        val deadline = LocalDate.of(2026, 8, 17)

        assertTrue(goalReminderIsWithinDeadline(deadline.toEpochDay(), deadline))
        assertFalse(goalReminderIsWithinDeadline(deadline.toEpochDay(), deadline.plusDays(1)))
        assertTrue(goalReminderIsWithinDeadline(null, deadline.plusYears(1)))
    }

    @Test
    fun restTimerRequiresAnActiveSessionAndTheCurrentDueDeadline() {
        assertTrue(restTimerShouldNotify(WorkoutSessionState.Active.name, 10_000L, 10_000L))
        assertFalse(restTimerShouldNotify(WorkoutSessionState.Finished.name, 10_000L, 10_000L))
        assertFalse(restTimerShouldNotify(WorkoutSessionState.Discarded.name, 10_000L, 10_000L))
        assertFalse(restTimerShouldNotify(WorkoutSessionState.Active.name, null, 10_000L))
        assertFalse(restTimerShouldNotify(WorkoutSessionState.Active.name, 20_000L, 10_000L))
    }
}
