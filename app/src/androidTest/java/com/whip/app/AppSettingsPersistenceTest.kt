package com.whip.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.HealthDataType
import com.whip.app.core.HomeSection
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.SharedPreferencesSettingsRepository
import com.whip.app.core.TrackedGymRecord
import com.whip.app.core.normalized
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.PersonalRecordType
import java.time.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingsPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun resetPreferences() {
        context.getSharedPreferences("whip-settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun settingsMigrationsRunAndAnExplicitSmartCaptureOptOutPersists() {
        val preferences = context.getSharedPreferences("whip-settings", Context.MODE_PRIVATE)
        preferences
            .edit()
            .clear()
            .putBoolean("naturalLanguageTaskCapture", false)
            .putString("savedReviewFilters", "legacy")
            .putString("selectedReviewFilterName", "Legacy")
            .commit()

        val migrated = SharedPreferencesSettingsRepository(context)
        assertTrue(migrated.current().naturalLanguageTaskCapture)
        assertFalse(preferences.contains("savedReviewFilters"))
        assertFalse(preferences.contains("selectedReviewFilterName"))

        migrated.update { settings -> settings.copy(naturalLanguageTaskCapture = false) }
        assertFalse(SharedPreferencesSettingsRepository(context).current().naturalLanguageTaskCapture)
    }

    @Test
    fun everyUserEditablePreferenceSurvivesRepositoryRecreation() {
        val expected = AppSettings(
            setupCompleted = true,
            powerMode = true,
            lowPressureMode = true,
            notificationPermissionRequested = true,
            themeMode = AppThemeMode.Dark,
            dynamicColor = false,
            compactItemLayout = true,
            firstDayOfWeek = DayOfWeek.SUNDAY,
            timeZoneId = "America/Toronto",
            dayCutoffMinutes = 180,
            massUnitId = "pound",
            distanceUnitId = "mile",
            volumeUnitId = "cup",
            gymWeightUnitId = "pound",
            numberPrecision = 3,
            oneRepMaxFormula = "Brzycki",
            oneRepMaxRepCutoff = 8,
            defaultRestSeconds = 150,
            restTimerPresetSeconds = listOf(45, 90, 180),
            timerSound = false,
            timerVibration = false,
            keepScreenAwake = true,
            restTimerAutoStart = false,
            showGymRpe = true,
            showGymRir = false,
            showGymTempo = false,
            includeWarmupsInGymStats = true,
            hardSetClassifications = setOf("Working", "Failure"),
            categoryAllocationMode = "PrimaryOnly",
            adjustE1rmForEffort = true,
            includeAssistedInPersonalRecords = true,
            quietStartMinutes = 1_320,
            quietEndMinutes = 420,
            homeSections = listOf(HomeSection.Goals, HomeSection.Tasks, HomeSection.Tracks, HomeSection.Habits, HomeSection.Gym),
            hiddenHomeSections = setOf(HomeSection.Goals, HomeSection.Gym),
            collapsedHomeSections = setOf(HomeSection.Habits),
            healthConnectEnabled = true,
            healthDataTypes = setOf(HealthDataType.Weight, HealthDataType.Sleep),
            healthSyncDays = 90,
            reviewPeriod = ReviewPeriod.Monthly,
            defaultTaskStepPolicy = RepeatStepPolicy.CarryUnfinished,
            showAllUpcomingTaskOccurrences = true,
            showHabitsInTaskPlanning = true,
            defaultHabitWeekStart = DayOfWeek.SUNDAY,
            naturalLanguageTaskCapture = true,
            customIdentityEmojis = listOf(
                CustomIdentityEmoji("🦊", "Fox"),
                CustomIdentityEmoji("🦄", "Unicorn"),
            ),
            gymCompactSetRows = true,
            trackedGymRecords = listOf(
                TrackedGymRecord("bench-uuid", PersonalRecordType.EstimatedOneRepMax),
                TrackedGymRecord(
                    "bench-uuid",
                    PersonalRecordType.MaxWeight,
                    machineProfileUuid = "rack-uuid",
                    position = 1,
                ),
            ),
        ).normalized()

        SharedPreferencesSettingsRepository(context).update { expected }

        assertEquals(expected, SharedPreferencesSettingsRepository(context).current())
    }
}
