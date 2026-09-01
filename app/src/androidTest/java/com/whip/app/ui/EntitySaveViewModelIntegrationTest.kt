package com.whip.app.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.WhipApplication
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.OperationStatus
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.TaskDraft
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntitySaveViewModelIntegrationTest {
    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() = runBlocking { app.backupRepository.deleteAllData() }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun authoritativeTaskCommitSurvivesTagFollowUpFailureWithoutDuplicateRetryState() = runBlocking {
        val viewModel = TaskViewModel(app)
        val accepted = viewModel.saveTask(
            taskId = null,
            draft = TaskDraft(title = "Committed once", tags = setOf(" ")),
            requestId = "warning-request",
        )

        assertTrue(accepted)
        @Suppress("UNCHECKED_CAST")
        val finished = withTimeout(5_000) {
            viewModel.editorSaveState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<EntitySaveReceipt>
        val success = finished.result as WhipResult.Success
        assertEquals(1, success.value.warnings.size)
        assertTrue(success.value.warnings.single().contains("tag suggestions"))
        assertEquals(1, app.taskRepository.tasks.first().count { it.title == "Committed once" })
        assertTrue(viewModel.operationFeedback.value.status is OperationStatus.Succeeded)
    }

    @Test
    fun repositoryFailureProducesOwnedRetryableFailureAndNoEntity() = runBlocking {
        val viewModel = TaskViewModel(app)
        val accepted = viewModel.saveTask(
            taskId = Long.MAX_VALUE,
            draft = TaskDraft(title = "Must not appear"),
            requestId = "failed-request",
        )

        assertTrue(accepted)
        @Suppress("UNCHECKED_CAST")
        val finished = withTimeout(5_000) {
            viewModel.editorSaveState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<EntitySaveReceipt>
        assertTrue(finished.result is WhipResult.Failure)
        assertTrue(app.taskRepository.tasks.first().none { it.title == "Must not appear" })
        assertEquals(OperationStatus.Idle, viewModel.operationFeedback.value.status)
    }

    @Test
    fun habitRepositoryFailureProducesOwnedRetryableFailureAndNoEntity() = runBlocking {
        val viewModel = HabitViewModel(app)
        val accepted = viewModel.saveHabit(
            id = Long.MAX_VALUE,
            draft = HabitDraft(name = "Must not appear", startDate = LocalDate.of(2026, 8, 31)),
            requestId = "failed-habit-request",
        )

        assertTrue(accepted)
        @Suppress("UNCHECKED_CAST")
        val finished = withTimeout(5_000) {
            viewModel.editorSaveState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<EntitySaveReceipt>
        assertTrue(finished.result is WhipResult.Failure)
        assertTrue(app.habitRepository.habits.first().none { it.name == "Must not appear" })
        assertEquals(OperationStatus.Idle, viewModel.operationStatus.value)
    }

    @Test
    fun habitHistoryRequestAdmitsExactlyOneMutationAndReturnsItsTypedReceipt() = runBlocking {
        val habitId = app.habitRepository.create(
            HabitDraft(
                name = "Owned check-in",
                trackingMode = HabitTrackingMode.Count,
                startDate = LocalDate.of(2026, 8, 31),
            ),
        )
        val viewModel = HabitViewModel(app)

        assertTrue(viewModel.log(habitId, 3.0, requestId = "history-one"))
        assertEquals(false, viewModel.log(habitId, 9.0, requestId = "history-two"))
        @Suppress("UNCHECKED_CAST")
        val finished = withTimeout(5_000) {
            viewModel.authoredMutationState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<HabitMutationReceipt>
        val success = finished.result as WhipResult.Success

        assertEquals("history-one", finished.requestId)
        assertEquals(HabitMutationKind.LogCreated, success.value.kind)
        assertEquals(habitId, success.value.habitId)
        assertTrue(success.value.logId != null)
        assertEquals(listOf(3.0), app.habitRepository.logs.first().mapNotNull { it.value })
        assertTrue(viewModel.operationStatus.value is OperationStatus.Succeeded)
    }

    @Test
    fun missingHabitHistoryTargetReturnsAnOwnedFailureWithoutGlobalErrorNoise() = runBlocking {
        val viewModel = HabitViewModel(app)
        assertTrue(
            viewModel.updateLog(
                logId = Long.MAX_VALUE,
                value = 1.0,
                status = HabitLogStatus.Recorded,
                date = LocalDate.of(2026, 8, 31),
                note = "retained draft",
                requestId = "missing-log",
            ),
        )

        @Suppress("UNCHECKED_CAST")
        val finished = withTimeout(5_000) {
            viewModel.authoredMutationState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<HabitMutationReceipt>
        assertTrue(finished.result is WhipResult.Failure)
        assertEquals(OperationStatus.Idle, viewModel.operationStatus.value)
    }

    @Test
    fun historicalSkipUndoAdmitsOneTapAndReportsARepeatedMissingTargetAsOwnedFailure() = runBlocking {
        val date = LocalDate.of(2026, 8, 31)
        val habitId = app.habitRepository.create(HabitDraft(name = "Training", startDate = date))
        app.habitRepository.skipDay(habitId, date)
        val viewModel = HabitViewModel(app)

        assertTrue(viewModel.undoSkip(habitId, date, requestId = "skip-undo-one"))
        assertEquals(false, viewModel.undoSkip(habitId, date, requestId = "skip-undo-two"))
        @Suppress("UNCHECKED_CAST")
        val success = withTimeout(5_000) {
            viewModel.authoredMutationState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<HabitMutationReceipt>
        assertEquals(HabitMutationKind.SkipDeleted, (success.result as WhipResult.Success).value.kind)
        assertTrue(app.habitRepository.skips.first().isEmpty())

        viewModel.consumeAuthoredMutationResult(success.requestId)
        assertTrue(viewModel.undoSkip(habitId, date, requestId = "skip-undo-missing"))
        @Suppress("UNCHECKED_CAST")
        val missing = withTimeout(5_000) {
            viewModel.authoredMutationState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<HabitMutationReceipt>
        assertTrue(missing.result is WhipResult.Failure)
        assertEquals(OperationStatus.Idle, viewModel.operationStatus.value)
    }

    @Test
    fun goalRepositoryFailureProducesOwnedRetryableFailureAndNoEntity() = runBlocking {
        val viewModel = GoalViewModel(app)
        val accepted = viewModel.saveGoal(
            id = Long.MAX_VALUE,
            draft = GoalDraft(
                name = "Must not appear",
                type = GoalType.ReachValue,
                targetMin = 10.0,
                startDate = LocalDate.of(2026, 8, 31),
            ),
            requestId = "failed-goal-request",
        )

        assertTrue(accepted)
        @Suppress("UNCHECKED_CAST")
        val finished = withTimeout(5_000) {
            viewModel.editorSaveState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<EntitySaveReceipt>
        assertTrue(finished.result is WhipResult.Failure)
        assertTrue(app.goalRepository.goals.first().none { it.name == "Must not appear" })
        assertEquals(OperationStatus.Idle, viewModel.operationStatus.value)
    }
}
