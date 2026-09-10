package com.whip.app.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackInsightWindowTest {
    @Test fun recentWindowsIncludeBothBoundariesAndExcludeFutureDates() {
        val today = LocalDate.of(2024, 3, 1)
        val dates = listOf(0L, 6L, 7L, 29L, 30L, 89L, 90L, -1L).map(today::minusDays)
        assertEquals(2, dates.trackInsightCount(today, 7))
        assertEquals(4, dates.trackInsightCount(today, 30))
        assertEquals(6, dates.trackInsightCount(today, 90))
        assertEquals(1, dates.trackInsightCount(today, 1))
        assertEquals(8, dates.size)
    }

    @Test fun emptyOrEntirelyFutureEvidenceHasNoRecentCount() {
        val today = LocalDate.of(2026, 1, 1)
        assertEquals(0, emptyList<LocalDate>().trackInsightCount(today, 30))
        assertEquals(0, listOf(today.plusDays(1), today.plusYears(1)).trackInsightCount(today, 90))
        assertEquals(2, listOf(today, today).trackInsightCount(today, 7))
    }
}
