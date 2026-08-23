package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.onNodeWithText("Add weekday reminder").performClick()
        compose.onNodeWithText("Choose weekday").assertIsDisplayed()
        compose.onNodeWithText("Nothing is scheduled until you choose a time and tap Add.").assertIsDisplayed()
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEach { day ->
            compose.onNodeWithText(day).assertIsDisplayed()
        }
        compose.onNodeWithText("Wednesday").performClick()
        compose.onNodeWithText("Wednesday reminder").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(reminderValues.get().values.all { it.isEmpty() })

        compose.onNodeWithText("Next repeat is based on").assertIsDisplayed()
        compose.onNodeWithText("Completion date").assertIsNotEnabled()
        compose.onNodeWithText("Under Repeats, choose Daily or an Every X option", substring = true).assertIsDisplayed()

        compose.runOnIdle { usesSelectedDays.value = false }
        compose.onNodeWithText("Completion date").assertIsEnabled().performClick()
        assertEquals(RecurrenceAnchor.Completion, selectedAnchor.get())
    }
}
