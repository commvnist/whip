package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.whip.app.domain.TrackDefinitionBoundary
import com.whip.app.domain.TrackDefinitionRemovalReview
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.editableNumericValue
import com.whip.app.domain.toWhipDoubleOrNull
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
    val dataGeneration: Long = 0L,
    val draft: TrackDraft? = null,
    val openingBoundary: TrackDefinitionBoundary? = null,
    val removalReview: TrackDefinitionRemovalReview? = null,
    val optionReplacementIds: Map<Long, Long> = emptyMap(),
) : Serializable

internal class TrackEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        savedStateHandle.get<TrackEditorState>(STATE_KEY) ?: TrackEditorState(),
    )
    val state = mutableState.asStateFlow()

    fun initialize(token: String, initialDraft: TrackDraft, dataGeneration: Long = 0L) {
        if (
            mutableState.value.token == token &&
            mutableState.value.dataGeneration == dataGeneration
        ) return
        set(TrackEditorState(token = token, dataGeneration = dataGeneration, draft = initialDraft))
    }

    fun updateDraft(transform: (TrackDraft) -> TrackDraft) = update { current ->
        current.copy(
            draft = current.draft?.let(transform),
            removalReview = null,
        )
    }

    fun installOpeningBoundary(boundary: TrackDefinitionBoundary) = update { current ->
        if (current.draft == null || boundary.trackId <= 0L) current
        else current.copy(openingBoundary = boundary)
    }

    fun installRemovalReview(review: TrackDefinitionRemovalReview) = update { current ->
        if (current.openingBoundary?.trackId != review.trackId) current
        else current.copy(removalReview = review)
    }

    fun clearRemovalReview() = update { it.copy(removalReview = null) }

    fun clearOptionReplacements() = update {
        it.copy(removalReview = null, optionReplacementIds = emptyMap())
    }

    fun updateOptionReplacement(optionId: Long, replacementId: Long?) = update { current ->
        current.copy(
            optionReplacementIds = if (replacementId == null) {
                current.optionReplacementIds - optionId
            } else {
                current.optionReplacementIds + (optionId to replacementId)
            },
        )
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
    val dataGeneration: Long = 0L,
    val draft: TrackEntryDraft? = null,
    /**
     * User-authored Number text is retained separately from its parsed value so
     * an optional but malformed value cannot be mistaken for an intentional blank.
     */
    val rawNumberValues: Map<String, String> = emptyMap(),
) : Serializable

internal class TrackEntryEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        savedStateHandle.get<TrackEntryEditorState>(STATE_KEY) ?: TrackEntryEditorState(),
    )
    val state = mutableState.asStateFlow()

    fun initialize(token: String, initialDraft: TrackEntryDraft, dataGeneration: Long = 0L) {
        if (
            mutableState.value.token == token &&
            mutableState.value.dataGeneration == dataGeneration
        ) return
        set(
            TrackEntryEditorState(
                token = token,
                dataGeneration = dataGeneration,
                draft = initialDraft,
                rawNumberValues = initialDraft.values.mapNotNull { (fieldUuid, value) ->
                    value.enteredNumber?.let { fieldUuid to editableNumericValue(it) }
                }.toMap(),
            ),
        )
    }

    fun updateDraft(transform: (TrackEntryDraft) -> TrackEntryDraft) {
        val draft = mutableState.value.draft ?: return
        set(mutableState.value.copy(draft = transform(draft)))
    }

    fun updateNumberValue(fieldUuid: String, rawText: String, enteredUnitId: String?) {
        val draft = mutableState.value.draft ?: return
        val current = draft.values[fieldUuid]
        set(
            mutableState.value.copy(
                draft = draft.copy(
                    values = draft.values + (
                        fieldUuid to (current ?: com.whip.app.domain.TrackValueDraft()).copy(
                            enteredNumber = rawText.toWhipDoubleOrNull(),
                            enteredUnitId = enteredUnitId,
                        )
                    ),
                ),
                rawNumberValues = mutableState.value.rawNumberValues + (fieldUuid to rawText),
            ),
        )
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
