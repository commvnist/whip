package com.whip.app.ui

import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.UnitDimension
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackInsightNumberFormatTest {
    @Test
    fun mixedDistancesHaveTheSameUnitAndPrecisionForTotalsAndAverages() {
        val format = TrackInsightNumberFormat(field(UnitDimension.Distance, "mile", 3), BuiltInUnits.all, Locale.US)
        assertTrue(format.showTotal)
        assertEquals("1.621 mi", format.format(1_609.344 + 1_000.0))
        assertEquals("0.811 mi", format.format((1_609.344 + 1_000.0) / 2))
        assertEquals("0.379 mi", format.formatDifference(1_609.344 - 1_000.0))
    }

    @Test
    fun temperaturesConvertAbsoluteReadingsAndDifferencesWithoutOfferingTotals() {
        val fahrenheit = TrackInsightNumberFormat(field(UnitDimension.Temperature, "fahrenheit", 1), BuiltInUnits.all, Locale.US)
        assertFalse(fahrenheit.showTotal)
        assertEquals("50.0 °F", fahrenheit.format((0.0 + 20.0) / 2))
        assertEquals("32.0 °F", fahrenheit.format(0.0))
        assertEquals("36.0 °F", fahrenheit.formatDifference(20.0))
        val celsius = TrackInsightNumberFormat(field(UnitDimension.Temperature, "celsius", 1), BuiltInUnits.all, Locale.US)
        assertFalse(celsius.showTotal)
        val kelvin = TrackInsightNumberFormat(field(UnitDimension.Temperature, "kelvin", 2), BuiltInUnits.all, Locale.US)
        assertFalse(kelvin.showTotal)
        assertEquals("273.15 K", kelvin.format(0.0))
    }

    @Test
    fun scaleAveragesRetainFractionsAndFineIncrementsWithoutOfferingTotals() {
        val scale = field(UnitDimension.Unitless, "unitless", 3).copy(type = TrackFieldType.Scale, dimension = null, unitId = null)
        val fine = TrackInsightNumberFormat(scale, emptyList(), Locale.US)
        assertFalse(fine.showTotal)
        assertEquals("0.125", fine.format(0.125))
        assertEquals("0.188", fine.format((0.125 + 0.25) / 2))
        val whole = TrackInsightNumberFormat(scale.copy(precision = 0), emptyList(), Locale.US)
        assertEquals("4.5", whole.format((4.0 + 5.0) / 2))
        assertEquals("10", whole.format(10.0))
    }

    @Test
    fun fieldPrecisionAndLocaleApplyToConvertedReadings() {
        val field = field(UnitDimension.Distance, "mile", 6)
        val french = TrackInsightNumberFormat(field, BuiltInUnits.all, Locale.FRANCE)
        assertEquals("1,621371 mi", french.format(2_609.344))
        val whole = TrackInsightNumberFormat(field.copy(precision = 0), BuiltInUnits.all, Locale.US)
        assertEquals("2 mi", whole.format(2_609.344))
    }

    @Test
    fun missingUnitsEmptyEvidenceAndOverflowRemainUnavailable() {
        val missing = TrackInsightNumberFormat(field(UnitDimension.Distance, "missing", 3), BuiltInUnits.all, Locale.US)
        assertFalse(missing.showTotal)
        assertEquals("—", missing.format(1_000.0))
        val format = TrackInsightNumberFormat(field(UnitDimension.Distance, "mile", 3), BuiltInUnits.all, Locale.US)
        assertEquals("—", format.format(null))
        assertEquals("—", format.format(Double.NaN))
        assertEquals("—", format.format(Double.POSITIVE_INFINITY))
    }

    private fun field(dimension: UnitDimension, unitId: String, precision: Int) = TrackField(
        id = 1, uuid = "field", trackId = 1, name = "Measurement", type = TrackFieldType.Number,
        position = 0, required = false, primary = false, showInList = true,
        dimension = dimension, unitId = unitId, precision = precision,
        scaleMin = null, scaleMax = null, scaleLowLabel = "", scaleHighLabel = "",
        createdAtMillis = 1, updatedAtMillis = 1,
    )
}
