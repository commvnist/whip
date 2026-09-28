package com.whip.app.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import com.whip.app.domain.BuiltInUnits
import org.junit.Test

class GoalProgressPresentationTest {
    @Test fun changingGoalUnitsPreservesMassAndAffineTargetsWithoutConsumingIncompleteText() {
        val kilogram = requireNotNull(BuiltInUnits.get("kilogram"))
        val pound = requireNotNull(BuiltInUnits.get("pound"))
        val original = listOf("80", "70", "")
        val pounds = requireNotNull(convertGoalDraftValues(original, kilogram, pound))
        assertEquals(80.0, pound.toCanonical(pounds[0].toDouble()), 0.000000001)
        assertEquals(70.0, pound.toCanonical(pounds[1].toDouble()), 0.000000001)
        assertEquals("", pounds[2])
        listOf(0.00000000001, 9999999999999.5).forEach { value ->
            val converted = requireNotNull(convertGoalDraftValues(listOf(value.toString()), kilogram, pound)).single().toDouble()
            assertEquals(value, pound.toCanonical(converted), kotlin.math.abs(value) * 1e-15)
        }
        assertNull(convertGoalDraftValues(listOf("1e", "70", ""), kilogram, pound))
        assertEquals(listOf("1e", "70.", ""), convertGoalDraftValues(listOf("1e", "70.", ""), kilogram, kilogram))
        val fahrenheit = requireNotNull(BuiltInUnits.get("fahrenheit"))
        val converted = requireNotNull(convertGoalDraftValues(listOf("0", "18", "22"), requireNotNull(BuiltInUnits.get("celsius")), fahrenheit))
        assertEquals(32.0, converted[0].toDouble(), 0.000000001)
        assertEquals(64.4, converted[1].toDouble(), 0.000000001)
        assertEquals(71.6, converted[2].toDouble(), 0.000000001)
    }

    @Test fun canonicalReadingsAndRatesUseSelectedUnitsAndPrecision() {
        assertEquals("150.0 lb", formatGoalCanonicalValue(68.0388555, "pound", 1, locale = Locale.US))
        assertEquals("-2.0 lb", formatGoalCanonicalValue(-0.90718474, "pound", 1, difference = true, locale = Locale.US))
        assertEquals("68,039 kg", formatGoalCanonicalValue(68.0388555, "kilogram", 3, locale = Locale.GERMANY))
        assertEquals("68.0 °F", formatGoalCanonicalValue(20.0, "fahrenheit", 1, locale = Locale.US))
        assertEquals("1.8 °F", formatGoalCanonicalValue(1.0, "fahrenheit", 1, difference = true, locale = Locale.US))
        val custom = com.whip.app.domain.UnitDefinition("stone", "stones", "st", com.whip.app.domain.UnitDimension.Mass, 6.35029318)
        assertEquals("10.00 st", formatGoalCanonicalValue(63.5029318, "stone", 2, listOf(custom), locale = Locale.US))
        assertEquals("2.0", formatGoalCanonicalValue(2.0, "count", 1, locale = Locale.US))
        assertEquals("50.0 %", formatGoalCanonicalValue(50.0, "percent", 1, locale = Locale.US))
        assertEquals("—", formatGoalCanonicalValue(null, "pound", 1))
    }

    @Test fun aBeginningAndNearlyReachedTargetNeverReadAsTheirEndpoints() {
        val cases = listOf(
            0.0 to "0%", -0.0 to "0%", Double.MIN_VALUE to "<0.1%",
            0.00005 to "<0.1%", 0.001 to "0.1%", 0.005 to "0.5%",
            0.999 to "99.9%", 0.99901 to ">99.9%", Math.nextDown(1.0) to ">99.9%", 1.0 to "100%",
            Math.nextUp(1.0) to ">100%", 1.00001 to ">100%", 1.005 to "100.5%", 1.2 to "120%",
        )
        cases.forEach { (progress, expected) -> assertEquals("Ratio $progress", expected, formatGoalProgressPercent(progress, Locale.US)) }
    }

    @Test fun ordinaryProgressUsesOneReadableDecimalWithoutTrailingZeros() {
        assertEquals("12.3%", formatGoalProgressPercent(0.123456, Locale.US))
        assertEquals("33.3%", formatGoalProgressPercent(1.0 / 3.0, Locale.US))
        assertEquals("50%", formatGoalProgressPercent(0.5, Locale.US))
        assertEquals("66.7%", formatGoalProgressPercent(2.0 / 3.0, Locale.US))
    }

    @Test fun localeControlsDecimalAndPercentSpacingForEveryState() {
        assertEquals("0,5\u00a0%", formatGoalProgressPercent(0.005, Locale.GERMANY))
        assertEquals("<0,1\u00a0%", formatGoalProgressPercent(0.00005, Locale.GERMANY))
        assertEquals(">99,9\u00a0%", formatGoalProgressPercent(0.99999, Locale.GERMANY))
        assertEquals("100\u00a0%", formatGoalProgressPercent(1.0, Locale.GERMANY))
        assertEquals(">100\u00a0%", formatGoalProgressPercent(1.00001, Locale.GERMANY))
        assertEquals("120\u00a0%", formatGoalProgressPercent(1.2, Locale.GERMANY))
    }
}
