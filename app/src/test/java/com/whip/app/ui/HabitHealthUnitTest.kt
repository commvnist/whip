package com.whip.app.ui

import com.whip.app.core.AppSettings
import com.whip.app.domain.MetricDefinition
import com.whip.app.domain.MetricValueKind
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

        assertEquals("pound", preferredHealthMetricUnitId(metric("weight", UnitDimension.Mass, "kilogram"), settings, emptyList()))
        assertEquals("mile", preferredHealthMetricUnitId(metric("distance", UnitDimension.Distance, "distance_m"), settings, emptyList()))
        assertEquals("fluid_ounce", preferredHealthMetricUnitId(metric("hydration", UnitDimension.Volume, "litre"), settings, emptyList()))
        assertEquals("count", preferredHealthMetricUnitId(metric("steps", UnitDimension.Count, "count"), settings, emptyList()))
    }

    private fun metric(id: String, dimension: UnitDimension, unitId: String) = MetricDefinition(
        id = id,
        name = id,
        valueKind = MetricValueKind.Decimal,
        dimension = dimension,
        defaultUnitId = unitId,
        precision = 2,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )
}
