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
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.data.HabitEntity
import com.whip.app.data.HabitLogEntity
import com.whip.app.data.HabitPauseEntity
import com.whip.app.data.HabitSkipEntity
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
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TargetPeriod
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.toWeekdays
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
import kotlinx.coroutines.flow.first

class HabitReminderScheduler(context: Context, private val settingsRepository: SettingsRepository? = null) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val dao = WhipDatabase.get(appContext).habitDao()

    suspend fun syncAll() = dao.getReminderHabitIds().forEach { syncHabit(it) }

    suspend fun syncHabit(habitId: Long) {
        workManager.cancelAllWorkByTag(tag(habitId))
        scheduleNext(habitId, System.currentTimeMillis())
    }

    suspend fun scheduleNext(habitId: Long, afterMillis: Long) {
        val habit = dao.getHabit(habitId) ?: return
        if (habit.archived || habit.paused) return
        val reminderMinutes = habit.reminderMinutesCsv.split(',').mapNotNull(String::toIntOrNull).distinct()
        val weekdayReminders = habit.weekdayReminderMinutesCsv.parseWeekdayReminderMinutes()
        if (reminderMinutes.isEmpty() && weekdayReminders.isEmpty()) return
        val zone = settingsRepository?.current()?.zoneId() ?: ZoneId.systemDefault()
        val after = Instant.ofEpochMilli(afterMillis).atZone(zone)
        val pauses = dao.getPauses(habitId)
        val app = appContext as? WhipApplication
        val domainHabit = app?.habitRepository?.habits?.first()?.firstOrNull { it.id == habitId }
        val domainLogs = app?.habitRepository?.logs?.first()?.filter { it.habitId == habitId }.orEmpty()
        val domainSkips = app?.habitRepository?.skips?.first()?.filter { it.habitId == habitId }.orEmpty()
        val customUnits = app?.measurementRepository?.customUnits?.first().orEmpty()
        var best: HabitReminderTime? = null
        for (offset in 0L..3_650L) {
            val date = after.toLocalDate().plusDays(offset)
            if (!habit.isScheduled(date) || pauses.any { date.toEpochDay() >= it.startEpochDay && (it.endEpochDay == null || date.toEpochDay() <= it.endEpochDay) }) continue
            if (domainHabit != null && !domainHabit.reminderNeededOn(domainLogs, date, customUnits, domainSkips)) continue
            (weekdayReminders[date.dayOfWeek] ?: reminderMinutes).forEach { minute ->
                val settings = settingsRepository?.current()
                val candidate = adjustForQuietHours(
                    date.atTime(minute / 60, minute % 60).atZone(zone).toInstant(),
                    zone,
                    settings?.quietStartMinutes,
                    settings?.quietEndMinutes,
                ).toEpochMilli()
                if (candidate >= afterMillis && (best == null || candidate < requireNotNull(best).triggerAtMillis)) {
                    best = HabitReminderTime(candidate, date)
                }
            }
            if (best != null) break
        }
        val reminder = best ?: return
        val trigger = reminder.triggerAtMillis
        val delay = (trigger - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(HabitReminderWorker.HABIT_ID, habitId)
                    .putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, reminder.logicalDate.toEpochDay())
                    .build(),
            )
            .addTag(tag(habitId))
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork("${tag(habitId)}-$trigger", ExistingWorkPolicy.REPLACE, request)
    }

    fun snooze(habitId: Long, logicalEpochDay: Long? = null, minutes: Int = 10) {
        val trigger = System.currentTimeMillis() + minutes.coerceIn(1, 1_440) * 60_000L
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay((trigger - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(HabitReminderWorker.HABIT_ID, habitId)
                    .putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, logicalEpochDay ?: Long.MIN_VALUE)
                    .build(),
            )
            .addTag(tag(habitId))
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork("${tag(habitId)}-snooze", ExistingWorkPolicy.REPLACE, request)
    }

    private fun tag(id: Long) = "whip-habit-reminder-$id"
}

private data class HabitReminderTime(val triggerAtMillis: Long, val logicalDate: LocalDate)

private fun String.parseWeekdayReminderMinutes(): Map<DayOfWeek, List<Int>> = split(';').mapNotNull { segment ->
    val pieces = segment.split('=', limit = 2)
    val day = pieces.getOrNull(0)?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
        ?: return@mapNotNull null
    day to pieces.getOrNull(1).orEmpty().split(',').mapNotNull(String::toIntOrNull)
}.toMap()

class HabitReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(HABIT_ID, -1)
        if (id < 0) return Result.failure()
        val app = applicationContext as WhipApplication
        val habit = WhipDatabase.get(applicationContext).habitDao().getHabit(id) ?: return Result.success()
        val domainHabit = app.habitRepository.habits.first().firstOrNull { it.id == id }
        val logs = app.habitRepository.logs.first().filter { it.habitId == id }
        val skips = app.habitRepository.skips.first().filter { it.habitId == id }
        val customUnits = app.measurementRepository.customUnits.first()
        val logicalDate = logicalHabitReminderDate(
            inputData.getLong(LOGICAL_EPOCH_DAY, Long.MIN_VALUE),
            app.clock.today(),
        )
        if (!habit.archived && !habit.paused && domainHabit?.reminderNeededOn(logs, logicalDate, customUnits, skips) != false) {
            HabitReminderNotifications.show(applicationContext, habit, logicalDate)
        }
        HabitReminderScheduler(applicationContext, app.settingsRepository).scheduleNext(id, System.currentTimeMillis() + 60_000)
        return Result.success()
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

    fun show(context: Context, habit: HabitEntity, logicalDate: LocalDate) {
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(habit.name)
            .setContentText("Time for your habit check-in").setContentIntent(intent)
            .setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .apply {
                val actionToken = System.currentTimeMillis()
                if (habit.trackingMode == "CheckOff") {
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
                                .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, actionToken),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                } else if (habit.trackingMode in setOf("Count", "Decimal")) {
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
                                .putExtra(HabitReminderActionReceiver.EXTRA_INCREMENT, habit.quickIncrement)
                                .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, actionToken),
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
                            .putExtra(HabitReminderActionReceiver.EXTRA_ACTION_TOKEN, actionToken),
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
        val logicalDate = logicalEpochDay.takeUnless { it == Long.MIN_VALUE }
            ?.let(LocalDate::ofEpochDay)
            ?: app.clock.today()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val action = HabitNotificationAction.fromIntentAction(intent.action)
            val increment = intent.getDoubleExtra(EXTRA_INCREMENT, 1.0)
            val actionId = "habit:$habitId:${intent.action}:$logicalEpochDay:${intent.getLongExtra(EXTRA_ACTION_TOKEN, 0L)}"
            val ledger = NotificationActionLedger(context)
            try {
                if (action == null || !app.isCurrentHabitNotificationAction(habitId, logicalDate, action, increment)) {
                    app.refreshStaleHabitNotification(context, habitId)
                    return@launch
                }
                if (!ledger.begin(actionId)) return@launch

                val applied = if (action == HabitNotificationAction.Snooze) {
                    // A second validation closes the edit/tap race before new work is enqueued.
                    if (!app.isCurrentHabitNotificationAction(habitId, logicalDate, action, increment)) false
                    else {
                        app.habitReminderScheduler.snooze(habitId, logicalDate.toEpochDay())
                        true
                    }
                } else {
                    app.database.withTransaction {
                        if (!app.isCurrentHabitNotificationAction(habitId, logicalDate, action, increment)) {
                            return@withTransaction false
                        }
                        when (action) {
                            HabitNotificationAction.Complete -> app.habitRepository.setCheckOff(habitId, logicalDate, true)
                            HabitNotificationAction.Increment -> app.habitRepository.log(
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
                }
                if (!applied) {
                    ledger.release(actionId)
                    app.refreshStaleHabitNotification(context, habitId)
                    return@launch
                }

                if (action != HabitNotificationAction.Snooze) {
                    app.habitReminderScheduler.syncHabit(habitId)
                }
                NotificationManagerCompat.from(context).cancel(HabitReminderNotifications.notificationId(habitId))
                ledger.complete(actionId)
            } catch (_: Throwable) {
                ledger.release(actionId)
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
    }
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
): Boolean {
    val dao = database.habitDao()
    val stored = dao.getHabit(habitId) ?: return false
    val habit = stored.toReminderDomain()
    if (habit.archived || habit.paused) return false

    val configuredMinutes = stored.weekdayReminderMinutesCsv.parseWeekdayReminderMinutes()[logicalDate.dayOfWeek]
        ?: stored.reminderMinutesCsv.split(',').mapNotNull(String::toIntOrNull)
    if (configuredMinutes.isEmpty()) return false

    val logs = dao.getAllLogs().asSequence()
        .filter { it.habitId == habitId }
        .map(HabitLogEntity::toReminderDomain)
        .toList()
    val pauses = dao.getPauses(habitId).map(HabitPauseEntity::toReminderDomain)
    val skips = dao.getSkips(habitId).map(HabitSkipEntity::toReminderDomain)
    val customUnits = database.measurementDao().getUnit(habit.unitId)
        ?.takeIf(UnitDefinitionEntity::custom)
        ?.let(UnitDefinitionEntity::toReminderDomain)
        ?.let(::listOf)
        .orEmpty()
    if (!habit.reminderNeededOn(logs, logicalDate, customUnits, skips, pauses)) return false

    return when (action) {
        HabitNotificationAction.Complete ->
            habit.trackingMode == HabitTrackingMode.CheckOff && habit.sourceMetricId == null
        HabitNotificationAction.Increment ->
            habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal) &&
                habit.sourceMetricId == null && increment.isFinite() && increment > 0.0 &&
                increment == habit.quickIncrement
        HabitNotificationAction.Snooze -> true
    }
}

private suspend fun WhipApplication.refreshStaleHabitNotification(context: Context, habitId: Long) {
    NotificationManagerCompat.from(context).cancel(HabitReminderNotifications.notificationId(habitId))
    habitReminderScheduler.syncHabit(habitId)
}

internal fun formatNotificationNumber(value: Double, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getNumberInstance(locale).run {
        isGroupingUsed = true
        minimumFractionDigits = 0
        maximumFractionDigits = 6
        format(value)
    }

private fun HabitEntity.toReminderDomain() = Habit(
    id, uuid, metricId, name, notes, areaId, area,
    tagsCsv.split(',').map(String::trim).filter(String::isNotBlank), icon,
    HabitTrackingMode.valueOf(trackingMode), UnitDimension.valueOf(dimension), unitId,
    precision, TargetComparison.valueOf(comparison), targetMin, targetMax,
    TargetPeriod.valueOf(targetPeriod), rollingDays, HabitScheduleType.valueOf(scheduleType),
    scheduleInterval, weekdaysMask.toWeekdays(), flexibleTimesPerWeek,
    LocalDate.ofEpochDay(startEpochDay), HabitEndType.valueOf(endType),
    endEpochDay?.let(LocalDate::ofEpochDay), endValue, quickIncrement,
    quickActionsCsv.split(',').mapNotNull(String::toDoubleOrNull),
    reminderMinutesCsv.split(',').mapNotNull(String::toIntOrNull),
    weekdayReminderMinutesCsv.parseWeekdayReminderMinutes(), DayOfWeek.valueOf(weekStart),
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
