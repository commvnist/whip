package com.whip.app.ui

import com.whip.app.domain.TaskEffort
import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetClassification
import java.time.DayOfWeek
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionConsistencyTest {
    @Test
    fun interfaceChromeUsesTitleCaseWithoutDamagingAcronymsOrApostrophes() {
        assertEquals("Review & Trends", "review & trends".uiTitleCase())
        assertEquals("Search Goals", "search goals".uiTitleCase())
        assertEquals("Start from a Goal Template", "start from a goal template".uiTitleCase())
        assertEquals("Preview and Restore Backup", "preview and restore backup".uiTitleCase())
        assertEquals("Today's Habits", "today's habits".uiTitleCase())
        assertEquals("Estimated 1RM", "estimated 1RM".uiTitleCase())
    }

    @Test
    fun taskEffortChoicesUseParallelPlainLanguageLabels() {
        assertEquals(listOf("Light", "Medium", "High"), TaskEffort.entries.map(TaskEffort::label))
    }

    @Test
    fun storedIdentifiersHaveCanonicalUserFacingNames() {
        assertEquals(
            listOf("Check Off", "Count", "Measurement", "Timer", "Checklist", "Rating", "Log Only"),
            HabitTrackingMode.entries.map(HabitTrackingMode::uiLabel),
        )
        assertEquals(
            listOf("Reset Subtasks", "Carry Unfinished Subtasks"),
            RepeatStepPolicy.entries.map(RepeatStepPolicy::uiLabel),
        )
        assertEquals(
            listOf("Warm-up", "Working", "Back-off", "Drop", "AMRAP", "Failure"),
            WorkoutSetClassification.entries.map(WorkoutSetClassification::uiLabel),
        )
        assertEquals(
            listOf("1 Month", "3 Months", "6 Months", "1 Year", "All Time", "Custom"),
            GymGraphRange.entries.map(GymGraphRange::uiLabel),
        )
        assertEquals("Estimated 1RM", LinkSourceMetric.EstimatedOneRepMax.uiLabel())
        assertEquals("No Unit", UnitDimension.Unitless.uiLabel())
    }

    @Test
    fun emptyOrMalformedWeekdayRemindersNeverBecomePhantomDays() {
        assertEquals(emptyMap<DayOfWeek, List<Int>>(), parseWeekdayReminderMap(""))
        assertEquals(emptyMap<DayOfWeek, List<Int>>(), parseWeekdayReminderMap("=;MON=;M=08:00;unknown=09:00"))
        assertEquals(
            mapOf(DayOfWeek.MONDAY to listOf(8 * 60), DayOfWeek.THURSDAY to listOf(9 * 60 + 30)),
            parseWeekdayReminderMap("MON=08:00;THURSDAY=09:30"),
        )
        assertEquals(
            "WED=08:00",
            formatWeekdayReminderMap(
                mapOf(
                    DayOfWeek.MONDAY to emptyList(),
                    DayOfWeek.WEDNESDAY to listOf(8 * 60),
                ),
            ),
        )
    }

    @Test
    fun habitValuesUseTheConfiguredPrecisionEverywhere() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("0", formatHabitValue(0.0, 0))
            assertEquals("1", formatHabitValue(1.0, 0))
            assertEquals("2.50", formatHabitValue(2.5, 2))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun gymNavigationHasFourPrimaryDestinationsAndAnExhaustiveLibrary() {
        assertEquals(
            listOf(GymDestination.Workout, GymDestination.History, GymDestination.Progress, GymDestination.Library),
            primaryGymDestinations,
        )
        assertEquals(
            GymDestination.entries.toSet(),
            (primaryGymDestinations + libraryGymDestinations).toSet(),
        )
        assertEquals(
            GymDestination.entries.size,
            primaryGymDestinations.size + libraryGymDestinations.size,
        )
    }

    @Test
    fun unavailableDependentControlsAlwaysCarryRecoveryGuidance() {
        assertTrue(runCatching { ControlAvailability(enabled = false) }.isFailure)

        val blocked = completionAnchorAvailability(usesSelectedWeekdays = true)
        assertFalse(blocked.enabled)
        assertTrue(blocked.unavailableExplanation.orEmpty().contains("Under Repeats"))
        assertTrue(blocked.unavailableExplanation.orEmpty().contains("choose Daily or an Every X option"))

        assertTrue(completionAnchorAvailability(usesSelectedWeekdays = false).enabled)
    }
}
