package com.whip.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.room.Room
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.*
import com.whip.app.ui.*
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class HomeHabitAudit2UiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val today = LocalDate.of(2026, 9, 28)

    @Test fun habitUnitChangesSaveConvertedConfigurationAndRetainOriginalHistory() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java).build()
        try {
            val clock = object : WhipClock { override fun now() = Instant.parse("2026-09-28T12:00:00Z") }
            var nextId = 0
            val ids = WhipIdGenerator { "unit-audit-${++nextId}" }
            val measurements = RoomMeasurementRepository(database, clock, ids)
            val repository = RoomHabitRepository(database, measurements, clock, ids)
            val unitId = runBlocking { measurements.createCustomUnit("Audit cup", "cup", UnitDimension.Volume, 250.0) }
            val units = runBlocking { measurements.customUnits.first() }
            val id = runBlocking {
                repository.create(HabitDraft("Water", trackingMode = HabitTrackingMode.Decimal,
                    dimension = UnitDimension.Volume, unitId = "litre", precision = 2,
                    comparison = TargetComparison.WithinRange, targetMin = 2.0, targetMax = 3.0,
                    startDate = today, quickIncrement = 0.5, quickActions = listOf(1.0, 2.0),
                    endType = HabitEndType.AfterTotal, endValue = 10.0)).also { repository.log(it, 1.0) }
            }
            val originalLog = runBlocking { repository.logs.first().single() }
            val existing = mutableStateOf(runBlocking { requireNotNull(repository.get(id)) })
            val generation = mutableStateOf(0)
            var saved: HabitDraft? = null
            compose.setContent { WhipTheme(dynamicColor = false) { key(generation.value) {
                HabitEditorDialog(existing.value, initialChecklist = emptyList(), today = today,
                    onDismiss = {}, onSave = { saved = it }, customUnits = units)
            } } }
            fun chooseUnit(label: String) {
                compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasContentDescription("Unit:", substring = true))
                compose.onNodeWithContentDescription("Unit:", substring = true).performClick()
                compose.onNodeWithText(label).performClick()
            }
            chooseUnit("Audit cup (cup)")
            compose.onNodeWithText("Save").performClick()
            runBlocking { repository.update(id, requireNotNull(saved)) }
            val reopened = runBlocking { requireNotNull(repository.get(id)) }
            assertEquals(unitId, reopened.unitId)
            assertEquals(8.0, reopened.targetMin!!, 0.0)
            assertEquals(12.0, reopened.targetMax!!, 0.0)
            assertEquals(2.0, reopened.quickIncrement, 0.0)
            assertEquals(listOf(4.0, 8.0), reopened.quickActions)
            assertEquals(40.0, reopened.endValue!!, 0.0)
            assertEquals(originalLog, runBlocking { repository.logs.first().single() })
            compose.runOnIdle { existing.value = reopened; generation.value++; saved = null }
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Minimum"))
            compose.onNodeWithText("Minimum").performTextReplacement("-")
            closeSoftKeyboard()
            chooseUnit("litres (L)")
            compose.onNodeWithTag("habit-unit-change-error").assertIsDisplayed()
            compose.onNodeWithContentDescription("Unit: Audit cup (cup)").assertIsDisplayed()
            compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Minimum"))
            compose.onNodeWithText("Minimum").assertTextContains("-").performTextReplacement("8")
            closeSoftKeyboard()
            chooseUnit("litres (L)")
            compose.onNodeWithText("Save").performClick()
            runBlocking { repository.update(id, requireNotNull(saved)) }
            assertEquals(2.0, runBlocking { repository.get(id) }!!.targetMin!!, 0.0)
            assertEquals(originalLog, runBlocking { repository.logs.first().single() })
            captureVisualCatalogSurface("product-audit.habits.converted-unit")
        } finally { database.close() }
    }

    @Test @AndroidFontScale fun secondaryHabitDraftsSurviveDismissalAndRecreation() {
        val restoration = StateRestorationTester(compose)
        val dialog = mutableStateOf(0)
        var dismissals = 0
        restoration.setContent { WhipTheme(dynamicColor = false) { key(dialog.value) {
            val dismiss = { dismissals++; Unit }
            when (dialog.value) {
                0 -> HabitValueDialog(progress(habit().copy(trackingMode = HabitTrackingMode.LogOnly)), dismiss, { _, _ -> })
                1 -> HabitHistoryLogDialog(progress(habit()), log(1, 1.0, HabitLogStatus.Success), today.minusDays(1), dismiss, { _, _, _, _ -> })
                else -> HabitPauseDialog(today, onDismiss = dismiss, onSave = { _, _, _ -> })
            }
        } } }
        listOf("habit-value-note", "habit-history-note", "habit-pause-note").forEachIndexed { index, tag ->
            compose.runOnIdle { dialog.value = index }
            compose.onNodeWithTag(tag).performScrollTo().performTextReplacement("Retain draft $index")
            closeSoftKeyboard()
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithTag(tag).performScrollTo().assertTextContains("Retain draft $index")
            compose.onNodeWithText("Cancel").performClick()
            compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
            compose.onNodeWithText("Keep Editing").performClick()
            compose.onNodeWithTag(tag).performScrollTo().assertTextContains("Retain draft $index")
            closeSoftKeyboard()
            pressBack()
            compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
            compose.assertDialogFontScale()
            if (index == 2) captureVisualCatalogSurface("product-audit.habits.discard-draft.large")
            compose.onNodeWithText("Discard Changes").performClick()
            compose.runOnIdle { assertEquals(index + 1, dismissals) }
        }
    }

    @Test fun historicalNoteEditingPreservesFailedAndModeChangedFacts() {
        val current = mutableStateOf(log(1, 0.0, HabitLogStatus.Failed))
        var saved: Pair<Double?, HabitLogStatus>? = null
        compose.setContent {
            CompositionLocalProvider(LocalWhipToday provides today) {
                WhipTheme(dynamicColor = false) {
                    HabitHistoryLogDialog(progress(habit()), current.value, today.minusDays(1), {},
                        { value, status, _, _ -> saved = value to status })
                }
            }
        }
        listOf(log(1, 0.0, HabitLogStatus.Failed), log(2, null, HabitLogStatus.Failed),
            log(3, 5.0, HabitLogStatus.Recorded), log(4, null, HabitLogStatus.Recorded),
            log(5, 1.234567891, HabitLogStatus.Failed)).forEach { fact ->
            compose.runOnIdle { current.value = fact; saved = null }
            compose.onNodeWithTag("habit-history-note").performScrollTo().performTextReplacement("Only the note changed")
            closeSoftKeyboard()
            compose.onNodeWithText("Save Changes").assertIsEnabled().performClick()
            compose.runOnIdle { assertEquals(fact.value to fact.status, saved) }
        }
        captureVisualCatalogSurface("ux-audit2.habit-history-preserved")
    }

    @Test @AndroidFontScale fun futureHistoryDateRetainsDraftAndOffersReachableCorrection() {
        val restoration = StateRestorationTester(compose)
        val unit = UnitDefinition("long-history-unit", "Concentrated mixture", "scoop of concentrated mixture", UnitDimension.Count, 2.0, custom = true)
        restoration.setContent {
            CompositionLocalProvider(LocalWhipToday provides today) {
                WhipTheme(dynamicColor = false) {
                    HabitHistoryLogDialog(progress(habit().copy(trackingMode = HabitTrackingMode.Count, unitId = unit.id)),
                        log(1, 1.0, HabitLogStatus.Success).copy(enteredUnitId = unit.id), today.plusDays(1), {}, { _, _, _, _ -> }, customUnits = listOf(unit))
                }
            }
        }
        compose.onNodeWithTag("habit-history-note").performScrollTo().performTextReplacement("Retain this correction")
        closeSoftKeyboard()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("habit-history-note").assertTextContains("Retain this correction")
        compose.onNodeWithTag("habit-history-value").performScrollTo().assertTextContains("Amount (${unit.symbol})")
        compose.onNodeWithText("Save Changes").assertIsNotEnabled()
        compose.onNodeWithTag("habit-history-date-error").performScrollTo().assertIsDisplayed()
        compose.assertDialogFontScale()
        captureVisualCatalogSurface("ux-audit2.habit-history-date-error.large")
        compose.onNodeWithText("Date ·", substring = true).performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Choose Date").assertIsDisplayed()
    }

    @Test fun invalidTaskDurationIsRetainedInsteadOfSilentlyReinterpreted() {
        val saved = mutableListOf<TaskDraft>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(TaskEditorRequest(sessionId = 2801L, initialCapture = "Plan an estimate"), {},
                    { _, draft, _ -> saved += draft }, onRequestNotificationPermission = {})
            }
        }
        compose.onNodeWithTag("task-editor-more-details").performScrollTo().performClick()
        for (raw in listOf("0", "1441", "-10", "10000")) {
            compose.onNodeWithTag("task-editor-duration").performScrollTo().performTextReplacement(raw)
            closeSoftKeyboard()
            compose.onNodeWithTag("task-editor-duration").assertTextContains(raw)
            compose.onNodeWithText("Save").performClick()
            compose.runOnIdle { assertTrue(saved.isEmpty()) }
        }
        compose.onNodeWithTag("task-editor-duration").performScrollTo().assertTextContains("10000")
        captureVisualCatalogSurface("ux-audit2.task-duration-error")
        for ((raw, expected) in listOf("1" to 1, "1440" to 1440, "" to null)) {
            compose.onNodeWithTag("task-editor-duration").performScrollTo().performTextReplacement(raw)
            closeSoftKeyboard()
            compose.onNodeWithText("Save").performClick()
            compose.runOnIdle { assertEquals(expected, saved.last().durationMinutes) }
        }
    }

    @Test fun existingHabitOnlyOffersCompatibleMeasurementChanges() {
        val unit = UnitDefinition("custom-volume", "Cup", "cup", UnitDimension.Volume, 0.25, custom = true)
        val existing = habit().copy(trackingMode = HabitTrackingMode.Count, dimension = UnitDimension.Volume, unitId = unit.id)
        var saved: HabitDraft? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitEditorDialog(existing, initialChecklist = emptyList(), today = today, onDismiss = {}, onSave = { saved = it }, customUnits = listOf(unit))
            }
        }
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasTestTag("habit-edit-measurement-contract"))
        compose.onNodeWithTag("habit-edit-measurement-contract").assertIsDisplayed()
        compose.onNodeWithText(HabitTrackingMode.Duration.uiLabel()).assertIsNotEnabled()
        compose.onNodeWithText(HabitTrackingMode.CheckOff.uiLabel()).assertIsNotEnabled()
        compose.onNodeWithText(HabitTrackingMode.Decimal.uiLabel()).performScrollTo().assertIsEnabled().performClick()
        captureVisualCatalogSurface("ux-audit2.habit-edit-capabilities")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertEquals(HabitTrackingMode.Decimal, requireNotNull(saved).trackingMode)
            assertEquals(UnitDimension.Volume, requireNotNull(saved).dimension)
            assertEquals(unit.id, requireNotNull(saved).unitId)
        }
    }

    @Test @AndroidFontScale fun homeSummaryDistinguishesTargetlessRecordsAtLargeText() {
        val summary = listOf(progress(habit()).copy(successful = true, dayState = HabitDayState.Completed),
            progress(habit().copy(id = 2, comparison = TargetComparison.None)).copy(status = HabitLogStatus.Recorded)).homeHabitSummary()
        compose.setContent { WhipTheme(dynamicColor = false) { TodayHeader(today, 0, summary, {}, {}) } }
        compose.onNodeWithTag("home-habit-progress-record").assertContentDescriptionContains("1 of 1 complete", substring = true)
            .assertContentDescriptionContains("1 of 1 check-ins without targets recorded", substring = true).assertIsDisplayed()
        captureVisualCatalogSurface("ux-audit2.home-neutral-habits.large")
    }

    @Test fun customUnitHistoryKeepsTheAuthoredUnitWhileCurrentEntryUsesCurrentUnit() {
        val old = UnitDefinition("archived-cup", "Original cup", "old cup", UnitDimension.Count, 2.0, custom = true, archived = true)
        val current = UnitDefinition("current-cup", "Current cup", "cup", UnitDimension.Count, 4.0, custom = true)
        val units = listOf(old, current)
        val item = progress(habit().copy(trackingMode = HabitTrackingMode.Count, unitId = current.id)).copy(value = 2.5)
        val editingHistory = mutableStateOf(true)
        compose.setContent { WhipTheme(dynamicColor = false) {
            if (editingHistory.value) HabitHistoryLogDialog(item,
                log(7, 5.0, HabitLogStatus.Recorded).copy(enteredUnitId = old.id), today.minusDays(1), {},
                { _, _, _, _ -> editingHistory.value = false }, customUnits = units)
            else HabitValueDialog(item, {}, { _, _ -> }, customUnits = units)
        } }
        compose.onNodeWithTag("habit-history-value").performScrollTo().assertTextContains("Amount (old cup)").assertTextContains("5")
        captureVisualCatalogSurface("ux-audit2.habit-custom-unit-history")
        compose.onNodeWithText("Save Changes").performClick()
        compose.onNodeWithTag("habit-value-input").assertTextContains("Today's Total (cup)").assertTextContains("2.5")
    }

    @Test fun currentPauseKeepsCompletedMissedAndDatedNeutralHistoryVisible() {
        val habit = habit().copy(paused = true, startDate = today.minusDays(4))
        val logs = listOf(log(1, 1.0, HabitLogStatus.Success).copy(localDate = today.minusDays(4)))
        val pauses = listOf(HabitPause(1, habit.id, today.minusDays(2), today.minusDays(2), "Known dated pause"))
        val skips = listOf(HabitSkip("skip", habit.id, today.minusDays(1), 1, 1, 1))
        val state = HabitUiState(all = listOf(progress(habit).copy(scheduled = false, dayState = HabitDayState.Paused, completionRate = 0.5)),
            logs = logs, pauses = pauses, skips = skips, currentDate = today, loading = false)
        compose.setContent { WhipTheme(dynamicColor = false) { HabitInsights(state, lowPressureMode = false) } }
        compose.onNodeWithTag("habit-insights-current-pause-context").performScrollTo().assertTextContains("Past dates use your schedule and saved pause dates.", substring = true)
        for ((offset, label) in listOf(4L to "completed", 3L to "missed", 2L to "paused", 1L to "skipped", 0L to "paused")) {
            val tag = "habit-activity-day-${today.minusDays(offset).toEpochDay()}"
            compose.onNodeWithTag("habit-insights-list").performScrollToNode(hasTestTag(tag))
            compose.onNodeWithTag(tag).assertContentDescriptionContains(label, substring = true).assertIsDisplayed()
        }
        captureVisualCatalogSurface("ux-audit2.habit-pause-history")
    }

    private fun progress(habit: Habit) = HabitDayProgress(habit, today, true, 0.0, null, null, emptyList(), 0, 0.0)
    private fun log(id: Long, value: Double?, status: HabitLogStatus) = HabitLog(id, "log$id", 1, value, value, "count", status,
        Instant.parse("2026-09-27T12:00:00Z"), today.minusDays(1), "UTC", 0, "Original note", MeasurementSourceType.Manual, null, null, 1, 1)
    private fun habit() = Habit(id = 1, uuid = "habit", measurementId = "measure", name = "Habit history", notes = "", area = "", tags = emptyList(), icon = "✓",
        trackingMode = HabitTrackingMode.CheckOff, dimension = UnitDimension.Count, unitId = "count", precision = 0,
        comparison = TargetComparison.AtLeast, targetMin = 1.0, targetMax = null, targetPeriod = TargetPeriod.Day, rollingDays = null,
        scheduleType = HabitScheduleType.Daily, scheduleInterval = 1, weekdays = emptySet(), flexibleTimesPerWeek = null, startDate = today.minusDays(7),
        endType = HabitEndType.Never, endDate = null, endValue = null, quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(),
        weekdayReminderMinutes = emptyMap(), weekStart = DayOfWeek.MONDAY, timerStartedAtMillis = null, pinned = false, position = 0, archived = false, paused = false,
        createdAtMillis = 1, updatedAtMillis = 1)
}
