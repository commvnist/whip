package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitTimerPresentationTest {
    @Test fun elapsedTimerFormattingUsesStableClockBoundaries() {
        assertEquals("0:00", formatElapsedDuration(0.0))
        assertEquals("0:59", formatElapsedDuration(59.999))
        assertEquals("1:00", formatElapsedDuration(60.0))
        assertEquals("59:59", formatElapsedDuration(3_599.0))
        assertEquals("1:00:00", formatElapsedDuration(3_600.0))
        assertEquals("1d 01:01:01", formatElapsedDuration(90_061.0))
    }

    @Test fun screenReaderDurationUsesSpokenUnitsInsteadOfPunctuation() {
        assertEquals("0 seconds", formatElapsedDurationSpoken(0.0))
        assertEquals("1 minute 1 second", formatElapsedDurationSpoken(61.0))
        assertEquals("1 day 1 hour 1 minute 1 second", formatElapsedDurationSpoken(90_061.0))
    }
}
