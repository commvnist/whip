package com.whip.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.whip.app.ui.FocusTimerCard
import com.whip.app.ui.NotificationDeliveryState
import com.whip.app.ui.theme.WhipTheme
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class FocusTimerCardUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val deadline = Instant.parse("2026-09-28T18:30:00Z").toEpochMilli()

    @Test @AndroidFontScale fun compactCardKeepsOpenStopAndAlertRecoveryReachableWithoutTickAnnouncements() {
        val seconds = mutableStateOf(1_800L)
        val notificationState = mutableStateOf(NotificationDeliveryState.OffInAndroid)
        var opened = 0
        var stopped = 0
        var repaired = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(360.dp)) {
                    FocusTimerCard("Read the complete project notes and plan the next Task", deadline, seconds.value,
                        ZoneId.of("America/Toronto"), notificationState.value,
                        false, null, { opened++ }, { stopped++ }, { repaired++ })
                }
            }
        }
        compose.onNodeWithTag("active-focus-countdown").assertTextEquals("Focus · 30:00")
        compose.onNodeWithTag("active-focus-metadata").assertIsDisplayed()
        compose.onNodeWithTag("active-focus-task").assertTextEquals("Read the complete project notes and plan the next Task")
        compose.onNodeWithText("Finishes at", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("focus-alert-unavailable").assertIsDisplayed()
        listOf("focus-open-task", "focus-stop", "focus-notification-settings").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(48.dp)
            compose.onNodeWithTag(tag).performClick()
        }
        assertEquals(1, opened)
        assertEquals(1, stopped)
        assertEquals(1, repaired)
        compose.runOnIdle { notificationState.value = NotificationDeliveryState.Blocked }
        compose.onNodeWithTag("focus-alert-unavailable").assertTextEquals("Notification permission needed").assertIsDisplayed()
        compose.onNodeWithTag("active-focus-metadata").assertIsDisplayed()
        compose.onNodeWithTag("active-focus-task").assertTextEquals("Read the complete project notes and plan the next Task")
        compose.onNodeWithTag("active-focus-finish-time").assertTextContains("Finishes at", substring = true).assertIsDisplayed()
        listOf("focus-open-task", "focus-stop", "focus-notification-settings").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(48.dp)
        }
        val runningState = compose.onNodeWithTag("active-focus-card").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        compose.runOnIdle { seconds.value = 1_799L }
        compose.onNodeWithTag("active-focus-countdown").assertTextEquals("Focus · 29:59")
        assertEquals("Focus timer running", runningState)
        assertEquals(runningState, compose.onNodeWithTag("active-focus-card").fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertFalse(compose.onNodeWithTag("active-focus-countdown").fetchSemanticsNode().config.contains(SemanticsProperties.LiveRegion))
        assertFalse(compose.onNodeWithTag("active-focus-card").fetchSemanticsNode().config.contains(SemanticsProperties.LiveRegion))
    }

    @Test fun busyStopDisablesDuplicateActionsAndFailureKeepsRecovery() {
        val busy = mutableStateOf(true)
        val error = mutableStateOf<String?>(null)
        var stopped = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                FocusTimerCard("Preserved Task", deadline, 20L, ZoneId.of("UTC"),
                    NotificationDeliveryState.Deliverable, busy.value, error.value, {}, { stopped++ }, {})
            }
        }
        compose.onNodeWithTag("focus-open-task").assertIsNotEnabled()
        compose.onNodeWithTag("focus-stop").assertIsNotEnabled()
        compose.onNodeWithText("Saving…").assertIsDisplayed()
        compose.runOnIdle { busy.value = false; error.value = "Could not save the stopped timer. Try again." }
        compose.onNodeWithTag("focus-stop-error").assertIsDisplayed()
        compose.onNodeWithTag("focus-stop").assertIsEnabled().performClick()
        assertEquals(1, stopped)
        compose.onNodeWithTag("active-focus-task").assertTextEquals("Preserved Task")
    }

    @Test fun dueSessionShowsCompletionWithoutNegativeTimeOrCompletingTheTask() {
        var dismissed = 0
        var opened = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                FocusTimerCard("Task still needs review", deadline, 0L, ZoneId.of("UTC"),
                    NotificationDeliveryState.Deliverable, false, null, { opened++ }, { dismissed++ }, {})
            }
        }
        compose.onNodeWithTag("active-focus-countdown").assertTextEquals("Focus complete")
        compose.onNodeWithText("Finished at", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("focus-open-task").performClick()
        compose.onNodeWithTag("focus-stop").assertContentDescriptionContains("Dismiss completed focus timer").performClick()
        assertEquals(1, opened)
        assertEquals(1, dismissed)
        compose.onAllNodesWithText("Complete Task").assertCountEquals(0)
    }
}
