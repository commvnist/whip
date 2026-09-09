package com.whip.app.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.TrackDefinitionBoundary
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackValueDraft
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackEditorCheckpointTest {
    @get:Rule val temporary = TemporaryFolder()
    private val app by lazy {
        object : Application() {
            override fun getNoBackupFilesDir(): File = temporary.root
        }
    }
    private val directory get() = File(temporary.root, "track-editor-checkpoints")
    private fun owner(handle: SavedStateHandle = SavedStateHandle()) = TrackEditorSessionViewModel(app, handle)

    @Test fun newOwnerRecoversExactDefinitionBoundaryAndAuthoredChoices() {
        val first = owner()
        val initial = TrackDraft(name = "Walking archive", fields = emptyList())
        val boundary = TrackDefinitionBoundary(7, "track-seven", 2, "opening-revision")
        first.routeState.value = TrackEditorRoute.Definition(7, initial, initial.name, boundary, 5, 11)
        first.definition.initialize("track-7-11-g5", initial, 5)
        first.definition.installOpeningBoundary(boundary)
        first.definition.updateDraft { it.copy(name = "Changed archive", description = "Keep this unsaved note") }
        first.definition.updateOptionReplacement(41, 42)
        val reference = first.saveCheckpoint()
        val second = owner(SavedStateHandle(mapOf(TrackEditorSessionViewModel.STATE_KEY to reference)))
        assertEquals(first.routeState.value, second.routeState.value)
        assertEquals(first.definition.state.value, second.definition.state.value)
        assertNull(second.recoveryProblem)
    }

    @Test fun checkpointKeepsLongTextInvalidNumberAndEffectiveDateExactly() {
        val first = entryOwner()
        first.entry.updateNumberValue("number", "12..4", null)
        val reference = first.saveCheckpoint()
        val second = owner(SavedStateHandle(mapOf(TrackEditorSessionViewModel.STATE_KEY to reference)))
        assertEquals(first.routeState.value, second.routeState.value)
        assertEquals(first.entry.state.value, second.entry.state.value)
        assertEquals("12..4", second.entry.state.value.rawNumberValues["number"])
        assertNull(second.recoveryProblem)
    }

    @Test fun completionClearsBothHoldersAndOnlyItsOwnFiles() {
        val first = entryOwner()
        val other = entryOwner()
        first.saveCheckpoint()
        val otherReference = other.saveCheckpoint()
        first.routeState.value = null
        assertNull(first.entry.state.value.draft)
        assertNull(first.definition.state.value.draft)
        assertTrue(first.saveCheckpoint().isEmpty)
        assertEquals(1, directory.listFiles()?.size)
        val recoveredOther = owner(SavedStateHandle(mapOf(TrackEditorSessionViewModel.STATE_KEY to otherReference)))
        assertEquals(other.entry.state.value, recoveredOther.entry.state.value)
    }

    @Test fun unchangedSavesReuseFilesAndFreshRestorePrunesOnlyOlderCheckpoints() {
        val first = entryOwner()
        val oldReference = first.saveCheckpoint()
        first.saveCheckpoint()
        val ownerDirectory = directory.listFiles()!!.single()
        assertEquals(1, ownerDirectory.listFiles()?.size)
        first.entry.updateNumberValue("number", "4", null)
        val latest = first.saveCheckpoint()
        assertEquals(2, ownerDirectory.listFiles()?.size)
        assertEquals("", TrackEditorCheckpointStore(directory).read(oldReference).entry.rawNumberValues["number"].orEmpty())
        val second = owner(SavedStateHandle(mapOf(TrackEditorSessionViewModel.STATE_KEY to latest)))
        assertEquals(first.entry.state.value, second.entry.state.value)
        assertEquals(1, ownerDirectory.listFiles()?.size)
    }

    @Test fun corruptCheckpointShowsRecoveryFailureAndCanBeRetriedWithoutSubstitution() {
        val first = entryOwner()
        val reference = first.saveCheckpoint()
        val file = directory.walkTopDown().single { it.isFile }
        val original = file.readBytes()
        file.writeText("incomplete checkpoint")
        val second = owner(SavedStateHandle(mapOf(TrackEditorSessionViewModel.STATE_KEY to reference)))
        assertNull(second.routeState.value)
        assertNull(second.entry.state.value.draft)
        assertNotNull(second.recoveryProblem)
        file.writeBytes(original)
        second.retryRecovery()
        assertEquals(first.entry.state.value, second.entry.state.value)
        assertNull(second.recoveryProblem)
    }

    @Test fun writeFailurePreservesLiveDraftAndCannotRecoverAnOlderValueAsCurrent() {
        val first = entryOwner()
        first.saveCheckpoint()
        first.entry.updateNumberValue("number", "9..8", null)
        directory.deleteRecursively()
        directory.writeText("storage unavailable")
        val failed = first.saveCheckpoint()
        assertNotNull(first.recoveryProblem)
        assertEquals("9..8", first.entry.state.value.rawNumberValues["number"])
        val second = owner(SavedStateHandle(mapOf(TrackEditorSessionViewModel.STATE_KEY to failed)))
        assertNotNull(second.recoveryProblem)
        assertNull(second.routeState.value)
        assertFalse(second.canRetryRecovery)
        assertFalse(second.recoveryProblem.orEmpty().contains("Retry", ignoreCase = true))
        directory.delete()
        first.retryRecovery()
        assertNull(first.recoveryProblem)
        assertFalse(first.saveCheckpoint().isEmpty)
    }

    @Test fun missingOrIncompatibleCheckpointCannotSilentlyBecomeANewEntry() {
        val first = entryOwner()
        val reference = first.saveCheckpoint()
        val futureVersion = android.os.Bundle(reference).apply { putInt("version", 2) }
        val incompatible = owner(SavedStateHandle(mapOf(TrackEditorSessionViewModel.STATE_KEY to futureVersion)))
        assertNotNull(incompatible.recoveryProblem)
        assertNull(incompatible.routeState.value)
        directory.deleteRecursively()
        val missing = owner(SavedStateHandle(mapOf(TrackEditorSessionViewModel.STATE_KEY to reference)))
        assertNotNull(missing.recoveryProblem)
        assertNull(missing.entry.state.value.draft)
        missing.dismissRecoveryProblem()
        assertTrue(missing.saveCheckpoint().isEmpty)
    }

    private fun entryOwner(): TrackEditorSessionViewModel = owner().also {
        it.routeState.value = TrackEditorRoute.Entry(7, 41, openingDataGeneration = 5, sessionId = 12)
        it.entry.initialize("entry-7-41-12-g5", TrackEntryDraft(
            entryDate = LocalDate.of(2026, 8, 1),
            values = mapOf("long" to TrackValueDraft(textValue = "Walking note 🧭 ".repeat(40_000))),
        ), 5)
    }
}
