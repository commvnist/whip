package com.whip.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.HabitHistoryLogDialog
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityHistoryUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 8, 30)

    @Test
    fun pastCheckInAsksWhatHappenedWithoutExposingStateOrValueStorageFields() {
        var savedValue: Double? = null
        var savedStatus: HabitLogStatus? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitHistoryLogDialog(
                    item = progress(HabitTrackingMode.CheckOff),
                    log = null,
                    initialDate = today.minusDays(1),
                    onDismiss = {},
                    onSave = { value, status, _, _ -> savedValue = value; savedStatus = status },
                )
            }
        }

        compose.onNodeWithText("Record Past Check-In").assertIsDisplayed()
        compose.onNodeWithText("💊 Medication").assertIsDisplayed()
        compose.onNodeWithText("Date · ${today.minusDays(1).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
            .assertIsDisplayed()
        compose.onAllNodesWithText("State").assertCountEquals(0)
        compose.onAllNodesWithText("Value", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Record").assertIsEnabled().performClick()
        assertEquals(1.0, savedValue ?: 0.0, 0.0)
        assertEquals(HabitLogStatus.Success, savedStatus)
    }

    @Test
    fun logOnlyPastEntryMakesTheNumberOptionalAndUsesAPlainLanguageNotePrompt() {
        var savedValue: Double? = 99.0
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitHistoryLogDialog(
                    item = progress(HabitTrackingMode.LogOnly),
                    log = null,
                    initialDate = today,
                    onDismiss = {},
                    onSave = { value, _, _, _ -> savedValue = value },
                )
            }
        }

        compose.onNodeWithText("Add an Earlier Entry").assertIsDisplayed()
        compose.onNodeWithText("Number · optional").assertIsDisplayed()
        compose.onNodeWithText("What happened? (optional)").assertIsDisplayed()
        compose.onNodeWithText("Record").assertIsEnabled().performClick()
        assertNull(savedValue)
    }

    @Test
    fun deletingARecordedCheckInRequiresConfirmation() {
        var deleted = false
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitHistoryLogDialog(
                    item = progress(HabitTrackingMode.CheckOff),
                    log = log(),
                    initialDate = today.minusDays(1),
                    onDismiss = {},
                    onSave = { _, _, _, _ -> },
                    onDelete = { deleted = true },
                )
            }
        }

        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete Check-In?").assertIsDisplayed()
        assertEquals(false, deleted)
        compose.onNodeWithTag("habit-history-confirm-delete").performClick()
        assertEquals(true, deleted)
    }

    private fun progress(mode: HabitTrackingMode) = HabitDayProgress(
        habit = habit(mode),
        date = today,
        scheduled = true,
        value = 0.0,
        status = null,
        successful = false,
        checklistItems = emptyList(),
        streak = 0,
        completionRate = 0.0,
        dayState = HabitDayState.Pending,
    )

    private fun habit(mode: HabitTrackingMode) = Habit(
        id = 1,
        uuid = "habit-1",
        metricId = "metric-habit-1",
        name = "Medication",
        notes = "",
        area = "Main",
        tags = emptyList(),
        icon = "💊",
        trackingMode = mode,
        dimension = UnitDimension.Count,
        unitId = "count",
        precision = 0,
        comparison = if (mode == HabitTrackingMode.LogOnly) TargetComparison.None else TargetComparison.AtLeast,
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

    private fun log() = HabitLog(
        id = 2,
        uuid = "log-2",
        habitId = 1,
        value = 1.0,
        canonicalValue = 1.0,
        enteredUnitId = "count",
        status = HabitLogStatus.Success,
        timestamp = Instant.parse("2026-08-29T14:00:00Z"),
        localDate = today.minusDays(1),
        zoneId = "UTC",
        offsetSeconds = 0,
        note = "",
        sourceType = MetricSourceType.Manual,
        sourceId = null,
        metricEntryId = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
