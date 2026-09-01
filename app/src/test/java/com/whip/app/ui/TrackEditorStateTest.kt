package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.Track
import com.whip.app.domain.TrackDefinitionBoundary
import com.whip.app.domain.TrackDefinitionRemovalReview
import com.whip.app.domain.TrackFieldRemovalImpact
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackEntry
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.TrackProjection
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackEditorStateTest {
    @Test
    fun definitionConflictCanOnlyReachTheExactOwningEditorSession() {
        val boundary = TrackDefinitionBoundary(8, "track-8", 1, "revision")
        val current = TrackDefinitionReviewUiState(
            sessionId = 22,
            trackId = 8,
            boundary = boundary,
        )

        assertTrue(definitionConflictBelongsToEditor(current, 22, 8, boundary))
        assertTrue(!definitionConflictBelongsToEditor(current, 21, 8, boundary))
        assertTrue(!definitionConflictBelongsToEditor(current, 22, 9, boundary))
        assertTrue(
            !definitionConflictBelongsToEditor(
                current,
                22,
                8,
                boundary.copy(semanticRevisionToken = "newer"),
            ),
        )
    }

    @Test
    fun entryEditRouteNeverFallsBackToAddWhenItsTrackOrEntryIsMissing() {
        val projection = TrackProjection(
            track = Track(7, "track-7", "Books", "", "📚", "", "", emptyList(), false, false, 0, 1, 1),
            fields = emptyList(),
            options = emptyList(),
            entries = listOf(
                TrackEntryProjection(
                    entry = TrackEntry(
                        id = 41,
                        uuid = "entry-41",
                        trackId = 7,
                        entryDate = LocalDate.of(2026, 9, 1),
                        createdAtMillis = 1,
                        updatedAtMillis = 1,
                    ),
                    values = emptyMap(),
                ),
            ),
        )
        val state = TrackUiState(projections = listOf(projection), loading = false)

        assertEquals(
            TrackEntryRouteAvailability.Available,
            trackEntryRouteAvailability(TrackEditorRoute.Entry(7, 41, 5, 1), state),
        )
        assertEquals(
            TrackEntryRouteAvailability.EntryMissing,
            trackEntryRouteAvailability(TrackEditorRoute.Entry(7, 99, 5, 2), state),
        )
        assertEquals(
            TrackEntryRouteAvailability.TrackMissing,
            trackEntryRouteAvailability(TrackEditorRoute.Entry(8, 41, 5, 3), state),
        )
        assertTrue(trackEditorRouteMatchesGeneration(TrackEditorRoute.Entry(7, 41, 5, 1), 5))
        assertTrue(!trackEditorRouteMatchesGeneration(TrackEditorRoute.Entry(7, 41, 5, 1), 6))
    }

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
                openingBoundary = TrackDefinitionBoundary(8, "track-8", 1, "definition-revision"),
                removalReview = TrackDefinitionRemovalReview(
                    trackId = 8,
                    definitionRevisionToken = "definition-revision",
                    removalRevisionToken = "removal-revision",
                    removedFields = listOf(
                        TrackFieldRemovalImpact(9, "field-9", "Notes", 3, 0, 0, 0, 0, 0),
                    ),
                    removedChoices = emptyList(),
                ),
                optionReplacementIds = mapOf(15L to 16L),
            )
        }

        val restored = TrackEditorViewModel(handle).state.value
        assertEquals("Reading Log", restored.draft?.name)
        assertEquals(listOf("Author", "Book"), restored.draft?.fields?.map { it.name })
        assertEquals(listOf("learning", "books"), restored.draft?.tags)
        assertEquals("definition-revision", restored.openingBoundary?.semanticRevisionToken)
        assertEquals("removal-revision", restored.removalReview?.removalRevisionToken)
        assertEquals(mapOf(15L to 16L), restored.optionReplacementIds)

        original.clear()
        assertNull(handle.get<TrackEditorState>("track-editor-state"))
    }

    @Test
    fun restoredTrackWithSameIdCannotInheritDefinitionDraftFromEarlierGeneration() {
        val handle = SavedStateHandle()
        val beforeRestore = TrackEditorViewModel(handle)
        beforeRestore.initialize(
            "track-8",
            TrackDraft(name = "Original", fields = emptyList()),
            dataGeneration = 2L,
        )
        beforeRestore.updateDraft { it.copy(name = "Unsaved old definition") }

        val afterProcessRecreation = TrackEditorViewModel(handle)
        afterProcessRecreation.initialize(
            "track-8",
            TrackDraft(name = "Restored definition", fields = emptyList()),
            dataGeneration = 3L,
        )

        assertEquals(3L, afterProcessRecreation.state.value.dataGeneration)
        assertEquals("Restored definition", afterProcessRecreation.state.value.draft?.name)
        assertNull(afterProcessRecreation.state.value.openingBoundary)
        assertNull(afterProcessRecreation.state.value.removalReview)
    }

    @Test
    fun editingTheDraftInvalidatesExactRemovalAuthorizationButKeepsUserReplacementIntent() {
        val handle = SavedStateHandle()
        val editor = TrackEditorViewModel(handle)
        editor.initialize(
            "track-8",
            TrackDraft(
                name = "Books",
                fields = listOf(TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true)),
            ),
        )
        val boundary = TrackDefinitionBoundary(8, "track-8", 1, "definition-revision")
        editor.installOpeningBoundary(boundary)
        editor.updateOptionReplacement(14, 15)
        editor.installRemovalReview(
            TrackDefinitionRemovalReview(
                trackId = 8,
                definitionRevisionToken = boundary.semanticRevisionToken,
                removalRevisionToken = "removal-revision",
                removedFields = emptyList(),
                removedChoices = emptyList(),
                choiceReplacementIds = mapOf(14L to 15L),
            ),
        )

        editor.updateDraft { it.copy(name = "Reading") }

        assertNull(editor.state.value.removalReview)
        assertEquals(mapOf(14L to 15L), editor.state.value.optionReplacementIds)
        assertEquals(boundary, editor.state.value.openingBoundary)
    }

    @Test
    fun replacementChangeRetainsImpactForDisplayButNoLongerMatchesItsAuthorizationPlan() {
        val editor = TrackEditorViewModel(SavedStateHandle())
        editor.initialize(
            "track-8",
            TrackDraft(
                name = "Books",
                fields = listOf(TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true)),
            ),
        )
        val boundary = TrackDefinitionBoundary(8, "track-8", 1, "definition-revision")
        val review = TrackDefinitionRemovalReview(
            trackId = 8,
            definitionRevisionToken = boundary.semanticRevisionToken,
            removalRevisionToken = "removal-revision",
            removedFields = emptyList(),
            removedChoices = emptyList(),
            choiceReplacementIds = emptyMap(),
        )
        editor.installOpeningBoundary(boundary)
        editor.installRemovalReview(review)

        editor.updateOptionReplacement(14, 15)

        assertEquals(review, editor.state.value.removalReview)
        assertTrue(editor.state.value.removalReview?.choiceReplacementIds != editor.state.value.optionReplacementIds)

        editor.clearOptionReplacements()

        assertTrue(editor.state.value.optionReplacementIds.isEmpty())
        assertNull(editor.state.value.removalReview)
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

    @Test
    fun restoredEntryWithSameIdCannotInheritTypedValuesFromEarlierGeneration() {
        val handle = SavedStateHandle()
        val beforeRestore = TrackEntryEditorViewModel(handle)
        beforeRestore.initialize(
            "entry-4-9",
            TrackEntryDraft(
                entryDate = LocalDate.of(2026, 8, 31),
                values = mapOf("name" to TrackValueDraft(textValue = "Original")),
            ),
            dataGeneration = 11L,
        )
        beforeRestore.updateDraft {
            it.copy(values = mapOf("name" to TrackValueDraft(textValue = "Unsaved old value")))
        }

        val afterProcessRecreation = TrackEntryEditorViewModel(handle)
        afterProcessRecreation.initialize(
            "entry-4-9",
            TrackEntryDraft(
                entryDate = LocalDate.of(2026, 8, 31),
                values = mapOf("name" to TrackValueDraft(textValue = "Restored value")),
            ),
            dataGeneration = 12L,
        )

        assertEquals(12L, afterProcessRecreation.state.value.dataGeneration)
        assertEquals("Restored value", afterProcessRecreation.state.value.draft?.values?.get("name")?.textValue)
    }
}
