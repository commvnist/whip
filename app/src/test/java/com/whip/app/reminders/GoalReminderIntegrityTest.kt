package com.whip.app.reminders

import androidx.work.Data
import com.whip.app.data.GoalEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalReminderIntegrityTest {
    private val zone = ZoneId.of("America/Toronto")
    private val date = LocalDate.of(2026, 8, 31)

    @Test
    fun scheduledClaimMustStillMatchEveryDeliverySemantic() {
        val goal = goal()
        val trigger = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val claim = claim(goal, trigger)

        assertTrue(valid(goal, claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal.copy(archived = true), claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal.copy(status = "Paused"), claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal.copy(reminderMinutes = null), claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal.copy(reminderMinutes = 600), claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal.copy(uuid = "replacement"), claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal.copy(startEpochDay = date.plusDays(1).toEpochDay()), claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal, claim.copy(logicalEpochDay = date.minusDays(1).toEpochDay()), Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal, claim.copy(logicalEpochDay = Long.MAX_VALUE), Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal, claim, Instant.ofEpochMilli(trigger - 1)))
        assertFalse(valid(goal, claim, Instant.ofEpochMilli(trigger), quietStart = 8 * 60, quietEnd = 10 * 60))
    }

    @Test
    fun presentationEditsUseLiveContentWithoutInvalidatingAValidClaim() {
        val goal = goal()
        val trigger = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val claim = claim(goal, trigger)

        assertTrue(
            valid(
                goal.copy(name = "Read more books", description = "New context"),
                claim,
                Instant.ofEpochMilli(trigger),
            ),
        )
        assertFalse(
            valid(
                goal.copy(type = "ElapsedSince"),
                claim,
                Instant.ofEpochMilli(trigger),
            ),
        )
    }

    @Test
    fun snoozeKeepsCurrentSemanticsButUsesItsOwnAuthorizedTrigger() {
        val goal = goal()
        val trigger = date.atTime(9, 10).atZone(zone).toInstant().toEpochMilli()
        val claim = claim(goal, trigger).copy(kind = ReminderDeliveryKind.Snoozed)

        assertTrue(valid(goal, claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal.copy(deadlineEpochDay = date.minusDays(1).toEpochDay()), claim, Instant.ofEpochMilli(trigger)))
        assertFalse(valid(goal.copy(reminderMinutes = 601), claim, Instant.ofEpochMilli(trigger)))
    }

    @Test
    fun quietHoursShiftTheDeliveryButPreserveTheLogicalDate() {
        val goal = goal(reminderMinutes = 23 * 60)
        val after = date.atTime(22, 0).atZone(zone).toInstant().toEpochMilli()

        val reminder = nextGoalReminder(goal, after, zone, 22 * 60, 7 * 60)

        requireNotNull(reminder)
        assertEquals(date, reminder.logicalDate)
        assertEquals(date.plusDays(1).atTime(7, 0).atZone(zone).toInstant().toEpochMilli(), reminder.triggerAtMillis)
    }

    @Test
    fun reconciliationAfterRawTimeKeepsTodaysQuietShiftedReminder() {
        val goal = goal(reminderMinutes = 6 * 60)
        val after = date.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()

        val reminder = nextGoalReminder(goal, after, zone, 22 * 60, 8 * 60)

        requireNotNull(reminder)
        assertEquals(date, reminder.logicalDate)
        assertEquals(date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli(), reminder.triggerAtMillis)
    }

    @Test
    fun reconciliationKeepsPriorLogicalDaysCrossMidnightQuietShift() {
        val goal = goal(reminderMinutes = 23 * 60, start = date.minusDays(1))
        val after = date.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()

        val reminder = nextGoalReminder(goal, after, zone, 22 * 60, 8 * 60)

        requireNotNull(reminder)
        assertEquals(date.minusDays(1), reminder.logicalDate)
        assertEquals(date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli(), reminder.triggerAtMillis)
    }

    @Test
    fun quietHourShiftCannotLeakBeyondTheGoalDeadline() {
        val goal = goal(reminderMinutes = 23 * 60, deadline = date)
        val after = date.atTime(22, 0).atZone(zone).toInstant().toEpochMilli()

        assertNull(nextGoalReminder(goal, after, zone, 22 * 60, 7 * 60))
    }

    @Test
    fun archivedGoalsNeverScheduleEvenWhenLifecycleRemainsActive() {
        val goal = goal().copy(archived = true)
        val after = date.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()

        assertNull(nextGoalReminder(goal, after, zone, null, null))
    }

    @Test
    fun legacyAndMalformedWorkClaimsFailClosed() {
        assertNull(Data.EMPTY.reminderDeliveryClaimOrNull())
        assertNull(
            Data.Builder()
                .putReminderDeliveryClaim(
                    ReminderDeliveryClaim(
                        kind = ReminderDeliveryKind.Scheduled,
                        stableEntityId = "goal-stable",
                        logicalEpochDay = date.toEpochDay(),
                        expectedTriggerAtMillis = -1,
                        definitionFingerprint = "fingerprint",
                    ),
                )
                .build()
                .reminderDeliveryClaimOrNull(),
        )
        assertNull(
            Data.Builder()
                .putReminderDeliveryClaim(
                    ReminderDeliveryClaim(
                        kind = ReminderDeliveryKind.Scheduled,
                        stableEntityId = "goal-stable",
                        logicalEpochDay = Long.MAX_VALUE,
                        expectedTriggerAtMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                        definitionFingerprint = "fingerprint",
                    ),
                )
                .build()
                .reminderDeliveryClaimOrNull(),
        )
    }

    @Test
    fun goalPromptMatchesTheKindOfProgress() {
        assertEquals("Log goal progress", goalReminderPrompt("ReachValue"))
        assertEquals("Record today's progress", goalReminderPrompt("Consistency"))
        assertEquals("Review your milestones", goalReminderPrompt("WeightedMilestones"))
        assertEquals("Review elapsed time", goalReminderPrompt("ElapsedSince"))
        assertEquals("Log goal progress", goalReminderPrompt("future_type"))
    }

    private fun claim(goal: GoalEntity, trigger: Long) = ReminderDeliveryClaim(
        kind = ReminderDeliveryKind.Scheduled,
        stableEntityId = goal.uuid,
        logicalEpochDay = date.toEpochDay(),
        expectedTriggerAtMillis = trigger,
        definitionFingerprint = goalReminderSemanticFingerprint(goal, zone, null, null),
    )

    private fun valid(
        goal: GoalEntity,
        claim: ReminderDeliveryClaim,
        now: Instant,
        quietStart: Int? = null,
        quietEnd: Int? = null,
    ) = currentGoalReminderClaimIsValid(goal, claim, now, zone, quietStart, quietEnd)

    private fun goal(
        reminderMinutes: Int? = 9 * 60,
        deadline: LocalDate? = date.plusDays(30),
        start: LocalDate = date,
    ) = GoalEntity(
        id = 7,
        uuid = "goal-stable",
        metricId = "goal-metric",
        name = "Read",
        description = "",
        areaId = null,
        area = "Personal",
        tagsCsv = "",
        icon = "flag",
        type = "ReachValue",
        dimension = "Count",
        unitId = "count",
        precision = 0,
        baseline = 0.0,
        targetMin = 10.0,
        targetMax = null,
        direction = "Increase",
        startEpochDay = start.toEpochDay(),
        deadlineEpochDay = deadline?.toEpochDay(),
        aggregation = "Latest",
        aggregationPeriod = "All",
        rollingDays = null,
        paceType = "None",
        consistencyPeriod = "Week",
        consistencyRequiredPeriods = null,
        elapsedStartMillis = null,
        elapsedDisplayUnit = "Auto",
        reminderMinutes = reminderMinutes,
        status = "Active",
        pinned = false,
        position = 0,
        createdAtMillis = 0,
        updatedAtMillis = 0,
    )
}
