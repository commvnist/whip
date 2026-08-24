package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import java.io.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One durable Track-definition transaction. Keeping the structure and every
 * destructive decision together prevents Activity recreation from restoring a
 * title while silently reverting Field order, deletions, or Choice mappings.
 */
internal data class TrackEditorState(
    val token: String = "",
    val draft: TrackDraft? = null,
    val confirmedFieldDeletes: Set<Long> = emptySet(),
    val confirmedOptionDeletes: Set<Long> = emptySet(),
    val optionReplacementIds: Map<Long, Long> = emptyMap(),
) : Serializable

internal class TrackEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        savedStateHandle.get<TrackEditorState>(STATE_KEY) ?: TrackEditorState(),
    )
    val state = mutableState.asStateFlow()

    fun initialize(token: String, initialDraft: TrackDraft) {
        if (mutableState.value.token == token) return
        set(TrackEditorState(token = token, draft = initialDraft))
    }

    fun updateDraft(transform: (TrackDraft) -> TrackDraft) = update { current ->
        current.copy(draft = current.draft?.let(transform))
    }

    fun update(transform: (TrackEditorState) -> TrackEditorState) {
        set(transform(mutableState.value))
    }

    fun clear() {
        savedStateHandle.remove<TrackEditorState>(STATE_KEY)
        mutableState.value = TrackEditorState()
    }

    private fun set(value: TrackEditorState) {
        mutableState.value = value
        savedStateHandle[STATE_KEY] = value
    }

    private companion object {
        const val STATE_KEY = "track-editor-state"
    }
}

/** A complete Entry edit, including typed values and its effective date. */
internal data class TrackEntryEditorState(
    val token: String = "",
    val draft: TrackEntryDraft? = null,
) : Serializable

internal class TrackEntryEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        savedStateHandle.get<TrackEntryEditorState>(STATE_KEY) ?: TrackEntryEditorState(),
    )
    val state = mutableState.asStateFlow()

    fun initialize(token: String, initialDraft: TrackEntryDraft) {
        if (mutableState.value.token == token) return
        set(TrackEntryEditorState(token = token, draft = initialDraft))
    }

    fun updateDraft(transform: (TrackEntryDraft) -> TrackEntryDraft) {
        val draft = mutableState.value.draft ?: return
        set(mutableState.value.copy(draft = transform(draft)))
    }

    fun clear() {
        savedStateHandle.remove<TrackEntryEditorState>(STATE_KEY)
        mutableState.value = TrackEntryEditorState()
    }

    private fun set(value: TrackEntryEditorState) {
        mutableState.value = value
        savedStateHandle[STATE_KEY] = value
    }

    private companion object {
        const val STATE_KEY = "track-entry-editor-state"
    }
}
