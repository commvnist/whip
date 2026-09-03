package com.whip.app.startup

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.whip.app.core.AppSettings
import com.whip.app.core.SharedPreferencesSettingsRepository
import com.whip.app.core.normalized
import com.whip.app.data.WhipDatabase
import com.whip.app.widget.HabitTrackingWidgetProvider
import com.whip.app.widget.WhipWidgetProvider
import java.io.File
import java.security.SecureRandom

/** Destructive epoch reset that deliberately has no repository dependency. */
class LocalDataResetter(private val context: Context) {
    fun resetAndVerify(): Long {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork().result.get()
        workManager.pruneWork().result.get()
        NotificationManagerCompat.from(context).cancelAll()
        releasePortableBackupGrant()

        WhipDatabase.closeForReset()
        check(context.deleteDatabase(DATABASE_NAME) || !context.getDatabasePath(DATABASE_NAME).exists()) {
            "Whip's database could not be removed"
        }
        deleteDatabaseSidecars()
        clearSharedPreferences()
        clearDirectory(context.filesDir)
        clearDirectory(context.cacheDir)
        clearDirectory(context.codeCacheDir)
        context.externalCacheDirs.filterNotNull().forEach(::clearDirectory)
        clearDirectory(
            context.noBackupFilesDir,
            retainedNames = setOf(DataEpochGate.MARKER_FILE_NAME),
            retainedPrefixes = setOf("androidx.work.workdb"),
        )

        val generation = nextGeneration()
        check(
            context.getSharedPreferences(RECOVERY_RUNTIME_PREFERENCES, Context.MODE_PRIVATE)
                .edit().putLong(USER_DATA_GENERATION, generation).commit(),
        ) { "Whip could not establish a fresh data generation" }

        val database = WhipDatabase.get(context)
        val sqliteDatabase = database.openHelper.writableDatabase
        sqliteDatabase.query("PRAGMA user_version").use { cursor ->
            check(cursor.moveToFirst() && cursor.getInt(0) == CANONICAL_DATABASE_VERSION) {
                "Whip could not verify the fresh database version"
            }
        }
        check(SharedPreferencesSettingsRepository(context).current() == AppSettings().normalized()) {
            "Whip could not verify fresh default settings"
        }
        check(
            generation != 0L &&
                context.getSharedPreferences(RECOVERY_RUNTIME_PREFERENCES, Context.MODE_PRIVATE)
                    .getLong(USER_DATA_GENERATION, 0L) == generation,
        ) { "Whip could not verify a fresh data generation" }
        showLockedWidgets()
        return generation
    }

    private fun releasePortableBackupGrant() {
        val stored = context.getSharedPreferences(PORTABLE_BACKUP_PREFERENCES, Context.MODE_PRIVATE)
            .getString(PORTABLE_BACKUP_FOLDER_URI, null)
            ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(stored),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    private fun clearSharedPreferences() {
        val directory = File(context.applicationInfo.dataDir, "shared_prefs")
        val files = directory.childrenOrFail()
        val ownedNames = DataEpochPolicy.selectWhipPreferenceNames(
            files.mapTo(mutableSetOf()) { file -> file.preferenceStoreName() },
        )
        ownedNames.forEach { name ->
            check(context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()) {
                "Whip could not clear local preferences"
            }
            files.filter { file -> file.preferenceStoreName() == name }.forEach { file ->
                if (file.exists()) check(file.delete()) { "Whip could not remove ${file.name}" }
            }
        }
    }

    private fun File.preferenceStoreName(): String = name.removeSuffix(".bak").removeSuffix(".xml")

    private fun deleteDatabaseSidecars() {
        val base = context.getDatabasePath(DATABASE_NAME)
        listOf("-wal", "-shm", "-journal").forEach { suffix ->
            File(base.path + suffix).takeIf(File::exists)?.let { file ->
                check(file.delete()) { "Whip could not remove ${file.name}" }
            }
        }
    }

    private fun clearDirectory(
        directory: File,
        retainedNames: Set<String> = emptySet(),
        retainedPrefixes: Set<String> = emptySet(),
    ) {
        directory.childrenOrFail().forEach { child ->
            if (
                child.name in retainedNames || child.name.removeSuffix(".bak") in retainedNames ||
                retainedPrefixes.any(child.name::startsWith)
            ) return@forEach
            check(child.deleteRecursively()) { "Whip could not remove ${child.name}" }
        }
    }

    private fun showLockedWidgets() {
        val manager = AppWidgetManager.getInstance(context)
        val taskIds = manager.getAppWidgetIds(ComponentName(context, WhipWidgetProvider::class.java))
        val habitIds = manager.getAppWidgetIds(ComponentName(context, HabitTrackingWidgetProvider::class.java))
        WhipWidgetProvider.showUpdateRequired(context, manager, taskIds, habitIds)
    }

    private fun nextGeneration(): Long {
        var value = SecureRandom().nextLong()
        while (value == 0L) value = SecureRandom().nextLong()
        return value
    }

    private fun File.childrenOrFail(): Array<File> {
        if (!exists()) return emptyArray()
        check(isDirectory) { "Whip expected ${path} to be a directory" }
        return checkNotNull(listFiles()) { "Whip could not inspect ${path}" }
    }

    private companion object {
        const val DATABASE_NAME = "whip.db"
        const val CANONICAL_DATABASE_VERSION = 44
        const val PORTABLE_BACKUP_PREFERENCES = "portable_backups"
        const val PORTABLE_BACKUP_FOLDER_URI = "folder_uri"
        const val RECOVERY_RUNTIME_PREFERENCES = "whip_recovery_runtime"
        const val USER_DATA_GENERATION = "user_data_generation"
    }
}
