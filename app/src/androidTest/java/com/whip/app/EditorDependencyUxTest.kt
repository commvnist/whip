package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditorDependencyUxTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun untouchedTaskTitleStaysNeutralUntilSaveExplainsTheRequirement() {
        val saved = AtomicReference<TaskDraft?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(
                    request = TaskEditorRequest(sessionId = 21L),
                    onDismiss = {},
                    onSave = { _, draft, _ -> saved.set(draft) },
                    onRequestNotificationPermission = {},
                )
            }
        }

        compose.onAllNodesWithText("Enter a Task title to save.").assertCountEquals(0)
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        compose.onNodeWithText("Enter a Task title to save.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(null, saved.get()) }
    }

    @Test
    fun optionalReminderStartsUnsetAndUnavailableCadenceExplainsHowToEnableIt() {
        val reminderValues = AtomicReference<Map<java.time.DayOfWeek, List<Int>>>(
            mapOf(java.time.DayOfWeek.MONDAY to emptyList()),
        )
        val selectedAnchor = AtomicReference(RecurrenceAnchor.Schedule)
        val usesSelectedDays = mutableStateOf(true)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    WeekdayReminderEditor(
                        values = reminderValues.get(),
                        onChange = reminderValues::set,
                    )
                    RecurrenceAnchorSelector(
                        selected = selectedAnchor.get(),
                        usesSelectedWeekdays = usesSelectedDays.value,
                        onSelect = selectedAnchor::set,
                    )
                }
            }
        }

        compose.onAllNodesWithText("Monday").assertCountEquals(0)
        compose.onNodeWithText("No weekday-specific reminders.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Add Weekday Reminder").performClick()
        compose.onNodeWithText("Choose Weekday").assertIsDisplayed()
        compose.onNodeWithText("Nothing is scheduled until you choose a time and tap Add.").assertIsDisplayed()
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEach { day ->
            compose.onNodeWithText(day).assertIsDisplayed()
        }
        compose.onNodeWithText("Wednesday").performClick()
        compose.onNodeWithText("Wednesday Reminder").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(reminderValues.get().values.all { it.isEmpty() })

        compose.onNodeWithText("Next repeat is based on").assertIsDisplayed()
        compose.onNodeWithText("Completion Date").assertIsNotEnabled()
        compose.onNodeWithText("Under Repeats, choose Daily or a custom interval", substring = true).assertIsDisplayed()

        compose.runOnIdle { usesSelectedDays.value = false }
        compose.onNodeWithText("Completion Date").assertIsEnabled().performClick()
        assertEquals(RecurrenceAnchor.Completion, selectedAnchor.get())
    }

    @Test
    fun repeatSettingsStayBetweenScheduleChoiceAndPlanningFields() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(
                    request = TaskEditorRequest(sessionId = 42L),
                    onDismiss = {},
                    onSave = { _, _, _ -> },
                    onRequestNotificationPermission = {},
                    powerMode = false,
                )
            }
        }

        compose.onNodeWithTag("task-repeat-toggle").performScrollTo().performClick()
        compose.onNodeWithText("Schedule and Repeat").performClick()
        compose.onNodeWithTag("task-schedule-consequence").assertIsDisplayed()
        compose.onNodeWithText("This Task repeats from its start date", substring = true).assertIsDisplayed()

        compose.onNodeWithText("Repeats").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Planning").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Subtasks").assertCountEquals(0)
        compose.onNodeWithText("Planning Details").performScrollTo().performClick()
        compose.onNodeWithText("Subtasks").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sourcePlacementIsVisibleAndAnytimeCannotSaveHiddenReminderData() {
        val saved = AtomicReference<TaskDraft?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(
                    request = TaskEditorRequest(
                        initialPlacement = TaskPlacement.Anytime,
                        sessionId = 84L,
                    ),
                    onDismiss = {},
                    onSave = { _, draft, _ -> saved.set(draft) },
                    onRequestNotificationPermission = {},
                )
            }
        }

        compose.onNodeWithTag("task-editor-title").performTextInput("Unscheduled Errand")
        compose.onNodeWithText("Anytime keeps this Task ready but unscheduled.").assertIsDisplayed()
        compose.onAllNodesWithTag("task-time-toggle").assertCountEquals(0)
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(ScheduleKind.Anytime, saved.get()?.scheduleKind)
            assertEquals(false, saved.get()?.inbox)
            assertEquals(null, saved.get()?.timeMinutes)
            assertEquals(false, saved.get()?.reminderEnabled)
        }
    }

    @Test
    fun taskEditorChoosesAndSavesSharedIdentityEmoji() {
        val saved = AtomicReference<TaskDraft?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(
                    request = TaskEditorRequest(sessionId = 105L),
                    onDismiss = {},
                    onSave = { _, draft, _ -> saved.set(draft) },
                    onRequestNotificationPermission = {},
                )
            }
        }

        compose.onNodeWithTag("task-editor-title").performTextInput("Clean Room")
        compose.onNodeWithTag("emoji-picker-trigger").performClick()
        compose.onNodeWithTag("emoji-preset-Cleaning").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle { assertEquals("🧹", saved.get()?.icon) }
    }

    @Test
    fun movingScheduledTaskToAnytimePreviewsAndClearsConsequences() {
        val saved = AtomicReference<TaskDraft?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(
                    request = TaskEditorRequest(
                        initialPlacement = TaskPlacement.Scheduled,
                        sessionId = 126L,
                    ),
                    onDismiss = {},
                    onSave = { _, draft, _ -> saved.set(draft) },
                    onRequestNotificationPermission = {},
                )
            }
        }

        compose.onNodeWithTag("task-editor-title").performTextInput("Move Me")
        compose.onNodeWithTag("task-time-toggle").performScrollTo().performClick()
        compose.onNodeWithText("Anytime").performScrollTo().performClick()
        compose.onNodeWithText("Remove Scheduling Details?").assertIsDisplayed()
        compose.onNodeWithText("The Scheduled Date will be removed.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Move to Anytime").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(ScheduleKind.Anytime, saved.get()?.scheduleKind)
            assertEquals(null, saved.get()?.timeMinutes)
            assertEquals(false, saved.get()?.reminderEnabled)
        }
    }

    @Test
    fun dependentSettingsNoticeExplainsItsRelationshipToAccessibilityServices() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                DependentSettingsNotice(
                    message = "Choosing Repeat reveals cadence controls below.",
                    testTag = "dependency-notice",
                )
            }
        }

        compose.onNodeWithTag("dependency-notice").assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "Dependent settings. Choosing Repeat reveals cadence controls below.",
        ).assertIsDisplayed()
    }

    @Test
    fun datePickerHonorsWhipWeekStartAndExposesFullDateSemantics() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipDatePickerDialog(
                    initialDate = LocalDate.of(2026, 8, 23),
                    onDismiss = {},
                    onDateSelected = {},
                    firstDayOfWeek = DayOfWeek.MONDAY,
                )
            }
        }

        val mondayLeft = compose.onNodeWithText("Mo").fetchSemanticsNode().boundsInRoot.left
        val sundayLeft = compose.onNodeWithText("Su").fetchSemanticsNode().boundsInRoot.left
        assertTrue("Monday must be the first calendar column", mondayLeft < sundayLeft)
        compose.onNodeWithContentDescription("Previous Month").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next Month").assertIsDisplayed()
        compose.onNodeWithContentDescription("Select August 23, 2026").assertIsDisplayed()
    }

    @Test
    fun datePickerSupportsSundayFirstAndReturnsTheExplicitSelection() {
        val selected = AtomicReference<LocalDate?>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipDatePickerDialog(
                    initialDate = LocalDate.of(2026, 8, 23),
                    onDismiss = {},
                    onDateSelected =(selected::set),
                    firstDayOfWeek = DayOfWeek.SUNDAY,
                )
            }
        }

        val sundayLeft = compose.onNodeWithText("Su").fetchSemanticsNode().boundsInRoot.left
        val mondayLeft = compose.onNodeWithText("Mo").fetchSemanticsNode().boundsInRoot.left
        assertTrue("Sunday must be the first calendar column", sundayLeft < mondayLeft)
        compose.onNodeWithContentDescription("Select August 24, 2026").performClick()
        compose.onNodeWithText("Set").performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2026, 8, 24), selected.get()) }
    }

    @Test
    fun datePickerJumpUsesYearMonthDayWheelsAndClampsInvalidDays() {
        val selected = AtomicReference<LocalDate?>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipDatePickerDialog(
                    initialDate = LocalDate.of(2024, 1, 31),
                    onDismiss = {},
                    onDateSelected = selected::set,
                )
            }
        }

        compose.onNodeWithTag("date-picker-month-year").performClick()
        compose.onNodeWithTag("date-picker-wheel-selector").assertIsDisplayed()
        compose.onNodeWithContentDescription("Year picker").assertIsDisplayed()
        compose.onNodeWithContentDescription("Month picker").assertIsDisplayed()
        compose.onNodeWithContentDescription("Day picker").assertIsDisplayed()

        compose.onNodeWithTag("date-picker-month-2").performClick()
        compose.onNodeWithText("February 29, 2024").assertIsDisplayed()
        compose.onNodeWithTag("date-picker-year-2025").performClick()
        compose.onNodeWithText("February 28, 2025").assertIsDisplayed()

        compose.onNodeWithTag("date-picker-year-wheel")
            .performScrollToIndex(2014)
        compose.onNodeWithTag("date-picker-year-2015").performClick()
        compose.onNodeWithTag("date-picker-month-wheel")
            .performScrollToIndex(5)
        compose.onNodeWithTag("date-picker-month-6").performClick()
        compose.onNodeWithTag("date-picker-day-wheel")
            .performScrollToIndex(19)
        compose.onNodeWithTag("date-picker-day-20").performClick()
        compose.onNodeWithText("June 20, 2015").assertIsDisplayed()
        compose.onNodeWithText("Set").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2015, 6, 20), selected.get()) }
    }
}
