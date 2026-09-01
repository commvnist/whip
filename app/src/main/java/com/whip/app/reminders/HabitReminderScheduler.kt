package com.whip.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.data.HabitEntity
import com.whip.app.data.HabitLogEntity
import com.whip.app.data.HabitPauseEntity
import com.whip.app.data.HabitSkipEntity
import com.whip.app.data.MetricEntryEntity
import com.whip.app.data.UnitDefinitionEntity
import com.whip.app.core.SettingsRepository
import com.whip.app.core.adjustForQuietHours
import com.whip.app.core.zoneId
import com.whip.app.core.WhipLaunchActions
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.reminderNeededOn
import com.whip.app.domain.Habit
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitLog
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitPause
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitSkip
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.toWeekdays
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HabitReminderScheduler(context: Context, private val settingsRepository: SettingsRepository? = null) {
    private val appContext = context.applicationContext
    private val app = appContext as WhipApplication
    private val workManager = WorkManager.getInstance(appContext)
    private val dao = WhipDatabase.get(appContext).habitDao()

    suspend fun syncAll(allowDuringRecovery: Boolean = false) {
        if (!allowDuringRecovery) {
            app.withUserDataAccess { syncAllInternal() }
            return
        }
        syncAllInternal()
    }

    private suspend fun syncAllInternal() = dao.getReminderHabitIds().forEach { syncHabitCoordinated(it) }

    suspend fun syncSourceMetric(sourceMetricId: String, allowDuringRecovery: Boolean = false) {
        if (sourceMetricId.isBlank()) return
        if (!allowDuringRecovery) {
            app.withUserDataAccess { syncSourceMetricInternal(sourceMetricId) }
            return
        }
        syncSourceMetricInternal(sourceMetricId)
    }

    private suspend fun syncSourceMetricInternal(sourceMetricId: String) {
        dao.getReminderHabitIdsForSourceMetric(sourceMetricId).forEach { syncHabitCoordinated(it) }
    }

    suspend fun syncUnit(unitId: String, allowDuringRecovery: Boolean = false) {
        if (unitId.isBlank()) return
        if (!allowDuringRecovery) {
            app.withUserDataAccess { syncUnitInternal(unitId) }
            return
        }
        syncUnitInternal(unitId)
    }

    private suspend fun syncUnitInternal(unitId: String) {
        dao.getReminderHabitIdsForUnit(unitId).forEach { syncHabitCoordinated(it) }
    }

    suspend fun syncHabit(habitId: Long, allowDuringRecovery: Boolean = false) {
        if (!allowDuringRecovery) {
            app.withUserDataAccess { syncHabitCoordinated(habitId) }
            return
        }
        syncHabitCoordinated(habitId)
    }

    private suspend fun syncHabitCoordinated(habitId: Long) {
        app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Habit, habitId) {
            syncHabitUnlocked(habitId)
        }
    }

    private suspend fun syncHabitUnlocked(habitId: Long) {
        NotificationManagerCompat.from(appContext).cancel(HabitReminderNotifications.notificationId(habitId))
        workManager.cancelAllWorkByTag(tag(habitId)).await()
        scheduleNextUnlocked(habitId, System.currentTimeMillis())
    }

    suspend fun scheduleNext(
        habitId: Long,
        afterMillis: Long,
        allowDuringRecovery: Boolean = false,
    ) {
        if (!allowDuringRecovery) {
            app.withUserDataAccess { scheduleNextCoordinated(habitId, afterMillis) }
            return
        }
        scheduleNextCoordinated(habitId, afterMillis)
    }

    private suspend fun scheduleNextCoordinated(habitId: Long, afterMillis: Long) {
        app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Habit, habitId) {
            scheduleNextUnlocked(habitId, afterMillis)
        }
    }

    internal suspend fun scheduleNextFromCoordinator(habitId: Long, afterMillis: Long) {
        scheduleNextUnlocked(habitId, afterMillis)
    }

    /** Worker reconciliation deliberately does not cancel the running worker's tag. */
    internal suspend fun reconcileFromCoordinator(habitId: Long, afterMillis: Long) {
        NotificationManagerCompat.from(appContext).cancel(HabitReminderNotifications.notificationId(habitId))
        scheduleNextUnlocked(habitId, afterMillis)
    }

    internal suspend fun syncHabitFromCoordinator(habitId: Long) {
        syncHabitUnlocked(habitId)
    }

    private suspend fun scheduleNextUnlocked(habitId: Long, afterMillis: Long) {
        val snapshot = loadHabitReminderSnapshot(WhipDatabase.get(appContext), habitId) ?: return
        val settings = settingsRepository?.current() ?: app.settingsRepository.current()
        val zone = settings.zoneId()
        val reminder = nextHabitReminder(
            afterMillis = afterMillis,
            zone = zone,
            firstLogicalDate = snapshot.habit.startDate,
            quietStartMinutes = settings.quietStartMinutes,
            quietEndMinutes = settings.quietEndMinutes,
            isEligible = snapshot::isEligibleOn,
            configuredMinutes = snapshot::configuredMinutes,
        ) ?: return
        val trigger = reminder.triggerAtMillis
        val claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Scheduled,
            stableEntityId = snapshot.stored.uuid,
            logicalEpochDay = reminder.logicalDate.toEpochDay(),
            expectedTriggerAtMillis = trigger,
            definitionFingerprint = snapshot.semanticFingerprint(
                reminder.logicalDate,
                zone,
                settings.quietStartMinutes,
                settings.quietEndMinutes,
            ),
        )
        val delay = (trigger - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                        .putLong(HabitReminderWorker.HABIT_ID, habitId)
                        .putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, reminder.logicalDate.toEpochDay())
                        .putReminderDeliveryClaim(claim)
                        .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
                    .build(),
            )
            .addTag(tag(habitId))
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork("${tag(habitId)}-$trigger", ExistingWorkPolicy.REPLACE, request).await()
    }

    suspend fun snooze(
        habitId: Long,
        logicalEpochDay: Long,
        stableEntityId: String,
        definitionFingerprint: String,
        minutes: Int = 10,
        allowDuringRecovery: Boolean = false,
    ): Boolean {
        if (allowDuringRecovery) {
            return snoozeCoordinated(habitId, logicalEpochDay, stableEntityId, definitionFingerprint, minutes)
        }
        return app.withUserDataAccess {
            snoozeCoordinated(habitId, logicalEpochDay, stableEntityId, definitionFingerprint, minutes)
        } ?: false
    }

    private suspend fun snoozeCoordinated(
        habitId: Long,
        logicalEpochDay: Long,
        stableEntityId: String,
        definitionFingerprint: String,
        minutes: Int,
    ): Boolean = app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Habit, habitId) {
        snoozeFromCoordinator(habitId, logicalEpochDay, stableEntityId, definitionFingerprint, minutes)
    }

    internal suspend fun snoozeFromCoordinator(
        habitId: Long,
        logicalEpochDay: Long,
        stableEntityId: String,
        definitionFingerprint: String,
        minutes: Int = 10,
    ): Boolean {
        val logicalDate = runCatching { LocalDate.ofEpochDay(logicalEpochDay) }.getOrNull() ?: return false
        val snapshot = loadHabitReminderSnapshot(WhipDatabase.get(appContext), habitId) ?: return false
        val settings = settingsRepository?.current() ?: app.settingsRepository.current()
        val fingerprint = snapshot.semanticFingerprint(
            logicalDate,
            settings.zoneId(),
            settings.quietStartMinutes,
            settings.quietEndMinutes,
        )
        if (
            snapshot.stored.uuid != stableEntityId || fingerprint != definitionFingerprint ||
            !snapshot.isEligibleOn(logicalDate)
        ) return false
        val trigger = System.currentTimeMillis() + minutes.coerceIn(1, 1_440) * 60_000L
        val claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Snoozed,
            stableEntityId = snapshot.stored.uuid,
            logicalEpochDay = logicalEpochDay,
            expectedTriggerAtMillis = trigger,
            definitionFingerprint = fingerprint,
        )
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay((trigger - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(HabitReminderWorker.HABIT_ID, habitId)
                    .putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, logicalEpochDay)
                    .putReminderDeliveryClaim(claim)
                    .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
                    .build(),
            )
            .addTag(tag(habitId))
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork("${tag(habitId)}-snooze", ExistingWorkPolicy.REPLACE, request).await()
        NotificationManagerCompat.from(appContext).cancel(HabitReminderNotifications.notificationId(habitId))
        return true
    }

    private fun tag(id: Long) = "whip-habit-reminder-$id"
}

internal data class HabitReminderTime(val triggerAtMillis: Long, val logicalDate: LocalDate)

/** Select by adjusted delivery instant and include yesterday: an overnight
 * quiet window may legitimately shift yesterday's logical reminder into today. */
internal fun nextHabitReminder(
    afterMillis: Long,
    zone: ZoneId,
    firstLogicalDate: LocalDate,
    quietStartMinutes: Int?,
    quietEndMinutes: Int?,
    isEligible: (LocalDate) -> Boolean,
    configuredMinutes: (LocalDate) -> List<Int>,
): HabitReminderTime? {
    val physicalDate = Instant.ofEpochMilli(afterMillis).atZone(zone).toLocalDate()
    val start = physicalDate.minusDays(1).coerceAtLeast(firstLogicalDate)
    for (offset in 0L..3_651L) {
        val logicalDate = start.plusDays(offset)
        if (!isEligible(logicalDate)) continue
        val best = configuredMinutes(logicalDate).asSequence()
            .filter { it in 0..1_439 }
            .map { minute ->
                adjustForQuietHours(
                    logicalDate.atTime(minute / 60, minute % 60).atZone(zone).toInstant(),
                    zone,
                    quietStartMinutes,
                    quietEndMinutes,
                ).toEpochMilli()
            }
            .filter { trigger -> trigger >= afterMillis }
            .minOrNull()
        if (best != null) return HabitReminderTime(best, logicalDate)
    }
    return null
}

internal data class HabitReminderSnapshot(
    val stored: HabitEntity,
    val habit: Habit,
    val logs: List<HabitLog>,
    val pauses: List<HabitPause>,
    val skips: List<HabitSkip>,
    val customUnits: List<UnitDefinition>,
    val reminderMinutes: List<Int>,
    val weekdayReminderMinutes: Map<DayOfWeek, List<Int>>,
) {
    fun configuredMinutes(date: LocalDate): List<Int> =
        weekdayReminderMinutes[date.dayOfWeek] ?: reminderMinutes

    fun isEligibleOn(date: LocalDate): Boolean =
        configuredMinutes(date).isNotEmpty() &&
            !habit.archived && !habit.paused &&
            habit.reminderNeededOn(logs, date, customUnits, skips, pauses)

    fun semanticFingerprint(
        logicalDate: LocalDate,
        zone: ZoneId,
        quietStartMinutes: Int?,
        quietEndMinutes: Int?,
    ): String = reminderSemanticFingerprint(
        "habit-v2",
        stored.uuid,
        habit.trackingMode,
        habit.dimension,
        habit.unitId,
        habit.precision,
        habit.comparison,
        habit.targetMin,
        habit.targetMax,
        habit.targetPeriod,
        habit.rollingDays,
        habit.scheduleType,
        habit.scheduleInterval,
        habit.weekdays.sortedBy { it.value }.joinToString(","),
        habit.flexibleTimesPerWeek,
        habit.startDate,
        habit.endType,
        habit.endDate,
        habit.endValue,
        habit.quickIncrement,
        habit.weekStart,
        habit.sourceMetricId,
        reminderMinutes.joinToString(","),
        weekdayReminderMinutes.toSortedMap(compareBy { it.value }).entries.joinToString(";") {
            "${it.key.name}=${it.value.joinToString(",")}"
        },
        logicalDate,
        zone.id,
        quietStartMinutes,
        quietEndMinutes,
    )
}

private fun String.parseReminderMinutesOrNull(): List<Int>? {
    if (isBlank()) return emptyList()
    val pieces = split(',')
    val parsed = pieces.map { token -> token.toIntOrNull()?.takeIf { it in 0..1_439 } ?: return null }
    return parsed.distinct().sorted()
}

private fun String.parseWeekdayReminderMinutesOrNull(): Map<DayOfWeek, List<Int>>? {
    if (isBlank()) return emptyMap()
    val parsed = linkedMapOf<DayOfWeek, List<Int>>()
    for (segment in split(';')) {
        val pieces = segment.split('=', limit = 2)
        if (pieces.size != 2) return null
        val day = runCatching { DayOfWeek.valueOf(pieces[0]) }.getOrNull() ?: return null
        if (day in parsed) return null
        parsed[day] = pieces[1].parseReminderMinutesOrNull() ?: return null
    }
    return parsed
}

private suspend fun loadHabitReminderSnapshot(
    database: WhipDatabase,
    habitId: Long,
): HabitReminderSnapshot? = database.withTransaction {
    val dao = database.habitDao()
    val stored = dao.getHabit(habitId) ?: return@withTransaction null
    val reminderMinutes = stored.reminderMinutesCsv.parseReminderMinutesOrNull()
        ?: return@withTransaction null
    val weekdayReminderMinutes = stored.weekdayReminderMinutesCsv.parseWeekdayReminderMinutesOrNull()
        ?: return@withTransaction null
    if (reminderMinutes.isEmpty() && weekdayReminderMinutes.isEmpty()) return@withTransaction null
    val habit = runCatching {
        stored.toReminderDomain(reminderMinutes, weekdayReminderMinutes)
    }.getOrNull() ?: return@withTransaction null

    val storedLogs = dao.getLogsForHabit(habitId)
    val directLogs = storedLogs.map { row ->
        runCatching(row::toReminderDomain).getOrNull() ?: return@withTransaction null
    }
    val pauses = dao.getPauses(habitId).map { row ->
        runCatching(row::toReminderDomain).getOrNull() ?: return@withTransaction null
    }
    val skips = dao.getSkips(habitId).map { row ->
        runCatching(row::toReminderDomain).getOrNull() ?: return@withTransaction null
    }
    val referencedUnitIds = buildSet {
        add(habit.unitId)
        storedLogs.mapNotNullTo(this) { it.enteredUnitId }
    }
    val customUnits = referencedUnitIds.mapNotNull { unitId ->
        if (com.whip.app.domain.BuiltInUnits.get(unitId) != null) return@mapNotNull null
        val row = database.measurementDao().getUnit(unitId) ?: return@withTransaction null
        if (!row.custom) return@withTransaction null
        runCatching(row::toReminderDomain).getOrNull() ?: return@withTransaction null
    }
    val targetCustomUnit = customUnits.firstOrNull { it.id == habit.unitId }
    val sourceLogs = if (habit.sourceMetricId == null) {
        emptyList()
    } else {
        val targetUnit = com.whip.app.domain.BuiltInUnits.get(habit.unitId) ?: targetCustomUnit
            ?: return@withTransaction null
        database.measurementDao().getEntriesForMetric(habit.sourceMetricId)
            .mapNotNull { row ->
                when (runCatching { MetricEntryStatus.valueOf(row.status) }.getOrNull()
                    ?: return@withTransaction null) {
                    MetricEntryStatus.Missing,
                    MetricEntryStatus.Skipped,
                    MetricEntryStatus.Excused,
                    -> null
                    MetricEntryStatus.Recorded,
                    MetricEntryStatus.Failed,
                    -> runCatching { row.toSourceHabitLogOrNull(habit, targetUnit) }.getOrNull()
                        ?: return@withTransaction null
                }
            }
    }
    HabitReminderSnapshot(
        stored = stored,
        habit = habit,
        logs = directLogs + sourceLogs,
        pauses = pauses,
        skips = skips,
        customUnits = customUnits,
        reminderMinutes = reminderMinutes,
        weekdayReminderMinutes = weekdayReminderMinutes,
    )
}

private fun MetricEntryEntity.toSourceHabitLogOrNull(
    habit: Habit,
    targetUnit: UnitDefinition,
): HabitLog? {
    val status = when (runCatching { MetricEntryStatus.valueOf(status) }.getOrNull()) {
        MetricEntryStatus.Recorded -> HabitLogStatus.Recorded
        MetricEntryStatus.Failed -> HabitLogStatus.Failed
        else -> return null
    }
    val sourceType = runCatching { MetricSourceType.valueOf(sourceType) }.getOrNull() ?: return null
    val value = when {
        enteredValue != null && enteredUnitId == habit.unitId -> enteredValue
        canonicalValue != null -> targetUnit.fromCanonical(canonicalValue)
        else -> null
    }
    val stable = ("${habit.id}:$id".hashCode().toLong() and 0x7fff_ffffL).let { if (it == 0L) -1L else -it }
    return HabitLog(
        id = stable,
        uuid = "metric:${habit.id}:$id",
        habitId = habit.id,
        value = value,
        canonicalValue = canonicalValue,
        enteredUnitId = habit.unitId,
        status = status,
        timestamp = Instant.ofEpochMilli(timestampMillis),
        localDate = LocalDate.ofEpochDay(localEpochDay),
        zoneId = zoneId,
        offsetSeconds = offsetSeconds,
        note = note,
        sourceType = sourceType,
        sourceId = sourceId,
        metricEntryId = id,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )
}

private fun currentHabitReminderClaimIsValid(
    snapshot: HabitReminderSnapshot,
    claim: ReminderDeliveryClaim,
    now: Instant,
    zone: ZoneId,
    quietStartMinutes: Int?,
    quietEndMinutes: Int?,
): Boolean {
    if (claim.expectedTriggerAtMillis > now.toEpochMilli()) return false
    val logicalDate = runCatching { LocalDate.ofEpochDay(claim.logicalEpochDay) }.getOrNull() ?: return false
    if (
        snapshot.stored.uuid != claim.stableEntityId ||
        !snapshot.isEligibleOn(logicalDate) ||
        snapshot.semanticFingerprint(logicalDate, zone, quietStartMinutes, quietEndMinutes) !=
        claim.definitionFingerprint
    ) return false
    val physicalToday = now.atZone(zone).toLocalDate()
    if (!reminderClaimIsForToday(claim, physicalToday, zone)) return false
    if (claim.kind == ReminderDeliveryKind.Snoozed) return true
    return snapshot.configuredMinutes(logicalDate).any { minute ->
        adjustForQuietHours(
            logicalDate.atTime(minute / 60, minute % 60).atZone(zone).toInstant(),
            zone,
            quietStartMinutes,
            quietEndMinutes,
        ).toEpochMilli() == claim.expectedTriggerAtMillis
    }
}

internal suspend fun WhipApplication.currentHabitReminderDeliveryClaim(
    habitId: Long,
    logicalDate: LocalDate,
    kind: ReminderDeliveryKind = ReminderDeliveryKind.Snoozed,
    expectedTriggerAtMillis: Long = clock.now().toEpochMilli(),
): ReminderDeliveryClaim? {
    val snapshot = loadHabitReminderSnapshot(database, habitId) ?: return null
    if (!snapshot.isEligibleOn(logicalDate)) return null
    val settings = settingsRepository.current()
    val claim = ReminderDeliveryClaim(
        kind = kind,
        stableEntityId = snapshot.stored.uuid,
        logicalEpochDay = logicalDate.toEpochDay(),
        expectedTriggerAtMillis = expectedTriggerAtMillis,
        definitionFingerprint = snapshot.semanticFingerprint(
            logicalDate,
            settings.zoneId(),
            settings.quietStartMinutes,
            settings.quietEndMinutes,
        ),
    )
    return claim.takeIf {
        currentHabitReminderClaimIsValid(
            snapshot,
            it,
            clock.now(),
            settings.zoneId(),
            settings.quietStartMinutes,
            settings.quietEndMinutes,
        )
    }
}

class HabitReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WhipApplication
        return app.withUserDataAccess {
            if (!app.isCurrentUserDataGeneration(
                    inputData.getLong(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION),
                )
            ) return@withUserDataAccess Result.success()
            val id = inputData.getLong(HABIT_ID, -1)
            if (id < 0) return@withUserDataAccess Result.success()
            app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Habit, id) {
                app.reminderDeliveryCoordinator.withStateBoundary {
                val scheduler = app.habitReminderScheduler
                val claim = inputData.reminderDeliveryClaimOrNull()
                val logicalEpochDay = inputData.getLong(LOGICAL_EPOCH_DAY, Long.MIN_VALUE)
                val snapshot = loadHabitReminderSnapshot(WhipDatabase.get(applicationContext), id)
                val settings = app.settingsRepository.current()
                val claimIsCurrent = claim != null && snapshot != null &&
                    claim.logicalEpochDay == logicalEpochDay &&
                    currentHabitReminderClaimIsValid(
                        snapshot,
                        claim,
                        app.clock.now(),
                        settings.zoneId(),
                        settings.quietStartMinutes,
                        settings.quietEndMinutes,
                    )
                if (!claimIsCurrent) {
                    // Early WorkManager execution is stale for posting but the
                    // exact due reminder is still ahead and must be requeued.
                    scheduler.reconcileFromCoordinator(id, System.currentTimeMillis() + 1L)
                    return@withStateBoundary Result.success()
                }
                HabitReminderNotifications.show(
                    applicationContext,
                    requireNotNull(snapshot).stored,
                    LocalDate.ofEpochDay(requireNotNull(claim).logicalEpochDay),
                    claim,
                )
                scheduler.scheduleNextFromCoordinator(
                    id,
                    maxOf(System.currentTimeMillis(), requireNotNull(claim).expectedTriggerAtMillis) + 1L,
                )
                Result.success()
                }
            }
        } ?: Result.retry()
    }

    companion object {
        const val HABIT_ID = "habit_id"
        const val LOGICAL_EPOCH_DAY = "logical_epoch_day"
    }
}

internal fun logicalHabitReminderDate(epochDay: Long, fallback: LocalDate): LocalDate =
    epochDay.takeUnless { it == Long.MIN_VALUE }?.let(LocalDate::ofEpochDay) ?: fallback

object HabitReminderNotifications {
    const val CHANNEL_ID = "habit_reminders"
    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders for habits you chose to track"
            },
        )
    }

    internal fun show(
        context: Context,
        habit: HabitEntity,
        logicalDate: LocalDate,
        claim: ReminderDeliveryClaim,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = PendingIntent.getActivity(
            context,
            habit.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .setAction(WhipLaunchActions.ACTION_OPEN_HABIT)
                .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, habit.id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dataGeneration = (context.applicationContext as WhipApplication).currentUserDataGeneration()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(habit.name)
            .setContentText("Time for your habit check-in").setContentIntent(intent)
            .setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .apply {
                val actionToken = System.currentTimeMillis()
                if (habit.sourceMetricId == null && habit.trackingMode == "CheckOff") {
                    addAction(
                        R.drawable.ic_notification,
                        "Mark done",
                        PendingIntent.getBroadcast(
                            context,
                            (habit.id xor 0x444f4e45).hashCode(),
                            Intent(context, HabitReminderActionReceiver::class.java)
                                .setAction(HabitReminderActionReceiver.ACTION_COMPLETE)
                                .putExtra(HabitReminderActionReceiver.EXTRA_HABIT_ID, habit.id)
                                .putExtra(HabitReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, logicalDate.toEpochDay())
                                .putExtra(HabitReminderActionReceiver.EXTRA_STABLE_ENTITY_ID, claim.stableEntityId)
                                .putExtra(HabitReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT, claim.definitionFingerprint)
                                .putExtra(HabitReminderActionReceiver.EXTRA_EXPECTED_TRIGGER_AT_MILLIS, claim.expectedTriggerAtMillis)
                                .putExtra(HabitReminderActionReceiver.EXTRA_DELIVERY_KIND, claim.kind.name)
                                .putExtra(HabitReminderActionReceiver.EXTRA_CLAIM_VERSION, claim.version)
                                .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, actionToken)
                                .putExtra(USER_DATA_GENERATION_KEY, dataGeneration),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                } else if (
                    habit.sourceMetricId == null &&
                    habit.trackingMode in setOf("Count", "Decimal")
                ) {
                    addAction(
                        R.drawable.ic_notification,
                        "+${formatNotificationNumber(habit.quickIncrement)}",
                        PendingIntent.getBroadcast(
                            context,
                            (habit.id xor 0x494e4352).hashCode(),
                            Intent(context, HabitReminderActionReceiver::class.java)
                                .setAction(HabitReminderActionReceiver.ACTION_INCREMENT)
                                .putExtra(HabitReminderActionReceiver.EXTRA_HABIT_ID, habit.id)
                                .putExtra(HabitReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, logicalDate.toEpochDay())
                                .putExtra(HabitReminderActionReceiver.EXTRA_STABLE_ENTITY_ID, claim.stableEntityId)
                                .putExtra(HabitReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT, claim.definitionFingerprint)
                                .putExtra(HabitReminderActionReceiver.EXTRA_EXPECTED_TRIGGER_AT_MILLIS, claim.expectedTriggerAtMillis)
                                .putExtra(HabitReminderActionReceiver.EXTRA_DELIVERY_KIND, claim.kind.name)
                                .putExtra(HabitReminderActionReceiver.EXTRA_CLAIM_VERSION, claim.version)
                                .putExtra(HabitReminderActionReceiver.EXTRA_INCREMENT, habit.quickIncrement)
                                .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, actionToken)
                                .putExtra(USER_DATA_GENERATION_KEY, dataGeneration),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }
                addAction(
                    R.drawable.ic_notification,
                    "Snooze 10 min",
                    PendingIntent.getBroadcast(
                        context,
                        (habit.id xor 0x534e4f5a).hashCode(),
                        Intent(context, HabitReminderActionReceiver::class.java)
                            .setAction(HabitReminderActionReceiver.ACTION_SNOOZE)
                            .putExtra(HabitReminderActionReceiver.EXTRA_HABIT_ID, habit.id)
                            .putExtra(HabitReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, logicalDate.toEpochDay())
                            .putExtra(HabitReminderActionReceiver.EXTRA_STABLE_ENTITY_ID, claim.stableEntityId)
                            .putExtra(HabitReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT, claim.definitionFingerprint)
                            .putExtra(HabitReminderActionReceiver.EXTRA_EXPECTED_TRIGGER_AT_MILLIS, claim.expectedTriggerAtMillis)
                            .putExtra(HabitReminderActionReceiver.EXTRA_DELIVERY_KIND, claim.kind.name)
                            .putExtra(HabitReminderActionReceiver.EXTRA_CLAIM_VERSION, claim.version)
                            .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, actionToken)
                            .putExtra(USER_DATA_GENERATION_KEY, dataGeneration),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(habit.id), notification)
    }

    internal fun notificationId(habitId: Long): Int = (habitId xor 0x48414249).hashCode()
}

class HabitReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(ACTION_COMPLETE, ACTION_INCREMENT, ACTION_SNOOZE)) return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return
        val pending = goAsync()
        val app = context.applicationContext as WhipApplication
        val logicalEpochDay = intent.getLongExtra(EXTRA_LOGICAL_EPOCH_DAY, Long.MIN_VALUE)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val accessed = app.withUserDataAccess {
                    app.handleHabitNotificationAction(
                        context = context,
                        intent = intent,
                        habitId = habitId,
                        logicalEpochDay = logicalEpochDay,
                    )
                }
                if (accessed == null) {
                    NotificationManagerCompat.from(context)
                        .cancel(HabitReminderNotifications.notificationId(habitId))
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "commvne.com.whip.app.action.COMPLETE_HABIT"
        const val ACTION_INCREMENT = "commvne.com.whip.app.action.INCREMENT_HABIT"
        const val ACTION_SNOOZE = "commvne.com.whip.app.action.SNOOZE_HABIT"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_INCREMENT = "habit_increment"
        const val EXTRA_ACTION_TOKEN = "action_token"
        const val EXTRA_LOGICAL_EPOCH_DAY = "logical_epoch_day"
        const val EXTRA_STABLE_ENTITY_ID = "stable_entity_id"
        const val EXTRA_DEFINITION_FINGERPRINT = "definition_fingerprint"
        const val EXTRA_EXPECTED_TRIGGER_AT_MILLIS = "expected_trigger_at_millis"
        const val EXTRA_DELIVERY_KIND = "delivery_kind"
        const val EXTRA_CLAIM_VERSION = "claim_version"
    }
}

private suspend fun WhipApplication.handleHabitNotificationAction(
    context: Context,
    intent: Intent,
    habitId: Long,
    logicalEpochDay: Long,
) {
    if (!isCurrentUserDataGeneration(intent.getLongExtra(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION))) {
        NotificationManagerCompat.from(context).cancel(HabitReminderNotifications.notificationId(habitId))
        return
    }
    val logicalDate = logicalEpochDay.takeUnless { it == Long.MIN_VALUE }
        ?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
    val claim = intent.habitReminderDeliveryClaimOrNull()
    val action = HabitNotificationAction.fromIntentAction(intent.action)
    val increment = intent.getDoubleExtra(HabitReminderActionReceiver.EXTRA_INCREMENT, 1.0)
    val actionId = "habit:$habitId:${intent.action}:$logicalEpochDay:${intent.getLongExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, 0L)}"
    val ledger = NotificationActionLedger(context)
    try {
        if (
            action == null || logicalDate == null || claim == null ||
            !isCurrentHabitNotificationAction(habitId, logicalDate, action, increment, claim)
        ) {
            refreshStaleHabitNotification(context, habitId)
            return
        }
        if (!ledger.begin(actionId)) return

        val applied = reminderDeliveryCoordinator.withEntity(ReminderDomain.Habit, habitId) {
            reminderDeliveryCoordinator.withStateBoundary {
            if (!isCurrentHabitNotificationAction(habitId, logicalDate, action, increment, claim)) {
                return@withStateBoundary false
            }
            if (action == HabitNotificationAction.Snooze) {
                habitReminderScheduler.snoozeFromCoordinator(
                    habitId = habitId,
                    logicalEpochDay = logicalEpochDay,
                    stableEntityId = claim.stableEntityId,
                    definitionFingerprint = claim.definitionFingerprint,
                )
            } else {
                val changed = database.withTransaction {
                    if (!isCurrentHabitNotificationAction(habitId, logicalDate, action, increment, claim)) {
                        return@withTransaction false
                    }
                    when (action) {
                        HabitNotificationAction.Complete -> rawHabitRepository.setCheckOff(habitId, logicalDate, true)
                        HabitNotificationAction.Increment -> rawHabitRepository.log(
                            habitId,
                            increment,
                            date = logicalDate,
                            sourceType = MetricSourceType.Habit,
                            sourceId = actionId,
                        )
                        HabitNotificationAction.Snooze -> error("Snooze is handled without a database mutation")
                    }
                    true
                }
                if (changed) habitReminderScheduler.syncHabitFromCoordinator(habitId)
                changed
            }
            }
        }
        if (!applied) {
            ledger.release(actionId)
            refreshStaleHabitNotification(context, habitId)
            return
        }

        ledger.complete(actionId)
    } catch (_: Throwable) {
        ledger.release(actionId)
    }
}

private fun Intent.habitReminderDeliveryClaimOrNull(): ReminderDeliveryClaim? {
    val claim = ReminderDeliveryClaim(
        version = getIntExtra(HabitReminderActionReceiver.EXTRA_CLAIM_VERSION, -1),
        kind = getStringExtra(HabitReminderActionReceiver.EXTRA_DELIVERY_KIND)
            ?.let { runCatching { ReminderDeliveryKind.valueOf(it) }.getOrNull() }
            ?: return null,
        stableEntityId = getStringExtra(HabitReminderActionReceiver.EXTRA_STABLE_ENTITY_ID).orEmpty(),
        logicalEpochDay = getLongExtra(HabitReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, Long.MIN_VALUE),
        expectedTriggerAtMillis = getLongExtra(
            HabitReminderActionReceiver.EXTRA_EXPECTED_TRIGGER_AT_MILLIS,
            -1L,
        ),
        definitionFingerprint = getStringExtra(
            HabitReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT,
        ).orEmpty(),
    )
    return claim.takeIf(ReminderDeliveryClaim::isStructurallyValid)
}

internal enum class HabitNotificationAction {
    Complete,
    Increment,
    Snooze;

    companion object {
        fun fromIntentAction(action: String?): HabitNotificationAction? = when (action) {
            HabitReminderActionReceiver.ACTION_COMPLETE -> Complete
            HabitReminderActionReceiver.ACTION_INCREMENT -> Increment
            HabitReminderActionReceiver.ACTION_SNOOZE -> Snooze
            else -> null
        }
    }
}

/** Rebuilds eligibility from the live habit definition and history. This is
 * deliberately stricter than repository mutation APIs because an action may
 * outlive the notification configuration that originally created it. */
internal suspend fun WhipApplication.isCurrentHabitNotificationAction(
    habitId: Long,
    logicalDate: LocalDate,
    action: HabitNotificationAction,
    increment: Double,
    claim: ReminderDeliveryClaim? = null,
): Boolean {
    val snapshot = loadHabitReminderSnapshot(database, habitId) ?: return false
    if (!snapshot.isEligibleOn(logicalDate)) return false
    if (claim != null) {
        val settings = settingsRepository.current()
        if (
            claim.logicalEpochDay != logicalDate.toEpochDay() ||
            !currentHabitReminderClaimIsValid(
                snapshot,
                claim,
                clock.now(),
                settings.zoneId(),
                settings.quietStartMinutes,
                settings.quietEndMinutes,
            )
        ) return false
    }

    return when (action) {
        HabitNotificationAction.Complete ->
            snapshot.habit.trackingMode == HabitTrackingMode.CheckOff && snapshot.habit.sourceMetricId == null
        HabitNotificationAction.Increment ->
            snapshot.habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal) &&
                snapshot.habit.sourceMetricId == null && increment.isFinite() && increment > 0.0 &&
                increment == snapshot.habit.quickIncrement
        HabitNotificationAction.Snooze -> true
    }
}

private suspend fun WhipApplication.refreshStaleHabitNotification(context: Context, habitId: Long) {
    NotificationManagerCompat.from(context).cancel(HabitReminderNotifications.notificationId(habitId))
    habitReminderScheduler.syncHabit(habitId, allowDuringRecovery = true)
}

internal fun formatNotificationNumber(value: Double, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getNumberInstance(locale).run {
        isGroupingUsed = true
        minimumFractionDigits = 0
        maximumFractionDigits = 6
        format(value)
    }

private fun HabitEntity.toReminderDomain(
    reminderMinutes: List<Int>,
    weekdayReminderMinutes: Map<DayOfWeek, List<Int>>,
) = Habit(
    id, uuid, metricId, name, notes, areaId, area,
    tagsCsv.split(',').map(String::trim).filter(String::isNotBlank), icon,
    HabitTrackingMode.valueOf(trackingMode), UnitDimension.valueOf(dimension), unitId,
    precision, TargetComparison.valueOf(comparison), targetMin, targetMax,
    TargetPeriod.valueOf(targetPeriod), rollingDays, HabitScheduleType.valueOf(scheduleType),
    scheduleInterval, weekdaysMask.toWeekdays(), flexibleTimesPerWeek,
    LocalDate.ofEpochDay(startEpochDay), HabitEndType.valueOf(endType),
    endEpochDay?.let(LocalDate::ofEpochDay), endValue, quickIncrement,
    quickActionsCsv.split(',').mapNotNull(String::toDoubleOrNull),
    reminderMinutes,
    weekdayReminderMinutes, DayOfWeek.valueOf(weekStart),
    timerStartedAtMillis, pinned, position, archived, paused, createdAtMillis, updatedAtMillis,
    sourceMetricId, autoCompleteFromItems,
)

private fun HabitLogEntity.toReminderDomain() = HabitLog(
    id, uuid, habitId, value, canonicalValue, enteredUnitId, HabitLogStatus.valueOf(status),
    Instant.ofEpochMilli(timestampMillis), LocalDate.ofEpochDay(localEpochDay), zoneId,
    offsetSeconds, note, MetricSourceType.valueOf(sourceType), sourceId, metricEntryId,
    createdAtMillis, updatedAtMillis,
)

private fun HabitPauseEntity.toReminderDomain() = HabitPause(
    id, habitId, LocalDate.ofEpochDay(startEpochDay), endEpochDay?.let(LocalDate::ofEpochDay), note,
)

private fun HabitSkipEntity.toReminderDomain() = HabitSkip(
    uuid, habitId, LocalDate.ofEpochDay(localEpochDay), skippedAtMillis, createdAtMillis, updatedAtMillis,
)

private fun UnitDefinitionEntity.toReminderDomain() = UnitDefinition(
    id, name, symbol, UnitDimension.valueOf(dimension), toCanonicalFactor, toCanonicalOffset,
    custom, archived, createdAtMillis, updatedAtMillis,
)

private fun HabitEntity.isScheduled(date: LocalDate): Boolean {
    if (date.toEpochDay() < startEpochDay || (endEpochDay != null && date.toEpochDay() > endEpochDay)) return false
    return when (scheduleType) {
        "Daily", "FlexibleTimesPerWeek", "FlexibleTimesPerMonth" -> true
        "EveryNDays" -> (date.toEpochDay() - startEpochDay) % scheduleInterval.coerceAtLeast(1) == 0L
        "SelectedWeekdays" -> weekdaysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
        else -> false
    }
}
