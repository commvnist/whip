package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import com.whip.app.core.OperationStatus
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.Track
import com.whip.app.domain.TrackChoiceOption
import com.whip.app.domain.TrackDefinitionBoundary
import com.whip.app.domain.TrackDefinitionRemovalReview
import com.whip.app.domain.TrackFieldRemovalImpact
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.DeletedTrackEntry
import com.whip.app.domain.TrackEntry
import com.whip.app.domain.TrackEntryBoundary
import com.whip.app.domain.TrackEntryConflictKind
import com.whip.app.domain.TrackEntryCreatePreparation
import com.whip.app.domain.TrackEntryCreateRequest
import com.whip.app.domain.TrackEntryEditSnapshot
import com.whip.app.domain.TrackEntryFieldContract
import com.whip.app.domain.TrackEntryFormBoundary
import com.whip.app.domain.TrackEntryFormSnapshot
import com.whip.app.domain.TrackEntryMutationKind
import com.whip.app.domain.TrackEntryMutationReceipt
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldValue
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun entryRouteAvailabilityUsesAtomicPreparationInsteadOfLaggingLiveFlow() {
        val snapshot = testEditSnapshot()
        val preparedRoute = TrackEditorRoute.Entry(
            trackId = 7,
            entryId = 41,
            openingEditSnapshot = snapshot,
            openingDataGeneration = 5,
            sessionId = 1,
        )

        assertEquals(
            TrackEntryRouteAvailability.Available,
            trackEntryRouteAvailability(preparedRoute, TrackUiState(loading = true)),
        )
        val archivedForm = snapshot.form.copy(
            boundary = snapshot.form.boundary.copy(writable = false),
            track = snapshot.form.track.copy(archived = true),
        )
        assertEquals(
            TrackEntryRouteAvailability.TrackArchived,
            trackEntryRouteAvailability(
                preparedRoute.copy(openingEditSnapshot = snapshot.copy(form = archivedForm)),
                TrackUiState(loading = true),
            ),
        )

        val unprepared = TrackEditorRoute.Entry(
            trackId = 7,
            entryId = 41,
            openingDataGeneration = 5,
            sessionId = 2,
        )
        assertEquals(
            TrackEntryRouteAvailability.Preparing,
            trackEntryRouteAvailability(unprepared, TrackUiState(loading = false)),
        )
        assertEquals(
            TrackEntryRouteAvailability.Preparing,
            trackEntryRouteAvailability(
                unprepared,
                TrackUiState(loading = false),
                TrackEntryPreparationUiState(
                    sessionId = 2,
                    trackId = 7,
                    entryId = 41,
                    loading = true,
                ),
            ),
        )
        assertEquals(
            TrackEntryRouteAvailability.EntryMissing,
            trackEntryRouteAvailability(
                unprepared,
                TrackUiState(loading = false),
                TrackEntryPreparationUiState(
                    sessionId = 2,
                    trackId = 7,
                    entryId = 41,
                    errorMessage = "Entry no longer exists",
                    conflictKind = TrackEntryConflictKind.TargetMissing,
                ),
            ),
        )
        assertEquals(
            TrackEntryRouteAvailability.TrackMissing,
            trackEntryRouteAvailability(
                unprepared.copy(entryId = null),
                TrackUiState(loading = false),
                TrackEntryPreparationUiState(
                    sessionId = 2,
                    trackId = 7,
                    entryId = null,
                    errorMessage = "Track no longer exists",
                    conflictKind = TrackEntryConflictKind.ParentMissing,
                ),
            ),
        )
        assertTrue(trackEditorRouteMatchesGeneration(preparedRoute, 5))
        assertTrue(!trackEditorRouteMatchesGeneration(preparedRoute, 6))
    }

    @Test
    fun entryMutationReceiptCanOnlyCloseItsExactRouteAndGeneration() {
        val snapshot = testEditSnapshot()
        val editRoute = TrackEditorRoute.Entry(
            trackId = 7,
            entryId = 41,
            openingEditSnapshot = snapshot,
            openingDataGeneration = 5,
            sessionId = 1,
        )
        val editReceipt = testEntryReceipt(TrackEntryMutationKind.Update)
        assertTrue(trackEntryReceiptMatchesRoute(editRoute, editReceipt, 5))
        assertTrue(trackEntryReceiptMatchesRoute(editRoute, editReceipt.copy(kind = TrackEntryMutationKind.Delete), 5))
        assertTrue(!trackEntryReceiptMatchesRoute(editRoute, editReceipt.copy(entryUuid = "replacement"), 5))
        assertTrue(!trackEntryReceiptMatchesRoute(editRoute, editReceipt.copy(entryId = 99), 5))
        assertTrue(!trackEntryReceiptMatchesRoute(editRoute, editReceipt, 6))

        val createPreparation = testCreatePreparation("new-entry-a")
        val createRoute = TrackEditorRoute.Entry(
            trackId = 7,
            openingCreatePreparation = createPreparation,
            openingDataGeneration = 5,
            sessionId = 2,
        )
        val createReceipt = testEntryReceipt(TrackEntryMutationKind.Create).copy(
            entryId = 42,
            entryUuid = "new-entry-a",
        )
        assertTrue(trackEntryReceiptMatchesRoute(createRoute, createReceipt, 5))
        assertTrue(!trackEntryReceiptMatchesRoute(createRoute, createReceipt.copy(entryUuid = "new-entry-b"), 5))
        assertTrue(!trackEntryReceiptMatchesRoute(createRoute, createReceipt.copy(kind = TrackEntryMutationKind.Update), 5))
    }

    @Test
    fun undoFailureCanOnlyRetryItsExactStillPendingDeletion() {
        val deleted = DeletedTrackEntry(
            entry = testEntry(),
            values = emptyList(),
            openingFormBoundary = testForm().boundary,
        )
        val failure = TrackEntryUndoUiState(
            token = 8,
            deletedEntry = deleted,
            status = OperationStatus.Failed("Restore failed"),
        )

        assertEquals(8L, trackEntryUndoRetryToken(failure))
        assertNull(trackEntryUndoRetryToken(failure.copy(deletedEntry = null)))
        assertNull(
            trackEntryUndoRetryToken(
                failure.copy(status = OperationStatus.Succeeded("Restored")),
            ),
        )
    }

    @Test
    fun admittingANewDeletionTerminallySupersedesThePreviousUndoSlot() {
        val deleted = DeletedTrackEntry(
            entry = testEntry(),
            values = emptyList(),
            openingFormBoundary = testForm().boundary,
        )

        val superseded = requireNotNull(
            supersededEntryUndoState(PendingTrackEntryUndo(token = 8, deletedEntry = deleted)),
        )

        assertEquals(8L, superseded.token)
        assertNull(superseded.deletedEntry)
        assertTrue(superseded.status is OperationStatus.Failed)
        assertTrue(
            (superseded.status as OperationStatus.Failed).message.contains(
                "another Entry deletion is finishing",
            ),
        )
        assertNull(trackEntryUndoRetryToken(superseded))

        val afterNewDeletion = entryUndoStateAfterNewDeletionRecorded(superseded)
        assertEquals(TrackEntryUndoUiState(), afterNewDeletion)

        val retryableFailure = TrackEntryUndoUiState(
            token = 9,
            deletedEntry = deleted,
            status = OperationStatus.Failed("Restore failed"),
        )
        assertEquals(
            retryableFailure,
            entryUndoStateAfterNewDeletionRecorded(retryableFailure),
        )
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

    @Test
    fun malformedOptionalNumberSurvivesRecreationWithoutBecomingAnIntentionalBlank() {
        val handle = SavedStateHandle()
        val editor = TrackEntryEditorViewModel(handle)
        editor.initialize(
            token = "entry-7-new-session-1",
            initialDraft = TrackEntryDraft(LocalDate.of(2026, 9, 1), emptyMap()),
            dataGeneration = 4,
        )

        editor.updateNumberValue("temperature", "-", "celsius")

        val restored = TrackEntryEditorViewModel(handle).state.value
        assertEquals("-", restored.rawNumberValues["temperature"])
        assertNull(restored.draft?.values?.get("temperature")?.enteredNumber)
        assertEquals("celsius", restored.draft?.values?.get("temperature")?.enteredUnitId)

        TrackEntryEditorViewModel(handle).initialize(
            token = "entry-7-new-session-2",
            initialDraft = TrackEntryDraft(LocalDate.of(2026, 9, 1), emptyMap()),
            dataGeneration = 4,
        )
        val nextSession = TrackEntryEditorViewModel(handle).state.value
        assertTrue(nextSession.rawNumberValues.isEmpty())
        assertTrue(nextSession.draft?.values.orEmpty().isEmpty())
    }

    @Test
    fun historyContentVersionDetectsSameTimestampValueAndChoiceChanges() {
        val field = testField()
        val entry = testEntry()
        val firstValue = TrackFieldValue(
            id = 1,
            uuid = "value-1",
            entryId = entry.id,
            fieldId = field.id,
            textValue = "Before",
            createdAtMillis = 100,
            updatedAtMillis = 100,
        )
        val first = TrackEntryPageContentVersion(
            fields = listOf(field),
            options = listOf(testOption(label = "Planned")),
            entries = listOf(TrackEntryProjection(entry, mapOf(field.id to firstValue))),
        )
        val changedValue = first.copy(
            entries = listOf(
                TrackEntryProjection(
                    entry,
                    mapOf(field.id to firstValue.copy(textValue = "After")),
                ),
            ),
        )
        val changedChoice = first.copy(options = listOf(testOption(label = "Completed")))

        assertNotEquals(first, changedValue)
        assertNotEquals(first, changedChoice)
    }

    private fun testTrack(archived: Boolean = false) = Track(
        id = 7,
        uuid = "track-7",
        name = "Books",
        description = "",
        icon = "📚",
        areaId = "",
        area = "",
        tags = emptyList(),
        pinned = false,
        archived = archived,
        position = 0,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun testField() = TrackField(
        id = 11,
        uuid = "title",
        trackId = 7,
        name = "Title",
        type = TrackFieldType.ShortText,
        position = 0,
        required = true,
        primary = true,
        showInList = true,
        dimension = null,
        unitId = null,
        precision = 1,
        scaleMin = null,
        scaleMax = null,
        scaleLowLabel = "",
        scaleHighLabel = "",
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun testOption(label: String) = TrackChoiceOption(
        id = 21,
        uuid = "choice-21",
        fieldId = 11,
        label = label,
        position = 0,
        createdAtMillis = 100,
        updatedAtMillis = 100,
    )

    private fun testForm(): TrackEntryFormSnapshot {
        val track = testTrack()
        val field = testField()
        val boundary = TrackEntryFormBoundary(
            trackId = track.id,
            trackUuid = track.uuid,
            trackCreatedAtMillis = track.createdAtMillis,
            writable = true,
            semanticRevisionToken = "form-r1",
            fieldContracts = listOf(
                TrackEntryFieldContract(
                    id = field.id,
                    uuid = field.uuid,
                    trackId = field.trackId,
                    name = field.name,
                    type = field.type,
                    required = field.required,
                    primary = field.primary,
                    dimension = field.dimension,
                    unitId = field.unitId,
                    precision = field.precision,
                    scaleMin = field.scaleMin,
                    scaleMax = field.scaleMax,
                    scaleLowLabel = field.scaleLowLabel,
                    scaleHighLabel = field.scaleHighLabel,
                    scaleStep = field.scaleStep,
                ),
            ),
        )
        return TrackEntryFormSnapshot(boundary, track, listOf(field), emptyList(), emptyList())
    }

    private fun testEntry() = TrackEntry(
        id = 41,
        uuid = "entry-41",
        trackId = 7,
        entryDate = LocalDate.of(2026, 9, 1),
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun testEditSnapshot(): TrackEntryEditSnapshot {
        val form = testForm()
        return TrackEntryEditSnapshot(
            boundary = TrackEntryBoundary(
                formBoundary = form.boundary,
                entryId = 41,
                entryUuid = "entry-41",
                entryCreatedAtMillis = 1,
                semanticRevisionToken = "entry-r1",
            ),
            form = form,
            draft = TrackEntryDraft(
                LocalDate.of(2026, 9, 1),
                mapOf("title" to TrackValueDraft(textValue = "The Dispossessed")),
            ),
            displayName = "The Dispossessed",
            populatedValueCount = 1,
        )
    }

    private fun testCreatePreparation(entryUuid: String) = TrackEntryCreatePreparation(
        request = TrackEntryCreateRequest(entryUuid, testForm().boundary),
        form = testForm(),
    )

    private fun testEntryReceipt(kind: TrackEntryMutationKind) = TrackEntryMutationReceipt(
        kind = kind,
        trackId = 7,
        trackUuid = "track-7",
        entryId = 41,
        entryUuid = "entry-41",
        changed = true,
        alreadyApplied = false,
        affectedValueCount = 1,
    )
}
