package com.whip.app.ui

import com.whip.app.domain.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class HabitPresentationTest {
    private val date = LocalDate.of(2026, 9, 27)
    private fun progress(habit: Habit, value: Double = 0.0, status: HabitLogStatus? = null) = HabitDayProgress(
        habit, date, true, value, status, null, emptyList(), 2, 0.5,
    )

    @Test fun targetRulesRetainBothBoundsAndPeriodWithoutConvertingDisplayValuesAgain() {
        val weekly = habit(HabitTrackingMode.Decimal).copy(unitId = "pound", precision = 0, targetPeriod = TargetPeriod.Week, targetMin = 10.0, targetMax = 20.0)
        for ((rule, expected) in listOf(TargetComparison.AtLeast to "at least 10", TargetComparison.AtMost to "at most 20", TargetComparison.Exactly to "exactly 10", TargetComparison.WithinRange to "10–20")) {
            val label = progress(weekly.copy(comparison = rule), 12.0).targetProgressLabel()
            assertTrue(label, label.startsWith("12 lb"))
            assertTrue(label, expected in label)
            assertTrue(label, "this week's total" in label)
        }
        val custom = UnitDefinition("custom-scoop", "Scoop", "scoop", UnitDimension.Count, 5.0, custom = true)
        assertTrue(progress(weekly.copy(unitId = custom.id), 12.0).targetProgressLabel(listOf(custom)).startsWith("12 scoop"))
    }

    @Test fun lowPressureHidesStreaksInCollapsedAndSkippedStates() {
        val item = progress(habit(HabitTrackingMode.CheckOff))
        assertFalse(item.compactCollectionStatus(lowPressureMode = true).contains("streak"))
        assertFalse(item.copy(dayState = HabitDayState.Skipped).compactCollectionStatus(true).contains("streak"))
        assertTrue(item.compactCollectionStatus().contains("streak"))
    }

    @Test fun managementCardsDescribeTheConfiguredScheduleAndRealAvailability() {
        val scheduled = habit(HabitTrackingMode.CheckOff).copy(scheduleType = HabitScheduleType.SelectedWeekdays,
            weekdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY))
        assertEquals("TUESDAY, FRIDAY", scheduled.collectionScheduleLabel { it.name })
        assertEquals("Every 3 days", scheduled.copy(scheduleType = HabitScheduleType.EveryNDays, scheduleInterval = 3).collectionScheduleLabel { it.name })
        assertEquals("4 times per month", scheduled.copy(scheduleType = HabitScheduleType.FlexibleTimesPerMonth, flexibleTimesPerWeek = 4).collectionScheduleLabel { it.name })
        assertEquals("Paused · no check-in expected", progress(scheduled).copy(dayState = HabitDayState.Paused).managementAvailabilityLabel())
        assertEquals("Paused · no check-in expected", progress(scheduled.copy(paused = true)).managementAvailabilityLabel())
        assertTrue(progress(scheduled).copy(scheduled = false).managementAvailabilityLabel().startsWith("No check-in expected today"))
    }

    @Test fun addingHistoryExplainsUnavailableStateWithoutBanningCorrections() {
        val active = habit(HabitTrackingMode.Duration)
        assertNull(active.newEntryUnavailableReason())
        assertTrue(active.copy(paused = true).newEntryUnavailableReason()!!.contains("Resume"))
        assertTrue(active.copy(archived = true).newEntryUnavailableReason()!!.contains("Existing history can still be corrected"))
        assertTrue(active.copy(sourceMeasurementId = "linked").newEntryUnavailableReason()!!.contains("linked measurement"))
    }

    @Test fun zeroRatingAndNoteOnlyEntriesRemainRecordedFacts() {
        for (mode in listOf(HabitTrackingMode.Rating, HabitTrackingMode.LogOnly)) {
            val item = progress(habit(mode).copy(comparison = TargetComparison.None), status = HabitLogStatus.Recorded)
            val summary = item.inspectorTodaySummary(0.0, ZoneId.of("UTC"), false)
            assertFalse(summary, summary.contains("No entries") || summary.contains("Not rated"))
            assertEquals("Recorded today", item.inspectorStatus(false))
            assertNull("A recorded fact must not invent a target outcome", item.successful)
        }
    }

    @Test fun flexibleStreaksAndAccumulatedTotalsNameTheirActualPeriods() {
        val habit = habit(HabitTrackingMode.Decimal).copy(scheduleType = HabitScheduleType.FlexibleTimesPerMonth, targetPeriod = TargetPeriod.Month, unitId = "litre")
        assertEquals("2 months", habit.streakUnitLabel(2))
        assertEquals("1 week", habit.copy(scheduleType = HabitScheduleType.FlexibleTimesPerWeek).streakUnitLabel(1))
        val summary = progress(habit, 3.0).inspectorTodaySummary(0.0, ZoneId.of("UTC"), false)
        assertTrue(summary, "this month's total" in summary)
        assertFalse(summary, "logged today" in summary)
    }

    @Test fun historySearchKeepsOriginalUnitsDatesNotesAndNeutralEvents() {
        val habit = habit(HabitTrackingMode.Decimal).copy(unitId = "litre")
        val log = HabitLog(41, "log-41", habit.id, 250.0, 0.25, "millilitre", HabitLogStatus.Recorded,
            date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), date.minusYears(2), "UTC", 0,
            "Forgot my bottle", MeasurementSourceType.Manual, null, null, 1, 1)
        val text = HabitHistoryEvent.Log(log).habitHistorySearchText(habit, date)
        assertTrue(text, "250" in text && "ml" in text && "2024-09-27" in text && "forgot my bottle" in text)
        for (locale in listOf(java.util.Locale.US, java.util.Locale.FRANCE)) {
            val localized = HabitHistoryEvent.Log(log).habitHistorySearchText(habit, date, locale = locale)
            val expectedDate = log.localDate.format(
                java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale),
            ).lowercase(java.util.Locale.ROOT)
            assertTrue(localized, expectedDate in localized)
            assertTrue(localized, "250" in localized && "ml" in localized && "forgot my bottle" in localized)
        }
        val skip = HabitSkip("skip", habit.id, date.minusDays(2), 10, 1, 1)
        assertTrue(HabitHistoryEvent.Skip(skip).habitHistorySearchText(habit, date).contains("skipped"))
        val pause = HabitPause(3, habit.id, date.minusDays(4), date.minusDays(1), "Travel")
        val pauseText = HabitHistoryEvent.Pause(pause).habitHistorySearchText(habit, date)
        assertTrue(pauseText, "travel" in pauseText && date.minusDays(1).toString() in pauseText)
        assertEquals("millilitre", log.enteredUnitId)
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
        precision = 0,
        comparison = TargetComparison.AtLeast,
        targetMin = 1.0,
        targetMax = null,
        targetPeriod = TargetPeriod.Day,
        rollingDays = null,
        scheduleType = HabitScheduleType.Daily,
        scheduleInterval = 1,
        weekdays = emptySet(),
        flexibleTimesPerWeek = null,
        startDate = date,
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

}
