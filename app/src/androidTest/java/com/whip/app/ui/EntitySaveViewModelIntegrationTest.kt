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
