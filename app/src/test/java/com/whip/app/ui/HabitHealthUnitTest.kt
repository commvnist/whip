package com.whip.app.ui

import com.whip.app.core.AppSettings
import com.whip.app.domain.MeasurementDefinition
import com.whip.app.domain.MeasurementValueKind
import com.whip.app.domain.UnitDimension
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitHealthUnitTest {
    @Test
    fun healthHabitsUseTheUsersCompatibleDisplayDefaults() {
        val settings = AppSettings(
            massUnitId = "pound",
            distanceUnitId = "mile",
            volumeUnitId = "fluid_ounce",
        )

        assertEquals("pound", preferredHealthMeasurementUnitId(measurement("weight", UnitDimension.Mass, "kilogram"), settings, emptyList()))
        assertEquals("mile", preferredHealthMeasurementUnitId(measurement("distance", UnitDimension.Distance, "distance_m"), settings, emptyList()))
        assertEquals("fluid_ounce", preferredHealthMeasurementUnitId(measurement("hydration", UnitDimension.Volume, "litre"), settings, emptyList()))
        assertEquals("count", preferredHealthMeasurementUnitId(measurement("steps", UnitDimension.Count, "count"), settings, emptyList()))
    }

    private fun measurement(id: String, dimension: UnitDimension, unitId: String) = MeasurementDefinition(
        id = id,
        name = id,
        valueKind = MeasurementValueKind.Decimal,
        dimension = dimension,
        defaultUnitId = unitId,
        precision = 2,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )
}
