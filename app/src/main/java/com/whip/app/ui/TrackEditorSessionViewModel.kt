package com.whip.app.ui

import android.app.Application
import android.os.Bundle
import android.util.AtomicFile
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whip.app.TRACK_EDITOR_CHECKPOINT_DIRECTORY
import com.whip.app.WhipApplication
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.security.MessageDigest
import java.util.UUID

@Composable
internal fun trackEditorSessionViewModel(): TrackEditorSessionViewModel =
    viewModel(key = TrackEditorSessionViewModel.KEY)

/** One active editor; the Activity registry carries a reference, never its authored graph. */
internal class TrackEditorSessionViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    // These handles are intentionally not registered with the Activity's saved-state registry.
    val definition = TrackEditorViewModel(SavedStateHandle())
    val entry = TrackEntryEditorViewModel(SavedStateHandle())
    private val store = TrackEditorCheckpointStore(File(application.noBackupFilesDir, TRACK_EDITOR_CHECKPOINT_DIRECTORY))
    private var owner = UUID.randomUUID().toString()
    private val route = mutableStateOf<TrackEditorRoute?>(null)
    private var reference: Bundle? = savedStateHandle[STATE_KEY]
    var recoveryProblem by mutableStateOf<String?>(null)
        private set
    val canRetryRecovery: Boolean
        get() = route.value != null || reference?.getString("hash") != null

    val routeState: MutableState<TrackEditorRoute?> = object : MutableState<TrackEditorRoute?> {
        override var value: TrackEditorRoute?
            get() = route.value
            set(value) {
                if (value == null || (route.value != null && value.sessionId != route.value?.sessionId)) {
                    clearSession()
                }
                route.value = value
            }
        override fun component1() = value
        override fun component2(): (TrackEditorRoute?) -> Unit = { value = it }
    }

    init {
        reference?.let { restore(it) }
        savedStateHandle.setSavedStateProvider(STATE_KEY, ::saveCheckpoint)
    }

    internal fun saveCheckpoint(): Bundle = synchronized(getApplication<Application>()) {
        val currentRoute = route.value ?: return@synchronized reference ?: Bundle()
        if (!generationMatches(currentRoute.openingDataGeneration)) {
            clearSession()
            return@synchronized Bundle()
        }
        try {
            val checkpoint = TrackEditorCheckpoint(currentRoute, definition.state.value, entry.state.value)
            store.write(owner, checkpoint).also {
                reference = it
                recoveryProblem = null
            }
        } catch (failure: Exception) {
            Log.e("WhipTrackRecovery", "Cannot checkpoint Track editor", failure)
            recoveryProblem = WRITE_PROBLEM
            // An older checkpoint must never masquerade as the current authored draft.
            Bundle().apply {
                putString("owner", owner)
                putLong("generation", currentRoute.openingDataGeneration)
                putBoolean("writeFailed", true)
            }.also { reference = it }
        }
    }

    fun retryRecovery() {
        if (route.value != null) saveCheckpoint() else reference?.let(::restore)
    }

    fun dismissRecoveryProblem() {
        if (route.value == null) clearSession() else recoveryProblem = null
    }

    private fun restore(saved: Bundle): Unit = synchronized(getApplication<Application>()) {
        if (saved.isEmpty) return@synchronized
        try {
            val restoredOwner = requireNotNull(saved.getString("owner"))
            require(UUID.fromString(restoredOwner).toString() == restoredOwner)
            owner = restoredOwner
            if (!generationMatches(saved.getLong("generation"))) {
                clearSession()
                return@synchronized
            }
            check(!saved.getBoolean("writeFailed"))
            val checkpoint = store.read(saved)
            route.value = checkpoint.route
            definition.restore(checkpoint.definition)
            entry.restore(checkpoint.entry)
            recoveryProblem = null
            cleanup { store.pruneOwner(owner, requireNotNull(saved.getString("hash"))) }
        } catch (failure: Exception) {
            Log.e("WhipTrackRecovery", "Cannot recover Track editor checkpoint", failure)
            recoveryProblem = READ_PROBLEM
        }
    }

    private fun generationMatches(generation: Long): Boolean =
        (getApplication<Application>() as? WhipApplication)?.isCurrentUserDataGeneration(generation) ?: true

    private fun clearSession() {
        route.value = null
        definition.clear()
        entry.clear()
        cleanup { store.clearOwner(owner) }
        owner = UUID.randomUUID().toString()
        reference = null
        recoveryProblem = null
    }

    private fun cleanup(action: () -> Unit) {
        try {
            action()
        } catch (failure: Exception) {
            // Cleanup cannot turn an already saved Entry into an apparent failed Save.
            Log.w("WhipTrackRecovery", "Cannot remove obsolete Track checkpoint", failure)
        }
    }

    companion object {
        const val KEY = "track-editor-session"
        internal const val STATE_KEY = "track-editor-checkpoint"
        private const val WRITE_PROBLEM = "Your Track draft is still open, but Whip could not keep a recovery copy. Save your changes or retry before leaving the app."
        private const val READ_PROBLEM = "Whip could not recover the unsaved Track draft. Your saved Tracks and Entries are unchanged."
    }
}

internal data class TrackEditorCheckpoint(
    val route: TrackEditorRoute,
    val definition: TrackEditorState,
    val entry: TrackEntryEditorState,
    val version: Int = 1,
) : Serializable

/** Immutable files allow Android's last saved reference to survive an interrupted later write. */
internal class TrackEditorCheckpointStore(private val root: File) {
    fun write(owner: String, checkpoint: TrackEditorCheckpoint): Bundle {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(checkpoint) }
            output.toByteArray()
        }
        val hash = digest(bytes)
        val directory = ownerDirectory(owner)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create checkpoint directory" }
        val file = File(directory, hash)
        if (!file.isFile || !file.readBytes().contentEquals(bytes)) {
            val atomic = AtomicFile(file)
            val output = atomic.startWrite()
            try {
                output.write(bytes)
                output.fd.sync()
                atomic.finishWrite(output)
            } catch (failure: Exception) {
                atomic.failWrite(output)
                throw failure
            }
        }
        return Bundle().apply {
            putInt("version", 1)
            putString("owner", owner)
            putString("hash", hash)
            putLong("generation", checkpoint.route.openingDataGeneration)
        }
    }

    fun read(reference: Bundle): TrackEditorCheckpoint {
        require(reference.getInt("version") == 1) { "Unsupported checkpoint reference" }
        val hash = requireNotNull(reference.getString("hash"))
        require(hash.matches(Regex("[a-f0-9]{64}")))
        val file = File(ownerDirectory(requireNotNull(reference.getString("owner"))), hash)
        val bytes = file.readBytes()
        check(digest(bytes) == hash) { "Checkpoint checksum mismatch" }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        require(restored is TrackEditorCheckpoint && restored.version == 1)
        return restored
    }

    fun clearOwner(owner: String) {
        ownerDirectory(owner).deleteRecursively()
    }

    fun pruneOwner(owner: String, retainedHash: String) {
        ownerDirectory(owner).listFiles()?.filter { it.name != retainedHash }?.forEach { it.delete() }
    }

    private fun ownerDirectory(owner: String): File {
        require(UUID.fromString(owner).toString() == owner)
        return File(root, owner)
    }

    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
