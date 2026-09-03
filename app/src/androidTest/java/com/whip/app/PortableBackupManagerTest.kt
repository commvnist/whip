package com.whip.app

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.data.BackupPreview
import com.whip.app.data.BackupRepository
import com.whip.app.data.PortableBackupDocumentStore
import com.whip.app.data.PortableBackupFile
import com.whip.app.data.PortableBackupManager
import com.whip.app.data.PortableBackupOutcome
import com.whip.app.data.PortableBackupScheduler
import com.whip.app.data.PortableBackupState
import com.whip.app.data.PORTABLE_BACKUP_WORK_NAME
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortableBackupManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferenceNames = mutableListOf<String>()

    @After
    fun tearDown() {
        preferenceNames.forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
    }

    @Test
    fun selectedFolderSettingsAndVerifiedBackupSurviveManagerRecreation() = runBlocking {
        val preferences = uniquePreferences()
        val store = FakeDocumentStore()
        val repository = FakeBackupRepository(records = 14)
        val manager = manager(preferences, repository, store)

        manager.configureFolder(TREE_URI)
        manager.setAutomaticEnabled(true)
        manager.setRetentionCount(3)
        val outcome = manager.backupNow()

        assertTrue(outcome is PortableBackupOutcome.Saved)
        assertTrue(store.reads >= 2)
        assertEquals("backup-test", store.lastWrittenContent)
        assertTrue(manager.state.value.lastBackupFileName!!.endsWith(".whip.json"))

        val recreated = manager(preferences, repository, store)
        assertEquals(TREE_URI.toString(), recreated.state.value.folderUri)
        assertEquals("Whip backups", recreated.state.value.folderLabel)
        assertTrue(recreated.state.value.automaticEnabled)
        assertEquals(3, recreated.state.value.retentionCount)
        assertEquals(FIXED_NOW.toEpochMilli(), recreated.state.value.lastBackupAtMillis)
    }

    @Test
    fun successfulWritePrunesOldBackupsButLeavesUnrelatedFiles() = runBlocking {
        val store = FakeDocumentStore().apply {
            addExisting("whip-older.whip.json", 1)
            addExisting("whip-old.whip.json", 2)
            addExisting("notes.txt", 0)
        }
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)
        manager.setRetentionCount(2)

        manager.backupNow()

        assertFalse(store.files.any { it.displayName == "whip-older.whip.json" })
        assertTrue(store.files.any { it.displayName == "whip-old.whip.json" })
        assertTrue(store.files.any { it.displayName == "notes.txt" })
        assertEquals(2, store.files.count { it.displayName.endsWith(".whip.json") })
    }

    @Test
    fun corruptProviderWriteIsDeletedAndNeverReportedAsSuccessful() = runBlocking {
        val store = FakeDocumentStore(corruptWrites = true)
        val manager = manager(uniquePreferences(), FakeBackupRepository(2), store)
        manager.configureFolder(TREE_URI)

        val failed = runCatching { manager.backupNow() }

        assertTrue(failed.isFailure)
        assertTrue(store.files.isEmpty())
        assertEquals(null, manager.state.value.lastBackupAtMillis)
        assertTrue(manager.state.value.lastError!!.contains("checksum"))
    }

    @Test
    fun stagingFileIsVerifiedThenRenamedAndNeverReportedAsIncomplete() = runBlocking {
        val store = FakeDocumentStore()
        val manager = manager(uniquePreferences(), FakeBackupRepository(2), store)
        manager.configureFolder(TREE_URI)

        manager.backupNow()

        assertTrue(store.writtenNames.single().startsWith("whip-INCOMPLETE-"))
        assertTrue(store.files.none { it.displayName.startsWith("whip-INCOMPLETE-") })
        assertTrue(store.files.single().displayName.endsWith(".whip.json"))
        assertEquals(1, store.renames)
    }

    @Test
    fun corruptNewestFileDoesNotDisplaceAnOlderVerifiedBackup() = runBlocking {
        val store = FakeDocumentStore().apply {
            addExisting("whip-valid.whip.json", 1)
            addExisting("whip-corrupt.whip.json", 2, content = "corrupt")
        }
        val manager = manager(uniquePreferences(), FakeBackupRepository(4), store)
        manager.configureFolder(TREE_URI)
        manager.setRetentionCount(2)

        manager.backupNow()

        assertTrue(store.files.any { it.displayName == "whip-valid.whip.json" })
        assertTrue(store.files.any { it.displayName == "whip-corrupt.whip.json" })
        assertTrue(manager.state.value.lastError!!.contains("ignored during retention"))
    }

    @Test
    fun duplicateProviderDisplayNameStillProtectsTheNewlyCommittedUri() = runBlocking {
        val duplicateName = "whip-2026-08-18-190102.whip.json"
        val store = FakeDocumentStore().apply { addExisting(duplicateName, 99) }
        val manager = manager(uniquePreferences(), FakeBackupRepository(4), store)
        manager.configureFolder(TREE_URI)
        manager.setRetentionCount(1)

        val outcome = manager.backupNow() as PortableBackupOutcome.Saved

        assertEquals(duplicateName, outcome.file.displayName)
        assertEquals(listOf(outcome.file.uri), store.files.filter { it.displayName.endsWith(".whip.json") }.map { it.uri })
    }

    @Test
    fun abandonedStagingFilesAreCleanedBeforeTheNextBackup() = runBlocking {
        val store = FakeDocumentStore().apply { addExisting("whip-INCOMPLETE-crashed.partial", 5) }
        val manager = manager(uniquePreferences(), FakeBackupRepository(1), store)
        manager.configureFolder(TREE_URI)

        manager.backupNow()

        assertTrue(store.files.none { it.displayName == "whip-INCOMPLETE-crashed.partial" })
    }

    @Test
    fun providerAndStorageFailuresNeverAdvanceSuccessOrExposeAPartialBackup() = runBlocking {
        listOf("list-offline", "write-low-storage", "verify-staged", "rename", "verify-committed").forEach { stage ->
            val store = FakeDocumentStore(failOperation = stage)
            val manager = manager(uniquePreferences(), FakeBackupRepository(3), store)
            manager.configureFolder(TREE_URI)

            val result = runCatching { manager.backupNow() }

            assertTrue("$stage must fail", result.isFailure)
            assertEquals("$stage must not record success", null, manager.state.value.lastBackupAtMillis)
            assertTrue("$stage must not expose a final archive", store.files.none { it.displayName.endsWith(".whip.json") })
            assertTrue("$stage must clean staging when deletion is available", store.files.none { it.displayName.startsWith("whip-INCOMPLETE-") })
            assertTrue(manager.state.value.lastError?.isNotBlank() == true)
        }
    }

    @Test
    fun scheduledBackupDoesNotRotateGoodHistoryWhenDatabaseIsEmpty() = runBlocking {
        val store = FakeDocumentStore().apply { addExisting("whip-last-good.whip.json", 1) }
        val manager = manager(uniquePreferences(), FakeBackupRepository(0), store)
        manager.configureFolder(TREE_URI)

        val outcome = manager.backupNow(allowEmpty = false)

        assertEquals(PortableBackupOutcome.SkippedEmptyDatabase, outcome)
        assertEquals(listOf("whip-last-good.whip.json"), store.files.map(PortableBackupFile::displayName))
    }

    @Test
    fun forgettingFolderStillClearsLocalConfigurationAfterProviderRevokesAccess() = runBlocking {
        val store = FakeDocumentStore(failOperation = "release")
        val manager = manager(uniquePreferences(), FakeBackupRepository(0), store)
        manager.configureFolder(TREE_URI)
        manager.setAutomaticEnabled(true)

        manager.clearFolder()

        assertEquals(null, manager.state.value.folderUri)
        assertFalse(manager.state.value.automaticEnabled)
    }

    @Test
    fun automaticBackupUsesOneRestartPersistentPeriodicJobAndCancelsWhenDisabled() {
        val workManager = WorkManager.getInstance(context)
        val scheduler = PortableBackupScheduler(context)
        try {
            scheduler.sync(
                PortableBackupState(
                    folderUri = TREE_URI.toString(),
                    automaticEnabled = true,
                ),
            )
            val scheduled = workManager.getWorkInfosForUniqueWork(PORTABLE_BACKUP_WORK_NAME).get()
            assertEquals(1, scheduled.size)
            assertTrue(scheduled.single().state == WorkInfo.State.ENQUEUED)

            scheduler.sync(PortableBackupState())
            workManager.getWorkInfosForUniqueWork(PORTABLE_BACKUP_WORK_NAME).get()
                .forEach { assertTrue(it.state == WorkInfo.State.CANCELLED) }
        } finally {
            workManager.cancelUniqueWork(PORTABLE_BACKUP_WORK_NAME).result.get()
        }
    }

    private fun manager(
        preferences: String,
        repository: BackupRepository,
        store: PortableBackupDocumentStore,
    ) = PortableBackupManager(
        context = context,
        backupRepository = repository,
        documentStore = store,
        now = { FIXED_NOW },
        zoneId = { ZoneId.of("America/Toronto") },
        preferencesName = preferences,
    )

    private fun uniquePreferences(): String = "portable-backup-test-${UUID.randomUUID()}".also(preferenceNames::add)

    private class FakeBackupRepository(private val records: Int) : BackupRepository {
        override suspend fun exportBackup() = "backup-test"
        override suspend fun previewBackup(json: String) = BackupPreview(
            envelopeVersion = 3,
            dataModelEpoch = 3,
            databaseVersion = 21,
            exportedAt = FIXED_NOW,
            tableCounts = mapOf("test" to records),
            totalRecords = records,
            duplicateStableIds = 0,
            checksumValid = json == "backup-test",
            settingsIncluded = true,
        )
        override suspend fun restoreBackup(json: String) = Unit
        override suspend fun mergeBackup(json: String) = com.whip.app.data.BackupMergeSummary(0, 0)
        override suspend fun exportTasksCsv() = ""
        override suspend fun exportHabitsCsv() = ""
        override suspend fun exportGoalsCsv() = ""
        override suspend fun exportTracksCsv() = ""
        override suspend fun exportGymCsv() = ""
        override suspend fun deleteAllData() = Unit
    }

    private class FakeDocumentStore(
        private val corruptWrites: Boolean = false,
        private val failOperation: String? = null,
    ) : PortableBackupDocumentStore {
        val files = mutableListOf<PortableBackupFile>()
        private val content = mutableMapOf<Uri, String>()
        var reads = 0
        var lastWrittenContent: String? = null
        var renames = 0
        val writtenNames = mutableListOf<String>()

        override fun persistAccess(treeUri: Uri) = Unit
        override fun releaseAccess(treeUri: Uri) {
            if (failOperation == "release") error("Provider already revoked access")
        }
        override fun folderLabel(treeUri: Uri) = "Whip backups"
        override fun write(treeUri: Uri, displayName: String, content: String): PortableBackupFile {
            if (failOperation == "write-low-storage") error("No space left on selected provider")
            lastWrittenContent = content
            writtenNames += displayName
            // Some real document providers do not publish LAST_MODIFIED immediately.
            val file = PortableBackupFile(Uri.parse("content://test/${files.size}/$displayName"), displayName, 0)
            files += file
            this.content[file.uri] = if (corruptWrites) "corrupt" else content
            return file
        }
        override fun rename(fileUri: Uri, displayName: String): PortableBackupFile {
            if (failOperation == "rename") error("Provider could not commit rename")
            renames++
            val index = files.indexOfFirst { it.uri == fileUri }
            require(index >= 0)
            val old = files[index]
            val renamed = old.copy(
                uri = Uri.parse("content://test/renamed/$displayName"),
                displayName = displayName,
            )
            val value = content.remove(old.uri)
            files[index] = renamed
            content[renamed.uri] = requireNotNull(value)
            return renamed
        }
        override fun read(fileUri: Uri): String {
            reads++
            if (failOperation == "verify-staged" && reads == 1) error("Provider went offline during staged verification")
            if (failOperation == "verify-committed" && reads == 2) error("Provider revoked access during committed verification")
            return content.getValue(fileUri)
        }
        override fun list(treeUri: Uri): List<PortableBackupFile> {
            if (failOperation == "list-offline") error("Selected provider is offline")
            return files.toList()
        }
        override fun delete(fileUri: Uri): Boolean {
            content.remove(fileUri)
            return files.removeAll { it.uri == fileUri }
        }
        fun addExisting(name: String, modified: Long, content: String = "backup-test") {
            val file = PortableBackupFile(Uri.parse("content://test/existing/$name"), name, modified)
            files += file
            this.content[file.uri] = content
        }
    }

    private companion object {
        val TREE_URI: Uri = Uri.parse("content://test/tree/whip")
        val FIXED_NOW: Instant = Instant.parse("2026-08-18T23:01:02Z")
    }
}
