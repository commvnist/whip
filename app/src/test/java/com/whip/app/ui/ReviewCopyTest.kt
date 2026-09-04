package com.whip.app.ui

import com.whip.app.core.ReviewPeriod
import com.whip.app.core.ReviewSection
import com.whip.app.domain.Track
import com.whip.app.domain.TrackEntry
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackProjection
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

    @Test fun trackEvidenceIsEmptyWhenTheSelectedPeriodHasNoEntries() {
        assertEquals(
            null,
            trackReviewEvidence(
                tracks(entries = listOf(entry(1, 1, LocalDate.of(2026, 8, 16)))),
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 23),
            ),
        )
    }

    @Test fun trackEvidenceCountsEntriesAndDistinctTracksForTheSelectedPeriod() {
        assertEquals(
            TrackReviewEvidence(entryCount = 3, touchedTrackCount = 2),
            trackReviewEvidence(
                tracks(
                    entries = listOf(
                        entry(1, 1, LocalDate.of(2026, 8, 17)),
                        entry(2, 1, LocalDate.of(2026, 8, 23)),
                        entry(3, 2, LocalDate.of(2026, 8, 20)),
                    ),
                ),
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 23),
            ),
        )
    }

    @Test fun trackEvidenceUsesThePeriodAndAllTracksRatherThanProductivityAreaScope() {
        val state = tracks(
            entries = listOf(
                entry(1, 1, LocalDate.of(2026, 8, 1)),
                entry(2, 2, LocalDate.of(2026, 8, 18)),
            ),
        )
        assertEquals(
            TrackReviewEvidence(entryCount = 1, touchedTrackCount = 1),
            trackReviewEvidence(state, LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23)),
        )
        assertEquals(
            TrackReviewEvidence(entryCount = 2, touchedTrackCount = 2),
            trackReviewEvidence(state, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
        )
    }

    @Test fun trackEvidenceCopyUsesSingularAndPluralCounts() {
        assertEquals(
            "1 entry across 1 touched Track.",
            formatTrackEvidenceSummary(TrackReviewEvidence(entryCount = 1, touchedTrackCount = 1)),
        )
        assertEquals(
            "2 entries across 3 touched Tracks.",
            formatTrackEvidenceSummary(TrackReviewEvidence(entryCount = 2, touchedTrackCount = 3)),
        )
    }

    private fun tracks(entries: List<TrackEntryProjection>) = TrackUiState(
        projections = entries.groupBy { it.entry.trackId }.map { (id, trackEntries) ->
            TrackProjection(
                track = Track(id, "track-$id", "Track $id", "", "📌", "area-$id", "Area $id", emptyList(), false, false, 0, 0, 0),
                fields = emptyList(),
                options = emptyList(),
                entries = trackEntries,
            )
        },
        loading = false,
    )

    private fun entry(id: Long, trackId: Long, date: LocalDate) = TrackEntryProjection(
        entry = TrackEntry(id, "entry-$id", trackId, date, 0, 0),
        values = emptyMap(),
    )
}
