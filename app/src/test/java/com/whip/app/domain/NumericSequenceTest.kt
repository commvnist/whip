package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumericSequenceTest {
    @Test
    fun numberedStackRangeExpandsWithAnExplicitIncrement() {
        val parsed = parseNumericSequence("1-10", 1.0)

        assertNull(parsed.error)
        assertTrue(parsed.isRange)
        assertEquals((1..10).map(Int::toDouble), parsed.values)
    }

    @Test
    fun massStackRangeSupportsLargeAndFractionalProgressions() {
        assertEquals(
            listOf(50.0, 60.0, 70.0, 80.0, 90.0, 100.0),
            parseNumericSequence("50-100", 10.0).values,
        )
        assertEquals(
            listOf(2.5, 5.0, 7.5, 10.0),
            parseNumericSequence("2.5–10", 2.5).values,
        )
    }

    @Test
    fun irregularMachineValuesRemainAFirstClassCustomList() {
        val parsed = parseNumericSequence("1, 2, 4, 7.5, 10", 1.0)

        assertNull(parsed.error)
        assertFalse(parsed.isRange)
        assertEquals(listOf(1.0, 2.0, 4.0, 7.5, 10.0), parsed.values)
    }

    @Test
    fun rangeRejectsAnIncrementThatCannotReachItsMaximum() {
        val parsed = parseNumericSequence("1-10", 4.0)

        assertTrue(parsed.values.isEmpty())
        assertEquals("The increment must land exactly on the range maximum", parsed.error)
    }

    @Test
    fun evenlySpacedStoredValuesRoundTripToCompactRangeNotation() {
        val compact = compactNumericSequence((1..10).map(Int::toDouble))

        assertEquals("1-10", compact.specification)
        assertEquals(1.0, compact.increment!!, 0.0)
        assertTrue(compact.isRange)
    }

    @Test
    fun stepperFollowsExactMachineValuesAndClampsAtTheEnds() {
        val values = listOf(1.0, 2.0, 4.0, 7.5, 10.0)

        assertEquals(7.5, steppedNumericValue("4", 1, 1.0, values), 0.0)
        assertEquals(4.0, steppedNumericValue("7.5", -1, 1.0, values), 0.0)
        assertEquals(10.0, steppedNumericValue("10", 1, 1.0, values), 0.0)
    }

    @Test
    fun poundsUseRealWorldBarPlateAndIncrementDefaults() {
        val pounds = standardWeightEquipment("pound")

        assertEquals(5.0, pounds.increment, 0.0)
        assertEquals(45.0, pounds.barWeight, 0.0)
        assertEquals(listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5), pounds.plates)
    }

    @Test
    fun convertingMetricDefaultsUsesNativePoundEquipmentValues() {
        val pounds = convertWeightEquipmentSetup(
            setup = standardWeightEquipment("kilogram"),
            fromUnitId = "kilogram",
            toUnitId = "pound",
        )

        assertEquals(5.0, pounds.increment, 0.0)
        assertEquals(45.0, pounds.barWeight, 0.0)
        assertEquals(listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5), pounds.plates)
    }

    @Test
    fun customEquipmentConversionAvoidsLongFloatingPointTails() {
        val pounds = convertWeightEquipmentSetup(
            setup = WeightEquipmentSetup(
                increment = 1.0,
                barWeight = 12.5,
                plates = listOf(7.5, 25.0),
            ),
            fromUnitId = "kilogram",
            toUnitId = "pound",
        )

        assertEquals(2.25, pounds.increment, 0.0)
        assertEquals(27.5, pounds.barWeight, 0.0)
        assertEquals(listOf(16.5, 55.0), pounds.plates)
    }

    @Test
    fun standardMachineRangesCoverMassAndOrdinalStacks() {
        assertEquals(MachineSequenceSetup("1-10", 1.0), standardMachineSequence(MachineLoadType.Level, ""))
        assertEquals(MachineSequenceSetup("10-200", 10.0), standardMachineSequence(MachineLoadType.Mass, "pound"))
        assertEquals(MachineSequenceSetup("5-100", 5.0), standardMachineSequence(MachineLoadType.Mass, "kilogram"))
    }
}
