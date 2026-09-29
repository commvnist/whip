package com.whip.app.ui

import com.whip.app.domain.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DeepProductivityTest {
    @Test fun completionOutcomeRetainsOriginalFactsUntilDeliberatelyChanged() {
        assertEquals(5.0, completionEntryValue(5.0, true), 0.0)
        assertEquals(-2.0, completionEntryValue(-2.0, false), 0.0)
        assertEquals(0.0, completionEntryValue(0.0, false), 0.0)
        assertEquals(0.0, completionEntryValue(5.0, false), 0.0)
        assertEquals(1.0, completionEntryValue(0.0, true), 0.0)
        assertEquals(1.0, completionEntryValue(null, true), 0.0)
    }

    @Test fun trackReviewScopeUsesInclusiveEntryDatesAndKeepsOriginalSeriesFacts() {
        val today = LocalDate.of(2026, 9, 29)
        assertTrue(TrackReviewRange.Month.includes(today.minusDays(29), today))
        assertFalse(TrackReviewRange.Month.includes(today.minusDays(30), today))
        assertFalse(TrackReviewRange.Month.includes(today.plusDays(1), today))
        assertTrue(TrackReviewRange.All.includes(today.plusDays(1), today))
        val projection = TrackStarter.Spending.draft(null).entryPreviewProjection()
        val field = projection.fields[1]
        fun entry(id: Long, daysAgo: Long, value: Double?) = TrackEntryProjection(
            TrackEntry(id, "$id", projection.track.id, today.minusDays(daysAgo), id, id),
            value?.let { mapOf(field.id to TrackFieldValue(id, "v$id", id, field.id,
                enteredNumber = it, canonicalNumber = it, enteredUnitId = "currency", createdAtMillis = id, updatedAtMillis = id)) }.orEmpty(),
        )
        val rows = listOf(entry(1, 30, 100.0), entry(2, 29, 1.0), entry(3, 4, 20.0),
            entry(4, 4, 2.0), entry(5, 0, null), entry(6, -1, 50.0))
        val scope = TrackReviewScope(TrackReviewRange.Month, listOf(
            TrackCondition(field.uuid, TrackConditionOperator.AtLeast, numberValue = 2.0),
            TrackCondition(field.uuid, TrackConditionOperator.Equals, numberValue = 1.0)), TrackConditionMode.MatchAny)
        val matching = projection.copy(entries = rows).reviewEntries(scope, today)
        assertEquals(listOf(2L, 3L, 4L), matching.map { it.entry.id })
        val series = trackReviewSeries(field, matching)
        assertEquals(listOf(1.0, 20.0, 2.0), series.map { it.value })
        assertEquals(listOf(2L, 3L, 4L), series.map { it.entry.entry.id })
        assertEquals(20.0, series[1].entry.value(field.id)!!.enteredNumber!!, 0.0)
        assertEquals(3, trackReviewSeries(field, matching + entry(7, 2, Double.NaN)).size)
    }

    @Test fun collectionSearchIncludesAuthoredNamesTagsAndStatus() {
        val track = TrackStarter.Reading.draft(null).entryPreviewProjection().track.copy(
            name = "Winter books", tags = listOf("Research"), description = "Long essays", archived = true)
        val prepared = track.collectionSearchText()
        for (query in listOf("winter", "research", "long essays", "archived")) assertTrue(prepared, query in prepared)
        assertFalse("active" in prepared)
    }
}
