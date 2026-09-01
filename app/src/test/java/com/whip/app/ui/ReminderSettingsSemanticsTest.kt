package com.whip.app.ui

import com.whip.app.core.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSettingsSemanticsTest {
    @Test
    fun quietHoursAndTimeZoneInvalidateReminderDelivery() {
        val baseline = AppSettings()

        assertTrue(
            reminderDeliverySemanticsChanged(
                baseline,
                baseline.copy(quietStartMinutes = 22 * 60, quietEndMinutes = 7 * 60),
            ),
        )
        assertTrue(
            reminderDeliverySemanticsChanged(
                baseline,
                baseline.copy(timeZoneId = "America/Toronto"),
            ),
        )
    }

    @Test
    fun unrelatedPreferencesDoNotForceReminderReconciliation() {
        val baseline = AppSettings()

        assertFalse(
            reminderDeliverySemanticsChanged(
                baseline,
                baseline.copy(dynamicColor = !baseline.dynamicColor),
            ),
        )
    }
}
