package com.whip.app.ui

import com.whip.app.core.ReviewPeriod
import com.whip.app.core.ReviewSection
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewCopyTest {
    @Test fun sameYearReviewRangeDoesNotRepeatTheYear() {
        assertEquals(
            "Aug 17–23, 2026",
            formatReviewRange(
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 23),
                Locale.US,
            ),
        )
    }

    @Test fun crossYearReviewRangeKeepsBothYearsExplicit() {
        assertEquals(
            "Dec 29, 2025–Jan 4, 2026",
            formatReviewRange(
                LocalDate.of(2025, 12, 29),
                LocalDate.of(2026, 1, 4),
                Locale.US,
            ),
        )
    }

    @Test fun reviewPeriodAndSelectedSectionsChangeTheRenderedScope() {
        val through = LocalDate.of(2026, 8, 23)
        assertEquals(LocalDate.of(2026, 8, 17), reviewStartDate(ReviewPeriod.Weekly, through))
        assertEquals(LocalDate.of(2026, 8, 1), reviewStartDate(ReviewPeriod.Monthly, through))
        assertEquals(
            listOf(ReviewSection.Tasks, ReviewSection.Goals),
            reviewSectionsInDisplayOrder(setOf(ReviewSection.Goals, ReviewSection.Tasks)),
        )
    }
}
