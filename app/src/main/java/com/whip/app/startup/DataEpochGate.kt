package com.whip.app.startup

import android.content.Context
import android.util.AtomicFile
import java.io.File

const val CURRENT_DATA_EPOCH = 3

sealed interface DataEpochState {
    data class Current(val epoch: Int) : DataEpochState
    data object ResetRequired : DataEpochState
    data class ResetInProgress(val epoch: Int) : DataEpochState
}

/** Pure policy kept separate from Android storage so the fail-closed rules have JVM coverage. */
object DataEpochPolicy {
    fun resolve(marker: String?, hasAppOwnedState: Boolean): DataEpochState {
        val parsed = marker?.trim()?.let(::decode)
        return when {
            parsed == DataEpochState.Current(CURRENT_DATA_EPOCH) -> parsed
            parsed == DataEpochState.ResetInProgress(CURRENT_DATA_EPOCH) -> parsed
            marker != null -> DataEpochState.ResetRequired
            hasAppOwnedState -> DataEpochState.ResetRequired
            else -> DataEpochState.Current(CURRENT_DATA_EPOCH)
        }
    }

    fun encode(state: DataEpochState): String = when (state) {
        is DataEpochState.Current -> "current:${state.epoch}"
        DataEpochState.ResetRequired -> "reset-required"
        is DataEpochState.ResetInProgress -> "reset-in-progress:${state.epoch}"
    }

    fun hasMeaningfulWhipState(
        databaseFileNames: Set<String>,
        sharedPreferenceNames: Set<String>,
        noBackupFileNames: Set<String>,
    ): Boolean = databaseFileNames.any(::isWhipDatabaseFile) ||
        selectWhipPreferenceNames(sharedPreferenceNames).isNotEmpty() ||
        noBackupFileNames.any(::isWhipRecoveryFile)

    fun selectWhipPreferenceNames(existingNames: Set<String>): Set<String> =
        existingNames.intersect(WHIP_OWNED_PREFERENCES)

    private fun decode(value: String): DataEpochState? = when {
        value == "reset-required" -> DataEpochState.ResetRequired
        value.startsWith("current:") -> value.substringAfter(':').toIntOrNull()?.let(DataEpochState::Current)
        value.startsWith("reset-in-progress:") ->
            value.substringAfter(':').toIntOrNull()?.let(DataEpochState::ResetInProgress)
        else -> null
    }

    private fun isWhipDatabaseFile(name: String): Boolean =
        name == "whip.db" || name in setOf("whip.db-wal", "whip.db-shm", "whip.db-journal")

    private fun isWhipRecoveryFile(name: String): Boolean =
        name == "restore-recovery.whip.json" ||
            name == "restore-recovery.whip.json.bak" ||
            name == "restore-recovery.whip.json.new"

    private val WHIP_OWNED_PREFERENCES = setOf(
        "whip-settings",
        "portable_backups",
        "whip_recovery_runtime",
        "whip_widget_areas",
        "whip_widget_snapshots",
        "whip_reminder_runtime",
        "notification_action_receipts",
        "reminder-deletion-cleanup",
        "automation_prompt_scheduler",
    )
}

/**
 * The only persistent state allowed to be read before the epoch decision. The marker lives in
 * no-backup storage so an OS restore cannot make old app data look current.
 */
class DataEpochGate(
    context: Context,
    fileName: String = MARKER_FILE_NAME,
    private val hasAppOwnedState: () -> Boolean = { context.hasPreEpochAppOwnedState(fileName) },
) {
    private val marker = AtomicFile(File(context.noBackupFilesDir, fileName))

    fun evaluate(): DataEpochState {
        val encoded = if (marker.baseFile.exists()) {
            runCatching { marker.readFully().toString(Charsets.UTF_8) }.getOrNull()
                ?: return DataEpochState.ResetRequired
        } else {
            null
        }
        val resolved = DataEpochPolicy.resolve(encoded, hasAppOwnedState())
        if (encoded == null && resolved is DataEpochState.Current) write(resolved)
        if (resolved == DataEpochState.ResetRequired && encoded != DataEpochPolicy.encode(resolved)) {
            // ResetRequired is advisory and no mutation is permitted in this state. If storage is
            // currently read-only, keep blocking in memory; destructive confirmation will retry
            // with the mandatory durable ResetInProgress marker before deleting anything.
            runCatching { write(resolved) }
        }
        return resolved
    }

    fun markResetInProgress() = write(DataEpochState.ResetInProgress(CURRENT_DATA_EPOCH))

    fun markCurrent() = write(DataEpochState.Current(CURRENT_DATA_EPOCH))

    private fun write(state: DataEpochState) {
        val output = marker.startWrite()
        try {
            output.write(DataEpochPolicy.encode(state).toByteArray(Charsets.UTF_8))
            output.fd.sync()
            marker.finishWrite(output)
        } catch (error: Throwable) {
            marker.failWrite(output)
            throw error
        }
    }

    companion object {
        const val MARKER_FILE_NAME = "whip-data-epoch"
    }
}

private fun Context.hasPreEpochAppOwnedState(markerFileName: String): Boolean {
    val databaseNames = getDatabasePath("whip.db").parentFile
        ?.childrenOrFail()
        .orEmpty()
        .mapTo(mutableSetOf()) { file -> file.name }
    val preferenceNames = File(applicationInfo.dataDir, "shared_prefs")
        .childrenOrFail()
        .mapTo(mutableSetOf()) { file -> file.name.removeSuffix(".bak").removeSuffix(".xml") }
    val noBackupNames = noBackupFilesDir.childrenOrFail()
        .map { file -> file.name }
        .filterNotTo(mutableSetOf()) { name ->
            name == markerFileName || name == "$markerFileName.bak" || name == "$markerFileName.new"
        }
    return DataEpochPolicy.hasMeaningfulWhipState(databaseNames, preferenceNames, noBackupNames)
}

private fun File.childrenOrFail(): Array<File> {
    if (!exists()) return emptyArray()
    check(isDirectory) { "Whip expected ${path} to be a directory" }
    return checkNotNull(listFiles()) { "Whip could not inspect ${path}" }
}
