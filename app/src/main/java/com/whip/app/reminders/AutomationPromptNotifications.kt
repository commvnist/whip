package com.whip.app.reminders

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.whip.app.WhipApplication

object AutomationPromptNotifications {
    const val CHANNEL_ID = "automation_prompts"

    fun notificationId(occurrenceId: Long): Int = 60_000 + (occurrenceId % 100_000).toInt()
}

class AutomationPromptScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val preferences = context.getSharedPreferences("automation_prompt_scheduler", Context.MODE_PRIVATE)

    /**
     * Automations are retired. Keep this method while older ViewModels and restored process state
     * can still reference the scheduler, but it may only tear down persisted work.
     */
    suspend fun syncAll() {
        retirePersistedWork()
    }

    fun cancel(occurrenceId: Long) {
        workManager.cancelUniqueWork(uniqueName(occurrenceId))
        NotificationManagerCompat.from(context).cancel(AutomationPromptNotifications.notificationId(occurrenceId))
    }

    private suspend fun retirePersistedWork() {
        workManager.cancelAllWorkByTag(AUTOMATION_PROMPT_WORK_TAG)
        val ids = buildSet {
            preferences.getStringSet(SCHEDULED_IDS, emptySet()).orEmpty().mapNotNullTo(this, String::toLongOrNull)
            val app = context.applicationContext as? WhipApplication
            app?.database?.linkDao()?.getAllTriggerOccurrences()?.mapTo(this) { it.id }
        }
        ids.forEach(::cancel)
        preferences.edit { clear() }
        context.getSystemService(NotificationManager::class.java)
            .deleteNotificationChannel(AutomationPromptNotifications.CHANNEL_ID)
    }

    private fun uniqueName(id: Long) = "whip-automation-prompt-$id"

    private companion object {
        const val AUTOMATION_PROMPT_WORK_TAG = "whip-automation-prompts"
        const val SCHEDULED_IDS = "known_occurrence_ids"
    }
}

class AutomationPromptWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    // Keep this exact worker class for persisted WorkManager rows from older releases.
    override suspend fun doWork(): Result = Result.success()

    companion object { const val OCCURRENCE_ID = "occurrence_id" }
}

@Suppress("UNUSED_PARAMETER")
internal fun automationPromptShouldNotify(
    enabled: Boolean,
    dismissedAtMillis: Long?,
    deliveredAtMillis: Long?,
    availableAtMillis: Long,
    nowMillis: Long,
    manualPrompt: Boolean = true,
): Boolean = false
