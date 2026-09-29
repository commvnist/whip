package com.whip.app.ui

import com.whip.app.domain.*
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class ProductivityExperienceTest {
    private val today = LocalDate.of(2026, 9, 28)

    @Test fun completionOpportunityRequiresAnAchievedActiveFiniteOutcome() {
        val goal = Goal(
            id = 1, uuid = "g", measurementId = "m", name = "Goal", description = "", area = "",
            tags = emptyList(), icon = "◎", type = GoalType.ReachValue,
            dimension = UnitDimension.Unitless, unitId = "unitless", precision = 1,
            baseline = 0.0, targetMin = 100.0, targetMax = null, direction = GoalDirection.Increase,
            startDate = today, deadline = null, aggregation = GoalAggregation.Latest,
            paceType = GoalPaceType.None, reminderMinutes = null, status = GoalStatus.Active,
            pinned = false, position = 0, createdAtMillis = 1, updatedAtMillis = 1,
        )
        val projection = GoalProjection(goal, 120.0, 1.2, null, null, null, null, null, emptyList(), emptyList())
        assertTrue(projection.offersCompletion())
        for (value in listOf(null, 0.99, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFalse(projection.copy(progress = value).offersCompletion())
        }
        for (type in listOf(GoalType.MaintainRange, GoalType.OpenEndedTrend, GoalType.ElapsedSince)) {
            assertFalse(projection.copy(goal = goal.copy(type = type)).offersCompletion())
        }
        for (status in GoalStatus.entries.filter { it != GoalStatus.Active }) {
            assertFalse(projection.copy(goal = goal.copy(status = status)).offersCompletion())
        }
        assertFalse(projection.copy(goal = goal.copy(archived = true)).offersCompletion())
        assertTrue(projection.copy(goal = goal.copy(type = GoalType.WeightedMilestones)).offersCompletion())
    }

    @Test fun goalHistorySearchUsesOriginalObservationFacts() {
        val entry = MeasurementEntry("e", "m", 68.0388555, 150.0, "pound", MeasurementEntryStatus.Recorded,
            today.atStartOfDay(ZoneOffset.UTC).toInstant(), today.minusYears(2), "UTC", 0,
            MeasurementSourceType.Manual, null, "After the long walk", 1, 1)
        val text = entry.goalHistorySearchText()
        assertTrue(text, "150" in text && "lb" in text && "2024-09-28" in text && "long walk" in text)
        assertFalse(text, "68.038" in text)
        for (locale in listOf(java.util.Locale.US, java.util.Locale.FRANCE)) {
            val localized = entry.goalHistorySearchText(locale = locale)
            val expectedDate = entry.localDate.format(
                java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale),
            ).lowercase(java.util.Locale.ROOT)
            assertTrue(localized, expectedDate in localized)
            assertTrue(localized, "150" in localized && "lb" in localized && "long walk" in localized)
        }
    }

    @Test fun trackStartersAndPreviewKeepAuthoredDefinitionsSeparateFromSampleFacts() {
        for (starter in TrackStarter.entries) {
            val draft = starter.draft("my-area")
            val projection = draft.entryPreviewProjection()
            assertEquals("my-area", projection.track.areaId)
            assertTrue(projection.entries.isEmpty())
            assertEquals(draft.fields.map { it.name }, projection.fields.map { it.name })
            assertTrue(projection.primaryFields.all { it.required })
            assertTrue(draft.fields.all { it.id == null && it.uuid == null })
            assertTrue(runCatching { validateTrackEntryDraft(projection.fields, projection.options,
                TrackEntryDraft(today, emptyMap())) }.isFailure)
        }
        val spending = TrackStarter.Spending.draft(null).entryPreviewProjection()
        val identity = spending.fields.first()
        val amount = spending.fields[1]
        assertEquals("currency", amount.unitId)
        validateTrackEntryDraft(spending.fields, spending.options, TrackEntryDraft(today, mapOf(
            identity.uuid to TrackValueDraft(textValue = "Train"),
            amount.uuid to TrackValueDraft(enteredNumber = 4.25, enteredUnitId = "currency"),
        )))
        val reflection = TrackStarter.Reflection.draft(null).entryPreviewProjection()
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0, 5.0), reflection.fields[1].let {
            trackScaleValues(requireNotNull(it.scaleMin), requireNotNull(it.scaleMax), it.scaleStep)
        })
    }
}
