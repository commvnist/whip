package com.whip.app.ui

import com.whip.app.domain.AvoidMissingPolicy
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitIntent
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitMetricMirrorTest {
    @Test fun sourceBoundHabitReflectsProviderUpdateDeletionAndProvenance() {
        val habit = habit()
        val original = entry(value = 6_000.0)

        val first = mirrorMetricEntriesAsHabitLogs(listOf(habit), listOf(original)).single()
        assertEquals(6_000.0, first.value ?: -1.0, 0.0)
        assertEquals(MetricSourceType.HealthConnect, first.sourceType)
        assertEquals("health:steps:a", first.sourceId)
        assertEquals("entry-health:steps:a", first.metricEntryId)

        val edited = mirrorMetricEntriesAsHabitLogs(listOf(habit), listOf(original.copy(canonicalValue = 6_500.0, enteredValue = 6_500.0))).single()
        assertEquals(first.id, edited.id)
        assertEquals(6_500.0, edited.value ?: -1.0, 0.0)
        assertTrue(mirrorMetricEntriesAsHabitLogs(listOf(habit), emptyList()).isEmpty())
    }

    private fun habit() = Habit(
        id = 7,
        uuid = "habit-7",
        metricId = "habit-metric",
        name = "Steps",
        notes = "",
        area = "Health",
        tags = emptyList(),
        icon = "✓",
        colorArgb = null,
        intent = HabitIntent.Build,
        trackingMode = HabitTrackingMode.Count,
        dimension = UnitDimension.Count,
        unitId = "count",
        precision = 0,
        comparison = TargetComparison.AtLeast,
        targetMin = 8_000.0,
        targetMax = null,
        targetPeriod = TargetPeriod.Day,
        rollingDays = null,
        scheduleType = HabitScheduleType.Daily,
        scheduleInterval = 1,
        weekdays = emptySet(),
        flexibleTimesPerWeek = null,
        startDate = LocalDate.of(2026, 8, 17),
        endType = HabitEndType.Never,
        endDate = null,
        endValue = null,
        timeWindowStartMinutes = null,
        timeWindowEndMinutes = null,
        quickIncrement = 1.0,
        quickActions = emptyList(),
        reminderMinutes = emptyList(),
        weekdayReminderMinutes = emptyMap(),
        weekStart = DayOfWeek.MONDAY,
        avoidMissingPolicy = AvoidMissingPolicy.Unknown,
        timerStartedAtMillis = null,
        pinned = false,
        position = 0,
        archived = false,
        paused = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        sourceMetricId = "health.steps",
    )

    private fun entry(value: Double) = MetricEntry(
        id = "entry-health:steps:a",
        metricId = "health.steps",
        canonicalValue = value,
        enteredValue = value,
        enteredUnitId = "count",
        status = MetricEntryStatus.Recorded,
        timestamp = Instant.parse("2026-08-17T16:00:00Z"),
        localDate = LocalDate.of(2026, 8, 17),
        zoneId = "UTC",
        offsetSeconds = 0,
        sourceType = MetricSourceType.HealthConnect,
        sourceId = "health:steps:a",
        note = "Imported from Health Connect",
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
