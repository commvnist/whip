package com.whip.app.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.AvoidMissingPolicy
import com.whip.app.domain.AreaScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class AppThemeMode { System, Light, Dark }
enum class HomeSection { Tasks, Habits, Goals, Gym }
enum class HealthDataType { Weight, Steps, Distance, Hydration, Sleep, Exercise }
enum class ReviewPeriod { Weekly, Monthly }

data class AppSettings(
    val setupCompleted: Boolean = false,
    val powerMode: Boolean = false,
    val lowPressureMode: Boolean = false,
    val backupPrivacyChoice: String = "Later",
    /** False until the first-run backup choice has opened the matching setup flow. */
    val backupPrivacyChoiceHandled: Boolean = true,
    val notificationPermissionRequested: Boolean = false,
    val activeAreaScope: String = AreaScope.All.storageKey,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val dynamicColor: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val timeZoneId: String? = null,
    val dayCutoffMinutes: Int = 0,
    val massUnitId: String = "kilogram",
    val distanceUnitId: String = "kilometre",
    val volumeUnitId: String = "litre",
    val gymWeightUnitId: String = "kilogram",
    val numberPrecision: Int = 1,
    val oneRepMaxFormula: String = "Epley",
    val oneRepMaxRepCutoff: Int = 10,
    val defaultRestSeconds: Int = 120,
    val timerSound: Boolean = true,
    val timerVibration: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val restTimerAutoStart: Boolean = true,
    val showGymRpe: Boolean = true,
    val showGymRir: Boolean = true,
    val showGymTempo: Boolean = true,
    val includeWarmupsInGymStats: Boolean = false,
    val hardSetClassifications: Set<String> = setOf("Working", "BackOff", "Drop", "Amrap", "Failure"),
    val categoryAllocationMode: String = "Fractional",
    val adjustE1rmForEffort: Boolean = false,
    val includeAssistedInPersonalRecords: Boolean = false,
    val quietStartMinutes: Int? = null,
    val quietEndMinutes: Int? = null,
    val homeSections: List<HomeSection> = HomeSection.entries,
    val hiddenHomeSections: Set<HomeSection> = emptySet(),
    val collapsedHomeSections: Set<HomeSection> = emptySet(),
    val healthConnectEnabled: Boolean = false,
    val healthDataTypes: Set<HealthDataType> = HealthDataType.entries.toSet(),
    val healthSyncDays: Int = 30,
    val reviewPeriod: ReviewPeriod = ReviewPeriod.Weekly,
    val defaultTaskStepPolicy: RepeatStepPolicy = RepeatStepPolicy.Reset,
    val showAllUpcomingTaskOccurrences: Boolean = false,
    val showHabitsInTaskPlanning: Boolean = false,
    val defaultHabitWeekStart: DayOfWeek = DayOfWeek.MONDAY,
    val defaultAvoidMissingPolicy: AvoidMissingPolicy = AvoidMissingPolicy.Unknown,
    val naturalLanguageTaskCapture: Boolean = false,
    val savedTaskFilters: List<SavedTaskFilter> = emptyList(),
    val homeTaskFilterName: String? = null,
    val savedReviewFilters: List<SavedReviewFilter> = emptyList(),
    val selectedReviewFilterName: String? = null,
    val reviewSections: Set<HomeSection> = HomeSection.entries.toSet(),
    val gymCompactSetRows: Boolean = false,
    val platePresets: List<PlatePreset> = emptyList(),
    val repPrescriptionSchemes: List<RepPrescriptionScheme> = emptyList(),
    val locationRemindersEnabled: Boolean = true,
    val focusTimerDeadlineMillis: Long? = null,
    val focusTimerTaskId: Long? = null,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    fun current(): AppSettings
    fun update(transform: (AppSettings) -> AppSettings)
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.getSharedPreferences("whip-settings", Context.MODE_PRIVATE)

    override val settings: Flow<AppSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(current()) }
        trySend(current())
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override fun current(): AppSettings = AppSettings(
        setupCompleted = preferences.getBoolean("setupCompleted", false),
        powerMode = preferences.getBoolean("powerMode", false),
        lowPressureMode = preferences.getBoolean("lowPressureMode", false),
        backupPrivacyChoice = preferences.getString("backupPrivacyChoice", "Later") ?: "Later",
        backupPrivacyChoiceHandled = preferences.getBoolean("backupPrivacyChoiceHandled", true),
        notificationPermissionRequested = preferences.getBoolean("notificationPermissionRequested", false),
        activeAreaScope = preferences.getString("activeAreaScope", AreaScope.All.storageKey)
            ?: AreaScope.All.storageKey,
        themeMode = preferences.enum("theme", AppThemeMode.System),
        dynamicColor = preferences.getBoolean("dynamicColor", true),
        firstDayOfWeek = preferences.enum("firstDay", DayOfWeek.MONDAY),
        timeZoneId = preferences.getString("timeZoneId", null)?.takeIf { runCatching { ZoneId.of(it) }.isSuccess },
        dayCutoffMinutes = preferences.getInt("dayCutoff", 0).coerceIn(0, 1439),
        massUnitId = normalizeMassUnit(preferences.getString("massUnit", "kilogram")),
        distanceUnitId = normalizeDistanceUnit(preferences.getString("distanceUnit", "kilometre")),
        volumeUnitId = normalizeVolumeUnit(preferences.getString("volumeUnit", "litre")),
        gymWeightUnitId = normalizeMassUnit(preferences.getString("gymWeightUnit", "kilogram")),
        numberPrecision = preferences.getInt("precision", 1).coerceIn(0, 6),
        oneRepMaxFormula = preferences.getString("e1rmFormula", "Epley") ?: "Epley",
        oneRepMaxRepCutoff = preferences.getInt("e1rmCutoff", 10).coerceIn(1, 36),
        defaultRestSeconds = preferences.getInt("defaultRest", 120).coerceAtLeast(0),
        timerSound = preferences.getBoolean("timerSound", true),
        timerVibration = preferences.getBoolean("timerVibration", true),
        keepScreenAwake = preferences.getBoolean("keepAwake", false),
        restTimerAutoStart = preferences.getBoolean("restAutoStart", true),
        showGymRpe = preferences.getBoolean("gymShowRpe", true),
        showGymRir = preferences.getBoolean("gymShowRir", true),
        showGymTempo = preferences.getBoolean("gymShowTempo", true),
        includeWarmupsInGymStats = preferences.getBoolean("gymIncludeWarmups", false),
        hardSetClassifications = preferences.getStringSet("gymHardSetClassifications", null)
            ?.filterTo(mutableSetOf()) { it in setOf("Working", "BackOff", "Drop", "Amrap", "Failure", "WarmUp") }
            ?.takeIf(Set<String>::isNotEmpty)
            ?: setOf("Working", "BackOff", "Drop", "Amrap", "Failure"),
        categoryAllocationMode = preferences.getString("gymCategoryAllocationMode", "Fractional")
            ?.takeIf { it in setOf("Full", "Fractional", "PrimaryOnly") } ?: "Fractional",
        adjustE1rmForEffort = preferences.getBoolean("gymAdjustE1rmForEffort", false),
        includeAssistedInPersonalRecords = preferences.getBoolean("gymIncludeAssistedInPr", false),
        quietStartMinutes = preferences.nullableInt("quietStart"),
        quietEndMinutes = preferences.nullableInt("quietEnd"),
        homeSections = preferences.getString("homeOrder", null)?.split(',')?.mapNotNull { runCatching { HomeSection.valueOf(it) }.getOrNull() }
            ?.takeIf { it.toSet() == HomeSection.entries.toSet() } ?: HomeSection.entries,
        hiddenHomeSections = preferences.enumSet("homeHidden", HomeSection.entries),
        collapsedHomeSections = preferences.enumSet("homeCollapsed", HomeSection.entries),
        healthConnectEnabled = preferences.getBoolean("healthEnabled", false),
        healthDataTypes = preferences.getStringSet("healthTypes", null)
            ?.mapNotNullTo(mutableSetOf()) { runCatching { HealthDataType.valueOf(it) }.getOrNull() }
            ?: HealthDataType.entries.toSet(),
        healthSyncDays = preferences.getInt("healthSyncDays", 30).coerceIn(1, 365),
        reviewPeriod = preferences.enum("reviewPeriod", ReviewPeriod.Weekly),
        defaultTaskStepPolicy = preferences.enum("taskStepPolicy", RepeatStepPolicy.Reset),
        showAllUpcomingTaskOccurrences = preferences.getBoolean("showAllUpcomingTaskOccurrences", false),
        showHabitsInTaskPlanning = preferences.getBoolean("showHabitsInTaskPlanning", false),
        defaultHabitWeekStart = preferences.enum("habitWeekStart", DayOfWeek.MONDAY),
        defaultAvoidMissingPolicy = preferences.enum("avoidMissingPolicy", AvoidMissingPolicy.Unknown),
        naturalLanguageTaskCapture = preferences.getBoolean("naturalLanguageTaskCapture", false),
        savedTaskFilters = preferences.getString("savedTaskFilters", null).decodeTaskFilters(),
        homeTaskFilterName = preferences.getString("homeTaskFilterName", null),
        savedReviewFilters = preferences.getString("savedReviewFilters", null).decodeReviewFilters(),
        selectedReviewFilterName = preferences.getString("selectedReviewFilterName", null),
        reviewSections = preferences.enumSet("reviewSections", HomeSection.entries)
            .ifEmpty { HomeSection.entries.toSet() },
        gymCompactSetRows = preferences.getBoolean("gymCompactSetRows", false),
        platePresets = preferences.getString("platePresets", null).decodePlatePresets(),
        repPrescriptionSchemes = preferences.getString("repPrescriptionSchemes", null).decodeRepPrescriptionSchemes(),
        locationRemindersEnabled = preferences.getBoolean("locationRemindersEnabled", true),
        focusTimerDeadlineMillis = preferences.nullableLong("focusTimerDeadlineMillis")
            ?.takeIf { it > System.currentTimeMillis() },
        focusTimerTaskId = preferences.nullableLong("focusTimerTaskId"),
    )

    @SuppressLint("UseKtx")
    override fun update(transform: (AppSettings) -> AppSettings) {
        val value = transform(current()).withValidUnits()
        preferences.edit()
            .putBoolean("setupCompleted", value.setupCompleted)
            .putBoolean("powerMode", value.powerMode)
            .putBoolean("lowPressureMode", value.lowPressureMode)
            .putString("backupPrivacyChoice", value.backupPrivacyChoice)
            .putBoolean("backupPrivacyChoiceHandled", value.backupPrivacyChoiceHandled)
            .putBoolean("notificationPermissionRequested", value.notificationPermissionRequested)
            .putString("activeAreaScope", value.activeAreaScope)
            .putString("theme", value.themeMode.name)
            .putBoolean("dynamicColor", value.dynamicColor)
            .putString("firstDay", value.firstDayOfWeek.name)
            .putNullableString("timeZoneId", value.timeZoneId)
            .putInt("dayCutoff", value.dayCutoffMinutes)
            .putString("massUnit", value.massUnitId)
            .putString("distanceUnit", value.distanceUnitId)
            .putString("volumeUnit", value.volumeUnitId)
            .putString("gymWeightUnit", value.gymWeightUnitId)
            .putInt("precision", value.numberPrecision)
            .putString("e1rmFormula", value.oneRepMaxFormula)
            .putInt("e1rmCutoff", value.oneRepMaxRepCutoff)
            .putInt("defaultRest", value.defaultRestSeconds)
            .putBoolean("timerSound", value.timerSound)
            .putBoolean("timerVibration", value.timerVibration)
            .putBoolean("keepAwake", value.keepScreenAwake)
            .putBoolean("restAutoStart", value.restTimerAutoStart)
            .putBoolean("gymShowRpe", value.showGymRpe)
            .putBoolean("gymShowRir", value.showGymRir)
            .putBoolean("gymShowTempo", value.showGymTempo)
            .putBoolean("gymIncludeWarmups", value.includeWarmupsInGymStats)
            .putStringSet("gymHardSetClassifications", value.hardSetClassifications)
            .putString("gymCategoryAllocationMode", value.categoryAllocationMode)
            .putBoolean("gymAdjustE1rmForEffort", value.adjustE1rmForEffort)
            .putBoolean("gymIncludeAssistedInPr", value.includeAssistedInPersonalRecords)
            .putNullableInt("quietStart", value.quietStartMinutes)
            .putNullableInt("quietEnd", value.quietEndMinutes)
            .putString("homeOrder", value.homeSections.joinToString(",", transform = HomeSection::name))
            .putStringSet("homeHidden", value.hiddenHomeSections.mapTo(mutableSetOf(), HomeSection::name))
            .putStringSet("homeCollapsed", value.collapsedHomeSections.mapTo(mutableSetOf(), HomeSection::name))
            .putBoolean("healthEnabled", value.healthConnectEnabled)
            .putStringSet("healthTypes", value.healthDataTypes.mapTo(mutableSetOf(), HealthDataType::name))
            .putInt("healthSyncDays", value.healthSyncDays.coerceIn(1, 365))
            .putString("reviewPeriod", value.reviewPeriod.name)
            .putString("taskStepPolicy", value.defaultTaskStepPolicy.name)
            .putBoolean("showAllUpcomingTaskOccurrences", value.showAllUpcomingTaskOccurrences)
            .putBoolean("showHabitsInTaskPlanning", value.showHabitsInTaskPlanning)
            .putString("habitWeekStart", value.defaultHabitWeekStart.name)
            .putString("avoidMissingPolicy", value.defaultAvoidMissingPolicy.name)
            .putBoolean("naturalLanguageTaskCapture", value.naturalLanguageTaskCapture)
            .putString("savedTaskFilters", value.savedTaskFilters.encodeTaskFilters())
            .putNullableString("homeTaskFilterName", value.homeTaskFilterName)
            .putString("savedReviewFilters", value.savedReviewFilters.encodeReviewFilters())
            .putNullableString("selectedReviewFilterName", value.selectedReviewFilterName)
            .putStringSet("reviewSections", value.reviewSections.mapTo(mutableSetOf(), HomeSection::name))
            .putBoolean("gymCompactSetRows", value.gymCompactSetRows)
            .putString("platePresets", value.platePresets.encodePlatePresets())
            .putString("repPrescriptionSchemes", value.repPrescriptionSchemes.encodeRepPrescriptionSchemes())
            .putBoolean("locationRemindersEnabled", value.locationRemindersEnabled)
            .putNullableLong("focusTimerDeadlineMillis", value.focusTimerDeadlineMillis)
            .putNullableLong("focusTimerTaskId", value.focusTimerTaskId)
            .apply()
    }
}

fun AppSettings.withValidUnits(): AppSettings = copy(
    massUnitId = normalizeMassUnit(massUnitId),
    distanceUnitId = normalizeDistanceUnit(distanceUnitId),
    volumeUnitId = normalizeVolumeUnit(volumeUnitId),
    gymWeightUnitId = normalizeMassUnit(gymWeightUnitId),
)

fun normalizeMassUnit(value: String?): String = when (value?.trim()?.lowercase()) {
    "pound", "pounds", "lb", "lbs" -> "pound"
    "gram", "grams", "g" -> "gram"
    else -> "kilogram"
}

fun normalizeDistanceUnit(value: String?): String = when (value?.trim()?.lowercase()) {
    "mile", "miles", "mi" -> "mile"
    "distance_m", "metre", "metres", "meter", "meters", "m" -> "distance_m"
    else -> "kilometre"
}

fun normalizeVolumeUnit(value: String?): String = when (value?.trim()?.lowercase()) {
    "millilitre", "millilitres", "milliliter", "milliliters", "ml" -> "millilitre"
    "cup", "cups" -> "cup"
    "fluid_ounce", "fluid ounce", "fluid ounces", "fl oz", "floz" -> "fluid_ounce"
    else -> "litre"
}

class SettingsWhipClock(
    private val settingsRepository: SettingsRepository,
    private val nowProvider: () -> Instant = Instant::now,
) : WhipClock {
    override fun now(): Instant = nowProvider()
    override fun zoneId(): ZoneId = settingsRepository.current().zoneId()
    override fun today(zoneId: ZoneId): LocalDate {
        val local = now().atZone(zoneId)
        val cutoff = settingsRepository.current().dayCutoffMinutes
        val minute = local.hour * 60 + local.minute
        return if (cutoff > 0 && minute < cutoff) local.toLocalDate().minusDays(1) else local.toLocalDate()
    }
}

fun AppSettings.zoneId(): ZoneId = timeZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() }
    ?: ZoneId.systemDefault()

private inline fun <reified T : Enum<T>> SharedPreferences.enum(key: String, default: T): T =
    getString(key, null)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

private fun <T : Enum<T>> SharedPreferences.enumSet(key: String, values: List<T>): Set<T> {
    val names = getStringSet(key, emptySet()).orEmpty()
    return values.filterTo(mutableSetOf()) { it.name in names }
}

private fun SharedPreferences.nullableInt(key: String): Int? = if (contains(key)) getInt(key, 0) else null
private fun SharedPreferences.nullableLong(key: String): Long? = if (contains(key)) getLong(key, 0L) else null
private fun SharedPreferences.Editor.putNullableInt(key: String, value: Int?): SharedPreferences.Editor =
    if (value == null) remove(key) else putInt(key, value)
private fun SharedPreferences.Editor.putNullableLong(key: String, value: Long?): SharedPreferences.Editor =
    if (value == null) remove(key) else putLong(key, value)
private fun SharedPreferences.Editor.putNullableString(key: String, value: String?): SharedPreferences.Editor =
    if (value == null) remove(key) else putString(key, value)
