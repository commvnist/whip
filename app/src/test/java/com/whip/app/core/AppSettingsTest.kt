package com.whip.app.core

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import com.whip.app.domain.AreaScope

class AppSettingsTest {
    @Test
    fun upcomingRepeatingTasksAreCompactByDefault() {
        assertEquals(false, AppSettings().showAllUpcomingTaskOccurrences)
        assertEquals(false, AppSettings().showHabitsInTaskPlanning)
        assertEquals(emptyList<RepPrescriptionScheme>(), AppSettings().repPrescriptionSchemes)
        assertEquals(AreaScope.All.storageKey, AppSettings().activeAreaScope)
        assertEquals(listOf(60, 90, 120, 150, 180, 300), AppSettings().restTimerPresetSeconds)
    }

    @Test
    fun restTimerPresetsAreValidatedSortedAndNeverEmpty() {
        assertEquals(listOf(45, 90, 300), normalizeRestTimerPresets(listOf(300, 90, 45, 90, -1, 4_000)))
        assertEquals(DEFAULT_REST_TIMER_PRESET_SECONDS, normalizeRestTimerPresets(emptyList()))
    }

    @Test
    fun configuredZoneAndLateNightCutoffDefineToday() {
        val settings = FakeSettingsRepository(
            AppSettings(timeZoneId = "America/Toronto", dayCutoffMinutes = 2 * 60),
        )
        val clock = SettingsWhipClock(settings) { Instant.parse("2026-08-18T05:30:00Z") }

        assertEquals("America/Toronto", clock.zoneId().id)
        assertEquals(LocalDate.of(2026, 8, 17), clock.today())
    }

    @Test
    fun userFacingUnitNamesAreNormalizedToSupportedIds() {
        assertEquals("pound", normalizeMassUnit("pounds"))
        assertEquals("pound", normalizeMassUnit("LBS"))
        assertEquals("kilometre", normalizeDistanceUnit("km"))
        assertEquals("fluid_ounce", normalizeVolumeUnit("fl oz"))
        assertEquals("kilogram", normalizeMassUnit("not a unit"))
    }

    private class FakeSettingsRepository(initial: AppSettings) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<AppSettings> = state
        override fun current(): AppSettings = state.value
        override fun update(transform: (AppSettings) -> AppSettings) { state.value = transform(state.value) }
    }
}
