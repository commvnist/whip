package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackEditorStateTest {
    @Test
    fun trackDefinitionRestoresAsOneTransaction() {
        val handle = SavedStateHandle()
        val original = TrackEditorViewModel(handle)
        original.initialize(
            "track-8",
            TrackDraft(
                name = "Books",
                fields = listOf(
                    TrackFieldDraft("Book", TrackFieldType.ShortText, required = true, primary = true, id = 1),
                    TrackFieldDraft("Author", TrackFieldType.ShortText, id = 2),
                ),
            ),
        )
        original.updateDraft { draft ->
            draft.copy(
                name = "Reading Log",
                fields = draft.fields.reversed(),
                tags = listOf("learning", "books"),
            )
        }
        original.update { state ->
            state.copy(
                confirmedFieldDeletes = setOf(9),
                confirmedOptionDeletes = setOf(14),
                optionReplacementIds = mapOf(15L to 16L),
            )
        }

        val restored = TrackEditorViewModel(handle).state.value
        assertEquals("Reading Log", restored.draft?.name)
        assertEquals(listOf("Author", "Book"), restored.draft?.fields?.map { it.name })
        assertEquals(listOf("learning", "books"), restored.draft?.tags)
        assertEquals(setOf(9L), restored.confirmedFieldDeletes)
        assertEquals(setOf(14L), restored.confirmedOptionDeletes)
        assertEquals(mapOf(15L to 16L), restored.optionReplacementIds)

        original.clear()
        assertNull(handle.get<TrackEditorState>("track-editor-state"))
    }

    @Test
    fun entryRestoresEveryTypedValueAndDateTogether() {
        val handle = SavedStateHandle()
        val original = TrackEntryEditorViewModel(handle)
        original.initialize(
            "entry-4-new",
            TrackEntryDraft(
                entryDate = LocalDate.of(2026, 8, 22),
                values = mapOf("title" to TrackValueDraft(textValue = "The Dispossessed")),
            ),
        )
        original.updateDraft { draft ->
            draft.copy(
                entryDate = LocalDate.of(2026, 8, 20),
                values = draft.values +
                    ("rating" to TrackValueDraft(scaleValue = 5.0)) +
                    ("notes" to TrackValueDraft(textValue = "Keep the whole draft")),
            )
        }

        val restored = TrackEntryEditorViewModel(handle).state.value.draft
        assertEquals(LocalDate.of(2026, 8, 20), restored?.entryDate)
        assertEquals(5.0, restored?.values?.get("rating")?.scaleValue)
        assertEquals("Keep the whole draft", restored?.values?.get("notes")?.textValue)

        original.clear()
        assertNull(handle.get<TrackEntryEditorState>("track-entry-editor-state"))
    }
}
