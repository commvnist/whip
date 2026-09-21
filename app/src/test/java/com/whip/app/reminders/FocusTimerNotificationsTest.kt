package com.whip.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerNotificationsTest {
    @Test
    fun `matching timer notifies only at or after its deadline`() {
        assertFalse(focusTimerShouldNotify(7, 10_000, 7, 10_000, 8_999))
        assertTrue(focusTimerShouldNotify(7, 10_000, 7, 10_000, 9_000))
        assertTrue(focusTimerShouldNotify(7, 10_000, 7, 10_000, 10_000))
    }

    @Test
    fun `replaced stopped and stale timers never notify`() {
        assertFalse(focusTimerShouldNotify(null, null, 7, 10_000, 11_000))
        assertFalse(focusTimerShouldNotify(8, 10_000, 7, 10_000, 11_000))
        assertFalse(focusTimerShouldNotify(7, 20_000, 7, 10_000, 11_000))
    }

    @Test
    fun `a long expired focus timer is not resurrected after upgrade or restore`() {
        val deadline = 10_000L
        val oneDay = 24 * 60 * 60 * 1_000L
        assertTrue(focusTimerShouldNotify(7, deadline, 7, deadline, deadline + oneDay))
        assertFalse(focusTimerShouldNotify(7, deadline, 7, deadline, deadline + oneDay + 1L))
        assertTrue(focusTimerDeadlineIsTooOld(deadline, deadline + oneDay + 1L))
    }
}
