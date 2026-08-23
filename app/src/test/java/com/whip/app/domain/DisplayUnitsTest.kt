package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayUnitsTest {
    @Test
    fun canonicalGymValuesDisplayAndRoundTripInPounds() {
        val pounds = massFromKilograms(100.0, "pound")
        assertEquals(220.462262, pounds, 0.000001)
        assertEquals(100.0, massToKilograms(pounds, "pound"), 0.000001)
    }

    @Test
    fun canonicalDistanceDisplaysInMiles() {
        assertEquals(1.0, distanceFromMetres(1_609.344, "mile"), 0.000001)
    }
}
