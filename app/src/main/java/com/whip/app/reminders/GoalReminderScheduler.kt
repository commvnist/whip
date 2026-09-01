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
import androidx.work.await
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.SettingsRepository
import com.whip.app.core.WhipLaunchActions
import com.whip.app.core.adjustForQuietHours
import com.whip.app.core.zoneId
import com.whip.app.data.GoalEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.GoalType
import com.whip.app.startup.MISSING_USER_DATA_GENERATION
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GoalReminderScheduler(context: Context, private val settingsRepository: SettingsRepository? = null) {
    private val appContext = context.applicationContext
    private val app = appContext as WhipApplication
    private val workManager = WorkManager.getInstance(appContext)
    private val dao = WhipDatabase.get(appContext).goalDao()

    suspend fun syncAll(allowDuringRecovery: Boolean = false) {
        if (!allowDuringRecovery) {
            app.withUserDataAccess { syncAllInternal() }
            return
        }
        syncAllInternal()
    }

    private suspend fun syncAllInternal() {
        dao.getReminderGoalIds().forEach { id -> syncGoalCoordinated(id) }
    }

    suspend fun syncGoal(id: Long, allowDuringRecovery: Boolean = false) {
        if (!allowDuringRecovery) {
            app.withUserDataAccess { syncGoalCoordinated(id) }
            return
        }
        syncGoalCoordinated(id)
    }

    private suspend fun syncGoalCoordinated(id: Long) {
        app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Goal, id) {
            NotificationManagerCompat.from(appContext).cancel(GoalReminderNotifications.notificationId(id))
            workManager.cancelAllWorkByTag(tag(id)).await()
            scheduleNextUnlocked(id, System.currentTimeMillis())
        }
    }

    suspend fun scheduleNext(
        id: Long,
        afterMillis: Long,
        allowDuringRecovery: Boolean = false,
    ) {
        if (!allowDuringRecovery) {
            app.withUserDataAccess { scheduleNextCoordinated(id, afterMillis) }
            return
        }
        scheduleNextCoordinated(id, afterMillis)
    }

    private suspend fun scheduleNextCoordinated(id: Long, afterMillis: Long) {
        app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Goal, id) {
            scheduleNextUnlocked(id, afterMillis)
        }
    }

    /** A worker cannot cancel its own tag while awaiting WorkManager. Old claims
     * are already fail-closed, so reconciliation drains them and installs the
     * authoritative successor without self-cancellation. */
    internal suspend fun reconcileFromWorker(id: Long, stale: Boolean) {
        app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Goal, id) {
            if (stale) {
                NotificationManagerCompat.from(appContext)
                    .cancel(GoalReminderNotifications.notificationId(id))
            }
            scheduleNextUnlocked(id, System.currentTimeMillis() + 1L)
        }
    }

    private suspend fun scheduleNextUnlocked(id: Long, afterMillis: Long) {
        val goal = dao.getGoal(id) ?: return
        val settings = settingsRepository?.current()
        val zone = settings?.zoneId() ?: ZoneId.systemDefault()
        val reminder = nextGoalReminder(
            goal = goal,
            afterMillis = afterMillis,
            zone = zone,
            quietStartMinutes = settings?.quietStartMinutes,
            quietEndMinutes = settings?.quietEndMinutes,
        ) ?: return
        val claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Scheduled,
            stableEntityId = goal.uuid,
            logicalEpochDay = reminder.logicalDate.toEpochDay(),
            expectedTriggerAtMillis = reminder.triggerAtMillis,
            definitionFingerprint = goalReminderSemanticFingerprint(
                goal,
                zone,
                settings?.quietStartMinutes,
                settings?.quietEndMinutes,
            ),
        )
        val request = OneTimeWorkRequestBuilder<GoalReminderWorker>()
            .setInitialDelay(
                (reminder.triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0),
                TimeUnit.MILLISECONDS,
            )
            .setInputData(
                Data.Builder()
                    .putLong(GoalReminderWorker.GOAL_ID, id)
                    .putReminderDeliveryClaim(claim)
                    .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
                    .build(),
            )
            .addTag(tag(id))
            .addTag(ALL_WHIP_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(
            "${tag(id)}-${reminder.triggerAtMillis}",
            ExistingWorkPolicy.REPLACE,
            request,
        ).await()
    }

    internal suspend fun snooze(
        id: Long,
        originatingClaim: ReminderDeliveryClaim,
        minutes: Int = 10,
        allowDuringRecovery: Boolean = false,
    ): Boolean {
        if (!allowDuringRecovery) {
            return app.withUserDataAccess {
                snoozeCoordinated(id, originatingClaim, minutes)
            } ?: false
        }
        return snoozeCoordinated(id, originatingClaim, minutes)
    }

    private suspend fun snoozeCoordinated(
        id: Long,
        originatingClaim: ReminderDeliveryClaim,
        minutes: Int,
    ): Boolean = app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Goal, id) {
        app.reminderDeliveryCoordinator.withStateBoundary {
            val goal = dao.getGoal(id) ?: return@withStateBoundary false
            val settings = settingsRepository?.current()
            val zone = settings?.zoneId() ?: ZoneId.systemDefault()
            if (!currentGoalReminderClaimIsValid(
                    goal = goal,
                    claim = originatingClaim,
                    now = app.clock.now(),
                    zone = zone,
                    quietStartMinutes = settings?.quietStartMinutes,
                    quietEndMinutes = settings?.quietEndMinutes,
                )
            ) return@withStateBoundary false

            val trigger = System.currentTimeMillis() + minutes.coerceIn(1, 1_440) * 60_000L
            val deliveryDate = Instant.ofEpochMilli(trigger).atZone(zone).toLocalDate()
            if (!goalReminderIsWithinDeadline(goal.deadlineEpochDay, deliveryDate)) {
                return@withStateBoundary false
            }
            val claim = ReminderDeliveryClaim(
                kind = ReminderDeliveryKind.Snoozed,
                stableEntityId = goal.uuid,
                logicalEpochDay = originatingClaim.logicalEpochDay,
                expectedTriggerAtMillis = trigger,
                definitionFingerprint = originatingClaim.definitionFingerprint,
            )
            val request = OneTimeWorkRequestBuilder<GoalReminderWorker>()
                .setInitialDelay((trigger - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putLong(GoalReminderWorker.GOAL_ID, id)
                        .putReminderDeliveryClaim(claim)
                        .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
                        .build(),
                )
                .addTag(tag(id))
                .addTag(ALL_WHIP_WORK_TAG)
                .build()
            workManager.enqueueUniqueWork("${tag(id)}-snooze", ExistingWorkPolicy.REPLACE, request).await()
            NotificationManagerCompat.from(appContext).cancel(GoalReminderNotifications.notificationId(id))
            true
        }
    }

    private fun tag(id: Long) = "whip-goal-reminder-$id"
}

internal data class GoalReminderTime(val triggerAtMillis: Long, val logicalDate: LocalDate)

internal fun nextGoalReminder(
    goal: GoalEntity,
    afterMillis: Long,
    zone: ZoneId,
    quietStartMinutes: Int?,
    quietEndMinutes: Int?,
): GoalReminderTime? {
    val minute = goal.reminderMinutes?.takeIf { it in 0..1_439 } ?: return null
    if (goal.archived || goal.status != "Active") return null
    val after = Instant.ofEpochMilli(afterMillis).atZone(zone)
    val goalStart = runCatching { LocalDate.ofEpochDay(goal.startEpochDay) }.getOrNull() ?: return null
    val firstLogicalDate = after.toLocalDate().minusDays(1)
        .coerceAtLeast(goalStart)
    // Quiet hours can shift a configured time later or across midnight. Select
    // using the adjusted instant, not the raw wall time, and include yesterday
    // so a cross-midnight delivery is not lost during reconciliation.
    return generateSequence(firstLogicalDate) { it.plusDays(1) }
        .take(4)
        .filter { logicalDate -> goalReminderLogicalDateIsEligible(goal, logicalDate.toEpochDay()) }
        .map { logicalDate ->
            val trigger = adjustForQuietHours(
                logicalDate.atTime(minute / 60, minute % 60).atZone(zone).toInstant(),
                zone,
                quietStartMinutes,
                quietEndMinutes,
            ).toEpochMilli()
            GoalReminderTime(trigger, logicalDate)
        }
        .filter { reminder -> reminder.triggerAtMillis >= afterMillis }
        .filter { reminder ->
            goalReminderIsWithinDeadline(
                goal.deadlineEpochDay,
                Instant.ofEpochMilli(reminder.triggerAtMillis).atZone(zone).toLocalDate(),
            )
        }
        .minByOrNull(GoalReminderTime::triggerAtMillis)
}

internal fun goalReminderIsWithinDeadline(deadlineEpochDay: Long?, deliveryDate: LocalDate): Boolean =
    deadlineEpochDay == null || deliveryDate.toEpochDay() <= deadlineEpochDay

internal fun goalReminderLogicalDateIsEligible(goal: GoalEntity, logicalEpochDay: Long): Boolean =
    logicalEpochDay >= goal.startEpochDay &&
        (goal.deadlineEpochDay == null || logicalEpochDay <= goal.deadlineEpochDay)

internal fun goalReminderSemanticFingerprint(
    goal: GoalEntity,
    zone: ZoneId,
    quietStartMinutes: Int?,
    quietEndMinutes: Int?,
): String = reminderSemanticFingerprint(
    "goal-v2",
    goal.uuid,
    goal.type,
    goal.status,
    goal.archived,
    goal.startEpochDay,
    goal.deadlineEpochDay,
    goal.reminderMinutes,
    zone.id,
    quietStartMinutes,
    quietEndMinutes,
)

internal fun currentGoalReminderClaimIsValid(
    goal: GoalEntity,
    claim: ReminderDeliveryClaim,
    now: Instant,
    zone: ZoneId,
    quietStartMinutes: Int?,
    quietEndMinutes: Int?,
): Boolean {
    val logicalDate = runCatching { LocalDate.ofEpochDay(claim.logicalEpochDay) }.getOrNull() ?: return false
    if (
        goal.archived || goal.status != "Active" || goal.reminderMinutes !in 0..1_439 ||
        goal.uuid != claim.stableEntityId ||
        !goalReminderLogicalDateIsEligible(goal, claim.logicalEpochDay) ||
        goalReminderSemanticFingerprint(goal, zone, quietStartMinutes, quietEndMinutes) !=
        claim.definitionFingerprint
    ) return false
    val today = now.atZone(zone).toLocalDate()
    if (!reminderClaimIsForToday(claim, today, zone)) return false
    if (!goalReminderIsWithinDeadline(goal.deadlineEpochDay, today)) return false
    if (claim.expectedTriggerAtMillis > now.toEpochMilli()) return false
    if (claim.kind == ReminderDeliveryKind.Snoozed) return true
    val minute = requireNotNull(goal.reminderMinutes)
    val exactTrigger = adjustForQuietHours(
        logicalDate
            .atTime(minute / 60, minute % 60)
            .atZone(zone)
            .toInstant(),
        zone,
        quietStartMinutes,
        quietEndMinutes,
    ).toEpochMilli()
    return exactTrigger == claim.expectedTriggerAtMillis
}

class GoalReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WhipApplication
        return app.withUserDataAccess {
            if (!app.isCurrentUserDataGeneration(
                    inputData.getLong(USER_DATA_GENERATION_KEY, MISSING_USER_DATA_GENERATION),
                )
            ) return@withUserDataAccess Result.success()
            val id = inputData.getLong(GOAL_ID, -1)
            if (id < 0) return@withUserDataAccess Result.success()
            val claim = inputData.reminderDeliveryClaimOrNull()
            var delivered = false
            if (claim != null) {
                delivered = app.reminderDeliveryCoordinator.withEntity(ReminderDomain.Goal, id) {
                    app.reminderDeliveryCoordinator.withStateBoundary {
                    val goal = WhipDatabase.get(applicationContext).goalDao().getGoal(id)
                        ?: return@withStateBoundary false
                    val settings = app.settingsRepository.current()
                    val zone = settings.zoneId()
                    if (!currentGoalReminderClaimIsValid(
                            goal,
                            claim,
                            app.clock.now(),
                            zone,
                            settings.quietStartMinutes,
                            settings.quietEndMinutes,
                        )
                    ) return@withStateBoundary false
                    GoalReminderNotifications.show(applicationContext, goal, claim)
                    true
                    }
                }
            }
            app.goalReminderScheduler.reconcileFromWorker(id, stale = !delivered)
            Result.success()
        } ?: Result.retry()
    }

    companion object {
        const val GOAL_ID = "goal_id"
    }
}

object GoalReminderNotifications {
    const val CHANNEL_ID = "goal_reminders"

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Goal reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders for goals you chose to review"
            },
        )
    }

    internal fun show(context: Context, goal: GoalEntity, claim: ReminderDeliveryClaim) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
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
        val snooze = PendingIntent.getBroadcast(
            context,
            (goal.id xor 0x534e4f5a).hashCode(),
            Intent(context, GoalReminderActionReceiver::class.java)
                .setAction(GoalReminderActionReceiver.ACTION_SNOOZE)
                .putExtra(GoalReminderActionReceiver.EXTRA_GOAL_ID, goal.id)
                .putExtra(GoalReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, claim.logicalEpochDay)
                .putExtra(GoalReminderActionReceiver.EXTRA_STABLE_ENTITY_ID, claim.stableEntityId)
                .putExtra(GoalReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT, claim.definitionFingerprint)
                .putExtra(GoalReminderActionReceiver.EXTRA_EXPECTED_TRIGGER_AT_MILLIS, claim.expectedTriggerAtMillis)
                .putExtra(GoalReminderActionReceiver.EXTRA_DELIVERY_KIND, claim.kind.name)
                .putExtra(GoalReminderActionReceiver.EXTRA_CLAIM_VERSION, claim.version)
                .putExtra(GoalReminderActionReceiver.EXTRA_ACTION_TOKEN, System.currentTimeMillis())
                .putExtra(USER_DATA_GENERATION_KEY, dataGeneration),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(goal.name)
            .setContentText(goalReminderPrompt(goal.type))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(R.drawable.ic_notification, "Snooze 10 min", snooze)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(goal.id), notification)
    }

    internal fun notificationId(goalId: Long): Int = (goalId xor 0x474f414c).hashCode()
}

internal fun goalReminderPrompt(goalType: String): String = when (
    runCatching { GoalType.valueOf(goalType) }.getOrNull()
) {
    GoalType.Consistency -> "Record today's progress"
    GoalType.WeightedMilestones -> "Review your milestones"
    GoalType.ElapsedSince -> "Review elapsed time"
    else -> "Log goal progress"
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
                        NotificationManagerCompat.from(context).cancel(GoalReminderNotifications.notificationId(goalId))
                        return@withUserDataAccess Unit
                    }
                    val actionId =
                        "goal:$goalId:${intent.action}:${intent.getLongExtra(EXTRA_ACTION_TOKEN, 0L)}"
                    val ledger = NotificationActionLedger(context)
                    if (!ledger.begin(actionId)) return@withUserDataAccess Unit
                    val claim = intent.goalReminderDeliveryClaimOrNull()
                    runCatching {
                        claim != null && app.goalReminderScheduler.snooze(
                                id = goalId,
                                originatingClaim = claim,
                                allowDuringRecovery = true,
                            )
                    }.onSuccess { applied ->
                        if (applied) {
                            NotificationManagerCompat.from(context)
                                .cancel(GoalReminderNotifications.notificationId(goalId))
                            ledger.complete(actionId)
                        } else {
                            ledger.release(actionId)
                            app.goalReminderScheduler.syncGoal(goalId, allowDuringRecovery = true)
                        }
                    }.onFailure { ledger.release(actionId) }
                }
                if (accessed == null) {
                    NotificationManagerCompat.from(context).cancel(GoalReminderNotifications.notificationId(goalId))
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
        const val EXTRA_LOGICAL_EPOCH_DAY = "logical_epoch_day"
        const val EXTRA_STABLE_ENTITY_ID = "stable_entity_id"
        const val EXTRA_DEFINITION_FINGERPRINT = "definition_fingerprint"
        const val EXTRA_EXPECTED_TRIGGER_AT_MILLIS = "expected_trigger_at_millis"
        const val EXTRA_DELIVERY_KIND = "delivery_kind"
        const val EXTRA_CLAIM_VERSION = "claim_version"
    }
}

private fun Intent.goalReminderDeliveryClaimOrNull(): ReminderDeliveryClaim? {
    val claim = ReminderDeliveryClaim(
        version = getIntExtra(GoalReminderActionReceiver.EXTRA_CLAIM_VERSION, -1),
        kind = getStringExtra(GoalReminderActionReceiver.EXTRA_DELIVERY_KIND)
            ?.let { runCatching { ReminderDeliveryKind.valueOf(it) }.getOrNull() }
            ?: return null,
        stableEntityId = getStringExtra(GoalReminderActionReceiver.EXTRA_STABLE_ENTITY_ID).orEmpty(),
        logicalEpochDay = getLongExtra(GoalReminderActionReceiver.EXTRA_LOGICAL_EPOCH_DAY, Long.MIN_VALUE),
        expectedTriggerAtMillis = getLongExtra(
            GoalReminderActionReceiver.EXTRA_EXPECTED_TRIGGER_AT_MILLIS,
            -1L,
        ),
        definitionFingerprint = getStringExtra(
            GoalReminderActionReceiver.EXTRA_DEFINITION_FINGERPRINT,
        ).orEmpty(),
    )
    return claim.takeIf(ReminderDeliveryClaim::isStructurallyValid)
}
