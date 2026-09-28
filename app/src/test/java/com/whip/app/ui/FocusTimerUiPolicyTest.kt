package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusTimerUiPolicyTest {
    @Test fun countdownPreservesTheInitialMinuteAndRoundsEveryPartialSecondUp() {
        val deadline = 1_800_000L
        assertEquals(1_800L, focusTimerRemainingSeconds(deadline, 0L))
        assertEquals(1_800L, focusTimerRemainingSeconds(deadline, 1L))
        assertEquals(1_800L, focusTimerRemainingSeconds(deadline, 999L))
        assertEquals(1_799L, focusTimerRemainingSeconds(deadline, 1_000L))
        assertEquals(1L, focusTimerRemainingSeconds(deadline, deadline - 1L))
    }

    @Test fun dueAndExpiredSessionsHaveNoNegativeCountdown() {
        assertEquals(0L, focusTimerRemainingSeconds(10_000L, 10_000L))
        assertEquals(0L, focusTimerRemainingSeconds(10_000L, 90_000L))
        assertEquals("0:00", formatFocusTimerDuration(-1L))
    }

    @Test fun countdownUsesHoursForLongCustomSessions() {
        assertEquals("30:00", formatFocusTimerDuration(1_800L))
        assertEquals("0:01", formatFocusTimerDuration(1L))
        assertEquals("1:00:00", formatFocusTimerDuration(3_600L))
        assertEquals("4:00:00", formatFocusTimerDuration(14_400L))
    }
}
