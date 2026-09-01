package com.whip.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whip.app.data.WhipDatabase
import com.whip.app.data.toDomain
import com.whip.app.WhipApplication
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WhipApplication
        return app.withUserDataAccess {
            if (!app.isCurrentUserDataGeneration(
                    inputData.getLong(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION),
                )
            ) return@withUserDataAccess Result.success()
            val taskId = inputData.getLong(TASK_ID, -1)
            if (taskId < 0) return@withUserDataAccess Result.failure()

            val dao = WhipDatabase.get(applicationContext).taskDao()
            val task = dao.getTask(taskId)?.toDomain()
                ?: return@withUserDataAccess Result.success()
            if (!task.archived && task.completedAtMillis == null) {
                ReminderNotifications.show(
                    applicationContext,
                    task,
                    inputData.getLong(ORIGINAL_EPOCH_DAY, Long.MIN_VALUE)
                        .takeUnless { it == Long.MIN_VALUE },
                    allowDirectCompletion = dao.getSteps(taskId).none { !it.archived },
                )
            }

            ReminderScheduler(applicationContext, app.settingsRepository).scheduleNext(
                taskId = taskId,
                afterMillis = System.currentTimeMillis() + 60_000,
                allowDuringRecovery = true,
            )
            Result.success()
        } ?: Result.retry()
    }

    companion object {
        const val TASK_ID = "task_id"
        const val ORIGINAL_EPOCH_DAY = "original_epoch_day"
        const val OFFSET_MINUTES = "offset_minutes"
    }
}
