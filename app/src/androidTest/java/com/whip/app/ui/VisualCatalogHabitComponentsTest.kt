package com.whip.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualCatalogHabitComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 9, 6)

    @Test
    fun captureHabitComponentCatalog() {
        val fixture = mutableStateOf(HabitFixture.CreateEditor)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    key(fixture.value) { HabitCatalogFixture(fixture.value, today) }
                }
            }
        }

        capture("habits.editor.create")
        show(fixture, HabitFixture.EditEditor)
        capture("habits.editor.edit")
        show(fixture, HabitFixture.PauseCreate)
        capture("habits.pause-create")
    }

    private fun show(state: androidx.compose.runtime.MutableState<HabitFixture>, value: HabitFixture) {
        compose.runOnIdle { state.value = value }
        compose.waitForIdle()
    }

    private fun capture(surfaceId: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface(surfaceId)
    }
}

private enum class HabitFixture { CreateEditor, EditEditor, PauseCreate }

@androidx.compose.runtime.Composable
private fun HabitCatalogFixture(fixture: HabitFixture, today: LocalDate) {
    when (fixture) {
        HabitFixture.CreateEditor -> HabitEditorDialog(
            habit = null,
            initialChecklist = emptyList(),
            today = today,
            onDismiss = {},
            onSave = {},
        )
        HabitFixture.EditEditor -> HabitEditorDialog(
            habit = catalogHabit(today),
            initialChecklist = emptyList(),
            today = today,
            onDismiss = {},
            onSave = {},
        )
        HabitFixture.PauseCreate -> HabitPauseDialog(
            today = today,
            onDismiss = {},
            onSave = { _, _, _ -> },
        )
    }
}

private fun catalogHabit(today: LocalDate) = Habit(
    id = 41,
    uuid = "habit-41",
    measurementId = "measurement-habit-41",
    name = "Morning medication",
    notes = "Take with breakfast.",
    area = "Health",
    tags = listOf("morning"),
    icon = "💊",
    trackingMode = HabitTrackingMode.CheckOff,
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
    startDate = today.minusDays(30),
    endType = HabitEndType.Never,
    endDate = null,
    endValue = null,
    quickIncrement = 1.0,
    quickActions = emptyList(),
    reminderMinutes = listOf(8 * 60),
    weekdayReminderMinutes = emptyMap(),
    weekStart = DayOfWeek.MONDAY,
    timerStartedAtMillis = null,
    pinned = false,
    position = 0,
    archived = false,
    paused = false,
    createdAtMillis = 1,
    updatedAtMillis = 2,
)
