package com.whip.app.ui

import com.whip.app.core.AppSettings
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SemanticDefaultsTest {
    @Test
    fun reachWeightTemplateUsesConfiguredMassUnitAndCanonicalTarget() {
        val today = LocalDate.of(2026, 9, 3)

        val kilograms = reachWeightTemplateDraft(AppSettings(massUnitId = "kilogram"), today)
        val pounds = reachWeightTemplateDraft(AppSettings(massUnitId = "pound"), today)
        val grams = reachWeightTemplateDraft(AppSettings(massUnitId = "gram"), today)

        assertEquals("kilogram", kilograms.unitId)
        assertEquals(75.0, kilograms.targetMin ?: Double.NaN, 0.0)
        assertEquals("pound", pounds.unitId)
        assertEquals(150.0, pounds.targetMin ?: Double.NaN, 0.0)
        assertEquals("gram", grams.unitId)
        assertEquals(75_000.0, grams.targetMin ?: Double.NaN, 0.0)
    }

    @Test
    fun nextUnusedReminderMinutePrefersMorningThenConventionalSlotsAndSanitizesInput() {
        assertEquals(8 * 60, nextUnusedReminderMinute(emptyList()))
        assertEquals(9 * 60, nextUnusedReminderMinute(listOf(8 * 60)))
        assertEquals(8 * 60 + 15, nextUnusedReminderMinute((0 until 24).map { (8 * 60 + it * 60) % 1440 }))
        assertEquals(8 * 60, nextUnusedReminderMinute(listOf(-1, 1440, 2_000)))
        assertEquals(10 * 60, nextUnusedReminderMinute(listOf(9 * 60), preferred = 9 * 60))
        assertNull(nextUnusedReminderMinute((0..1439).toList()))
    }

    @Test
    fun freshNumberUnitUsesAnActivePreferenceOrTheFirstActiveCompatibleUnit() {
        val units = listOf(
            UnitDefinition("archived-pound", "archived pounds", "lb", UnitDimension.Mass, 1.0, archived = true),
            UnitDefinition("kilogram", "kilograms", "kg", UnitDimension.Mass, 1.0),
            UnitDefinition("pound", "pounds", "lb", UnitDimension.Mass, 0.45359237),
        )

        assertEquals(
            "pound",
            freshTrackNumberUnitId(AppSettings(massUnitId = "pound"), units, UnitDimension.Mass),
        )
        assertEquals(
            "kilogram",
            freshTrackNumberUnitId(AppSettings(massUnitId = "missing"), units, UnitDimension.Mass),
        )
    }
}
