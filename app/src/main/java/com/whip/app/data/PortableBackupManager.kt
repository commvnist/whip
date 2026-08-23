package com.whip.app.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.whip.app.WhipApplication
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PortableBackupState(
    val folderUri: String? = null,
    val folderLabel: String? = null,
    val automaticEnabled: Boolean = false,
    val retentionCount: Int = DEFAULT_PORTABLE_BACKUP_RETENTION,
    val lastBackupAtMillis: Long? = null,
    val lastBackupFileName: String? = null,
    val lastError: String? = null,
) {
    val configured: Boolean get() = folderUri != null
}

data class PortableBackupFile(
    val uri: Uri,
    val displayName: String,
    val lastModifiedMillis: Long,
)

sealed interface PortableBackupOutcome {
    data class Saved(val file: PortableBackupFile, val recordCount: Int) : PortableBackupOutcome
    data object SkippedEmptyDatabase : PortableBackupOutcome
}

/** Abstraction around Android's Storage Access Framework so backup policy is testable. */
interface PortableBackupDocumentStore {
    fun persistAccess(treeUri: Uri)
    fun releaseAccess(treeUri: Uri)
    fun folderLabel(treeUri: Uri): String
    fun write(treeUri: Uri, displayName: String, content: String): PortableBackupFile
    fun rename(fileUri: Uri, displayName: String): PortableBackupFile
    fun read(fileUri: Uri): String
    fun list(treeUri: Uri): List<PortableBackupFile>
    fun delete(fileUri: Uri): Boolean
}

class SafPortableBackupDocumentStore(
    private val resolver: ContentResolver,
) : PortableBackupDocumentStore {
    override fun persistAccess(treeUri: Uri) {
        resolver.takePersistableUriPermission(treeUri, URI_PERMISSION_FLAGS)
        try {
            require(folderDocument(treeUri).let(::isWritableDirectory)) {
                "The selected location is not a writable folder"
            }
        } catch (error: Throwable) {
            releaseAccess(treeUri)
            throw error
        }
    }

    override fun releaseAccess(treeUri: Uri) {
        runCatching { resolver.releasePersistableUriPermission(treeUri, URI_PERMISSION_FLAGS) }
    }

    override fun folderLabel(treeUri: Uri): String = queryDocument(folderDocument(treeUri))?.displayName
        ?.takeIf(String::isNotBlank)
        ?: treeUri.lastPathSegment
        ?: "Selected folder"

    override fun write(treeUri: Uri, displayName: String, content: String): PortableBackupFile {
        val uri = DocumentsContract.createDocument(
            resolver,
            folderDocument(treeUri),
            BACKUP_MIME_TYPE,
            displayName,
        ) ?: error("The selected folder could not create a backup file")
        try {
            resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(content)
            } ?: error("The backup file could not be opened for writing")
            return queryDocument(uri) ?: PortableBackupFile(uri, displayName, 0L)
        } catch (error: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            throw error
        }
    }

    override fun read(fileUri: Uri): String =
        resolver.openInputStream(fileUri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("The backup file could not be opened for verification")

    override fun rename(fileUri: Uri, displayName: String): PortableBackupFile {
        val renamed = DocumentsContract.renameDocument(resolver, fileUri, displayName)
            ?: error("The selected storage provider could not commit the verified backup")
        return queryDocument(renamed) ?: PortableBackupFile(renamed, displayName, 0L)
    }

    override fun list(treeUri: Uri): List<PortableBackupFile> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        return resolver.query(childrenUri, DOCUMENT_PROJECTION, null, null, null)?.use { cursor ->
            buildList {
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val modifiedIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIndex)
                    add(
                        PortableBackupFile(
                            uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                            displayName = cursor.getString(nameIndex).orEmpty(),
                            lastModifiedMillis = if (cursor.isNull(modifiedIndex)) 0L else cursor.getLong(modifiedIndex),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    override fun delete(fileUri: Uri): Boolean = DocumentsContract.deleteDocument(resolver, fileUri)

    private fun folderDocument(treeUri: Uri): Uri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri),
    )

    private fun isWritableDirectory(uri: Uri): Boolean {
        val info = queryDocumentInfo(uri) ?: return false
        return info.mimeType == DocumentsContract.Document.MIME_TYPE_DIR &&
            info.flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0
    }

    private fun queryDocument(uri: Uri): PortableBackupFile? = queryDocumentInfo(uri)?.let { info ->
        PortableBackupFile(uri, info.displayName, info.lastModifiedMillis)
    }

    private fun queryDocumentInfo(uri: Uri): DocumentInfo? =
        resolver.query(uri, DOCUMENT_PROJECTION, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            DocumentInfo(
                displayName = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                ).orEmpty(),
                lastModifiedMillis = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let(cursor::getLong)
                    ?: 0L,
                mimeType = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE),
                ).orEmpty(),
                flags = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let(cursor::getInt)
                    ?: 0,
            )
        }

    private data class DocumentInfo(
        val displayName: String,
        val lastModifiedMillis: Long,
        val mimeType: String,
        val flags: Int,
    )

    private companion object {
        val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
        )
        const val URI_PERMISSION_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}

class PortableBackupManager(
    context: Context,
    private val backupRepository: BackupRepository,
    private val documentStore: PortableBackupDocumentStore = SafPortableBackupDocumentStore(context.contentResolver),
    private val now: () -> Instant = Instant::now,
    private val zoneId: () -> ZoneId = ZoneId::systemDefault,
    preferencesName: String = PORTABLE_BACKUP_PREFERENCES,
) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(readState())
    val state: StateFlow<PortableBackupState> = mutableState

    suspend fun configureFolder(treeUri: Uri) = mutex.withLock {
        val previousUri = mutableState.value.folderUri?.let(Uri::parse)
        documentStore.persistAccess(treeUri)
        try {
            val label = documentStore.folderLabel(treeUri)
            updateState {
                it.copy(
                    folderUri = treeUri.toString(),
                    folderLabel = label,
                    lastError = null,
                )
            }
            if (previousUri != null && previousUri != treeUri) documentStore.releaseAccess(previousUri)
        } catch (error: Throwable) {
            if (previousUri != treeUri) documentStore.releaseAccess(treeUri)
            throw error
        }
    }

    fun setAutomaticEnabled(enabled: Boolean) {
        require(!enabled || mutableState.value.configured) { "Choose a backup folder first" }
        updateState { it.copy(automaticEnabled = enabled, lastError = null) }
    }

    fun setRetentionCount(count: Int) {
        updateState { it.copy(retentionCount = count.coerceIn(MIN_PORTABLE_BACKUP_RETENTION, MAX_PORTABLE_BACKUP_RETENTION)) }
    }

    suspend fun clearFolder() = mutex.withLock {
        mutableState.value.folderUri?.let(Uri::parse)?.let(documentStore::releaseAccess)
        updateState {
            it.copy(
                folderUri = null,
                folderLabel = null,
                automaticEnabled = false,
                lastError = null,
            )
        }
    }

    suspend fun recoverInterruptedWrites() = mutex.withLock {
        val treeUri = mutableState.value.folderUri?.let(Uri::parse) ?: return@withLock
        cleanupStagingFiles(treeUri)
    }

    suspend fun backupNow(allowEmpty: Boolean = true): PortableBackupOutcome = mutex.withLock {
        val current = mutableState.value
        val treeUri = current.folderUri?.let(Uri::parse) ?: error("Choose a backup folder first")
        try {
            val json = backupRepository.exportBackup()
            val sourcePreview = backupRepository.previewBackup(json)
            require(sourcePreview.checksumValid) { "Generated backup checksum does not match" }
            if (!allowEmpty && sourcePreview.totalRecords == 0) {
                updateState { it.copy(lastError = null) }
                return@withLock PortableBackupOutcome.SkippedEmptyDatabase
            }

            cleanupStagingFiles(treeUri)
            val requestedName = portableBackupFileName(now(), zoneId())
            val stagingName = "$PORTABLE_BACKUP_STAGING_PREFIX${UUID.randomUUID()}.partial"
            val staged = documentStore.write(treeUri, stagingName, json)
            try {
                val writtenPreview = backupRepository.previewBackup(documentStore.read(staged.uri))
                require(writtenPreview.checksumValid) { "Written backup checksum does not match" }
                require(writtenPreview.totalRecords == sourcePreview.totalRecords) {
                    "Written backup record count does not match"
                }
            } catch (error: Throwable) {
                runCatching { documentStore.delete(staged.uri) }
                throw error
            }

            val created = try {
                documentStore.rename(staged.uri, requestedName)
            } catch (error: Throwable) {
                runCatching { documentStore.delete(staged.uri) }
                throw error
            }
            try {
                val committedPreview = backupRepository.previewBackup(documentStore.read(created.uri))
                require(committedPreview.checksumValid && committedPreview.totalRecords == sourcePreview.totalRecords) {
                    "Committed backup verification failed"
                }
            } catch (error: Throwable) {
                runCatching { documentStore.delete(created.uri) }
                throw error
            }

            val allFiles = documentStore.list(treeUri)
            val eligibleExisting = allFiles.filter {
                it.uri != created.uri && it.displayName.startsWith("whip-") && it.displayName.endsWith(".whip.json")
            }
            val validExisting = eligibleExisting.filter { file ->
                runCatching {
                    val preview = backupRepository.previewBackup(documentStore.read(file.uri))
                    preview.checksumValid
                }.getOrDefault(false)
            }
            val invalidCount = eligibleExisting.size - validExisting.size
            val pruneFailures = portableBackupFilesToPrune(
                files = validExisting + created,
                retentionCount = current.retentionCount,
                protectedUri = created.uri,
            ).count { old -> runCatching { documentStore.delete(old.uri) }.getOrDefault(false).not() }
            val savedAt = now().toEpochMilli()
            updateState {
                it.copy(
                    lastBackupAtMillis = savedAt,
                    lastBackupFileName = created.displayName,
                    lastError = buildList {
                        if (invalidCount > 0) add("$invalidCount corrupt or unreadable backup${if (invalidCount == 1) " was" else "s were"} ignored during retention")
                        if (pruneFailures > 0) add("$pruneFailures old backup${if (pruneFailures == 1) "" else "s"} could not be removed")
                    }.takeIf(List<String>::isNotEmpty)?.joinToString("; "),
                )
            }
            PortableBackupOutcome.Saved(created, sourcePreview.totalRecords)
        } catch (error: Throwable) {
            updateState { it.copy(lastError = error.message ?: "Portable backup failed") }
            throw error
        }
    }

    private fun cleanupStagingFiles(treeUri: Uri): Int = documentStore.list(treeUri)
        .filter { it.displayName.startsWith(PORTABLE_BACKUP_STAGING_PREFIX) }
        .count { file -> runCatching { documentStore.delete(file.uri) }.getOrDefault(false) }

    private fun readState(): PortableBackupState = PortableBackupState(
        folderUri = preferences.getString(KEY_FOLDER_URI, null),
        folderLabel = preferences.getString(KEY_FOLDER_LABEL, null),
        automaticEnabled = preferences.getBoolean(KEY_AUTOMATIC, false),
        retentionCount = preferences.getInt(KEY_RETENTION, DEFAULT_PORTABLE_BACKUP_RETENTION)
            .coerceIn(MIN_PORTABLE_BACKUP_RETENTION, MAX_PORTABLE_BACKUP_RETENTION),
        lastBackupAtMillis = preferences.getLong(KEY_LAST_AT, Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE },
        lastBackupFileName = preferences.getString(KEY_LAST_FILE, null),
        lastError = preferences.getString(KEY_LAST_ERROR, null),
    )

    @android.annotation.SuppressLint("UseKtx")
    private fun updateState(transform: (PortableBackupState) -> PortableBackupState) {
        val updated = transform(mutableState.value)
        preferences.edit()
            .putNullableString(KEY_FOLDER_URI, updated.folderUri)
            .putNullableString(KEY_FOLDER_LABEL, updated.folderLabel)
            .putBoolean(KEY_AUTOMATIC, updated.automaticEnabled)
            .putInt(KEY_RETENTION, updated.retentionCount)
            .putNullableLong(KEY_LAST_AT, updated.lastBackupAtMillis)
            .putNullableString(KEY_LAST_FILE, updated.lastBackupFileName)
            .putNullableString(KEY_LAST_ERROR, updated.lastError)
            .apply()
        mutableState.value = updated
    }
}

class PortableBackupScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun sync(state: PortableBackupState) {
        if (!state.configured || !state.automaticEnabled) {
            workManager.cancelUniqueWork(PORTABLE_BACKUP_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<PortableBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .addTag(PORTABLE_BACKUP_WORK_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PORTABLE_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

class PortableBackupWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WhipApplication
        return runCatching { app.portableBackupManager.backupNow(allowEmpty = false) }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { if (runAttemptCount < 2) Result.retry() else Result.failure() },
            )
    }
}

fun portableBackupFileName(instant: Instant, zoneId: ZoneId): String =
    "whip-${BACKUP_FILE_TIME_FORMAT.format(instant.atZone(zoneId))}.whip.json"

fun portableBackupFilesToPrune(
    files: List<PortableBackupFile>,
    retentionCount: Int,
    protectedUri: Uri? = null,
): List<PortableBackupFile> = portableBackupItemsToPrune(
    items = files,
    retentionCount = retentionCount,
    displayName = PortableBackupFile::displayName,
    lastModifiedMillis = PortableBackupFile::lastModifiedMillis,
    protected = { protectedUri != null && it.uri == protectedUri },
)

fun <T> portableBackupItemsToPrune(
    items: List<T>,
    retentionCount: Int,
    displayName: (T) -> String,
    lastModifiedMillis: (T) -> Long,
    protected: (T) -> Boolean = { false },
): List<T> {
    val keep = retentionCount.coerceIn(MIN_PORTABLE_BACKUP_RETENTION, MAX_PORTABLE_BACKUP_RETENTION)
    val eligible = items
        .filter { displayName(it).startsWith("whip-") && displayName(it).endsWith(".whip.json") }
    val protectedCount = eligible.count(protected).coerceAtMost(keep)
    return eligible
        .filterNot(protected)
        .sortedWith(compareByDescending<T> { lastModifiedMillis(it) }.thenByDescending { displayName(it) })
        .drop(keep - protectedCount)
}

private fun android.content.SharedPreferences.Editor.putNullableString(key: String, value: String?) = apply {
    if (value == null) remove(key) else putString(key, value)
}

private fun android.content.SharedPreferences.Editor.putNullableLong(key: String, value: Long?) = apply {
    if (value == null) remove(key) else putLong(key, value)
}

private const val BACKUP_MIME_TYPE = "application/json"
internal const val PORTABLE_BACKUP_STAGING_PREFIX = "whip-INCOMPLETE-"
private const val PORTABLE_BACKUP_PREFERENCES = "portable_backups"
private const val KEY_FOLDER_URI = "folder_uri"
private const val KEY_FOLDER_LABEL = "folder_label"
private const val KEY_AUTOMATIC = "automatic_enabled"
private const val KEY_RETENTION = "retention_count"
private const val KEY_LAST_AT = "last_backup_at"
private const val KEY_LAST_FILE = "last_backup_file"
private const val KEY_LAST_ERROR = "last_error"
const val PORTABLE_BACKUP_WORK_NAME = "whip-portable-backup"
private const val PORTABLE_BACKUP_WORK_TAG = "whip-portable-backup"
private const val DEFAULT_PORTABLE_BACKUP_RETENTION = 7
private const val MIN_PORTABLE_BACKUP_RETENTION = 1
private const val MAX_PORTABLE_BACKUP_RETENTION = 30
private val BACKUP_FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
