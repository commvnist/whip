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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.whip.app.MainActivity
import com.whip.app.WhipApplication
import com.whip.app.R
import com.whip.app.data.GoalEntity
import com.whip.app.core.SettingsRepository
import com.whip.app.core.adjustForQuietHours
import com.whip.app.core.zoneId
import com.whip.app.core.WhipLaunchActions
import com.whip.app.data.WhipDatabase
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GoalReminderScheduler(context: Context, private val settingsRepository: SettingsRepository? = null) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val dao = WhipDatabase.get(appContext).goalDao()
    suspend fun syncAll(allowDuringRecovery: Boolean = false) {
        if (!allowDuringRecovery) {
            val app = appContext as? WhipApplication ?: return
            app.withUserDataAccess { syncAllInternal() }
            return
        }
        syncAllInternal()
    }
    private suspend fun syncAllInternal() = dao.getReminderGoalIds().forEach { syncGoalInternal(it) }
    suspend fun syncGoal(id: Long, allowDuringRecovery: Boolean = false) {
        if (!allowDuringRecovery) {
            val app = appContext as? WhipApplication ?: return
            app.withUserDataAccess { syncGoalInternal(id) }
            return
        }
        syncGoalInternal(id)
    }
    private suspend fun syncGoalInternal(id: Long) {
        workManager.cancelAllWorkByTag(tag(id))
        scheduleNextInternal(id, System.currentTimeMillis())
    }
    suspend fun scheduleNext(
        id: Long,
        afterMillis: Long,
        allowDuringRecovery: Boolean = false,
    ) {
        if (!allowDuringRecovery) {
            val app = appContext as? WhipApplication ?: return
            app.withUserDataAccess { scheduleNextInternal(id, afterMillis) }
            return
        }
        scheduleNextInternal(id, afterMillis)
    }
    private suspend fun scheduleNextInternal(id: Long, afterMillis: Long) {
        val goal = dao.getGoal(id) ?: return
        val minute = goal.reminderMinutes ?: return
        if (goal.status != "Active") return
        val zone = settingsRepository?.current()?.zoneId() ?: ZoneId.systemDefault()
        val after = Instant.ofEpochMilli(afterMillis).atZone(zone)
        var date = after.toLocalDate().coerceAtLeast(java.time.LocalDate.ofEpochDay(goal.startEpochDay))
        var trigger = date.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
        if (trigger < afterMillis) { date = date.plusDays(1); trigger = date.atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli() }
        if (goal.deadlineEpochDay != null && date.toEpochDay() > goal.deadlineEpochDay) return
        val settings = settingsRepository?.current()
        trigger = adjustForQuietHours(
            Instant.ofEpochMilli(trigger),
            zone,
            settings?.quietStartMinutes,
            settings?.quietEndMinutes,
        ).toEpochMilli()
        val adjustedDate = Instant.ofEpochMilli(trigger).atZone(zone).toLocalDate()
        if (!goalReminderIsWithinDeadline(goal.deadlineEpochDay, adjustedDate)) return
        val request = OneTimeWorkRequestBuilder<GoalReminderWorker>().setInitialDelay((trigger - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(GoalReminderWorker.GOAL_ID, id)
                    .putLong(GoalReminderWorker.LOGICAL_EPOCH_DAY, date.toEpochDay())
                    .putLong(
                        USER_DATA_GENERATION_KEY,
                        (appContext as WhipApplication).currentUserDataGeneration(),
                    )
                    .build(),
            ).addTag(tag(id)).addTag(ALL_WHIP_WORK_TAG).build()
        workManager.enqueueUniqueWork("${tag(id)}-$trigger", ExistingWorkPolicy.REPLACE, request)
    }
    fun snooze(id: Long, minutes: Int = 10, allowDuringRecovery: Boolean = false) {
        val app = appContext as WhipApplication
        if (allowDuringRecovery) {
            snoozeInternal(id, minutes, app.currentUserDataGeneration())
        } else {
            app.tryWithUserDataAccessNow {
                snoozeInternal(id, minutes, app.currentUserDataGeneration())
            }
        }
    }
    private fun snoozeInternal(id: Long, minutes: Int, generation: Long) {
        val request = OneTimeWorkRequestBuilder<GoalReminderWorker>()
            .setInitialDelay(minutes.coerceIn(1, 1_440).toLong(), TimeUnit.MINUTES)
            .setInputData(
                Data.Builder()
                    .putLong(GoalReminderWorker.GOAL_ID, id)
                    .putLong(
                        USER_DATA_GENERATION_KEY,
                        generation,
                    )
                    .build(),
            )
            .addTag(tag(id)).addTag(ALL_WHIP_WORK_TAG).build()
        workManager.enqueueUniqueWork("${tag(id)}-snooze", ExistingWorkPolicy.REPLACE, request)
    }
    private fun tag(id: Long) = "whip-goal-reminder-$id"
}

internal fun goalReminderIsWithinDeadline(deadlineEpochDay: Long?, deliveryDate: java.time.LocalDate): Boolean =
    deadlineEpochDay == null || deliveryDate.toEpochDay() <= deadlineEpochDay

class GoalReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WhipApplication
        return app.withUserDataAccess {
            if (!app.isCurrentUserDataGeneration(
                    inputData.getLong(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION),
                )
            ) return@withUserDataAccess Result.success()
            val id = inputData.getLong(GOAL_ID, -1)
            if (id < 0) return@withUserDataAccess Result.failure()
            val goal = WhipDatabase.get(applicationContext).goalDao().getGoal(id)
                ?: return@withUserDataAccess Result.success()
            val today = app.clock.today()
            val beforeOrOnDeadline = goal.deadlineEpochDay == null || today.toEpochDay() <= goal.deadlineEpochDay
            if (goal.status == "Active" && beforeOrOnDeadline) GoalReminderNotifications.show(applicationContext, goal)
            GoalReminderScheduler(applicationContext, app.settingsRepository).scheduleNext(
                id,
                System.currentTimeMillis() + 60_000,
                allowDuringRecovery = true,
            )
            Result.success()
        } ?: Result.retry()
    }
    companion object {
        const val GOAL_ID = "goal_id"
        const val LOGICAL_EPOCH_DAY = "logical_epoch_day"
    }
}

object GoalReminderNotifications {
    const val CHANNEL_ID = "goal_reminders"
    fun createChannel(context: Context) { context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "Goal reminders", NotificationManager.IMPORTANCE_DEFAULT)) }
    fun show(context: Context, goal: GoalEntity) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val dataGeneration = (context.applicationContext as WhipApplication).currentUserDataGeneration()
        val pending = PendingIntent.getActivity(
            context,
            goal.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .setAction(WhipLaunchActions.ACTION_OPEN_GOAL)
                .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, goal.id)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setContentTitle(goal.name)
            .setContentText("Record a goal measurement").setContentIntent(pending).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(
                R.drawable.ic_notification,
                "Snooze 10 min",
                PendingIntent.getBroadcast(
                    context,
                    (goal.id xor 0x534e4f5a).hashCode(),
                    Intent(context, GoalReminderActionReceiver::class.java)
                        .setAction(GoalReminderActionReceiver.ACTION_SNOOZE)
                        .putExtra(GoalReminderActionReceiver.EXTRA_GOAL_ID, goal.id)
                        .putExtra(GoalReminderActionReceiver.EXTRA_ACTION_TOKEN, System.currentTimeMillis())
                        .putExtra(USER_DATA_GENERATION_KEY, dataGeneration),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        NotificationManagerCompat.from(context).notify((goal.id xor 0x474f414c).hashCode(), notification)
    }
}

class GoalReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE) return
        val goalId = intent.getLongExtra(EXTRA_GOAL_ID, -1)
        if (goalId < 0) return
        val pending = goAsync()
        val app = context.applicationContext as WhipApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val accessed = app.withUserDataAccess {
                    if (!app.isCurrentUserDataGeneration(
                            intent.getLongExtra(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION),
                        )
                    ) {
                        NotificationManagerCompat.from(context).cancel((goalId xor 0x474f414c).hashCode())
                        return@withUserDataAccess Unit
                    }
                    val actionId = "goal:$goalId:${intent.action}:${intent.getLongExtra(EXTRA_ACTION_TOKEN, 0L)}"
                    val ledger = NotificationActionLedger(context)
                    if (!ledger.begin(actionId)) return@withUserDataAccess Unit
                    runCatching {
                        app.goalReminderScheduler.snooze(goalId, allowDuringRecovery = true)
                        NotificationManagerCompat.from(context).cancel((goalId xor 0x474f414c).hashCode())
                    }.onSuccess { ledger.complete(actionId) }.onFailure { ledger.release(actionId) }
                }
                if (accessed == null) {
                    NotificationManagerCompat.from(context).cancel((goalId xor 0x474f414c).hashCode())
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "commvne.com.whip.app.action.SNOOZE_GOAL"
        const val EXTRA_GOAL_ID = "goal_id"
        const val EXTRA_ACTION_TOKEN = "action_token"
    }
}
