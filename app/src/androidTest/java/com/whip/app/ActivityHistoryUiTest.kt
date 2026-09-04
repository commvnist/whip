package com.whip.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.HabitDayState
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.HabitHistoryLogDialog
import com.whip.app.ui.HabitActionsDialog
import com.whip.app.ui.HabitAreaContent
import com.whip.app.ui.HabitInsights
import com.whip.app.ui.HabitPauseDialog
import com.whip.app.ui.HabitUiState
import com.whip.app.ui.HabitViewModel
import com.whip.app.ui.HabitValueDialog
import com.whip.app.ui.LocalWhipDialogPlacement
import com.whip.app.ui.LocalWhipZone
import com.whip.app.ui.WhipDialogPlacement
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun valueDraftIsShieldedDuringPersistenceAndRetainedAfterFailure() {
        var saving by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitValueDialog(
                    item = progress(HabitTrackingMode.Count),
                    onDismiss = {},
                    onLog = { _, _ -> saving = true },
                    saving = saving,
                    persistenceError = error,
                )
            }
        }

        compose.onNodeWithTag("habit-value-input").performTextReplacement("6")
        compose.onNodeWithTag("habit-value-note").performTextReplacement("Felt strong")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithTag("persistence-saving-overlay")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Editing is temporarily unavailable", substring = true)
        compose.onAllNodesWithTag("habit-value-input").assertCountEquals(0)
        pressBack()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()

        compose.runOnIdle {
            saving = false
            error = "Storage unavailable"
        }
        compose.onNodeWithTag("habit-value-save-problem").assertIsDisplayed()
        compose.onNodeWithTag("habit-value-input").assertTextContains("6")
        compose.onNodeWithTag("habit-value-note").assertTextContains("Felt strong")
        compose.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun absoluteValueEditorExplainsThatALowerNumberSetsRatherThanAdds() {
        var savedValue: Double? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitValueDialog(
                    item = progress(HabitTrackingMode.Count).copy(value = 5.0),
                    onDismiss = {},
                    onLog = { value, _ -> savedValue = value },
                )
            }
        }

        compose.onNodeWithText("Set Today's Total").assertIsDisplayed()
        compose.onNodeWithText("Saving sets this total; it does not add to it.", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("habit-value-input").performTextReplacement("3")
        compose.onNodeWithTag("habit-value-input").assertTextContains("3")
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle { assertEquals(3.0, savedValue ?: -1.0, 0.0) }
    }

    @Test
    fun editedHistoryDraftWaitsForItsExactOutcomeAndSurvivesFailure() {
        var saving by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitHistoryLogDialog(
                    item = progress(HabitTrackingMode.Count),
                    log = log().copy(value = 1.0, status = HabitLogStatus.Recorded, note = "Original"),
                    initialDate = today.minusDays(1),
                    onDismiss = {},
                    onSave = { _, _, _, _ -> saving = true },
                    saving = saving,
                    persistenceError = error,
                )
            }
        }

        compose.onNodeWithTag("habit-history-value").performTextReplacement("4")
        compose.onNodeWithTag("habit-history-note").performTextReplacement("Corrected")
        compose.onNodeWithText("Save Changes").performClick()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.onAllNodesWithTag("habit-history-value").assertCountEquals(0)

        compose.runOnIdle {
            saving = false
            error = "History could not be updated"
        }
        compose.onNodeWithTag("habit-history-save-problem").assertIsDisplayed()
        compose.onNodeWithTag("habit-history-value").assertTextContains("4")
        compose.onNodeWithTag("habit-history-note").assertTextContains("Corrected")
        compose.onNodeWithText("Save Changes").assertIsEnabled()
    }

    @Test
    fun scheduledPauseSupportsNoEndDateRetainsFailureDraftAndConfirmsDelete() {
        var saving by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        var savedEnd: LocalDate? = today
        var deleted = false
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitPauseDialog(
                    today = today,
                    pause = HabitPause(8, 1, today, today.plusDays(3), "Travel"),
                    onDismiss = {},
                    onSave = { _, end, _ ->
                        savedEnd = end
                        saving = true
                    },
                    onDelete = { deleted = true },
                    saving = saving,
                    persistenceError = error,
                )
            }
        }

        compose.onNodeWithTag("habit-pause-no-end").performClick()
        compose.onNodeWithTag("habit-pause-note").performTextReplacement("Long recovery")
        compose.onNodeWithTag("habit-pause-save").performClick()
        compose.runOnIdle { assertNull(savedEnd) }
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()

        compose.runOnIdle {
            saving = false
            error = "Storage unavailable"
        }
        compose.onNodeWithTag("habit-pause-save-problem").assertIsDisplayed()
        compose.onNodeWithText("No end date", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("habit-pause-history-impact").assertIsDisplayed()
        compose.onNodeWithTag("habit-pause-note").assertTextContains("Long recovery")
        compose.onNodeWithTag("habit-pause-delete").performClick()
        compose.onNodeWithText("Delete Scheduled Pause?").assertIsDisplayed()
        compose.onNodeWithText("unlogged past dates may become missed", substring = true).assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, deleted) }
        compose.onNodeWithTag("habit-pause-confirm-delete").performClick()
        compose.runOnIdle { assertEquals(true, deleted) }
    }

    @Test
    fun inspectorMakesScheduledPausesEditableAndHistoricalSkipsUndoable() {
        val upcomingPause = HabitPause(8, 1, today.plusDays(1), null, "Travel")
        val pastPause = HabitPause(9, 1, today.minusDays(5), today.minusDays(3), "Recovery")
        val skippedDate = today.minusDays(2)
        val skip = HabitSkip("skip-1", 1, skippedDate, 1L, 1L, 1L)
        var editedPauseId: Long? = null
        var undoneSkipDate: LocalDate? = null
        var saving by mutableStateOf(false)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitActionsDialog(
                    item = progress(HabitTrackingMode.CheckOff),
                    onDismiss = {},
                    onEdit = {},
                    onDuplicate = {},
                    onPin = {},
                    onPause = {},
                    onSchedulePause = {},
                    onQuick = {},
                    onSkip = {},
                    onUndoSkip = {},
                    onUndoHistoricalSkip = { undoneSkipDate = it; saving = true },
                    logs = emptyList(),
                    skips = listOf(skip),
                    pauses = listOf(upcomingPause, pastPause),
                    onAddHistoricalLog = {},
                    onEditLog = {},
                    onEditPause = { editedPauseId = it.id },
                    onArchive = {},
                    onDelete = {},
                    mutationSaving = saving,
                )
            }
        }

        compose.onNodeWithText("Options").performClick()
        compose.onNodeWithTag("entity-inspector-action-pause-8").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(8L, editedPauseId) }

        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("Habit History").assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-action-pause-history-9")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertEquals(9L, editedPauseId) }
        compose.onNodeWithTag("entity-inspector-action-skip-${skippedDate.toEpochDay()}")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertEquals(skippedDate, undoneSkipDate) }
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.onAllNodesWithTag("entity-inspector-action-skip-${skippedDate.toEpochDay()}")
            .assertCountEquals(0)
    }

    @Test
    fun pauseOnlyHabitShowsNeutralInsightsInsteadOfClaimingThereIsNoActivity() {
        val pause = HabitPause(8, 1, today, today, "Recovery")
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitInsights(
                    state = HabitUiState(
                        all = listOf(progress(HabitTrackingMode.CheckOff)),
                        pauses = listOf(pause),
                        currentDate = today,
                        loading = false,
                    ),
                    lowPressureMode = false,
                )
            }
        }

        compose.onAllNodesWithText("No activity yet", substring = true).assertCountEquals(0)
        compose.onNodeWithText("30-day completion: No scored periods").assertIsDisplayed()
        compose.onNodeWithText("Last 30 Days: 0 Completed · 0 Skipped · 0 Missed/Below Target")
            .assertIsDisplayed()
        compose.onNodeWithTag("habit-activity-day-${today.toEpochDay()}")
            .assertContentDescriptionContains("paused", substring = true)
    }

    @Test
    fun scheduledPauseHasNoMisleadingCheckInAction() {
        var quickRequested = false
        val paused = progress(HabitTrackingMode.CheckOff).copy(
            scheduled = false,
            dayState = HabitDayState.Paused,
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitActionsDialog(
                    item = paused,
                    onDismiss = {}, onEdit = {}, onDuplicate = {}, onPin = {}, onPause = {},
                    onSchedulePause = {}, onQuick = { quickRequested = true }, onSkip = {}, onUndoSkip = {},
                    onUndoHistoricalSkip = {}, logs = emptyList(), skips = emptyList(), pauses = emptyList(),
                    onAddHistoricalLog = {}, onEditLog = {}, onEditPause = {}, onArchive = {}, onDelete = {},
                )
            }
        }

        compose.onNodeWithText("Paused").assertIsDisplayed()
        compose.onNodeWithText("Paused today. No check-in is expected.").assertIsDisplayed()
        compose.onAllNodesWithTag("entity-inspector-primary-check-in").assertCountEquals(0)
        compose.onAllNodesWithTag("entity-inspector-primary-check-in-outside-schedule").assertCountEquals(0)
        compose.runOnIdle { assertEquals(false, quickRequested) }
    }

    @Test
    fun offScheduleLoggingRequiresAnExplicitInspectorAction() {
        var quickRequested = false
        val offSchedule = progress(HabitTrackingMode.Count).copy(
            scheduled = false,
            dayState = HabitDayState.NotScheduled,
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitActionsDialog(
                    item = offSchedule,
                    onDismiss = {}, onEdit = {}, onDuplicate = {}, onPin = {}, onPause = {},
                    onSchedulePause = {}, onQuick = { quickRequested = true }, onSkip = {}, onUndoSkip = {},
                    onUndoHistoricalSkip = {}, logs = emptyList(), skips = emptyList(), pauses = emptyList(),
                    onAddHistoricalLog = {}, onEditLog = {}, onEditPause = {}, onArchive = {}, onDelete = {},
                )
            }
        }

        compose.onNodeWithTag("entity-inspector-primary-check-in-outside-schedule")
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertEquals(true, quickRequested) }
    }

    @Test
    fun timerStartTimeUsesWhipsConfiguredZone() {
        val zone = ZoneId.of("Asia/Tokyo")
        val startedAt = Instant.parse("2026-08-30T01:30:00Z")
        val expectedTime = startedAt.atZone(zone).toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        val timer = progress(HabitTrackingMode.Duration).copy(
            habit = habit(HabitTrackingMode.Duration).copy(
                dimension = UnitDimension.Duration,
                unitId = "second",
                timerStartedAtMillis = startedAt.toEpochMilli(),
                timerSessionId = "session-1",
            ),
        )
        compose.setContent {
            CompositionLocalProvider(LocalWhipZone provides zone) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    HabitActionsDialog(
                        item = timer,
                        onDismiss = {}, onEdit = {}, onDuplicate = {}, onPin = {}, onPause = {},
                        onSchedulePause = {}, onQuick = {}, onSkip = {}, onUndoSkip = {},
                        onUndoHistoricalSkip = {}, logs = emptyList(), skips = emptyList(), pauses = emptyList(),
                        onAddHistoricalLog = {}, onEditLog = {}, onEditPause = {}, onArchive = {}, onDelete = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Timer started $expectedTime", substring = true).assertIsDisplayed()
    }

    @Test
    fun removedLogSnapshotKeepsHabitHistoryEditorRestorableUntilRequestDelivery() {
        var state by mutableStateOf(
            HabitUiState(
                today = listOf(progress(HabitTrackingMode.Count)),
                all = listOf(progress(HabitTrackingMode.Count)),
                logs = listOf(log().copy(status = HabitLogStatus.Recorded, note = "Retained after commit")),
                currentDate = today,
                loading = false,
            ),
        )
        var openRequest by mutableStateOf<Long?>(1L)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                val habitViewModel: HabitViewModel = viewModel()
                HabitAreaContent(
                    state = state,
                    innerPadding = PaddingValues(),
                    viewModel = habitViewModel,
                    openHabitIdRequest = openRequest,
                    onOpenHabitRequestConsumed = { openRequest = null },
                    showWorkspace = false,
                )
            }
        }

        compose.onNodeWithText("History").performClick()
        compose.onNodeWithTag("entity-inspector-action-log-2").performClick()
        compose.onNodeWithTag("habit-history-dialog").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(logs = emptyList()) }
        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("habit-history-dialog").assertIsDisplayed()
        compose.onNodeWithTag("habit-history-note").assertTextContains("Retained after commit")
    }

    @Test
    fun removedPauseSnapshotKeepsHabitPauseEditorRestorableUntilRequestDelivery() {
        val pause = HabitPause(8, 1, today.plusDays(1), null, "Retained recovery")
        var state by mutableStateOf(
            HabitUiState(
                today = listOf(progress(HabitTrackingMode.Count)),
                all = listOf(progress(HabitTrackingMode.Count)),
                pauses = listOf(pause),
                currentDate = today,
                loading = false,
            ),
        )
        var openRequest by mutableStateOf<Long?>(1L)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                val habitViewModel: HabitViewModel = viewModel()
                HabitAreaContent(
                    state = state,
                    innerPadding = PaddingValues(),
                    viewModel = habitViewModel,
                    openHabitIdRequest = openRequest,
                    onOpenHabitRequestConsumed = { openRequest = null },
                    showWorkspace = false,
                )
            }
        }

        compose.onNodeWithText("Options").performClick()
        compose.onNodeWithTag("entity-inspector-action-pause-8").performScrollTo().performClick()
        compose.onNodeWithTag("habit-pause-dialog").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(pauses = emptyList()) }
        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("habit-pause-dialog").assertIsDisplayed()
        compose.onNodeWithTag("habit-pause-note").assertTextContains("Retained recovery")
    }

    @Test
    fun pauseEditorRemainsScrollableAndActionableAt320DpAndTwoHundredPercentText() {
        val largeText = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides largeText,
                LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
            ) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    HabitPauseDialog(
                        today = today,
                        pause = HabitPause(8, 1, today, null, "A long recovery note that must remain reachable"),
                        onDismiss = {},
                        onSave = { _, _, _ -> },
                        onDelete = {},
                        persistenceError = "Storage is temporarily unavailable; review the retained draft and try again.",
                    )
                }
            }
        }

        val dialog = compose.onNodeWithTag("habit-pause-dialog").getUnclippedBoundsInRoot()
        assertTrue(dialog.right - dialog.left <= 321.dp)
        compose.onNodeWithTag("habit-pause-note").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("habit-pause-save").assertIsDisplayed().assertIsEnabled()
        val save = compose.onNodeWithTag("habit-pause-save").getUnclippedBoundsInRoot()
        assertTrue(save.bottom - save.top >= 48.dp)
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
        sourceType = MeasurementSourceType.Manual,
        sourceId = null,
        measurementEntryId = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
