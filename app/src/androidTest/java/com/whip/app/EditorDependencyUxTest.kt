package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.ui.theme.WhipTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditorDependencyUxTest {
    @get:Rule val compose = createComposeRule()

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
        compose.onNodeWithText("Under Repeats, choose Daily or an Every X option", substring = true).assertIsDisplayed()

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
                    powerMode = true,
                )
            }
        }

        compose.onNodeWithText("Repeat").performScrollTo().performClick()
        compose.onNodeWithTag("task-schedule-consequence").assertIsDisplayed()
        compose.onNodeWithText("Repeat creates future occurrences", substring = true).assertIsDisplayed()

        val repeatTop = compose.onNodeWithText("Repeats").fetchSemanticsNode().boundsInRoot.top
        val estimateTop = compose.onNodeWithText("Planning Estimate").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Repeat settings must appear before planning estimates", repeatTop < estimateTop)
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
}
