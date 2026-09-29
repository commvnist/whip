package com.whip.app

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.data.EncryptedBackupCodec
import com.whip.app.domain.TaskDraft
import com.whip.app.core.OperationStatus
import com.whip.app.ui.SettingsViewModel
import java.io.File
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BackupSelectionRecoveryTest {
    @Test fun invalidSelectionClearsPriorPlainAndEncryptedCandidatesWithoutChangingData() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val taskId = app.taskRepository.create(TaskDraft("Keep this task"))
        val plain = app.backupRepository.exportBackup()
        val selected = File.createTempFile("backup-selection-", ".json", app.cacheDir)
        val invalid = File.createTempFile("invalid-selection-", ".json", app.cacheDir).apply { writeText("{}") }
        val viewModel = SettingsViewModel(app)
        val store = ViewModelStore().apply { put("settings", viewModel) }
        val collector = launch { viewModel.uiState.collect() }
        try {
            for (encrypted in listOf(false, true)) {
                selected.writeText(if (encrypted) EncryptedBackupCodec.encrypt(plain, "valid password".toCharArray()) else plain)
                viewModel.previewRestore(Uri.fromFile(selected))
                withTimeout(10_000) {
                    viewModel.uiState.first { if (encrypted) it.encryptedRestorePending else it.backupPreview != null }
                }
                viewModel.previewRestore(Uri.fromFile(invalid))
                val failed = withTimeout(10_000) {
                    viewModel.uiState.first { it.operation is OperationStatus.Failed }
                }
                assertNull(failed.backupPreview)
                assertFalse(failed.encryptedRestorePending)
                org.junit.Assert.assertNotNull(app.taskRepository.getTask(taskId))
            }
        } finally {
            collector.cancelAndJoin()
            InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
            selected.delete()
            invalid.delete()
            app.backupRepository.deleteAllData()
        }
    }
}
