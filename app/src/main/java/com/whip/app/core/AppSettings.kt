package com.whip.app.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.AreaScope
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.normalizeCustomIdentityEmojis
import com.whip.app.domain.WorkoutSetClassification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive

enum class AppThemeMode(val label: String) {
    System("Follow device"),
    Light("Light"),
    Dark("Dark"),
}
enum class AreaOpeningMode { LastUsed, Chosen }
enum class HomeSection(val label: String) {
    Tasks("Tasks"),
    Habits("Habits"),
    Goals("Goals"),
    Tracks("Tracks"),
    Gym("Gym"),
}
enum class ReviewSection(val label: String) {
    Tasks("Tasks"),
    Habits("Habits"),
    Goals("Goals"),
    Gym("Gym"),
}
enum class HealthDataType(val label: String) {
    Weight("Weight"),
    Steps("Steps"),
    Distance("Distance"),
    Hydration("Hydration"),
    Sleep("Sleep"),
    Exercise("Exercise sessions"),
}
enum class ReviewPeriod(val label: String) {
    Weekly("Weekly"),
    Monthly("Monthly"),
}

val DEFAULT_REST_TIMER_PRESET_SECONDS: List<Int> = listOf(60, 90, 120, 150, 180, 300)

fun normalizeRestTimerPresets(values: Iterable<Int>): List<Int> = values
    .filter { it in 15..3_600 }
    .distinct()
    .sorted()
    .take(12)
    .ifEmpty { DEFAULT_REST_TIMER_PRESET_SECONDS }

data class AppSettings(
    val setupCompleted: Boolean = false,
    val powerMode: Boolean = false,
    val lowPressureMode: Boolean = false,
    val notificationPermissionRequested: Boolean = false,
    val activeAreaScope: String = AreaScope.All.storageKey,
    val areaOpeningMode: AreaOpeningMode = AreaOpeningMode.LastUsed,
    val chosenOpeningAreaScope: String = AreaScope.All.storageKey,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val dynamicColor: Boolean = false,
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
    val restTimerPresetSeconds: List<Int> = DEFAULT_REST_TIMER_PRESET_SECONDS,
    val timerSound: Boolean = true,
    val timerVibration: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val restTimerAutoStart: Boolean = true,
    val showGymRpe: Boolean = false,
    val showGymRir: Boolean = false,
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
    val healthDataTypes: Set<HealthDataType> = emptySet(),
    val healthSyncDays: Int = 30,
    /** Local-only sync receipt metadata; portable backups do not export it. */
    val healthLastSyncMillis: Long? = null,
    val healthLastSyncCount: Int = 0,
    /** Local-only crash-recovery journal; never exported in portable backups. */
    val healthConnectDeletionPending: Boolean = false,
    val reviewPeriod: ReviewPeriod = ReviewPeriod.Weekly,
    val defaultTaskStepPolicy: RepeatStepPolicy = RepeatStepPolicy.Reset,
    val showAllUpcomingTaskOccurrences: Boolean = false,
    val showHabitsInTaskPlanning: Boolean = false,
    val activeTaskSortMode: String = "Smart",
    val defaultHabitWeekStart: DayOfWeek = DayOfWeek.MONDAY,
    val naturalLanguageTaskCapture: Boolean = true,
    val customIdentityEmojis: List<CustomIdentityEmoji> = emptyList(),
    val savedTaskFilters: List<SavedTaskFilter> = emptyList(),
    val homeTaskFilterName: String? = null,
    val reviewSections: Set<ReviewSection> = ReviewSection.entries.toSet(),
    val gymCompactSetRows: Boolean = false,
    val platePresets: List<PlatePreset> = emptyList(),
    val repPrescriptionSchemes: List<RepPrescriptionScheme> = emptyList(),
    val trackedGymRecords: List<TrackedGymRecord> = emptyList(),
    val focusTimerDeadlineMillis: Long? = null,
    val focusTimerTaskId: Long? = null,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    fun current(): AppSettings
    fun update(transform: (AppSettings) -> AppSettings)

    /**
     * Persists a settings mutation synchronously and reports whether durable
     * storage accepted it. Authored editors use this boundary before telling a
     * person that Save succeeded; fire-and-forget system maintenance may keep
     * using [update].
     */
    fun updateAndConfirm(transform: (AppSettings) -> AppSettings): Boolean {
        update(transform)
        return true
    }
}

/**
 * An explicit pin action must have a visible destination. Preserve the user's
 * Home ordering while revealing and expanding the owning component.
 */
fun AppSettings.withHomeSectionRevealed(section: HomeSection): AppSettings = copy(
    hiddenHomeSections = hiddenHomeSections - section,
    collapsedHomeSections = collapsedHomeSections - section,
)

fun SettingsRepository.revealHomeSection(section: HomeSection) {
    update { settings -> settings.withHomeSectionRevealed(section) }
}

data class WhipCalendarContext(
    val zoneId: ZoneId,
    val physicalDate: LocalDate,
    val logicalDate: LocalDate,
    val cutoffMinutes: Int,
    val followsDeviceTimeZone: Boolean,
)

fun AppSettings.calendarContextAt(instant: Instant): WhipCalendarContext {
    val zone = zoneId()
    val local = instant.atZone(zone)
    val cutoff = dayCutoffMinutes.coerceIn(0, 1_439)
    val minute = local.hour * 60 + local.minute
    return WhipCalendarContext(
        zoneId = zone,
        physicalDate = local.toLocalDate(),
        logicalDate = if (cutoff > 0 && minute < cutoff) local.toLocalDate().minusDays(1) else local.toLocalDate(),
        cutoffMinutes = cutoff,
        followsDeviceTimeZone = timeZoneId == null,
    )
}

private fun calendarBoundaryTicks(clock: WhipClock): Flow<Unit> = flow {
    while (currentCoroutineContext().isActive) {
        emit(Unit)
        val millisIntoMinute = Math.floorMod(clock.now().toEpochMilli(), 60_000L)
        delay((60_000L - millisIntoMinute).coerceIn(1L, 60_000L))
    }
}

/**
 * One semantic calendar stream for live UI and background projections. Unlike a
 * LocalDate-only stream, this emits when the active zone changes on the same day.
 */
fun SettingsRepository.calendarContextFlow(
    clock: WhipClock,
    invalidations: Flow<Unit> = emptyFlow(),
): Flow<WhipCalendarContext> = combine(
    merge(calendarBoundaryTicks(clock), invalidations),
    settings,
) { _, current -> current.calendarContextAt(clock.now()) }.distinctUntilChanged()

/** Re-evaluates Today after time-zone/cutoff changes and at aligned minute boundaries. */
fun SettingsRepository.currentDateFlow(clock: WhipClock): Flow<LocalDate> =
    calendarContextFlow(clock).map { it.logicalDate }.distinctUntilChanged()

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
        notificationPermissionRequested = preferences.getBoolean("notificationPermissionRequested", false),
        activeAreaScope = preferences.getString("activeAreaScope", AreaScope.All.storageKey)
            ?: AreaScope.All.storageKey,
        areaOpeningMode = preferences.enum("areaOpeningMode", AreaOpeningMode.LastUsed),
        chosenOpeningAreaScope = preferences.getString("chosenOpeningAreaScope", AreaScope.All.storageKey)
            ?: AreaScope.All.storageKey,
        themeMode = preferences.enum("theme", AppThemeMode.System),
        dynamicColor = preferences.getBoolean("dynamicColor", false),
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
        defaultRestSeconds = preferences.getInt("defaultRest", 120).coerceIn(15, 3_600),
        restTimerPresetSeconds = normalizeRestTimerPresets(
            preferences.getString("restTimerPresets", null)
                ?.split(',')
                ?.mapNotNull { it.toIntOrNull() }
                .orEmpty()
                .ifEmpty { DEFAULT_REST_TIMER_PRESET_SECONDS },
        ),
        timerSound = preferences.getBoolean("timerSound", true),
        timerVibration = preferences.getBoolean("timerVibration", true),
        keepScreenAwake = preferences.getBoolean("keepAwake", false),
        restTimerAutoStart = preferences.getBoolean("restAutoStart", true),
        showGymRpe = preferences.getBoolean("gymShowRpe", false),
        showGymRir = preferences.getBoolean("gymShowRir", false),
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
        healthDataTypes = preferences.healthDataTypes(),
        healthSyncDays = preferences.getInt("healthSyncDays", 30).coerceIn(1, 365),
        healthLastSyncMillis = preferences.nullableLong("healthLastSyncMillis"),
        healthLastSyncCount = preferences.getInt("healthLastSyncCount", 0).coerceAtLeast(0),
        healthConnectDeletionPending = preferences.getBoolean("healthDeletionPending", false),
        reviewPeriod = preferences.enum("reviewPeriod", ReviewPeriod.Weekly),
        defaultTaskStepPolicy = preferences.enum("taskStepPolicy", RepeatStepPolicy.Reset),
        showAllUpcomingTaskOccurrences = preferences.getBoolean("showAllUpcomingTaskOccurrences", false),
        showHabitsInTaskPlanning = preferences.getBoolean("showHabitsInTaskPlanning", false),
        activeTaskSortMode = preferences.getString("activeTaskSortMode", "Smart") ?: "Smart",
        defaultHabitWeekStart = preferences.enum("habitWeekStart", DayOfWeek.MONDAY),
        naturalLanguageTaskCapture = preferences.getBoolean("naturalLanguageTaskCapture", true),
        customIdentityEmojis = preferences.getString("customIdentityEmojis", null).decodeCustomIdentityEmojis(),
        savedTaskFilters = preferences.getString("savedTaskFilters", null).decodeTaskFilters(),
        homeTaskFilterName = preferences.getString("homeTaskFilterName", null),
        reviewSections = preferences.enumSet("reviewSections", ReviewSection.entries)
            .ifEmpty { ReviewSection.entries.toSet() },
        gymCompactSetRows = preferences.getBoolean("gymCompactSetRows", false),
        platePresets = preferences.getString("platePresets", null).decodePlatePresets(),
        repPrescriptionSchemes = preferences.getString("repPrescriptionSchemes", null).decodeRepPrescriptionSchemes(),
        trackedGymRecords = preferences.getString("trackedGymRecords", null).decodeTrackedGymRecords(),
        focusTimerDeadlineMillis = preferences.nullableLong("focusTimerDeadlineMillis")
            ?.takeIf { it > System.currentTimeMillis() },
        focusTimerTaskId = preferences.nullableLong("focusTimerTaskId"),
    ).normalized()

    override fun update(transform: (AppSettings) -> AppSettings) {
        persist(transform, confirm = false)
    }

    override fun updateAndConfirm(transform: (AppSettings) -> AppSettings): Boolean =
        persist(transform, confirm = true)

    @SuppressLint("UseKtx")
    private fun persist(
        transform: (AppSettings) -> AppSettings,
        confirm: Boolean,
    ): Boolean = synchronized(updateLock) {
        val before = current()
        val value = transform(before).normalized()
        val editor = preferences.edit()
            .putBoolean("setupCompleted", value.setupCompleted)
            .putBoolean("powerMode", value.powerMode)
            .putBoolean("lowPressureMode", value.lowPressureMode)
            .remove("backupPrivacyChoice")
            .remove("backupPrivacyChoiceHandled")
            .putBoolean("notificationPermissionRequested", value.notificationPermissionRequested)
            .putString("activeAreaScope", value.activeAreaScope)
            .putString("areaOpeningMode", value.areaOpeningMode.name)
            .putString("chosenOpeningAreaScope", value.chosenOpeningAreaScope)
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
            .putString("restTimerPresets", normalizeRestTimerPresets(value.restTimerPresetSeconds).joinToString(","))
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
            .putNullableLong("healthLastSyncMillis", value.healthLastSyncMillis)
            .putInt("healthLastSyncCount", value.healthLastSyncCount.coerceAtLeast(0))
            .putBoolean("healthDeletionPending", value.healthConnectDeletionPending)
            .putString("reviewPeriod", value.reviewPeriod.name)
            .putString("taskStepPolicy", value.defaultTaskStepPolicy.name)
            .putBoolean("showAllUpcomingTaskOccurrences", value.showAllUpcomingTaskOccurrences)
            .putBoolean("showHabitsInTaskPlanning", value.showHabitsInTaskPlanning)
            .putString("activeTaskSortMode", value.activeTaskSortMode)
            .putString("habitWeekStart", value.defaultHabitWeekStart.name)
            .putBoolean("naturalLanguageTaskCapture", value.naturalLanguageTaskCapture)
            .putString("customIdentityEmojis", value.customIdentityEmojis.encodeCustomIdentityEmojis())
            .putString("savedTaskFilters", value.savedTaskFilters.encodeTaskFilters())
            .putNullableString("homeTaskFilterName", value.homeTaskFilterName)
            .putStringSet("reviewSections", value.reviewSections.mapTo(mutableSetOf(), ReviewSection::name))
            .putBoolean("gymCompactSetRows", value.gymCompactSetRows)
            .putString("platePresets", value.platePresets.encodePlatePresets())
            .putString("repPrescriptionSchemes", value.repPrescriptionSchemes.encodeRepPrescriptionSchemes())
            .putString("trackedGymRecords", value.trackedGymRecords.encodeTrackedGymRecords())
            .putNullableLong("focusTimerDeadlineMillis", value.focusTimerDeadlineMillis)
            .putNullableLong("focusTimerTaskId", value.focusTimerTaskId)
        if (confirm) {
            val committed = editor.commit()
            if (!committed) {
                // SharedPreferences updates its process-local map before it
                // knows whether disk accepted commit(). Restore the last
                // durable snapshot so current()/listeners cannot advertise a
                // value that will disappear after process restart.
                persist(transform = { before }, confirm = false)
            }
            committed
        } else {
            editor.apply()
            true
        }
    }

    private companion object {
        /** All repository instances target the same process-local preference file. */
        val updateLock = Any()
    }
}

fun AppSettings.normalized(): AppSettings {
    val normalizedOrder = homeSections
        .filter { it in HomeSection.entries }
        .distinct()
        .let { it + HomeSection.entries.filterNot(it::contains) }
    val knownHidden = hiddenHomeSections.intersect(HomeSection.entries.toSet())
    val normalizedHidden = if (knownHidden.size == HomeSection.entries.size) {
        knownHidden - normalizedOrder.first()
    } else {
        knownHidden
    }
    val validFocusTimerPair = focusTimerDeadlineMillis != null && (focusTimerTaskId ?: 0L) > 0L
    return copy(
        activeAreaScope = AreaScope.fromStorageKey(activeAreaScope).storageKey,
        chosenOpeningAreaScope = AreaScope.fromStorageKey(chosenOpeningAreaScope).storageKey,
        timeZoneId = timeZoneId?.takeIf { runCatching { ZoneId.of(it) }.isSuccess },
        dayCutoffMinutes = dayCutoffMinutes.coerceIn(0, 1439),
        massUnitId = normalizeMassUnit(massUnitId),
        distanceUnitId = normalizeDistanceUnit(distanceUnitId),
        volumeUnitId = normalizeVolumeUnit(volumeUnitId),
        gymWeightUnitId = normalizeMassUnit(gymWeightUnitId),
        numberPrecision = numberPrecision.coerceIn(0, 6),
        oneRepMaxFormula = oneRepMaxFormula.takeIf { it in setOf("Epley", "Brzycki") } ?: "Epley",
        oneRepMaxRepCutoff = oneRepMaxRepCutoff.coerceIn(1, 36),
        defaultRestSeconds = defaultRestSeconds.coerceIn(15, 3_600),
        restTimerPresetSeconds = normalizeRestTimerPresets(restTimerPresetSeconds),
        hardSetClassifications = hardSetClassifications
            .intersect(WorkoutSetClassification.entries.mapTo(mutableSetOf(), WorkoutSetClassification::name))
            .ifEmpty { setOf("Working") },
        categoryAllocationMode = categoryAllocationMode
            .takeIf { it in setOf("Full", "Fractional", "PrimaryOnly") } ?: "Fractional",
        homeSections = normalizedOrder,
        hiddenHomeSections = normalizedHidden,
        collapsedHomeSections = collapsedHomeSections.intersect(HomeSection.entries.toSet()),
        healthConnectEnabled = healthConnectEnabled &&
            healthDataTypes.any(HealthDataType.entries.toSet()::contains) &&
            !healthConnectDeletionPending,
        healthDataTypes = healthDataTypes.intersect(HealthDataType.entries.toSet()),
        healthSyncDays = healthSyncDays.coerceIn(1, 365),
        healthLastSyncCount = healthLastSyncCount.coerceAtLeast(0),
        activeTaskSortMode = activeTaskSortMode.takeIf {
            it in setOf("Smart", "Manual", "Scheduled Date", "Deadline", "Priority", "Title")
        } ?: "Smart",
        customIdentityEmojis = normalizeCustomIdentityEmojis(customIdentityEmojis),
        trackedGymRecords = normalizeTrackedGymRecords(trackedGymRecords),
        focusTimerDeadlineMillis = focusTimerDeadlineMillis.takeIf { validFocusTimerPair },
        focusTimerTaskId = focusTimerTaskId.takeIf { validFocusTimerPair },
    )
}

/** Resolves the Area used once when a new app session is created. */
fun AppSettings.openingAreaScope(): AreaScope = when (areaOpeningMode) {
    AreaOpeningMode.LastUsed -> AreaScope.fromStorageKey(activeAreaScope)
    AreaOpeningMode.Chosen -> AreaScope.fromStorageKey(chosenOpeningAreaScope)
}

fun AppSettings.visibleHomeSections(): List<HomeSection> =
    homeSections.filterNot(hiddenHomeSections::contains)

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
    override fun today(zoneId: ZoneId): LocalDate = settingsRepository.current().let { settings ->
        val local = now().atZone(zoneId)
        val cutoff = settings.dayCutoffMinutes.coerceIn(0, 1_439)
        val minute = local.hour * 60 + local.minute
        if (cutoff > 0 && minute < cutoff) local.toLocalDate().minusDays(1) else local.toLocalDate()
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

private fun SharedPreferences.healthDataTypes(): Set<HealthDataType> =
    getStringSet("healthTypes", null)
        ?.mapNotNullTo(mutableSetOf()) { runCatching { HealthDataType.valueOf(it) }.getOrNull() }
        ?: emptySet()

private fun SharedPreferences.nullableInt(key: String): Int? = if (contains(key)) getInt(key, 0) else null
private fun SharedPreferences.nullableLong(key: String): Long? = if (contains(key)) getLong(key, 0L) else null
private fun SharedPreferences.Editor.putNullableInt(key: String, value: Int?): SharedPreferences.Editor =
    if (value == null) remove(key) else putInt(key, value)
private fun SharedPreferences.Editor.putNullableLong(key: String, value: Long?): SharedPreferences.Editor =
    if (value == null) remove(key) else putLong(key, value)
private fun SharedPreferences.Editor.putNullableString(key: String, value: String?): SharedPreferences.Editor =
    if (value == null) remove(key) else putString(key, value)
