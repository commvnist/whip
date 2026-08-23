package com.whip.app.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class InteractionConsistencyTest {
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
}
