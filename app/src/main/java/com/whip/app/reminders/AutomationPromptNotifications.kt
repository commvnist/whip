package com.whip.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
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
import com.whip.app.domain.TriggerAction
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
    private val preferences = context.getSharedPreferences("automation_prompt_scheduler", Context.MODE_PRIVATE)

    suspend fun syncAll() {
        val app = context.applicationContext as WhipApplication
        val rules = app.linkRepository.triggerRules.first().associateBy { it.id }
        val occurrences = app.linkRepository.triggerOccurrences.first()
        val currentIds = occurrences.mapTo(mutableSetOf()) { it.id }
        preferences.getStringSet(SCHEDULED_IDS, emptySet()).orEmpty()
            .mapNotNull(String::toLongOrNull)
            .filterNot(currentIds::contains)
            .forEach(::cancel)
        occurrences.forEach { occurrence ->
            val rule = rules[occurrence.triggerRuleId]
            if (
                rule == null || !rule.enabled || !rule.notificationEnabled ||
                rule.action == TriggerAction.CheckOffHabit || occurrence.dismissedAt != null ||
                occurrence.fulfilledEntryId != null || occurrence.deliveredAt != null
            ) {
                cancel(occurrence.id)
            } else {
                val dueAt = occurrence.remindAt ?: occurrence.availableAt
                val delay = (dueAt.toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0L)
                val request = OneTimeWorkRequestBuilder<AutomationPromptWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder().putLong(AutomationPromptWorker.OCCURRENCE_ID, occurrence.id).build())
                    .addTag(ALL_WHIP_WORK_TAG)
                    .addTag(AUTOMATION_PROMPT_WORK_TAG)
                    .addTag("automation-prompt-${occurrence.id}")
                    .build()
                workManager.enqueueUniqueWork(uniqueName(occurrence.id), ExistingWorkPolicy.REPLACE, request)
            }
        }
        preferences.edit { putStringSet(SCHEDULED_IDS, currentIds.map(Long::toString).toSet()) }
    }

    fun cancel(occurrenceId: Long) {
        workManager.cancelUniqueWork(uniqueName(occurrenceId))
        NotificationManagerCompat.from(context).cancel(AutomationPromptNotifications.notificationId(occurrenceId))
    }

    private fun uniqueName(id: Long) = "whip-automation-prompt-$id"

    private companion object {
        const val AUTOMATION_PROMPT_WORK_TAG = "whip-automation-prompts"
        const val SCHEDULED_IDS = "known_occurrence_ids"
    }
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
        val dueAt = occurrence.remindAtMillis ?: occurrence.availableAtMillis
        val manualPrompt = rule.action != TriggerAction.CheckOffHabit.name
        if (!automationPromptShouldNotify(rule.enabled && rule.notificationEnabled, occurrence.dismissedAtMillis, occurrence.deliveredAtMillis, dueAt, now, manualPrompt) || occurrence.fulfilledEntryId != null) {
            return if (manualPrompt && rule.enabled && rule.notificationEnabled && occurrence.dismissedAtMillis == null && occurrence.deliveredAtMillis == null && occurrence.fulfilledEntryId == null && dueAt > now) Result.retry() else Result.success()
        }
        if (dao.markTriggerOccurrenceDelivered(occurrenceId, System.currentTimeMillis()) == 0) return Result.success()

        val targetType = TriggerTargetType.valueOf(rule.targetType)
        val targetName = when (targetType) {
            TriggerTargetType.Habit -> app.database.habitDao().getHabit(rule.targetEntityId)?.name ?: "Habit"
            TriggerTargetType.Task -> app.database.taskDao().getTask(rule.targetEntityId)?.title ?: "Task"
            TriggerTargetType.Track -> app.database.trackDao().getTrack(rule.targetEntityId)?.name ?: "Track"
        }
        val action = when (targetType) {
            TriggerTargetType.Habit -> WhipLaunchActions.ACTION_OPEN_HABIT
            TriggerTargetType.Task -> WhipLaunchActions.ACTION_OPEN_TASK
            TriggerTargetType.Track -> WhipLaunchActions.ACTION_OPEN_TRACK
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
            .setAction(action)
            .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, rule.targetEntityId)
            .putExtra(WhipLaunchActions.EXTRA_AUTOMATION_OCCURRENCE_ID, occurrenceId)
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
    manualPrompt: Boolean = true,
): Boolean = enabled && manualPrompt && dismissedAtMillis == null && deliveredAtMillis == null && availableAtMillis <= nowMillis + 1_000L
