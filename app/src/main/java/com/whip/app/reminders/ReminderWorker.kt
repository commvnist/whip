package com.whip.app.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whip.app.WhipApplication
import com.whip.app.core.zoneId
import com.whip.app.data.WhipDatabase
import com.whip.app.data.toDomain
import com.whip.app.domain.visibleTaskStepsForOccurrence
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.LocalDate

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
            val taskId = inputData.getLong(TASK_ID, -1L)
            if (taskId < 0L) return@withUserDataAccess Result.success()

            app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Task, taskId) {
                app.reminderDeliveryCoordinator.withStateBoundary {
                val scheduler = ReminderScheduler(applicationContext, app.settingsRepository)
                val dao = WhipDatabase.get(applicationContext).taskDao()
                val claim = inputData.reminderDeliveryClaimOrNull()
                val originalEpochDay = inputData.getLong(ORIGINAL_EPOCH_DAY, Long.MIN_VALUE)
                val originalDate = originalEpochDay
                    .takeUnless { it == Long.MIN_VALUE }
                    ?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
                val offsetMinutes = inputData.getInt(OFFSET_MINUTES, Int.MIN_VALUE)
                val stored = dao.getTask(taskId)
                val settings = app.settingsRepository.current()
                val occurrences = stored?.let {
                    dao.getOccurrences(taskId).toTaskReminderOccurrencesOrNull()
                }
                val snapshot = if (stored != null && originalDate != null && occurrences != null) {
                    resolveCurrentTaskReminder(
                        stored = stored,
                        occurrences = occurrences,
                        originalDate = originalDate,
                        offsetMinutes = offsetMinutes,
                        settings = settings,
                        requireOpen = true,
                    )
                } else null
                val now = app.clock.now()
                val physicalToday = now.atZone(settings.zoneId()).toLocalDate()
                val claimIsCurrent = taskReminderClaimMatchesCurrent(
                    claim = claim,
                    snapshot = snapshot,
                    inputOriginalEpochDay = originalEpochDay,
                    physicalToday = physicalToday,
                    zoneId = settings.zoneId(),
                    currentTimeMillis = now.toEpochMilli(),
                )

                if (!claimIsCurrent) {
                    scheduler.reconcileAfterWorkerFromCoordinator(
                        taskId = taskId,
                        afterMillis = System.currentTimeMillis() + 1L,
                    )
                    return@withStateBoundary Result.success()
                }

                val currentClaim = requireNotNull(claim)
                val currentSnapshot = requireNotNull(snapshot)
                val occurrenceKey = requireNotNull(originalDate).toEpochDay()
                val steps = dao.getSteps(taskId).map { it.toDomain() }
                val visibleSteps = visibleTaskStepsForOccurrence(
                    steps = steps,
                    snapshots = dao.getStepSnapshotsForTask(taskId).map { it.toDomain() },
                    occurrenceKey = occurrenceKey,
                    policy = currentSnapshot.task.repeatStepPolicy,
                )
                val stepStates = dao.getStepStates(taskId, occurrenceKey).associateBy { it.stepId }
                val hasUnfinishedCurrentSubtask = visibleSteps.any { step ->
                    stepStates[step.id]?.completed != true
                }
                ReminderNotifications.show(
                    context = applicationContext,
                    task = currentSnapshot.task,
                    originalEpochDay = occurrenceKey,
                    offsetMinutes = offsetMinutes,
                    stableEntityId = currentClaim.stableEntityId,
                    definitionFingerprint = currentClaim.definitionFingerprint,
                    allowDirectCompletion = !hasUnfinishedCurrentSubtask,
                )
                scheduler.scheduleNextFromCoordinator(
                    taskId = taskId,
                    afterMillis = maxOf(System.currentTimeMillis(), currentClaim.expectedTriggerAtMillis) + 1L,
                )
                Result.success()
                }
            }
        } ?: Result.retry()
    }

    companion object {
        const val TASK_ID = "task_id"
        const val ORIGINAL_EPOCH_DAY = "original_epoch_day"
        const val OFFSET_MINUTES = "offset_minutes"
    }
}

internal fun taskReminderClaimMatchesCurrent(
    claim: ReminderDeliveryClaim?,
    snapshot: CurrentTaskReminder?,
    inputOriginalEpochDay: Long,
    physicalToday: LocalDate,
    zoneId: java.time.ZoneId,
    currentTimeMillis: Long,
): Boolean = claim != null && snapshot != null &&
    snapshot.task.reminderEnabled &&
    claim.logicalEpochDay == inputOriginalEpochDay &&
    claim.logicalEpochDay == snapshot.originalDate.toEpochDay() &&
    claim.stableEntityId == snapshot.stableEntityId &&
    claim.definitionFingerprint == snapshot.definitionFingerprint &&
    reminderClaimIsForToday(claim, physicalToday, zoneId) &&
    claim.expectedTriggerAtMillis <= currentTimeMillis &&
    (claim.kind == ReminderDeliveryKind.Snoozed ||
        claim.expectedTriggerAtMillis == snapshot.expectedScheduledTriggerAtMillis)
