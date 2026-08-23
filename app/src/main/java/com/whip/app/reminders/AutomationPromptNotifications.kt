package com.whip.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.WhipLaunchActions
import com.whip.app.domain.TriggerTargetType
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

object AutomationPromptNotifications {
    const val CHANNEL_ID = "automation_prompts"

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Automation prompts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Next-action prompts created by Whip automations" },
        )
    }

    fun notificationId(occurrenceId: Long): Int = 60_000 + (occurrenceId % 100_000).toInt()
}

class AutomationPromptScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun syncAll() {
        val app = context.applicationContext as WhipApplication
        val rules = app.linkRepository.triggerRules.first().associateBy { it.id }
        app.linkRepository.triggerOccurrences.first().forEach { occurrence ->
            val rule = rules[occurrence.triggerRuleId]
            if (rule == null || !rule.enabled || occurrence.dismissedAt != null || occurrence.deliveredAt != null) {
                cancel(occurrence.id)
            } else {
                val delay = (occurrence.availableAt.toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0L)
                val request = OneTimeWorkRequestBuilder<AutomationPromptWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder().putLong(AutomationPromptWorker.OCCURRENCE_ID, occurrence.id).build())
                    .addTag(ALL_WHIP_WORK_TAG)
                    .addTag("automation-prompt-${occurrence.id}")
                    .build()
                workManager.enqueueUniqueWork(uniqueName(occurrence.id), ExistingWorkPolicy.REPLACE, request)
            }
        }
    }

    fun cancel(occurrenceId: Long) {
        workManager.cancelUniqueWork(uniqueName(occurrenceId))
        NotificationManagerCompat.from(context).cancel(AutomationPromptNotifications.notificationId(occurrenceId))
    }

    private fun uniqueName(id: Long) = "whip-automation-prompt-$id"
}

class AutomationPromptWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val occurrenceId = inputData.getLong(OCCURRENCE_ID, -1L)
        if (occurrenceId < 0) return Result.failure()
        val app = applicationContext as WhipApplication
        val dao = app.database.linkDao()
        val occurrence = dao.getTriggerOccurrence(occurrenceId) ?: return Result.success()
        val rule = dao.getTriggerRule(occurrence.triggerRuleId) ?: return Result.success()
        val now = System.currentTimeMillis()
        if (!automationPromptShouldNotify(rule.enabled, occurrence.dismissedAtMillis, occurrence.deliveredAtMillis, occurrence.availableAtMillis, now)) {
            return if (rule.enabled && occurrence.dismissedAtMillis == null && occurrence.deliveredAtMillis == null && occurrence.availableAtMillis > now) Result.retry() else Result.success()
        }
        if (dao.markTriggerOccurrenceDelivered(occurrenceId, System.currentTimeMillis()) == 0) return Result.success()

        val targetType = TriggerTargetType.valueOf(rule.targetType)
        val targetName = when (targetType) {
            TriggerTargetType.Habit -> app.database.habitDao().getHabit(rule.targetEntityId)?.name ?: "Habit"
            TriggerTargetType.Task -> app.database.taskDao().getTask(rule.targetEntityId)?.title ?: "Task"
        }
        val action = when (targetType) {
            TriggerTargetType.Habit -> WhipLaunchActions.ACTION_OPEN_HABIT
            TriggerTargetType.Task -> WhipLaunchActions.ACTION_OPEN_TASK
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
            .setAction(action)
            .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, rule.targetEntityId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            occurrenceId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, AutomationPromptNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(rule.name)
            .setContentText("Ready now · $targetName")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(AutomationPromptNotifications.notificationId(occurrenceId), notification)
        } catch (_: SecurityException) {
            // The occurrence stays visible inside Whip when notifications are declined.
        }
        return Result.success()
    }

    companion object { const val OCCURRENCE_ID = "occurrence_id" }
}

internal fun automationPromptShouldNotify(
    enabled: Boolean,
    dismissedAtMillis: Long?,
    deliveredAtMillis: Long?,
    availableAtMillis: Long,
    nowMillis: Long,
): Boolean = enabled && dismissedAtMillis == null && deliveredAtMillis == null && availableAtMillis <= nowMillis + 1_000L
