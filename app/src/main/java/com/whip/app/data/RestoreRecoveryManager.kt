package com.whip.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File

/**
 * Keeps the pre-restore state in app-private storage until database, settings,
 * and background reconstruction have all succeeded. A process death at any
 * point causes the next launch to restore the old, internally consistent state.
 */
class RestoreRecoveryManager(
    context: Context,
    private val backups: BackupRepository,
    fileName: String = RECOVERY_FILE_NAME,
) {
    private val recoveryFile = AtomicFile(File(context.noBackupFilesDir, fileName))

    suspend fun restore(
        targetJson: String,
        onRecoveryPrepared: suspend () -> Unit = {},
        rebuildBackgroundState: suspend () -> Unit,
    ) {
        require(!recoveryFile.baseFile.exists()) { "A previous restore still needs recovery" }
        val rollbackJson = backups.exportRecoveryBackup()
        writeRecovery(rollbackJson)
        try {
            // Anything that invalidates old external actions must happen only
            // after the rollback snapshot is durable, but before target data
            // can replace live data.
            onRecoveryPrepared()
            backups.restoreBackup(targetJson)
            rebuildBackgroundState()
            recoveryFile.delete()
        } catch (restoreError: Throwable) {
            val rollback = runCatching {
                backups.restoreBackup(rollbackJson)
                rebuildBackgroundState()
                recoveryFile.delete()
            }
            rollback.exceptionOrNull()?.let(restoreError::addSuppressed)
            throw restoreError
        }
    }

    suspend fun recoverIfNeeded(rebuildBackgroundState: suspend () -> Unit): Boolean {
        if (!recoveryFile.baseFile.exists()) return false
        val rollbackJson = recoveryFile.readFully().toString(Charsets.UTF_8)
        val preview = backups.previewBackup(rollbackJson)
        require(preview.checksumValid) { "Restore recovery snapshot is corrupt" }
        backups.restoreBackup(rollbackJson)
        rebuildBackgroundState()
        recoveryFile.delete()
        return true
    }

    fun hasPendingRecovery(): Boolean = recoveryFile.baseFile.exists()

    private fun writeRecovery(json: String) {
        val output = recoveryFile.startWrite()
        try {
            output.write(json.toByteArray(Charsets.UTF_8))
            output.fd.sync()
            recoveryFile.finishWrite(output)
        } catch (error: Throwable) {
            recoveryFile.failWrite(output)
            throw error
        }
    }

    private companion object {
        const val RECOVERY_FILE_NAME = "restore-recovery.whip.json"
    }
}
