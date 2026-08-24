package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun kilogramRepetitionUnitIsNotPartOfTheCatalog() {
        assertNull(BuiltInUnits.get("kilogram_rep"))
        assertNull(BuiltInUnits.all.firstOrNull { it.symbol == "kg·rep" })
    }

    @Test
    fun builtInNumberUnitsCoverCommonEverydayHealthAndFitnessMeasurements() {
        val unitIds = BuiltInUnits.all.map(UnitDefinition::id).toSet()
        assertEquals(
            emptySet<String>(),
            setOf(
                "day", "week", "ounce", "stone", "millimetre", "foot", "yard",
                "celsius", "fahrenheit", "kelvin",
                "metre_per_second", "kilometre_per_hour", "mile_per_hour",
                "minute_per_kilometre", "minute_per_mile", "hertz", "per_minute",
            ) - unitIds,
        )

        assertEquals(
            20.0,
            convertMeasurement(
                68.0,
                requireNotNull(BuiltInUnits.get("fahrenheit")),
                requireNotNull(BuiltInUnits.get("celsius")),
            ),
            0.000001,
        )
        assertEquals(
            10.0,
            convertMeasurement(
                6.21371192,
                requireNotNull(BuiltInUnits.get("mile_per_hour")),
                requireNotNull(BuiltInUnits.get("kilometre_per_hour")),
            ),
            0.000001,
        )
        assertEquals(
            8.04672,
            convertMeasurement(
                5.0,
                requireNotNull(BuiltInUnits.get("minute_per_kilometre")),
                requireNotNull(BuiltInUnits.get("minute_per_mile")),
            ),
            0.000001,
        )
    }
}
