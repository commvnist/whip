package com.whip.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.whip.app.domain.AreaScope
import com.whip.app.domain.CustomIdentityEmoji

class AppSettingsTest {
    @Test
    fun upcomingRepeatingTasksAreCompactByDefault() {
        assertEquals(false, AppSettings().showAllUpcomingTaskOccurrences)
        assertEquals(false, AppSettings().showHabitsInTaskPlanning)
        assertEquals(false, AppSettings().compactItemLayout)
        assertEquals(false, AppSettings().dynamicColor)
        assertEquals(true, AppSettings().naturalLanguageTaskCapture)
        assertEquals(emptyList<RepPrescriptionScheme>(), AppSettings().repPrescriptionSchemes)
        assertEquals(AreaScope.All.storageKey, AppSettings().activeAreaScope)
        assertEquals(AreaOpeningMode.LastUsed, AppSettings().areaOpeningMode)
        assertEquals(AreaScope.All.storageKey, AppSettings().chosenOpeningAreaScope)
        assertEquals(listOf(60, 90, 120, 150, 180, 300), AppSettings().restTimerPresetSeconds)
    }

    @Test
    fun openingAreaCanFollowLastUsedOrKeepASeparateChosenDefault() {
        val lastUsed = AreaScope.One("last-used")
        val chosen = AreaScope.One("chosen")
        val settings = AppSettings(
            activeAreaScope = lastUsed.storageKey,
            chosenOpeningAreaScope = chosen.storageKey,
        )

        assertEquals(lastUsed, settings.openingAreaScope())
        assertEquals(
            chosen,
            settings.copy(areaOpeningMode = AreaOpeningMode.Chosen).openingAreaScope(),
        )
        assertEquals(lastUsed.storageKey, settings.activeAreaScope)
    }

    @Test
    fun defaultClockTodayUsesTheClockZone() {
        val clock = object : WhipClock {
            override fun now(): Instant = Instant.parse("2026-08-25T01:30:00Z")
            override fun zoneId(): ZoneId = ZoneId.of("America/Toronto")
        }

        assertEquals(LocalDate.of(2026, 8, 24), clock.today())
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

    @Test
    fun normalizationKeepsAtLeastOneHomeSectionVisibleAndRepairsNumericDefaults() {
        val normalized = AppSettings(
            homeSections = listOf(HomeSection.Goals, HomeSection.Tasks),
            hiddenHomeSections = HomeSection.entries.toSet(),
            collapsedHomeSections = HomeSection.entries.toSet() + HomeSection.Goals,
            defaultRestSeconds = 0,
            numberPrecision = 20,
            healthSyncDays = 0,
            customIdentityEmojis = listOf(
                CustomIdentityEmoji("🦊", "Fox"),
                CustomIdentityEmoji("✅", "Built-In"),
                CustomIdentityEmoji("text", "Invalid"),
                CustomIdentityEmoji("🦊", "Duplicate Fox"),
                CustomIdentityEmoji("🦄", "Unicorn"),
            ),
        ).normalized()

        assertEquals(HomeSection.Goals, normalized.visibleHomeSections().single())
        assertFalse(HomeSection.Goals in normalized.hiddenHomeSections)
        assertTrue(HomeSection.entries.all { it in normalized.homeSections })
        assertEquals(15, normalized.defaultRestSeconds)
        assertEquals(6, normalized.numberPrecision)
        assertEquals(1, normalized.healthSyncDays)
        assertEquals(
            listOf(CustomIdentityEmoji("🦊", "Fox"), CustomIdentityEmoji("🦄", "Unicorn")),
            normalized.customIdentityEmojis,
        )
    }

    @Test
    fun changingTimeZoneRecomputesTodayWithoutWaitingForTheMinuteTicker() = runBlocking {
        val settings = FakeSettingsRepository(AppSettings(timeZoneId = "UTC"))
        val clock = SettingsWhipClock(settings) { Instant.parse("2026-08-23T02:00:00Z") }
        val dates = mutableListOf<LocalDate>()
        val firstEmission = CompletableDeferred<Unit>()
        val collection = launch {
            settings.currentDateFlow(clock).take(2).collect { date ->
                dates += date
                firstEmission.complete(Unit)
            }
        }
        withTimeout(5_000) { firstEmission.await() }

        settings.update { it.copy(timeZoneId = "America/Toronto") }
        withTimeout(5_000) { collection.join() }

        assertEquals(listOf(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 22)), dates)
    }

    @Test
    fun calendarContextRetainsZoneChangesEvenWhenTheDateDoesNotChange() = runBlocking {
        val settings = FakeSettingsRepository(AppSettings(timeZoneId = "UTC"))
        val clock = SettingsWhipClock(settings) { Instant.parse("2026-08-23T12:00:00Z") }
        val contexts = mutableListOf<WhipCalendarContext>()
        val firstEmission = CompletableDeferred<Unit>()
        val collection = launch {
            settings.calendarContextFlow(clock).take(2).collect { context ->
                contexts += context
                firstEmission.complete(Unit)
            }
        }
        withTimeout(5_000) { firstEmission.await() }

        settings.update { it.copy(timeZoneId = "Europe/London") }
        withTimeout(5_000) { collection.join() }

        assertEquals(listOf("UTC", "Europe/London"), contexts.map { it.zoneId.id })
        assertEquals(listOf(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 23)), contexts.map { it.logicalDate })
    }

    @Test
    fun calendarContextDistinguishesPhysicalAndCutoffAdjustedDates() {
        val instant = Instant.parse("2026-09-01T05:30:00Z") // 01:30 in Toronto.
        val context = AppSettings(
            timeZoneId = "America/Toronto",
            dayCutoffMinutes = 4 * 60,
        ).calendarContextAt(instant)

        assertEquals(LocalDate.of(2026, 9, 1), context.physicalDate)
        assertEquals(LocalDate.of(2026, 8, 31), context.logicalDate)
        assertEquals(240, context.cutoffMinutes)
        assertFalse(context.followsDeviceTimeZone)
    }

    @Test
    fun explicitInvalidationRecomputesAFixedZoneAcrossTheLogicalBoundary() = runBlocking {
        val settings = FakeSettingsRepository(AppSettings(timeZoneId = "America/Toronto"))
        var now = Instant.parse("2026-09-01T07:59:59Z")
        settings.update { it.copy(dayCutoffMinutes = 4 * 60) }
        val clock = SettingsWhipClock(settings) { now }
        val invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val contexts = mutableListOf<WhipCalendarContext>()
        val firstEmission = CompletableDeferred<Unit>()
        val collection = launch {
            settings.calendarContextFlow(clock, invalidations).take(2).collect { context ->
                contexts += context
                firstEmission.complete(Unit)
            }
        }
        withTimeout(5_000) { firstEmission.await() }

        now = Instant.parse("2026-09-01T08:00:00Z")
        invalidations.emit(Unit)
        withTimeout(5_000) { collection.join() }

        assertEquals(listOf(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 1)), contexts.map { it.logicalDate })
        assertTrue(contexts.all { it.zoneId.id == "America/Toronto" })
    }

    private class FakeSettingsRepository(initial: AppSettings) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<AppSettings> = state
        override fun current(): AppSettings = state.value
        override fun update(transform: (AppSettings) -> AppSettings) { state.value = transform(state.value) }
    }
}
