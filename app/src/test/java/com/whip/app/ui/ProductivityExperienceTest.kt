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
        val legacyReduce = goal.copy(type = GoalType.ReduceValue, baseline = null, direction = GoalDirection.Decrease, targetMin = 80.0)
        assertFalse(projection.copy(goal = legacyReduce, progress = null, currentValue = 90.0).offersCompletion())
        assertTrue(projection.copy(goal = legacyReduce, progress = null, currentValue = 70.0).offersCompletion())
        assertFalse(projection.copy(goal = legacyReduce, progress = null, currentValue = null).offersCompletion())
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

    @Test fun planningUsesClosedPeriodTruthAndRetainsTheFiniteEarningDate() {
        val earnedDate = LocalDate.of(2026, 9, 16)
        val habit = planningHabit(earnedDate).copy(endType = HabitEndType.AfterCompletions, endValue = 1.0)
        val logs = listOf(planningLog(habit.id, earnedDate))
        val state = buildHabitUiState(
            HabitData(listOf(habit), emptyList(), logs, emptyList(), emptyList(), emptyList()),
            earnedDate.plusDays(1), emptyList(),
        )
        val earned = state.plannedOn(earnedDate).single()
        assertEquals(HabitDayState.Completed, earned.dayState)
        assertEquals(true, earned.successful)
        assertTrue(state.plannedOn(earnedDate.plusDays(2)).isEmpty())

        val bounded = habit.copy(endType = HabitEndType.Never, endValue = null,
            targetPeriod = TargetPeriod.Week, comparison = TargetComparison.AtMost, targetMin = 2.0)
        val boundedState = buildHabitUiState(
            HabitData(listOf(bounded), emptyList(), listOf(planningLog(habit.id, earnedDate, 2.0)),
                emptyList(), emptyList(), emptyList()), earnedDate.plusDays(1), emptyList(),
        )
        assertNull(boundedState.plannedOn(earnedDate).single().successful)
        assertEquals(HabitDayState.Pending, boundedState.plannedOn(earnedDate).single().dayState)
    }

    @Test fun planningDoesNotImportEvidenceFromAfterItsRequestedOrCurrentDate() {
        val date = LocalDate.of(2026, 9, 16)
        val habit = planningHabit(date).copy(targetPeriod = TargetPeriod.Week, targetMin = 2.0)
        val state = buildHabitUiState(
            HabitData(listOf(habit), emptyList(), listOf(planningLog(habit.id, date.plusDays(2), 2.0)),
                emptyList(), emptyList(), emptyList()), date.plusDays(1), emptyList(),
        )
        for (requested in listOf(date, date.plusDays(2))) {
            val planned = state.plannedOn(requested).single()
            assertEquals(0.0, planned.value, 0.0)
            assertNull(planned.successful)
        }
    }

    private fun planningHabit(date: LocalDate) = Habit(
        id = 1, uuid = "planning", measurementId = "planning-measurement", name = "Planning",
        notes = "", area = "", tags = emptyList(), icon = "", trackingMode = HabitTrackingMode.Count,
        dimension = UnitDimension.Count, unitId = "count", precision = 0,
        comparison = TargetComparison.AtLeast, targetMin = 1.0, targetMax = null,
        targetPeriod = TargetPeriod.Day, rollingDays = null, scheduleType = HabitScheduleType.Daily,
        scheduleInterval = 1, weekdays = emptySet(), flexibleTimesPerWeek = null,
        startDate = date, endType = HabitEndType.Never, endDate = null, endValue = null,
        quickIncrement = 1.0, quickActions = emptyList(), reminderMinutes = emptyList(),
        weekdayReminderMinutes = emptyMap(), weekStart = java.time.DayOfWeek.MONDAY,
        timerStartedAtMillis = null, pinned = false, position = 0, archived = false, paused = false,
        createdAtMillis = 1, updatedAtMillis = 1,
    )

    private fun planningLog(id: Long, date: LocalDate, value: Double = 1.0) = HabitLog(
        id = id, uuid = "planning-log", habitId = id, value = value, canonicalValue = value,
        enteredUnitId = "count", status = HabitLogStatus.Recorded,
        timestamp = date.atStartOfDay(ZoneOffset.UTC).toInstant(), localDate = date,
        zoneId = "UTC", offsetSeconds = 0, note = "", sourceType = MeasurementSourceType.Habit,
        sourceId = null, measurementEntryId = null, createdAtMillis = 1, updatedAtMillis = 1,
    )

}
