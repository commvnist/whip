package com.whip.app

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
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
    fun failedFolderCommitKeepsThePreviousSelectionAndReleasesOnlyTheNewGrant() = runBlocking {
        val preferences = uniquePreferences()
        val store = FakeDocumentStore()
        val contextWithFailure = CommitFailureContext(context)
        val manager = manager(preferences, FakeBackupRepository(2), store, contextWithFailure)
        manager.configureFolder(TREE_URI)
        contextWithFailure.failCommit = true

        val replacement = runCatching { manager.configureFolder(SECOND_TREE_URI) }

        assertTrue(replacement.isFailure)
        assertEquals(TREE_URI.toString(), manager.state.value.folderUri)
        assertEquals(TREE_URI.toString(), manager(preferences, FakeBackupRepository(2), store).state.value.folderUri)
        assertTrue(SECOND_TREE_URI in store.releasedUris)
        assertFalse(TREE_URI in store.releasedUris)
    }

    @Test
    fun failedReceiptCommitCannotReportAVerifiedBackupAsSaved() = runBlocking {
        val preferences = uniquePreferences()
        val store = FakeDocumentStore()
        val contextWithFailure = CommitFailureContext(context)
        val manager = manager(preferences, FakeBackupRepository(2), store, contextWithFailure)
        manager.configureFolder(TREE_URI)
        contextWithFailure.failCommit = true

        val backup = runCatching { manager.backupNow() }

        assertTrue(backup.isFailure)
        assertEquals(null, manager.state.value.lastBackupAtMillis)
        assertEquals(null, manager.state.value.lastBackupFileName)
        assertTrue(manager.state.value.lastError.orEmpty().contains("receipt could not be recorded"))
        assertTrue(store.files.any { it.displayName.endsWith(".whip.json") })
    }

    @Test
    fun failedAutomaticToggleCommitKeepsThePreviousDurableSetting() = runBlocking {
        val preferences = uniquePreferences()
        val store = FakeDocumentStore()
        val contextWithFailure = CommitFailureContext(context)
        val manager = manager(preferences, FakeBackupRepository(2), store, contextWithFailure)
        manager.configureFolder(TREE_URI)
        contextWithFailure.failCommit = true

        val enable = runCatching { manager.setAutomaticEnabled(true) }

        assertTrue("An unconfirmed toggle must not report success", enable.isFailure)
        assertFalse(manager.state.value.automaticEnabled)
        assertFalse(manager(preferences, FakeBackupRepository(2), store).state.value.automaticEnabled)

        contextWithFailure.failCommit = false
        manager.setAutomaticEnabled(true)
        contextWithFailure.failCommit = true

        val disable = runCatching { manager.setAutomaticEnabled(false) }

        assertTrue("An unconfirmed disable must not report success", disable.isFailure)
        assertTrue(manager.state.value.automaticEnabled)
        assertTrue(manager(preferences, FakeBackupRepository(2), store).state.value.automaticEnabled)
    }

    @Test
    fun failedNewReceiptCannotPruneThePreviouslyVerifiedFile() = runBlocking {
        val preferences = uniquePreferences()
        val store = FakeDocumentStore()
        val contextWithFailure = CommitFailureContext(context)
        var clock = FIXED_NOW
        val manager = manager(preferences, FakeBackupRepository(2), store, contextWithFailure) { clock }
        manager.configureFolder(TREE_URI)
        assertTrue(manager.setRetentionCountAndConfirm(1))
        val previous = manager.backupNow() as PortableBackupOutcome.Saved
        val previousReceipt = manager.state.value
        clock = clock.plusSeconds(10)
        contextWithFailure.failCommit = true

        val replacement = runCatching { manager.backupNow() }

        assertTrue(replacement.isFailure)
        assertEquals(previousReceipt.lastBackupAtMillis, manager.state.value.lastBackupAtMillis)
        assertEquals(previousReceipt.lastBackupFileName, manager.state.value.lastBackupFileName)
        assertTrue("The old receipt must still identify a real file", store.files.any { it.uri == previous.file.uri })
        assertTrue("The newly verified file may remain for manual recovery", store.files.any { it.displayName != previous.file.displayName && it.displayName.endsWith(".whip.json") })
    }

    @Test
    fun failedRetentionWarningCommitStillKeepsTheNewVerifiedReceipt() = runBlocking {
        val preferences = uniquePreferences()
        val store = FakeDocumentStore(failOperation = "delete-backup").apply {
            addExisting("whip-2026-08-17-120000.whip.json", 1)
        }
        val contextWithFailure = CommitFailureContext(context)
        val manager = manager(preferences, FakeBackupRepository(2), store, contextWithFailure)
        manager.configureFolder(TREE_URI)
        assertTrue(manager.setRetentionCountAndConfirm(1))
        contextWithFailure.successfulCommitsBeforeFailure = 1

        val backup = runCatching { manager.backupNow() }

        assertTrue(backup.isFailure)
        assertTrue(backup.exceptionOrNull()?.message.orEmpty().contains("retention warning could not be recorded"))
        val receipt = manager.state.value
        assertEquals(FIXED_NOW.toEpochMilli(), receipt.lastBackupAtMillis)
        assertTrue(receipt.lastBackupFileName.orEmpty().endsWith(".whip.json"))
        assertTrue(store.files.any { it.displayName == receipt.lastBackupFileName })
        assertEquals(receipt.lastBackupFileName, manager(preferences, FakeBackupRepository(2), store).state.value.lastBackupFileName)
    }

    @Test
    fun successfulWritePrunesOldBackupsButLeavesUnrelatedFiles() = runBlocking {
        val store = FakeDocumentStore().apply {
            addExisting("whip-2026-08-16-120000.whip.json", 1)
            addExisting("whip-2026-08-17-120000.whip.json", 2)
            addExisting("notes.txt", 0)
        }
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)
        manager.setRetentionCount(2)

        manager.backupNow()

        assertFalse(store.files.any { it.displayName == "whip-2026-08-16-120000.whip.json" })
        assertTrue(store.files.any { it.displayName == "whip-2026-08-17-120000.whip.json" })
        assertTrue(store.files.any { it.displayName == "notes.txt" })
        assertEquals(2, store.files.count { it.displayName.endsWith(".whip.json") })
    }

    @Test
    fun automaticRetentionNeverPrunesAManuallySavedPlainBackup() = runBlocking {
        val manualName = "whip-2026-08-17.whip.json"
        val oldAutomaticName = "whip-2026-08-16-120000.whip.json"
        val store = FakeDocumentStore().apply {
            addExisting(manualName, 1)
            addExisting(oldAutomaticName, 2)
        }
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)
        manager.setRetentionCount(1)

        manager.backupNow()

        assertTrue("Manual backup must remain even when it is old", store.files.any { it.displayName == manualName })
        assertFalse(store.files.any { it.displayName == oldAutomaticName })
        assertTrue(store.files.any { it.displayName == "whip-2026-08-18-190102.whip.json" })
    }

    @Test
    fun crashCleanupDoesNotDeleteAnUnrelatedFileWithTheIncompletePrefix() = runBlocking {
        val unrelatedName = "whip-INCOMPLETE-personal-notes.txt"
        val stagedName = "whip-INCOMPLETE-00000000-0000-0000-0000-000000000001.partial"
        val store = FakeDocumentStore().apply {
            addExisting(unrelatedName, 1)
            addExisting(stagedName, 2)
        }
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)

        manager.recoverInterruptedWrites()

        assertTrue("Unrelated note must remain", store.files.any { it.displayName == unrelatedName })
        assertFalse(store.files.any { it.displayName == stagedName })
    }

    @Test
    fun startupRecoveryReportsRevokedProviderAndReselectionClearsTheWarning() = runBlocking {
        val stagedName = "whip-INCOMPLETE-00000000-0000-0000-0000-000000000003.partial"
        val store = FakeDocumentStore().apply { addExisting(stagedName, 1) }
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)
        manager.setAutomaticEnabled(true)
        store.failOperation = "list-offline"

        val recovery = runCatching { manager.recoverInterruptedWrites() }

        assertTrue(recovery.isFailure)
        assertTrue(manager.state.value.lastError.orEmpty().contains("offline"))
        assertTrue(manager.state.value.automaticEnabled)
        assertEquals(null, manager.state.value.lastBackupAtMillis)

        store.failOperation = null
        manager.configureFolder(TREE_URI)
        manager.recoverInterruptedWrites()

        assertEquals(null, manager.state.value.lastError)
        assertFalse(store.files.any { it.displayName == stagedName })
    }

    @Test
    fun revokedProviderWarningNeverExposesPlatformProcessDetails() = runBlocking {
        val store = FakeDocumentStore(failOperation = "list-revoked-security")
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)

        val recovery = runCatching { manager.recoverInterruptedWrites() }

        assertEquals(
            "Whip no longer has access to the selected backup folder",
            recovery.exceptionOrNull()?.message,
        )
        val warning = manager.state.value.lastError.orEmpty()
        assertTrue(warning.contains("no longer has access"))
        assertFalse(warning.contains("ProcessRecord"))
        assertFalse(warning.contains("uid="))
    }

    @Test
    fun grantLossDuringFolderSelectionUsesTheSameSafeRecoveryMessage() = runBlocking {
        val store = FakeDocumentStore(failOperation = "persist-revoked-security")
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)

        val selection = runCatching { manager.configureFolder(TREE_URI) }

        assertEquals(
            "Whip no longer has access to the selected backup folder",
            selection.exceptionOrNull()?.message,
        )
        assertFalse(manager.state.value.configured)
        assertFalse(selection.exceptionOrNull()?.message.orEmpty().contains("ProcessRecord"))
    }

    @Test
    fun changingFolderStillSucceedsWhenTheOldGrantIsAlreadyGone() = runBlocking {
        val store = FakeDocumentStore()
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)
        manager.setAutomaticEnabled(true)
        store.failOperation = "release"

        manager.configureFolder(SECOND_TREE_URI)

        assertEquals(SECOND_TREE_URI.toString(), manager.state.value.folderUri)
        assertTrue(manager.state.value.automaticEnabled)
        assertEquals(null, manager.state.value.lastError)
    }

    @Test
    fun startupRecoveryReportsAnOwnedPartialThatProviderRefusesToDelete() = runBlocking {
        val stagedName = "whip-INCOMPLETE-00000000-0000-0000-0000-000000000004.partial"
        val store = FakeDocumentStore().apply { addExisting(stagedName, 1) }
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)
        store.failOperation = "delete-staging"

        manager.recoverInterruptedWrites()

        assertTrue(manager.state.value.lastError.orEmpty().contains("could not be removed"))
        assertTrue(store.files.any { it.displayName == stagedName })
        assertEquals(null, manager.state.value.lastBackupAtMillis)
    }

    @Test
    fun verifiedBackupKeepsSuccessReceiptAndReportsRefusedPartialCleanup() = runBlocking {
        val stagedName = "whip-INCOMPLETE-00000000-0000-0000-0000-000000000005.partial"
        val store = FakeDocumentStore().apply { addExisting(stagedName, 1) }
        val manager = manager(uniquePreferences(), FakeBackupRepository(5), store)
        manager.configureFolder(TREE_URI)
        store.failOperation = "delete-staging"

        val outcome = manager.backupNow()

        assertTrue(outcome is PortableBackupOutcome.Saved)
        assertEquals(FIXED_NOW.toEpochMilli(), manager.state.value.lastBackupAtMillis)
        assertTrue(manager.state.value.lastError.orEmpty().contains("could not be removed"))
        assertTrue(store.files.any { it.displayName == stagedName })
        assertTrue(store.files.any { it.displayName == "whip-2026-08-18-190102.whip.json" })
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
            addExisting("whip-2026-08-16-120000.whip.json", 1)
            addExisting("whip-2026-08-17-120000.whip.json", 2, content = "corrupt")
        }
        val manager = manager(uniquePreferences(), FakeBackupRepository(4), store)
        manager.configureFolder(TREE_URI)
        manager.setRetentionCount(2)

        manager.backupNow()

        assertTrue(store.files.any { it.displayName == "whip-2026-08-16-120000.whip.json" })
        assertTrue(store.files.any { it.displayName == "whip-2026-08-17-120000.whip.json" })
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
        val stagedName = "whip-INCOMPLETE-00000000-0000-0000-0000-000000000002.partial"
        val store = FakeDocumentStore().apply { addExisting(stagedName, 5) }
        val manager = manager(uniquePreferences(), FakeBackupRepository(1), store)
        manager.configureFolder(TREE_URI)

        manager.backupNow()

        assertTrue(store.files.none { it.displayName == stagedName })
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
        val lastGoodName = "whip-2026-08-17-120000.whip.json"
        val store = FakeDocumentStore().apply { addExisting(lastGoodName, 1) }
        val manager = manager(uniquePreferences(), FakeBackupRepository(0), store)
        manager.configureFolder(TREE_URI)

        val outcome = manager.backupNow(allowEmpty = false)

        assertEquals(PortableBackupOutcome.SkippedEmptyDatabase, outcome)
        assertEquals(listOf(lastGoodName), store.files.map(PortableBackupFile::displayName))
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
        managerContext: Context = context,
        now: () -> Instant = { FIXED_NOW },
    ) = PortableBackupManager(
        context = managerContext,
        backupRepository = repository,
        documentStore = store,
        now = now,
        zoneId = { ZoneId.of("America/Toronto") },
        preferencesName = preferences,
    )

    private fun uniquePreferences(): String = "portable-backup-test-${UUID.randomUUID()}".also(preferenceNames::add)

    private class CommitFailureContext(base: Context) : ContextWrapper(base) {
        var failCommit = false
        var successfulCommitsBeforeFailure: Int? = null

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val actual = super.getSharedPreferences(name, mode)
            return object : SharedPreferences by actual {
                override fun edit(): SharedPreferences.Editor {
                    val editor = actual.edit()
                    return object : SharedPreferences.Editor by editor {
                        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                            editor.putString(key, value)
                            return this
                        }

                        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                            editor.putBoolean(key, value)
                            return this
                        }

                        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                            editor.putInt(key, value)
                            return this
                        }

                        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                            editor.putLong(key, value)
                            return this
                        }

                        override fun remove(key: String?): SharedPreferences.Editor {
                            editor.remove(key)
                            return this
                        }

                        override fun commit(): Boolean {
                            val remaining = successfulCommitsBeforeFailure
                            if (remaining != null) {
                                if (remaining == 0) {
                                    successfulCommitsBeforeFailure = null
                                    return false
                                }
                                successfulCommitsBeforeFailure = remaining - 1
                            }
                            return if (failCommit) false else editor.commit()
                        }
                    }
                }
            }
        }
    }

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
        var failOperation: String? = null,
    ) : PortableBackupDocumentStore {
        val files = mutableListOf<PortableBackupFile>()
        private val content = mutableMapOf<Uri, String>()
        var reads = 0
        var lastWrittenContent: String? = null
        var renames = 0
        val writtenNames = mutableListOf<String>()
        val releasedUris = mutableListOf<Uri>()

        override fun persistAccess(treeUri: Uri) {
            if (failOperation == "persist-revoked-security") {
                throw SecurityException("Permission Denial from ProcessRecord{test} (pid=123, uid=456)")
            }
        }
        override fun releaseAccess(treeUri: Uri) {
            releasedUris += treeUri
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
            if (failOperation == "list-revoked-security") {
                throw SecurityException("Permission Denial from ProcessRecord{test} (pid=123, uid=456)")
            }
            return files.toList()
        }
        override fun delete(fileUri: Uri): Boolean {
            if (failOperation == "delete-staging" && files.any { it.uri == fileUri && it.displayName.startsWith("whip-INCOMPLETE-") }) {
                return false
            }
            if (failOperation == "delete-backup" && files.any { it.uri == fileUri && it.displayName.endsWith(".whip.json") }) {
                return false
            }
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
        val SECOND_TREE_URI: Uri = Uri.parse("content://test/tree/whip-new")
        val FIXED_NOW: Instant = Instant.parse("2026-08-18T23:01:02Z")
    }
}
