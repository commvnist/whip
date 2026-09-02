package com.whip.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.data.BackupPreview
import com.whip.app.data.BackupRepository
import com.whip.app.data.RestoreRecoveryManager
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreRecoveryManagerTest {
    @Test
    fun corruptRecoverySnapshotKeepsMarkerAndNeverRebuildsBackgroundState() = runBlocking {
        val repository = FakeBackupRepository("live")
        repository.previewChecksumValid = false
        val fileName = "restore-test-${UUID.randomUUID()}.json"
        val recoveryFile = File(
            ApplicationProvider.getApplicationContext<android.content.Context>().noBackupFilesDir,
            fileName,
        )
        recoveryFile.writeText("corrupt rollback snapshot")
        val manager = manager(repository, fileName)
        var rebuilds = 0

        try {
            val result = runCatching { manager.recoverIfNeeded { rebuilds++ } }

            assertTrue(result.isFailure)
            assertEquals("live", repository.state)
            assertEquals(0, rebuilds)
            assertTrue(manager.hasPendingRecovery())
        } finally {
            recoveryFile.delete()
        }
    }

    @Test
    fun injectedRecoveryRestoreFailureKeepsMarkerAndNeverRebuildsBackgroundState() = runBlocking {
        val repository = FakeBackupRepository("mixed")
        repository.failRestoreValue = "rollback"
        val fileName = "restore-test-${UUID.randomUUID()}.json"
        val recoveryFile = File(
            ApplicationProvider.getApplicationContext<android.content.Context>().noBackupFilesDir,
            fileName,
        )
        recoveryFile.writeText("rollback")
        val manager = manager(repository, fileName)
        var rebuilds = 0

        try {
            val result = runCatching { manager.recoverIfNeeded { rebuilds++ } }

            assertTrue(result.isFailure)
            assertEquals("mixed", repository.state)
            assertEquals(0, rebuilds)
            assertTrue(manager.hasPendingRecovery())
        } finally {
            recoveryFile.delete()
        }
    }

    @Test
    fun backgroundRebuildFailureRollsDatabaseAndSettingsBack() = runBlocking {
        val repository = FakeBackupRepository("old")
        val manager = manager(repository)
        var rebuilds = 0

        val result = runCatching {
            manager.restore("target") {
                rebuilds++
                if (rebuilds == 1) error("scheduler failure")
            }
        }

        assertTrue(result.isFailure)
        assertEquals("old", repository.state)
        assertEquals(2, rebuilds)
        assertFalse(manager.hasPendingRecovery())
    }

    @Test
    fun rollbackUsesThePrivateRecoverySnapshotRatherThanThePortableExport() = runBlocking {
        val repository = FakeBackupRepository("portable-old").apply {
            recoverySnapshotOverride = "private-old-with-local-journal"
        }
        val manager = manager(repository)
        var rebuilds = 0

        val result = runCatching {
            manager.restore("target") {
                rebuilds++
                if (rebuilds == 1) error("force rollback")
            }
        }

        assertTrue(result.isFailure)
        assertEquals("private-old-with-local-journal", repository.state)
        assertFalse(manager.hasPendingRecovery())
    }

    @Test
    fun failedRollbackLeavesMarkerAndNextLaunchRecoversOldState() = runBlocking {
        val repository = FakeBackupRepository("old")
        val fileName = "restore-test-${UUID.randomUUID()}.json"
        val manager = manager(repository, fileName)
        repository.failRestoreValue = "old"

        val result = runCatching { manager.restore("target") { error("rebuild failed") } }

        assertTrue(result.isFailure)
        assertEquals("target", repository.state)
        assertTrue(manager.hasPendingRecovery())

        repository.failRestoreValue = null
        val recreated = manager(repository, fileName)
        assertTrue(recreated.recoverIfNeeded { })
        assertEquals("old", repository.state)
        assertFalse(recreated.hasPendingRecovery())
    }

    @Test
    fun partialTargetApplyIsRolledBackBeforeItCanBecomeMixedLiveState() = runBlocking {
        val repository = FakeBackupRepository("old")
        val manager = manager(repository)
        repository.failAfterApplyingValue = "target"

        val result = runCatching { manager.restore("target") { } }

        assertTrue(result.isFailure)
        assertEquals("old", repository.state)
        assertFalse(manager.hasPendingRecovery())
    }

    @Test
    fun externalActionGenerationAdvancesOnlyAfterRecoverySnapshotIsDurable() = runBlocking {
        val repository = FakeBackupRepository("old")
        val fileName = "restore-test-${UUID.randomUUID()}.json"
        val recoveryFile = File(
            ApplicationProvider.getApplicationContext<android.content.Context>().noBackupFilesDir,
            fileName,
        )
        val manager = manager(repository, fileName)
        var markerWasDurableBeforeGenerationChange = false

        manager.restore(
            targetJson = "target",
            onRecoveryPrepared = {
                markerWasDurableBeforeGenerationChange = recoveryFile.exists() && recoveryFile.length() > 0L
            },
        ) { }

        assertTrue(markerWasDurableBeforeGenerationChange)
        assertEquals("target", repository.state)
        assertFalse(manager.hasPendingRecovery())
    }

    @Test
    fun recoveryPreparationFailureRollsBackAndRebuildsBeforeRemovingMarker() = runBlocking {
        val repository = FakeBackupRepository("old")
        val manager = manager(repository)
        var rebuilds = 0

        val result = runCatching {
            manager.restore(
                targetJson = "target",
                onRecoveryPrepared = { error("generation persistence failed") },
            ) { rebuilds++ }
        }

        assertTrue(result.isFailure)
        assertEquals("old", repository.state)
        assertEquals(1, rebuilds)
        assertFalse(manager.hasPendingRecovery())
    }

    private fun manager(repository: BackupRepository, name: String = "restore-test-${UUID.randomUUID()}.json") =
        RestoreRecoveryManager(ApplicationProvider.getApplicationContext(), repository, name)

    private class FakeBackupRepository(initial: String) : BackupRepository {
        var state = initial
        var failRestoreValue: String? = null
        var failAfterApplyingValue: String? = null
        var previewChecksumValid = true
        var recoverySnapshotOverride: String? = null

        override suspend fun exportBackup() = state
        override suspend fun exportRecoveryBackup() = recoverySnapshotOverride ?: state
        override suspend fun previewBackup(json: String) = BackupPreview(
            envelopeVersion = 2,
            databaseVersion = 21,
            exportedAt = Instant.EPOCH,
            tableCounts = emptyMap(),
            totalRecords = 1,
            duplicateStableIds = 0,
            checksumValid = previewChecksumValid,
            settingsIncluded = true,
        )
        override suspend fun restoreBackup(json: String) {
            if (json == failRestoreValue) error("injected restore failure")
            state = json
            if (json == failAfterApplyingValue) error("injected failure after partial target apply")
        }
        override suspend fun mergeBackup(json: String) = com.whip.app.data.BackupMergeSummary(0, 0)
        override suspend fun exportTasksCsv() = ""
        override suspend fun exportHabitsCsv() = ""
        override suspend fun exportGoalsCsv() = ""
        override suspend fun exportTracksCsv() = ""
        override suspend fun exportGymCsv() = ""
        override suspend fun deleteAllData() = Unit
    }
}
