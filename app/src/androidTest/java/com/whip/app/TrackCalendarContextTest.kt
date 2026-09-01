package com.whip.app

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.TrackViewModel
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackCalendarContextTest {
    @Test
    fun trackRollsToANewLogicalDateWithoutARepositoryEmission() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val original = app.settingsRepository.current()
        val now = app.clock.now()
        val utcHour = now.atZone(ZoneOffset.UTC).hour
        val zone: ZoneId = if (utcHour >= 21) ZoneOffset.ofHours(-10) else ZoneOffset.UTC
        val local = now.atZone(zone)
        val cutoffAfterNow = local.hour * 60 + local.minute + 60
        try {
            app.settingsRepository.update {
                it.copy(timeZoneId = zone.id, dayCutoffMinutes = 0)
            }
            val viewModel = TrackViewModel(app, SavedStateHandle())
            val physicalDate = local.toLocalDate()
            assertEquals(
                physicalDate,
                withTimeout(5_000) { viewModel.uiState.first { !it.loading }.currentDate },
            )

            // No Track row changes. A calendar-context settings emission alone must reproject.
            app.settingsRepository.update { it.copy(dayCutoffMinutes = cutoffAfterNow) }

            assertEquals(
                physicalDate.minusDays(1),
                withTimeout(5_000) {
                    viewModel.uiState.first { !it.loading && it.currentDate == physicalDate.minusDays(1) }.currentDate
                },
            )
        } finally {
            app.settingsRepository.update { original }
        }
    }
}
