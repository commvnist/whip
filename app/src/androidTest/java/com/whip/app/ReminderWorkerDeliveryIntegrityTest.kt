package com.whip.app

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.whip.app.data.toDomain
import com.whip.app.data.TaskOccurrenceEntity
import com.whip.app.core.zoneId
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.reminders.ALL_WHIP_WORK_TAG
import com.whip.app.reminders.GoalReminderNotifications
import com.whip.app.reminders.GoalReminderWorker
import com.whip.app.reminders.HabitReminderNotifications
import com.whip.app.reminders.HabitReminderWorker
import com.whip.app.reminders.ReminderDeliveryClaim
import com.whip.app.reminders.ReminderDeliveryKind
import com.whip.app.reminders.ReminderDeletionCleanupStore
import com.whip.app.reminders.ReminderDomain
import com.whip.app.reminders.ReminderNotifications
import com.whip.app.reminders.ReminderWorker
import com.whip.app.reminders.currentHabitReminderDeliveryClaim
import com.whip.app.reminders.goalReminderSemanticFingerprint
import com.whip.app.reminders.putReminderDeliveryClaim
import com.whip.app.reminders.resolveCurrentTaskReminder
import com.whip.app.startup.USER_DATA_GENERATION_KEY
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderWorkerDeliveryIntegrityTest {
    private lateinit var app: WhipApplication
    private lateinit var notifications: NotificationManager

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
        app.backupRepository.deleteAllData()
        app.reconcilePendingReminderDeletions()
        app.settingsRepository.update {
            it.copy(timeZoneId = "UTC", quietStartMinutes = null, quietEndMinutes = null, dayCutoffMinutes = 0)
        }
        notifications = app.getSystemService(NotificationManager::class.java)
        notifications.cancelAll()
        ReminderNotifications.createChannel(app)
        GoalReminderNotifications.createChannel(app)
        HabitReminderNotifications.createChannel(app)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                app.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    @After
    fun tearDown() = runBlocking {
        notifications.cancelAll()
        WorkManager.getInstance(app).cancelAllWorkByTag(ALL_WHIP_WORK_TAG).result.get()
        app.backupRepository.deleteAllData()
    }

    @Test
    fun taskWorkerPostsOnlyTheExactCurrentClaim() = runBlocking {
        val now = app.clock.now().atZone(ZoneOffset.UTC)
        val logicalDate = now.toLocalDate()
        val taskId = app.taskRepository.create(
            TaskDraft(
                title = "Current Task claim",
                scheduleKind = ScheduleKind.Once,
                date = logicalDate,
                timeMinutes = now.hour * 60 + now.minute,
                reminderEnabled = true,
            ),
        )
        val dao = app.database.taskDao()
        val stored = requireNotNull(dao.getTask(taskId))
        val settings = app.settingsRepository.current()
        val snapshot = requireNotNull(
            resolveCurrentTaskReminder(
                stored = stored,
                occurrences = dao.getOccurrences(taskId).map { it.toDomain() },
                originalDate = logicalDate,
                offsetMinutes = 0,
                settings = settings,
                requireOpen = true,
            ),
        )
        val claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Scheduled,
            stableEntityId = snapshot.stableEntityId,
            logicalEpochDay = logicalDate.toEpochDay(),
            expectedTriggerAtMillis = snapshot.expectedScheduledTriggerAtMillis,
            definitionFingerprint = snapshot.definitionFingerprint,
        )
        val input = Data.Builder()
            .putLong(ReminderWorker.TASK_ID, taskId)
            .putLong(ReminderWorker.ORIGINAL_EPOCH_DAY, logicalDate.toEpochDay())
            .putInt(ReminderWorker.OFFSET_MINUTES, 0)
            .putReminderDeliveryClaim(claim)
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()

        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<ReminderWorker>(app).setInputData(input).build().doWork(),
        )
        assertTrue(notifications.activeNotifications.any { it.id == taskId.hashCode() })

        notifications.cancelAll()
        dao.updateTask(stored.copy(reminderEnabled = false, updatedAtMillis = stored.updatedAtMillis + 1))
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<ReminderWorker>(app).setInputData(input).build().doWork(),
        )
        assertFalse(notifications.activeNotifications.any { it.id == taskId.hashCode() })
    }

    @Test
    fun malformedPersistedTaskOccurrenceFailsThatReminderClosed() = runBlocking {
        val now = app.clock.now().atZone(ZoneOffset.UTC)
        val logicalDate = now.toLocalDate()
        val taskId = app.taskRepository.create(
            TaskDraft(
                title = "Malformed occurrence",
                scheduleKind = ScheduleKind.Once,
                date = logicalDate,
                timeMinutes = now.hour * 60 + now.minute,
                reminderEnabled = true,
            ),
        )
        app.database.taskDao().upsertOccurrence(
            TaskOccurrenceEntity(
                taskId = taskId,
                originalEpochDay = logicalDate.toEpochDay(),
                scheduledEpochDay = logicalDate.toEpochDay(),
                state = "not-a-valid-state",
                completedAtMillis = null,
            ),
        )

        app.reminderScheduler.syncTask(taskId)

        assertTrue(
            WorkManager.getInstance(app)
                .getWorkInfosByTag("whip-reminder-$taskId")
                .get()
                .none { it.state != androidx.work.WorkInfo.State.CANCELLED },
        )
        assertFalse(notifications.activeNotifications.any { it.id == taskId.hashCode() })
    }

    @Test
    fun habitWorkerAcceptsPartialHistoryButRejectsCompletionEditedDefinitionsAndConnectedWork() = runBlocking {
        val now = app.clock.now().atZone(ZoneOffset.UTC)
        val logicalDate = now.toLocalDate()
        val original = HabitDraft(
            name = "Current Habit claim",
            trackingMode = HabitTrackingMode.Count,
            targetMin = 10.0,
            startDate = logicalDate,
            reminderMinutes = listOf(now.hour * 60 + now.minute),
        )
        val habitId = app.habitRepository.create(original)
        val expectedTrigger = logicalDate.atTime(now.hour, now.minute).atZone(ZoneOffset.UTC)
            .toInstant().toEpochMilli()
        val claim = requireNotNull(
            app.currentHabitReminderDeliveryClaim(
                habitId = habitId,
                logicalDate = logicalDate,
                kind = ReminderDeliveryKind.Scheduled,
                expectedTriggerAtMillis = expectedTrigger,
            ),
        )
        val input = Data.Builder()
            .putLong(HabitReminderWorker.HABIT_ID, habitId)
            .putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, logicalDate.toEpochDay())
            .putReminderDeliveryClaim(claim)
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()

        val partialLogId = app.habitRepository.log(habitId, 5.0, date = logicalDate)
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<HabitReminderWorker>(app).setInputData(input).build().doWork(),
        )
        assertTrue(
            notifications.activeNotifications.any {
                it.id == HabitReminderNotifications.notificationId(habitId)
            },
        )

        notifications.cancelAll()
        val completingLogId = app.habitRepository.log(habitId, 5.0, date = logicalDate)
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<HabitReminderWorker>(app).setInputData(input).build().doWork(),
        )
        assertFalse(
            notifications.activeNotifications.any {
                it.id == HabitReminderNotifications.notificationId(habitId)
            },
        )

        app.habitRepository.undoLog(completingLogId)
        app.habitRepository.setPaused(habitId, true)
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<HabitReminderWorker>(app).setInputData(input).build().doWork(),
        )
        assertTrue(notifications.activeNotifications.isEmpty())

        app.habitRepository.setPaused(habitId, false)
        app.habitRepository.undoLog(partialLogId)
        app.habitRepository.skipDay(habitId, logicalDate)
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<HabitReminderWorker>(app).setInputData(input).build().doWork(),
        )
        assertTrue(notifications.activeNotifications.isEmpty())

        app.habitRepository.undoSkip(habitId, logicalDate)
        app.habitRepository.update(
            habitId,
            original.copy(reminderMinutes = listOf((now.hour * 60 + now.minute + 1) % 1_440)),
        )
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<HabitReminderWorker>(app).setInputData(input).build().doWork(),
        )
        assertTrue(notifications.activeNotifications.isEmpty())

        app.habitRepository.update(habitId, original)
        val connectedInput = Data.Builder()
            .putLong(HabitReminderWorker.HABIT_ID, habitId)
            .putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, logicalDate.toEpochDay())
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<HabitReminderWorker>(app).setInputData(connectedInput).build().doWork(),
        )
        assertTrue(notifications.activeNotifications.isEmpty())
    }

    @Test
    fun earlyHabitWorkerRequeuesTheExactDueReminderWithoutPosting() = runBlocking {
        val now = app.clock.now().atZone(ZoneOffset.UTC)
        val future = now.plusMinutes(2).withSecond(0).withNano(0)
        val logicalDate = future.toLocalDate()
        val habitId = app.habitRepository.create(
            HabitDraft(
                name = "Early Habit claim",
                trackingMode = HabitTrackingMode.CheckOff,
                startDate = logicalDate,
                reminderMinutes = listOf(future.hour * 60 + future.minute),
            ),
        )
        val fingerprintClaim = requireNotNull(
            app.currentHabitReminderDeliveryClaim(
                habitId = habitId,
                logicalDate = logicalDate,
                kind = ReminderDeliveryKind.Snoozed,
                expectedTriggerAtMillis = app.clock.now().toEpochMilli(),
            ),
        )
        val dueTrigger = future.toInstant().toEpochMilli()
        val scheduledClaim = fingerprintClaim.copy(
            kind = ReminderDeliveryKind.Scheduled,
            expectedTriggerAtMillis = dueTrigger,
        )
        val input = Data.Builder()
            .putLong(HabitReminderWorker.HABIT_ID, habitId)
            .putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, logicalDate.toEpochDay())
            .putReminderDeliveryClaim(scheduledClaim)
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()

        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<HabitReminderWorker>(app).setInputData(input).build().doWork(),
        )

        assertFalse(
            notifications.activeNotifications.any {
                it.id == HabitReminderNotifications.notificationId(habitId)
            },
        )
        assertTrue(
            WorkManager.getInstance(app)
                .getWorkInfosForUniqueWork("whip-habit-reminder-$habitId-$dueTrigger")
                .get()
                .any { it.state != androidx.work.WorkInfo.State.CANCELLED },
        )
    }

    @Test
    fun goalWorkerRejectsAnEditedDefinitionAndConnectedWork() = runBlocking {
        val now = app.clock.now().atZone(ZoneOffset.UTC)
        val logicalDate = now.toLocalDate()
        val goalId = app.goalRepository.create(
            GoalDraft(
                name = "Current Goal claim",
                type = GoalType.ReachValue,
                startDate = logicalDate,
                targetMin = 10.0,
                reminderMinutes = now.hour * 60 + now.minute,
            ),
        )
        val dao = app.database.goalDao()
        val stored = requireNotNull(dao.getGoal(goalId))
        val settings = app.settingsRepository.current()
        val zone = settings.zoneId()
        val trigger = logicalDate.atTime(now.hour, now.minute).atZone(zone)
            .toInstant().toEpochMilli()
        val claim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Scheduled,
            stableEntityId = stored.uuid,
            logicalEpochDay = logicalDate.toEpochDay(),
            expectedTriggerAtMillis = trigger,
            definitionFingerprint = goalReminderSemanticFingerprint(stored, zone, null, null),
        )
        val validInput = Data.Builder()
            .putLong(GoalReminderWorker.GOAL_ID, goalId)
            .putReminderDeliveryClaim(claim)
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()

        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<GoalReminderWorker>(app).setInputData(validInput).build().doWork(),
        )
        assertTrue(
            notifications.activeNotifications.any { notification ->
                notification.notification.extras.getCharSequence("android.title") == "Current Goal claim"
            },
        )

        notifications.cancelAll()
        dao.updateGoal(stored.copy(reminderMinutes = ((stored.reminderMinutes ?: 0) + 1) % 1_440))
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<GoalReminderWorker>(app).setInputData(validInput).build().doWork(),
        )
        assertTrue(notifications.activeNotifications.isEmpty())

        val connectedInput = Data.Builder()
            .putLong(GoalReminderWorker.GOAL_ID, goalId)
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<GoalReminderWorker>(app).setInputData(connectedInput).build().doWork(),
        )
        assertTrue(notifications.activeNotifications.isEmpty())
    }

    @Test
    fun productionMutationsCommitBeforeTaskHabitAndGoalWorkersCanPost() = runBlocking {
        val now = app.clock.now().atZone(ZoneOffset.UTC)
        val logicalDate = now.toLocalDate()

        val taskId = app.taskRepository.create(
            TaskDraft(
                title = "Linearized Task",
                scheduleKind = ScheduleKind.Once,
                date = logicalDate,
                timeMinutes = now.hour * 60 + now.minute,
                reminderEnabled = true,
            ),
        )
        val taskStored = requireNotNull(app.database.taskDao().getTask(taskId))
        val taskSnapshot = requireNotNull(
            resolveCurrentTaskReminder(
                taskStored,
                emptyList(),
                logicalDate,
                0,
                app.settingsRepository.current(),
                true,
            ),
        )
        val taskClaim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Scheduled,
            stableEntityId = taskSnapshot.stableEntityId,
            logicalEpochDay = logicalDate.toEpochDay(),
            expectedTriggerAtMillis = taskSnapshot.expectedScheduledTriggerAtMillis,
            definitionFingerprint = taskSnapshot.definitionFingerprint,
        )
        val taskInput = Data.Builder()
            .putLong(ReminderWorker.TASK_ID, taskId)
            .putLong(ReminderWorker.ORIGINAL_EPOCH_DAY, logicalDate.toEpochDay())
            .putInt(ReminderWorker.OFFSET_MINUTES, 0)
            .putReminderDeliveryClaim(taskClaim)
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()
        assertProductionMutationHonorsStateBoundary {
            app.taskRepository.completeOccurrence(taskId, logicalDate)
        }
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<ReminderWorker>(app).setInputData(taskInput).build().doWork(),
        )
        assertFalse(notifications.activeNotifications.any { it.id == taskId.hashCode() })

        val habitId = app.habitRepository.create(
            HabitDraft(
                name = "Linearized Habit",
                trackingMode = HabitTrackingMode.CheckOff,
                startDate = logicalDate,
                reminderMinutes = listOf(now.hour * 60 + now.minute),
            ),
        )
        val habitTrigger = logicalDate.atTime(now.hour, now.minute).atZone(ZoneOffset.UTC)
            .toInstant().toEpochMilli()
        val habitClaim = requireNotNull(
            app.currentHabitReminderDeliveryClaim(
                habitId,
                logicalDate,
                ReminderDeliveryKind.Scheduled,
                habitTrigger,
            ),
        )
        val habitInput = Data.Builder()
            .putLong(HabitReminderWorker.HABIT_ID, habitId)
            .putLong(HabitReminderWorker.LOGICAL_EPOCH_DAY, logicalDate.toEpochDay())
            .putReminderDeliveryClaim(habitClaim)
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()
        assertProductionMutationHonorsStateBoundary {
            app.habitRepository.setCheckOff(habitId, logicalDate, true)
        }
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<HabitReminderWorker>(app).setInputData(habitInput).build().doWork(),
        )
        assertFalse(
            notifications.activeNotifications.any {
                it.id == HabitReminderNotifications.notificationId(habitId)
            },
        )

        val goalId = app.goalRepository.create(
            GoalDraft(
                name = "Linearized Goal",
                type = GoalType.ReachValue,
                startDate = logicalDate,
                targetMin = 10.0,
                reminderMinutes = now.hour * 60 + now.minute,
            ),
        )
        val goalStored = requireNotNull(app.database.goalDao().getGoal(goalId))
        val goalTrigger = logicalDate.atTime(now.hour, now.minute).atZone(ZoneOffset.UTC)
            .toInstant().toEpochMilli()
        val goalClaim = ReminderDeliveryClaim(
            kind = ReminderDeliveryKind.Scheduled,
            stableEntityId = goalStored.uuid,
            logicalEpochDay = logicalDate.toEpochDay(),
            expectedTriggerAtMillis = goalTrigger,
            definitionFingerprint = goalReminderSemanticFingerprint(goalStored, ZoneOffset.UTC, null, null),
        )
        val goalInput = Data.Builder()
            .putLong(GoalReminderWorker.GOAL_ID, goalId)
            .putReminderDeliveryClaim(goalClaim)
            .putLong(USER_DATA_GENERATION_KEY, app.currentUserDataGeneration())
            .build()
        assertProductionMutationHonorsStateBoundary {
            app.goalRepository.setStatus(goalId, GoalStatus.Paused)
        }
        assertEquals(
            ListenableWorker.Result.success(),
            TestListenableWorkerBuilder<GoalReminderWorker>(app).setInputData(goalInput).build().doWork(),
        )
        assertFalse(
            notifications.activeNotifications.any {
                it.id == GoalReminderNotifications.notificationId(goalId)
            },
        )
    }

    @Test
    fun permanentDeletionDismissesVisibleTaskHabitAndGoalRemindersAtTheCommitBoundary() = runBlocking {
        val today = app.clock.now().atZone(ZoneOffset.UTC).toLocalDate()
        val taskId = app.taskRepository.create(
            TaskDraft(title = "Delete reminder task", scheduleKind = ScheduleKind.Once, date = today),
        )
        val habitId = app.habitRepository.create(HabitDraft(name = "Delete reminder habit", startDate = today))
        val goalId = app.goalRepository.create(
            GoalDraft(
                name = "Delete reminder goal",
                type = GoalType.ReachValue,
                startDate = today,
                targetMin = 1.0,
            ),
        )
        val manager = NotificationManagerCompat.from(app)
        manager.notify(
            taskId.hashCode(),
            NotificationCompat.Builder(app, ReminderNotifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Task reminder")
                .build(),
        )
        manager.notify(
            ReminderNotifications.completionUndoNotificationId(taskId),
            NotificationCompat.Builder(app, ReminderNotifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Task undo")
                .build(),
        )
        manager.notify(
            HabitReminderNotifications.notificationId(habitId),
            NotificationCompat.Builder(app, HabitReminderNotifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Habit reminder")
                .build(),
        )
        manager.notify(
            GoalReminderNotifications.notificationId(goalId),
            NotificationCompat.Builder(app, GoalReminderNotifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Goal reminder")
                .build(),
        )
        assertTrue(awaitActiveNotificationCount(4))

        app.taskDeletionCoordinator.delete(taskId)
        val habitDeletionImpact = requireNotNull(
            app.domainDeletionCoordinator.previewHabitDeletion(habitId),
        )
        app.domainDeletionCoordinator.deleteHabit(
            habitId,
            habitDeletionImpact.habitUuid,
            habitDeletionImpact.revisionToken,
        )
        app.domainDeletionCoordinator.deleteGoal(goalId)

        assertTrue(awaitActiveNotificationCount(0))
    }

    private suspend fun awaitActiveNotificationCount(expected: Int): Boolean =
        withTimeoutOrNull(5_000) {
            while (notifications.activeNotifications.size != expected) delay(10)
            true
        } == true

    @Test
    fun rejectedTaskDeletionKeepsTheValidNotificationAndClearsItsCleanupJournal() = runBlocking {
        val today = app.clock.now().atZone(ZoneOffset.UTC).toLocalDate()
        val taskId = app.taskRepository.create(
            TaskDraft(title = "Keep reminder task", scheduleKind = ScheduleKind.Once, date = today),
        )
        NotificationManagerCompat.from(app).notify(
            taskId.hashCode(),
            NotificationCompat.Builder(app, ReminderNotifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Still valid")
                .build(),
        )

        assertTrue(runCatching { app.taskDeletionCoordinator.delete(taskId, "unreviewed-revision") }.isFailure)

        assertTrue(app.database.taskDao().getTask(taskId) != null)
        assertTrue(notifications.activeNotifications.any { it.id == taskId.hashCode() })
        assertTrue(ReminderDeletionCleanupStore(app).pending().isEmpty())
    }

    @Test
    fun interruptedCommittedDeletionIsCleanedFromItsDurableJournal() = runBlocking {
        val today = app.clock.now().atZone(ZoneOffset.UTC).toLocalDate()
        val taskId = app.taskRepository.create(
            TaskDraft(title = "Interrupted delete", scheduleKind = ScheduleKind.Once, date = today),
        )
        NotificationManagerCompat.from(app).notify(
            taskId.hashCode(),
            NotificationCompat.Builder(app, ReminderNotifications.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Must be cleaned")
                .build(),
        )
        ReminderDeletionCleanupStore(app).prepare(ReminderDomain.Task, setOf(taskId))
        assertTrue(app.rawTaskRepository.deletePermanently(taskId))

        app.reconcilePendingReminderDeletions()

        assertFalse(notifications.activeNotifications.any { it.id == taskId.hashCode() })
        assertTrue(ReminderDeletionCleanupStore(app).pending().isEmpty())
    }

    private suspend fun assertProductionMutationHonorsStateBoundary(mutation: suspend () -> Unit) {
        val boundaryEntered = CompletableDeferred<Unit>()
        val releaseBoundary = CompletableDeferred<Unit>()
        val holder = kotlinx.coroutines.CoroutineScope(Dispatchers.Default).async {
            app.reminderDeliveryCoordinator.withStateBoundary {
                boundaryEntered.complete(Unit)
                releaseBoundary.await()
            }
        }
        boundaryEntered.await()
        val mutationStarted = CompletableDeferred<Unit>()
        val pendingMutation = kotlinx.coroutines.CoroutineScope(Dispatchers.Default).async {
            mutationStarted.complete(Unit)
            mutation()
        }
        mutationStarted.await()
        assertEquals(null, withTimeoutOrNull(100) { pendingMutation.await() })
        releaseBoundary.complete(Unit)
        holder.await()
        pendingMutation.await()
    }
}
