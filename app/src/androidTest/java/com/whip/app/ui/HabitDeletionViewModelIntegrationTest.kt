package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.WhipApplication
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.HabitDraft
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDeletionViewModelIntegrationTest {
    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() = runBlocking { app.backupRepository.deleteAllData() }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun reviewedCandidateRestoresAndAStaleCommitMustRefreshBeforeCompletion() = runBlocking {
        val habitId = app.habitRepository.create(
            HabitDraft(name = "Lifecycle review", startDate = LocalDate.of(2026, 9, 3)),
        )
        val habit = requireNotNull(app.habitRepository.get(habitId))
        val handle = SavedStateHandle()
        val openingViewModel = HabitViewModel(app, handle)
        openingViewModel.preparePermanentDeletion(habit.id, habit.uuid, habit.name)
        val openingReview = awaitReadyReview(openingViewModel)

        val restoredViewModel = HabitViewModel(app, handle)
        val restoredReview = restoredViewModel.habitDeletionReviewState.value
        assertEquals(openingReview.requestNamespace, restoredReview.requestNamespace)
        assertEquals(openingReview.impact?.revisionToken, restoredReview.impact?.revisionToken)
        assertEquals(habit.uuid, restoredReview.habitUuid)

        app.habitRepository.log(habitId, 1.0)
        val staleRequestId = "${restoredReview.requestNamespace}:stale-attempt"
        assertTrue(
            restoredViewModel.deletePermanently(
                habitId,
                requireNotNull(restoredReview.impact).revisionToken,
                staleRequestId,
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val stale = withTimeout(5_000) {
            restoredViewModel.habitDeletionRequestState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<HabitDeletionReceipt>
        assertTrue(stale.result is WhipResult.Failure)
        assertTrue(restoredViewModel.habitDeletionReviewState.value.reviewRequired)
        assertFalse(restoredViewModel.habitDeletionReviewState.value.targetMissing)
        assertTrue(restoredViewModel.habitDeletionReviewState.value.impact != null)
        assertTrue(app.habitRepository.get(habitId) != null)

        restoredViewModel.consumeHabitDeletionResult(stale.requestId)
        restoredViewModel.refreshPermanentDeletionReview()
        val refreshed = awaitReadyReview(restoredViewModel)
        assertTrue(refreshed.impact?.revisionToken != openingReview.impact?.revisionToken)
        val commitRequestId = "${refreshed.requestNamespace}:committed-attempt"
        assertTrue(
            restoredViewModel.deletePermanently(
                habitId,
                requireNotNull(refreshed.impact).revisionToken,
                commitRequestId,
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val committed = withTimeout(5_000) {
            restoredViewModel.habitDeletionRequestState.first { it is PersistenceRequestState.Finished }
        } as PersistenceRequestState.Finished<HabitDeletionReceipt>
        val receipt = (committed.result as WhipResult.Success).value
        assertTrue(receipt.summary.habitDeleted)
        assertEquals(1, receipt.summary.logsDeleted)
        assertNull(app.habitRepository.get(habitId))

        restoredViewModel.consumeHabitDeletionResult(committed.requestId)
        assertNull(restoredViewModel.habitDeletionReviewState.value.habitId)
        assertTrue(restoredViewModel.habitDeletionRequestState.value is PersistenceRequestState.Idle)
    }

    @Test
    fun missingOrReusedCandidateIdentityNeverBecomesConfirmable() = runBlocking {
        val habitId = app.habitRepository.create(
            HabitDraft(name = "Identity owner", startDate = LocalDate.of(2026, 9, 3)),
        )
        val viewModel = HabitViewModel(app, SavedStateHandle())

        viewModel.preparePermanentDeletion(habitId, "not-the-current-habit", "Old identity")
        val state = withTimeout(5_000) {
            viewModel.habitDeletionReviewState.first { !it.preparing && it.errorMessage != null }
        }

        assertTrue(state.targetMissing)
        assertTrue(state.reviewRequired)
        assertNull(state.impact)
        assertFalse(
            viewModel.deletePermanently(
                habitId,
                expectedRevisionToken = "unreviewed",
                requestId = "${state.requestNamespace}:blocked",
            ),
        )
        assertTrue(app.habitRepository.get(habitId) != null)
    }

    private suspend fun awaitReadyReview(viewModel: HabitViewModel): HabitDeletionReviewUiState =
        withTimeout(5_000) {
            viewModel.habitDeletionReviewState.first {
                !it.preparing && it.impact != null && !it.reviewRequired
            }
        }
}
