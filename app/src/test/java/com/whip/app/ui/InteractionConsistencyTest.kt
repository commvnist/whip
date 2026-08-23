package com.whip.app.ui

import java.time.DayOfWeek
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionConsistencyTest {
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
    fun gymNavigationHasThreePrimaryDestinationsAndAnExhaustiveLibrary() {
        assertEquals(
            listOf(GymDestination.Workout, GymDestination.History, GymDestination.Progress),
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
