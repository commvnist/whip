package com.whip.app.reminders

import com.whip.app.core.AppSettings
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.WhipTask
import com.whip.app.domain.WorkoutSessionState
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
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
    fun habitReconciliationKeepsTodaysQuietShiftedReminder() {
        val zone = ZoneId.of("America/Toronto")
        val date = LocalDate.of(2026, 8, 31)
        val after = date.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()

        val reminder = nextHabitReminder(
            afterMillis = after,
            zone = zone,
            firstLogicalDate = date,
            quietStartMinutes = 22 * 60,
            quietEndMinutes = 8 * 60,
            isEligible = { true },
            configuredMinutes = { listOf(6 * 60) },
        )

        requireNotNull(reminder)
        assertEquals(date, reminder.logicalDate)
        assertEquals(date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli(), reminder.triggerAtMillis)
    }

    @Test
    fun habitReconciliationKeepsPriorLogicalDaysCrossMidnightQuietShift() {
        val zone = ZoneId.of("America/Toronto")
        val date = LocalDate.of(2026, 8, 31)
        val after = date.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()

        val reminder = nextHabitReminder(
            afterMillis = after,
            zone = zone,
            firstLogicalDate = date.minusDays(1),
            quietStartMinutes = 22 * 60,
            quietEndMinutes = 8 * 60,
            isEligible = { true },
            configuredMinutes = { listOf(23 * 60) },
        )

        requireNotNull(reminder)
        assertEquals(date.minusDays(1), reminder.logicalDate)
        assertEquals(date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli(), reminder.triggerAtMillis)
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

    @Test
    fun restTimerRecoveryCeilsAnImminentDeadlineInsteadOfCancellingIt() {
        assertEquals(1, restTimerScheduleDelaySeconds(deadlineMillis = 10_001L, nowMillis = 10_000L))
        assertEquals(1, restTimerScheduleDelaySeconds(deadlineMillis = 10_999L, nowMillis = 10_000L))
        assertEquals(2, restTimerScheduleDelaySeconds(deadlineMillis = 11_001L, nowMillis = 10_000L))
        assertEquals(null, restTimerScheduleDelaySeconds(deadlineMillis = 10_000L, nowMillis = 10_000L))
    }

    @Test
    fun restTimerDeliveryPostsBeforePersistenceAndReplaysTheSameNotificationAfterFailure() = runBlocking {
        var posts = 0
        var completionAttempts = 0
        var staleCancellations = 0

        val interrupted = runCatching {
            deliverRestTimerAtLeastOnce(
                post = { posts++ },
                complete = {
                    completionAttempts++
                    error("Database unavailable after notification post")
                },
                cancelStale = { staleCancellations++ },
            )
        }
        assertTrue(interrupted.isFailure)
        assertEquals(1, posts)

        assertTrue(
            deliverRestTimerAtLeastOnce(
                post = { posts++ },
                complete = {
                    completionAttempts++
                    true
                },
                cancelStale = { staleCancellations++ },
            ),
        )
        assertEquals(2, posts)
        assertEquals(2, completionAttempts)
        assertEquals(0, staleCancellations)

        assertFalse(
            deliverRestTimerAtLeastOnce(
                post = { posts++ },
                complete = { false },
                cancelStale = { staleCancellations++ },
            ),
        )
        assertEquals(1, staleCancellations)
    }

    @Test
    fun taskClaimRequiresExactIdentityOccurrenceTriggerFingerprintAndPhysicalDate() {
        val zone = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 8, 20)
        val trigger = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val task = WhipTask(
            id = 1,
            title = "Task",
            notes = "",
            scheduleKind = ScheduleKind.Once,
            date = date,
            recurrence = null,
            timeMinutes = 9 * 60,
            reminderEnabled = true,
            archived = false,
            completedAtMillis = null,
            createdAtMillis = 0,
            updatedAtMillis = 0,
        )
        val snapshot = CurrentTaskReminder(
            task = task,
            stableEntityId = "stable-task",
            originalDate = date,
            scheduledDate = date,
            occurrenceState = OccurrenceState.Open,
            offsetMinutes = 0,
            expectedScheduledTriggerAtMillis = trigger,
            definitionFingerprint = "fingerprint",
        )
        val claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Scheduled,
            stableEntityId = "stable-task",
            logicalEpochDay = date.toEpochDay(),
            expectedTriggerAtMillis = trigger,
            definitionFingerprint = "fingerprint",
        )

        assertTrue(taskReminderClaimMatchesCurrent(claim, snapshot, date.toEpochDay(), date, zone, trigger))
        assertFalse(taskReminderClaimMatchesCurrent(null, snapshot, date.toEpochDay(), date, zone, trigger))
        assertFalse(taskReminderClaimMatchesCurrent(claim.copy(stableEntityId = "other"), snapshot, date.toEpochDay(), date, zone, trigger))
        assertFalse(taskReminderClaimMatchesCurrent(claim.copy(logicalEpochDay = date.plusDays(1).toEpochDay()), snapshot, date.toEpochDay(), date, zone, trigger))
        assertFalse(taskReminderClaimMatchesCurrent(claim.copy(expectedTriggerAtMillis = trigger + 1), snapshot, date.toEpochDay(), date, zone, trigger))
        assertFalse(taskReminderClaimMatchesCurrent(claim.copy(definitionFingerprint = "stale"), snapshot, date.toEpochDay(), date, zone, trigger))
        assertFalse(taskReminderClaimMatchesCurrent(claim, snapshot, date.toEpochDay(), date.plusDays(1), zone, trigger + 86_400_000L))
        assertFalse(taskReminderClaimMatchesCurrent(claim, snapshot, date.toEpochDay(), date, zone, trigger - 1L))
    }

    @Test
    fun snoozeKeepsExactDefinitionButUsesItsOwnExpectedTrigger() {
        val zone = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 8, 20)
        val scheduledTrigger = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val snoozeTrigger = date.atTime(9, 10).atZone(zone).toInstant().toEpochMilli()
        val task = WhipTask(
            id = 1, title = "Task", notes = "", scheduleKind = ScheduleKind.Once, date = date,
            recurrence = null, timeMinutes = 540, reminderEnabled = true, archived = false,
            completedAtMillis = null, createdAtMillis = 0, updatedAtMillis = 0,
        )
        val snapshot = CurrentTaskReminder(
            task, "stable", date, date, OccurrenceState.Open, 0, scheduledTrigger, "definition",
        )
        val snoozed = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Snoozed,
            stableEntityId = "stable",
            logicalEpochDay = date.toEpochDay(),
            expectedTriggerAtMillis = snoozeTrigger,
            definitionFingerprint = "definition",
        )

        assertTrue(taskReminderClaimMatchesCurrent(snoozed, snapshot, date.toEpochDay(), date, zone, snoozeTrigger))
        assertFalse(
            taskReminderClaimMatchesCurrent(
                snoozed.copy(definitionFingerprint = "changed"), snapshot, date.toEpochDay(), date, zone,
                snoozeTrigger,
            ),
        )
    }
}
