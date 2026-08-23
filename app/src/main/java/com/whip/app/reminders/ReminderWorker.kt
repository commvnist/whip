package com.whip.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whip.app.data.WhipDatabase
import com.whip.app.data.toDomain
import com.whip.app.WhipApplication

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(TASK_ID, -1)
        if (taskId < 0) return Result.failure()

        val dao = WhipDatabase.get(applicationContext).taskDao()
        val task = dao.getTask(taskId)?.toDomain()
            ?: return Result.success()
        if (!task.archived && task.completedAtMillis == null) {
            ReminderNotifications.show(
                applicationContext,
                task,
                inputData.getLong(ORIGINAL_EPOCH_DAY, Long.MIN_VALUE)
                    .takeUnless { it == Long.MIN_VALUE },
                allowDirectCompletion = dao.getSteps(taskId).none { !it.archived },
            )
        }

        val app = applicationContext as WhipApplication
        ReminderScheduler(applicationContext, app.settingsRepository).scheduleNext(
            taskId = taskId,
            afterMillis = System.currentTimeMillis() + 60_000,
        )
        return Result.success()
    }

    companion object {
        const val TASK_ID = "task_id"
        const val ORIGINAL_EPOCH_DAY = "original_epoch_day"
        const val OFFSET_MINUTES = "offset_minutes"
    }
}
