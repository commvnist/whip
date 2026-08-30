package com.whip.app.reminders

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationNumberFormatTest {
    @Test
    fun quickIncrementLabelsUseLocaleSeparatorsWithoutRawDoubleNoise() {
        assertEquals("1.25", formatNotificationNumber(1.25, Locale.US))
        assertEquals("1,25", formatNotificationNumber(1.25, Locale.GERMANY))
        assertEquals("1,000.5", formatNotificationNumber(1_000.5, Locale.US))
        assertEquals("0.3", formatNotificationNumber(0.1 + 0.2, Locale.US))
    }
}
