package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TaskPriority
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
        compose.onNodeWithTag("task-save-problem").assertExists()
        compose.onNodeWithText("Task title is required").assertIsDisplayed()
        compose.runOnIdle { assertEquals(null, saved.get()) }
    }

    @Test
    fun smartCaptureHighlightsPreviewsAndAppliesEverySupportedDetailBeforeSave() {
        val saved = AtomicReference<TaskDraft?>(null)
        val today = LocalDate.of(2026, 8, 25)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(
                    request = TaskEditorRequest(sessionId = 22L),
                    onDismiss = {},
                    onSave = { _, draft, _ -> saved.set(draft) },
                    onRequestNotificationPermission = {},
                    naturalLanguageCapture = true,
                    today = today,
                )
            }
        }

        compose.onNodeWithTag("task-editor-title").performTextInput(
            "Plan launch every 2 weeks on 2026-09-01 at 9am deadline 2026-10-01 !high for 45m light effort #work remind me",
        )
        compose.onNodeWithTag("task-editor-title").assert(
            SemanticsMatcher("describes every highlighted Smart Capture assumption") { node ->
                val description = node.config[SemanticsProperties.StateDescription]
                listOf(
                    "every 2 weeks",
                    "2026-09-01",
                    "9:00 AM",
                    "2026-10-01",
                    "Priority · High",
                    "Duration · 45 min",
                    "Effort · Light",
                    "Tag · #work",
                    "Reminder · At scheduled time",
                ).all(description::contains)
            },
        )
        compose.onNodeWithTag("smart-task-editor-preview").assertIsDisplayed()
        compose.onNodeWithText("Repeat · every 2 weeks").assertIsDisplayed()
        compose.onNodeWithText("Schedule · 2026-09-01").assertIsDisplayed()
        compose.onNodeWithText("Time · 9:00 AM").assertIsDisplayed()
        compose.onNodeWithText("Deadline · 2026-10-01").assertIsDisplayed()
        compose.onNodeWithText("Priority · High").assertIsDisplayed()
        compose.onNodeWithText("Duration · 45 min").assertIsDisplayed()
        compose.onNodeWithText("Effort · Light").assertIsDisplayed()
        compose.onNodeWithText("Tag · #work").assertIsDisplayed()
        compose.onNodeWithText("Reminder · At scheduled time").assertIsDisplayed()

        compose.onNodeWithTag("smart-task-capture-apply").performClick()
        compose.onNodeWithTag("task-editor-title").assertTextContains("Plan launch")
        compose.onNodeWithTag("smart-task-capture-applied").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            val draft = requireNotNull(saved.get())
            assertEquals("Plan launch", draft.title)
            assertEquals(ScheduleKind.Recurring, draft.scheduleKind)
            assertEquals(null, draft.date)
            assertEquals(LocalDate.of(2026, 9, 1), draft.recurrence?.startDate)
            assertEquals(RecurrenceUnit.Weeks, draft.recurrence?.unit)
            assertEquals(2, draft.recurrence?.interval)
            assertEquals(LocalDate.of(2026, 10, 1), draft.deadline)
            assertEquals(9 * 60, draft.timeMinutes)
            assertEquals(true, draft.reminderEnabled)
            assertEquals(listOf(0), draft.reminderOffsetsMinutes)
            assertEquals(TaskPriority.High, draft.priority)
            assertEquals(45, draft.durationMinutes)
            assertEquals(TaskEffort.Light, draft.effort)
            assertEquals(setOf("work"), draft.tags)
        }
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
    fun inboxPlacementIsVisibleAndCannotSaveHiddenReminderData() {
        val saved = AtomicReference<TaskDraft?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskEditorDialog(
                    request = TaskEditorRequest(
                        initialPlacement = TaskPlacement.Inbox,
                        sessionId = 84L,
                    ),
                    onDismiss = {},
                    onSave = { _, draft, _ -> saved.set(draft) },
                    onRequestNotificationPermission = {},
                )
            }
        }

        compose.onNodeWithTag("task-editor-title").performTextInput("Unscheduled Errand")
        compose.onNodeWithText("Inbox keeps this Task unscheduled", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("Anytime").assertCountEquals(0)
        compose.onAllNodesWithTag("task-time-toggle").assertCountEquals(0)
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(ScheduleKind.Anytime, saved.get()?.scheduleKind)
            assertEquals(true, saved.get()?.inbox)
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
    fun movingScheduledTaskToInboxPreviewsAndClearsConsequences() {
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
        compose.onNodeWithText("Inbox").performScrollTo().performClick()
        compose.onNodeWithText("Remove Scheduling Details?").assertIsDisplayed()
        compose.onNodeWithText("The Scheduled Date will be removed.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Move to Inbox").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(ScheduleKind.Anytime, saved.get()?.scheduleKind)
            assertEquals(true, saved.get()?.inbox)
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
