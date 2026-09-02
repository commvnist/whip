package com.whip.app.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.WhipApplication
import com.whip.app.core.AppSettings
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import java.time.LocalDate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagMutationViewModelTest {
    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetData() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true) }
    }

    @Test
    fun rejectedRenameReturnsToItsExactOwnerWithoutChangingTags() = runBlocking {
        val existingId = app.measurementRepository.ensureTag("Existing")
        val viewModel = SettingsViewModel(app)
        val requestId = "tag-test:missing-rename"

        assertTrue(viewModel.renameTagMutation(requestId, "missing", "Reviewed"))
        val finished = viewModel.awaitTagMutation(requestId)

        assertTrue(finished.result is WhipResult.Failure)
        assertEquals(listOf(existingId), app.measurementRepository.tags.first().map { it.id })
        viewModel.consumeTagMutation(requestId)
        assertEquals(PersistenceRequestState.Idle, viewModel.tagMutationState.value)
    }

    @Test
    fun mergePublishesTheExactSourceAndDestination() = runBlocking {
        val sourceId = app.measurementRepository.ensureTag("Source")
        val targetId = app.measurementRepository.ensureTag("Target")
        val viewModel = SettingsViewModel(app)
        val requestId = "tag-test:merge"

        assertTrue(viewModel.mergeTagsMutation(requestId, sourceId, targetId))
        val receipt = (viewModel.awaitTagMutation(requestId).result as WhipResult.Success).value

        assertEquals(TagMutationKind.Merge, receipt.kind)
        assertEquals(sourceId, receipt.tagId)
        assertEquals(targetId, receipt.relatedTagId)
        assertEquals(listOf(targetId), app.measurementRepository.tags.first().map { it.id })
    }

    @Test
    fun onlyOneTagMutationCanBeAdmittedAtATime() = runBlocking {
        val tagId = app.measurementRepository.ensureTag("One")
        val viewModel = SettingsViewModel(app)

        assertTrue(viewModel.setTagArchivedMutation("tag-test:first", tagId, true))
        assertFalse(viewModel.setTagArchivedMutation("tag-test:second", tagId, false))
        val receipt = (viewModel.awaitTagMutation("tag-test:first").result as WhipResult.Success).value

        assertEquals(TagMutationKind.Archive, receipt.kind)
        assertTrue(app.measurementRepository.tags.first().single().archived)
    }

    @Test
    fun settingsCountsTagUsageAcrossEverySupportedProductArea() = runBlocking {
        val tagId = app.measurementRepository.ensureTag("Focus")
        app.taskRepository.create(TaskDraft(title = "Task", tags = setOf("focus")))
        app.habitRepository.create(
            HabitDraft(name = "Habit", tags = listOf("Focus"), startDate = LocalDate.of(2026, 9, 2)),
        )
        app.goalRepository.create(
            GoalDraft(
                name = "Goal",
                type = GoalType.OpenEndedTrend,
                tags = listOf("FOCUS"),
                startDate = LocalDate.of(2026, 9, 2),
            ),
        )
        app.trackRepository.create(
            TrackDraft(
                name = "Track",
                tags = listOf("Focus"),
                fields = listOf(TrackFieldDraft("Value", TrackFieldType.ShortText, primary = true)),
            ),
        )

        val state = withTimeout(5_000) {
            SettingsViewModel(app).uiState.filter { it.taxonomyLoaded && it.tagUsage[tagId]?.total == 4 }.first()
        }

        assertEquals(TagUsageCounts(tasks = 1, habits = 1, goals = 1, tracks = 1), state.tagUsage[tagId])
    }
}

private suspend fun SettingsViewModel.awaitTagMutation(
    requestId: String,
): PersistenceRequestState.Finished<TagMutationReceipt> = withTimeout(5_000) {
    tagMutationState.filter { state ->
        state is PersistenceRequestState.Finished && state.requestId == requestId
    }.first() as PersistenceRequestState.Finished<TagMutationReceipt>
}
