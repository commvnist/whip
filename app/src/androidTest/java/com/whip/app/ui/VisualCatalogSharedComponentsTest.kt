package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.startup.StartupBlockReason
import com.whip.app.startup.StartupRecoveryState
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualCatalogSharedComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureSharedComponentCatalog() {
        val fixture = mutableStateOf(SharedFixture.FirstRun)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    key(fixture.value) {
                        SharedCatalogFixture(fixture.value)
                    }
                }
            }
        }

        capture("shared.first-run.setup")
        show(fixture, SharedFixture.StartupFailure)
        capture("shared.startup-recovery.failure")
        show(fixture, SharedFixture.DomainLoading)
        capture("shared.domain.loading")
        show(fixture, SharedFixture.DomainFailure)
        capture("shared.domain.failure")
        show(fixture, SharedFixture.Inspector)
        capture("shared.inspector.base")

        show(fixture, SharedFixture.EmojiPicker)
        compose.onNodeWithTag("emoji-picker-trigger").performClick()
        capture("shared.identity-emoji.picker")
        compose.onNodeWithTag("emoji-picker-custom-option").performClick()
        capture("shared.identity-emoji.custom")

        show(fixture, SharedFixture.ColorPicker)
        capture("shared.color-picker")
        show(fixture, SharedFixture.DatePicker)
        capture("shared.date-picker")
        show(fixture, SharedFixture.ClockPicker)
        capture("shared.clock-picker")

        show(fixture, SharedFixture.WeekdayReminders)
        compose.onNodeWithText("Add Weekday Reminder").performClick()
        capture("shared.weekday-reminders")

        show(fixture, SharedFixture.UnitMenu)
        compose.onNodeWithContentDescription("Unit: Repetitions (reps)").performClick()
        capture("shared.unit.menu")
        show(fixture, SharedFixture.DisclosureMenu)
        compose.onNodeWithText("Today").performClick()
        capture("shared.disclosure.menu")
        show(fixture, SharedFixture.OverflowMenu)
        capture("shared.overflow.menu")

        show(fixture, SharedFixture.Unsaved)
        capture("shared.unsaved-changes")
        show(fixture, SharedFixture.LaunchQueue)
        capture("shared.launch-queue-overflow")
        show(fixture, SharedFixture.PermanentDelete)
        capture("shared.permanent-delete")
    }

    private fun show(state: androidx.compose.runtime.MutableState<SharedFixture>, value: SharedFixture) {
        compose.runOnIdle { state.value = value }
        compose.waitForIdle()
    }

    private fun capture(surfaceId: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface(surfaceId)
    }
}

private enum class SharedFixture {
    FirstRun,
    StartupFailure,
    DomainLoading,
    DomainFailure,
    Inspector,
    EmojiPicker,
    ColorPicker,
    DatePicker,
    ClockPicker,
    WeekdayReminders,
    UnitMenu,
    DisclosureMenu,
    OverflowMenu,
    Unsaved,
    LaunchQueue,
    PermanentDelete,
}

@androidx.compose.runtime.Composable
private fun SharedCatalogFixture(fixture: SharedFixture) {
    Box(Modifier.fillMaxSize()) {
        when (fixture) {
            SharedFixture.FirstRun -> FirstRunSetupDialog(onComplete = { _, _, _, _, _ -> }, onUseDefaults = {})
            SharedFixture.StartupFailure -> StartupRecoveryScreen(
                state = StartupRecoveryState.Blocked(StartupBlockReason.Recovery),
                onRetry = {},
            )
            SharedFixture.DomainLoading -> DomainLoadContent("Habits", PaddingValues())
            SharedFixture.DomainFailure -> DomainLoadContent(
                domain = "Habits",
                innerPadding = PaddingValues(),
                errorMessage = "Whip couldn't load your Habits. Your saved data has not been changed.",
            )
            SharedFixture.Inspector -> EntityInspector(
                entityType = "Habit",
                title = "Morning medication",
                emoji = "💊",
                context = "Main · Daily",
                status = "Ready Today",
                sections = listOf(
                    EntityInspectorSection("today", "Today"),
                    EntityInspectorSection("history", "History"),
                    EntityInspectorSection("options", "Options"),
                ),
                selectedSectionId = "today",
                onSelectSection = {},
                onDismiss = {},
                onEdit = {},
            ) {
                EntityInspectorGroup("Today's Check-in", supportingText = "Your daily action and context stay together.") {
                    Text("0 logged today")
                }
            }
            SharedFixture.EmojiPicker -> Column(Modifier.fillMaxWidth().padding(24.dp)) {
                WhipEmojiPicker(value = "💊", defaultEmoji = "✨", onValueChange = {})
            }
            SharedFixture.ColorPicker -> WhipColorPickerDialog(
                title = "Choose Habit Color",
                initialColor = 0xFF5B8DEF,
                onDismiss = {},
                onConfirm = {},
            )
            SharedFixture.DatePicker -> WhipDatePickerDialog(
                initialDate = LocalDate.of(2026, 9, 6),
                onDismiss = {},
                onDateSelected = {},
                firstDayOfWeek = DayOfWeek.MONDAY,
                preferWheelSelector = false,
            )
            SharedFixture.ClockPicker -> ClockPickerDialog(
                title = "Choose Reminder Time",
                initialMinutes = 8 * 60 + 30,
                onDismiss = {},
                onSet = {},
            )
            SharedFixture.WeekdayReminders -> Column(Modifier.fillMaxWidth().padding(24.dp)) {
                WeekdayReminderEditor(
                    values = mapOf(DayOfWeek.MONDAY to listOf(8 * 60)),
                    onChange = {},
                )
            }
            SharedFixture.UnitMenu -> Column(Modifier.fillMaxWidth().padding(24.dp)) {
                UnitSelectionField(
                    units = listOf(
                        UnitDefinition("reps", "Repetitions", "reps", UnitDimension.Count, 1.0),
                        UnitDefinition("times", "Times", "×", UnitDimension.Count, 1.0),
                    ),
                    selectedUnitId = "reps",
                    dimension = UnitDimension.Count,
                    onSelect = {},
                    onCreateUnit = UnavailableCreateCustomUnitAction,
                )
            }
            SharedFixture.DisclosureMenu -> Column(Modifier.fillMaxWidth().padding(24.dp)) {
                SelectionField(
                    label = "View",
                    values = listOf("Today", "Upcoming", "History"),
                    selected = "Today",
                    valueText = { it },
                    onSelect = {},
                )
            }
            SharedFixture.OverflowMenu -> Box(Modifier.fillMaxSize().padding(24.dp)) {
                WhipOverflowMenu(
                    label = "More Task actions",
                    expanded = true,
                    onExpandedChange = {},
                ) {
                    WhipMenuItem("Edit", {})
                    WhipMenuItem("Duplicate", {})
                    WhipMenuItem("Archive", {})
                }
            }
            SharedFixture.Unsaved -> UnsavedChangesDialog("Habit", {}, {})
            SharedFixture.LaunchQueue -> {
                val state = LaunchQueueOverflowState().apply { admit(41L, 2) }
                LaunchQueueOverflowDialog(state, Modifier)
            }
            SharedFixture.PermanentDelete -> PermanentDeleteDialog(
                title = "Delete This Item Permanently?",
                impacts = listOf("The item and its settings", "12 recorded history entries"),
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}
