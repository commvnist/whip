package com.whip.app.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.WhipApplication
import com.whip.app.core.AppSettings
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.AreaScope
import com.whip.app.domain.TaskDraft
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
class AreaMutationViewModelTest {
    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetData() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true) }
    }

    @Test
    fun rejectedAreaMutationReturnsToItsExactOwnerWithoutChangingTheArea() = runBlocking {
        val mainId = app.areaRepository.ensureDefaultArea()
        val viewModel = SettingsViewModel(app)
        val requestId = "area-test:archive-only-area"

        assertTrue(viewModel.setAreaArchivedMutation(requestId, mainId, true))
        val finished = viewModel.awaitAreaMutation(requestId)

        assertTrue(finished.result is WhipResult.Failure)
        assertFalse(app.areaRepository.areas.first().single { it.id == mainId }.archived)
        viewModel.consumeAreaMutation(requestId)
        assertEquals(PersistenceRequestState.Idle, viewModel.areaMutationState.value)
    }

    @Test
    fun successfulMergeReturnsOneReceiptAndMovesTheSavedAreaScope() = runBlocking {
        val mainId = app.areaRepository.ensureDefaultArea()
        val sourceId = app.areaRepository.create("Project Area")
        val taskId = app.taskRepository.create(
            TaskDraft(title = "Move with Area", areaId = sourceId, area = "Project Area"),
        )
        app.settingsRepository.update {
            it.copy(
                activeAreaScope = AreaScope.One(sourceId).storageKey,
                chosenOpeningAreaScope = AreaScope.One(sourceId).storageKey,
            )
        }
        val viewModel = SettingsViewModel(app)
        val requestId = "area-test:merge"

        assertTrue(viewModel.mergeAreasMutation(requestId, sourceId, mainId))
        val finished = viewModel.awaitAreaMutation(requestId)
        val receipt = (finished.result as WhipResult.Success).value

        assertEquals(AreaMutationKind.Merge, receipt.kind)
        assertEquals(sourceId, receipt.areaId)
        assertEquals(mainId, receipt.relatedAreaId)
        assertTrue(receipt.warnings.isEmpty())
        assertEquals(mainId, app.taskRepository.tasks.first().single { it.id == taskId }.areaId)
        assertEquals(AreaScope.One(mainId).storageKey, app.settingsRepository.current().activeAreaScope)
        assertEquals(AreaScope.One(mainId).storageKey, app.settingsRepository.current().chosenOpeningAreaScope)
        assertFalse(app.areaRepository.areas.first().any { it.id == sourceId })
    }

    @Test
    fun deleteKeepingItemsUsesRepositoryTruthWithoutACollectedUiProjection() = runBlocking {
        val mainId = app.areaRepository.ensureDefaultArea()
        val sourceId = app.areaRepository.create("Delete Source")
        val taskId = app.taskRepository.create(
            TaskDraft(title = "Keep this history", areaId = sourceId, area = "Delete Source"),
        )
        val viewModel = SettingsViewModel(app)
        val requestId = "area-test:delete-keeping-items"

        assertTrue(viewModel.deleteAreaKeepingItemsMutation(requestId, sourceId, mainId))
        val finished = viewModel.awaitAreaMutation(requestId)
        val receipt = (finished.result as WhipResult.Success).value

        assertEquals(AreaMutationKind.DeleteKeepingItems, receipt.kind)
        assertEquals(mainId, app.taskRepository.tasks.first().single { it.id == taskId }.areaId)
        assertFalse(app.areaRepository.areas.first().any { it.id == sourceId })
    }
}

private suspend fun SettingsViewModel.awaitAreaMutation(
    requestId: String,
): PersistenceRequestState.Finished<AreaMutationReceipt> = withTimeout(5_000) {
    areaMutationState.filter { state ->
        state is PersistenceRequestState.Finished && state.requestId == requestId
    }.first() as PersistenceRequestState.Finished<AreaMutationReceipt>
}
