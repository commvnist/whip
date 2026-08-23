package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MeasurementTest {
    @Test
    fun massConvertsBetweenEnteredAndCanonicalUnits() {
        val pounds = requireNotNull(BuiltInUnits.get("pound"))
        val kilograms = requireNotNull(BuiltInUnits.get("kilogram"))

        assertEquals(
            45.359237,
            convertMeasurement(100.0, pounds, kilograms),
            0.000001,
        )
    }

    @Test
    fun incompatibleDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            convertMeasurement(
                1.0,
                requireNotNull(BuiltInUnits.get("kilogram")),
                requireNotNull(BuiltInUnits.get("minute")),
            )
        }
    }
}
