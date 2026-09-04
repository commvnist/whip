package com.whip.app.ui

import com.whip.app.domain.Habit
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MeasurementEntry
import com.whip.app.domain.MeasurementEntryStatus
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPresentationTest {
    private val today = LocalDate.of(2026, 8, 30)

    @Test
    fun habitHistoryCopyFollowsTheCheckInInsteadOfTheStorageModel() {
        val checkOff = habit(HabitTrackingMode.CheckOff)
        assertEquals("Check In for an Earlier Day", checkOff.pastCheckInActionLabel())
        assertEquals("Record Past Check-In", checkOff.historyDialogTitle(editing = false))
        assertEquals("Edit Check-In", checkOff.historyDialogTitle(editing = true))

        val count = habit(HabitTrackingMode.Count)
        assertEquals("Log an Earlier Day", count.pastCheckInActionLabel())
        assertEquals("Amount", count.historyAmountLabel())

        val rating = habit(HabitTrackingMode.Rating)
        assertEquals("Rating", rating.historyAmountLabel())
        assertEquals("Rate an Earlier Day", rating.historyDialogTitle(editing = false))

        val logOnly = habit(HabitTrackingMode.LogOnly)
        assertEquals("Number · optional", logOnly.historyAmountLabel(optional = true))
        assertEquals("Add an Earlier Entry", logOnly.pastCheckInActionLabel())
    }

    @Test
    fun activityRowsExplainMeaningAndConnectedSourceWithoutRawEnumNames() {
        val count = habit(HabitTrackingMode.Count)
        val manual = log(value = 3.0, sourceType = MeasurementSourceType.Manual, note = "After lunch")
        assertEquals("Logged 3", manual.activityTitle(count))
        assertEquals("Yesterday · After lunch", manual.activitySupportingText(today))
        assertTrue(manual.isUserEditable())

        val synced = log(id = -4, value = 6_500.0, sourceType = MeasurementSourceType.HealthConnect)
        assertEquals("Logged 6500", synced.activityTitle(count))
        assertEquals("Yesterday · Synced from Health Connect", synced.activitySupportingText(today))
        assertFalse(synced.isUserEditable())

        val completed = log(value = 1.0, status = HabitLogStatus.Success)
        assertEquals("Checked in", completed.activityTitle(habit(HabitTrackingMode.CheckOff)))
        assertFalse(completed.activitySupportingText(today).contains("Success"))
    }

    @Test
    fun habitHistoryUsesEffectiveDatesAndIncludesOnlyStartedPauses() {
        val newerDayOlderWrite = log(id = 2).copy(
            localDate = today.minusDays(1),
            timestamp = Instant.parse("2026-08-20T10:00:00Z"),
        )
        val olderDayNewerWrite = log(id = 3).copy(
            localDate = today.minusDays(3),
            timestamp = Instant.parse("2026-08-30T10:00:00Z"),
        )
        val skip = HabitSkip("skip-1", 1, today.minusDays(2), 9_000L, 9_000L, 9_000L)
        val startedPause = HabitPause(8, 1, today.minusDays(4), today.minusDays(3), "Travel")
        val upcomingPause = HabitPause(9, 1, today.plusDays(1), null, "Future")

        val events = habitHistoryEvents(
            logs = listOf(olderDayNewerWrite, newerDayOlderWrite),
            skips = listOf(skip),
            pauses = listOf(upcomingPause, startedPause),
            throughDate = today,
        )

        assertEquals(
            listOf(today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(4)),
            events.map(HabitHistoryEvent::effectiveDate),
        )
        assertTrue(events.last() is HabitHistoryEvent.Pause)
        assertFalse(events.any { it is HabitHistoryEvent.Pause && it.value.id == upcomingPause.id })
    }

    @Test
    fun goalHistoryUsesProgressLanguageAndOnlyWhipUpdatesAreEditable() {
        val manual = measurementEntry(sourceType = MeasurementSourceType.Goal, value = 72.5, note = "Weekly weigh-in")
        assertEquals("72.5 kg", manual.historyTitle())
        assertEquals("Aug 29, 2026 · Weekly weigh-in", manual.historySupportingText())
        assertTrue(manual.isUserEditableGoalUpdate())

        val synced = measurementEntry(sourceType = MeasurementSourceType.HealthConnect, value = 72.5)
        assertEquals("Aug 29, 2026 · Synced from Health Connect", synced.historySupportingText())
        assertFalse(synced.isUserEditableGoalUpdate())

        val skipped = synced.copy(enteredValue = null, enteredUnitId = null, status = MeasurementEntryStatus.Skipped)
        assertEquals("Skipped", skipped.historyTitle())
    }

    private fun habit(mode: HabitTrackingMode) = Habit(
        id = 1,
        uuid = "habit-1",
        measurementId = "measurement-habit-1",
        name = "Medication",
        notes = "",
        area = "Main",
        tags = emptyList(),
        icon = "💊",
        trackingMode = mode,
        dimension = UnitDimension.Count,
        unitId = "count",
        precision = if (mode == HabitTrackingMode.Rating) 1 else 0,
        comparison = TargetComparison.AtLeast,
        targetMin = 1.0,
        targetMax = null,
        targetPeriod = TargetPeriod.Day,
        rollingDays = null,
        scheduleType = HabitScheduleType.Daily,
        scheduleInterval = 1,
        weekdays = emptySet(),
        flexibleTimesPerWeek = null,
        startDate = today,
        endType = HabitEndType.Never,
        endDate = null,
        endValue = null,
        quickIncrement = 1.0,
        quickActions = emptyList(),
        reminderMinutes = emptyList(),
        weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY,
        timerStartedAtMillis = null,
        pinned = false,
        position = 0,
        archived = false,
        paused = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun log(
        id: Long = 2,
        value: Double? = null,
        status: HabitLogStatus = HabitLogStatus.Recorded,
        sourceType: MeasurementSourceType = MeasurementSourceType.Manual,
        note: String = "",
    ) = HabitLog(
        id = id,
        uuid = "log-$id",
        habitId = 1,
        value = value,
        canonicalValue = value,
        enteredUnitId = "count".takeIf { value != null },
        status = status,
        timestamp = Instant.parse("2026-08-29T14:00:00Z"),
        localDate = today.minusDays(1),
        zoneId = "UTC",
        offsetSeconds = 0,
        note = note,
        sourceType = sourceType,
        sourceId = null,
        measurementEntryId = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun measurementEntry(
        sourceType: MeasurementSourceType,
        value: Double?,
        note: String = "",
    ) = MeasurementEntry(
        id = "entry-1",
        measurementId = "goal-1",
        canonicalValue = value,
        enteredValue = value,
        enteredUnitId = "kilogram".takeIf { value != null },
        status = MeasurementEntryStatus.Recorded,
        timestamp = Instant.parse("2026-08-29T14:00:00Z"),
        localDate = today.minusDays(1),
        zoneId = "UTC",
        offsetSeconds = 0,
        sourceType = sourceType,
        sourceId = null,
        note = note,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
