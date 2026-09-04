package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.UnitDefinitionEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.TrackChoiceOptionDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryConflictException
import com.whip.app.domain.TrackEntryConflictKind
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDimension
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackEntryIntegrityTest {
    private lateinit var database: WhipDatabase
    private lateinit var tracks: RoomTrackRepository

    @Before
    fun setUp() = runBlocking {
        TestClock.instant = Instant.parse("2026-09-01T12:00:00Z")
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).addCallback(WhipDatabase.integrityGuardCallback).build()
        val ids = TestIds()
        RoomAreaRepository(database, TestClock, ids).ensureDefaultArea()
        tracks = RoomTrackRepository(database, TestClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test
    fun stableCreateIsIdempotentButDifferentContentOrTrackCollidesAndFreshIdentityDuplicates() = runBlocking {
        val trackId = tracks.create(textTrack())
        val preparation = requireNotNull(tracks.prepareEntryCreate(trackId))
        val draft = titleDraft(preparation.form.fields.single { it.primary }.uuid, "  Exact history  ")

        val first = tracks.addEntry(preparation.request, draft)
        val retry = tracks.addEntry(preparation.request, draft.copy(values = draft.values.mapValues { TrackValueDraft(textValue = "Exact history") }))
        assertTrue(first.changed)
        assertTrue(retry.alreadyApplied)
        assertEquals(first.entryId, retry.entryId)

        val collision = runCatching {
            tracks.addEntry(
                preparation.request,
                titleDraft(preparation.form.fields.single { it.primary }.uuid, "Different"),
            )
        }.exceptionOrNull()
        assertEntryConflict(TrackEntryConflictKind.IdentityCollision, collision)

        val duplicate = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            titleDraft(preparation.form.fields.single { it.primary }.uuid, "Exact history"),
        )
        assertNotEquals(first.entryId, duplicate.entryId)

        val otherTrack = tracks.create(textTrack().copy(name = "Other"))
        val other = requireNotNull(tracks.prepareEntryCreate(otherTrack))
        val otherCollision = runCatching {
            tracks.addEntry(
                other.request.copy(entryUuid = preparation.request.entryUuid),
                titleDraft(other.form.fields.single { it.primary }.uuid, "Exact history"),
            )
        }.exceptionOrNull()
        assertEntryConflict(TrackEntryConflictKind.IdentityCollision, otherCollision)
    }

    @Test
    fun committedCreateRetrySurvivesNewRequiredFieldAndArchive() = runBlocking {
        val trackId = tracks.create(textTrack())
        val preparation = requireNotNull(tracks.prepareEntryCreate(trackId))
        val draft = titleDraft(preparation.form.fields.single { it.primary }.uuid, "Committed")
        val first = tracks.addEntry(preparation.request, draft)
        val current = requireNotNull(tracks.projection(trackId))
        tracks.update(
            trackId,
            current.toDraft().copy(
                fields = current.toDraft().fields + TrackFieldDraft("New required", TrackFieldType.YesNo, required = true),
            ),
            requireNotNull(tracks.definitionBoundary(trackId)),
        )
        tracks.setArchived(trackId, true)

        val retry = tracks.addEntry(preparation.request, draft)

        assertTrue(retry.alreadyApplied)
        assertEquals(first.entryId, retry.entryId)
        assertEquals(1, requireNotNull(tracks.projection(trackId)).entries.size)
    }

    @Test
    fun staleEntryUpdateRejectsWithoutErasingConcurrentOptionalValue() = runBlocking {
        val trackId = tracks.create(textTrack())
        val projection = requireNotNull(tracks.projection(trackId))
        val title = projection.fields.single { it.name == "Title" }
        val notes = projection.fields.single { it.name == "Notes" }
        val entryId = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            TrackEntryDraft(TestClock.today(), mapOf(title.uuid to TrackValueDraft(textValue = "Before"))),
        ).entryId
        val stale = requireNotNull(tracks.prepareEntryEdit(entryId))
        val concurrent = requireNotNull(tracks.prepareEntryEdit(entryId))
        val concurrentDraft = concurrent.draft.copy(
            values = concurrent.draft.values + (notes.uuid to TrackValueDraft(textValue = "Do not erase")),
        )
        tracks.updateEntry(concurrent.boundary, concurrentDraft)

        val failure = runCatching {
            tracks.updateEntry(
                stale.boundary,
                stale.draft.copy(values = stale.draft.values + (title.uuid to TrackValueDraft(textValue = "Stale title"))),
            )
        }.exceptionOrNull()

        assertEntryConflict(TrackEntryConflictKind.EntryChanged, failure)
        val current = requireNotNull(tracks.projection(trackId)).entries.single()
        assertEquals("Before", current.value(title.id)?.textValue)
        assertEquals("Do not erase", current.value(notes.id)?.textValue)
    }

    @Test
    fun achievedUpdateRetryWinsBeforeLaterFormChangeAndArchiveButNewMutationDoesNot() = runBlocking {
        val trackId = tracks.create(textTrack())
        var projection = requireNotNull(tracks.projection(trackId))
        val title = projection.fields.single { it.name == "Title" }
        val entryId = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            titleDraft(title.uuid, "Before"),
        ).entryId
        val opening = requireNotNull(tracks.prepareEntryEdit(entryId))
        val intended = opening.draft.copy(
            values = opening.draft.values + (title.uuid to TrackValueDraft(textValue = "After")),
        )
        tracks.updateEntry(opening.boundary, intended)
        projection = requireNotNull(tracks.projection(trackId))
        val renamed = projection.toDraft().copy(
            fields = projection.toDraft().fields.map { field ->
                if (field.id == title.id) field.copy(name = "Renamed Title") else field
            },
        )
        tracks.update(trackId, renamed, requireNotNull(tracks.definitionBoundary(trackId)))
        tracks.setArchived(trackId, true)

        val retry = tracks.updateEntry(opening.boundary, intended)
        assertTrue(retry.alreadyApplied)

        val changedAgain = intended.copy(
            values = intended.values + (title.uuid to TrackValueDraft(textValue = "Another mutation")),
        )
        val failure = runCatching { tracks.updateEntry(opening.boundary, changedAgain) }.exceptionOrNull()
        assertEntryConflict(TrackEntryConflictKind.FormChanged, failure)
        assertEquals("After", requireNotNull(tracks.projection(trackId)).entries.single().value(title.id)?.textValue)
    }

    @Test
    fun malformedTypePayloadAndChangedSelectedUnitAreRejectedWithoutRows() = runBlocking {
        val trackId = tracks.create(textTrack())
        val preparation = requireNotNull(tracks.prepareEntryCreate(trackId))
        val titleUuid = preparation.form.fields.single { it.primary }.uuid
        val malformed = runCatching {
            tracks.addEntry(
                preparation.request,
                TrackEntryDraft(
                    TestClock.today(),
                    mapOf(titleUuid to TrackValueDraft(textValue = "Text", booleanValue = true)),
                ),
            )
        }.exceptionOrNull()
        assertTrue(malformed is IllegalArgumentException)
        assertTrue(requireNotNull(tracks.projection(trackId)).entries.isEmpty())

        val custom = UnitDefinitionEntity(
            id = "tablet",
            name = "servings",
            symbol = "srv",
            dimension = UnitDimension.Count.name,
            toCanonicalFactor = 1.0,
            toCanonicalOffset = 0.0,
            custom = true,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        database.measurementDao().upsertUnit(custom)
        val numberTrack = tracks.create(numberTrack())
        val numberPreparation = requireNotNull(tracks.prepareEntryCreate(numberTrack))
        val numberFields = numberPreparation.form.fields.associateBy { it.name }
        assertEquals("servings", numberPreparation.request.openingFormBoundary.unitContracts.single { it.id == custom.id }.name)
        database.measurementDao().upsertUnit(custom.copy(name = "tablets", symbol = "tab", updatedAtMillis = 2))
        assertEquals("tablets", requireNotNull(database.measurementDao().getUnit(custom.id)).name)
        val unitFailure = runCatching {
            tracks.addEntry(
                numberPreparation.request,
                TrackEntryDraft(
                    TestClock.today(),
                    mapOf(
                        requireNotNull(numberFields["Title"]).uuid to TrackValueDraft(textValue = "Dose"),
                        requireNotNull(numberFields["Amount"]).uuid to TrackValueDraft(enteredNumber = 2.0, enteredUnitId = custom.id),
                    ),
                ),
            )
        }.exceptionOrNull()
        assertEntryConflict(TrackEntryConflictKind.FormChanged, unitFailure)
        assertTrue(requireNotNull(tracks.projection(numberTrack)).entries.isEmpty())

        database.measurementDao().upsertUnit(
            custom.copy(
                name = "overflow units",
                symbol = "ovf",
                toCanonicalFactor = Double.MAX_VALUE,
                updatedAtMillis = 3,
            ),
        )
        val overflowPreparation = requireNotNull(tracks.prepareEntryCreate(numberTrack))
        val overflowFields = overflowPreparation.form.fields.associateBy { it.name }
        val overflow = runCatching {
            tracks.addEntry(
                overflowPreparation.request,
                TrackEntryDraft(
                    TestClock.today(),
                    mapOf(
                        requireNotNull(overflowFields["Title"]).uuid to TrackValueDraft(textValue = "Overflow"),
                        requireNotNull(overflowFields["Amount"]).uuid to TrackValueDraft(
                            enteredNumber = 2.0,
                            enteredUnitId = custom.id,
                        ),
                    ),
                ),
            )
        }.exceptionOrNull()
        assertTrue(overflow is IllegalArgumentException)
        assertTrue(requireNotNull(tracks.projection(numberTrack)).entries.isEmpty())
    }

    @Test
    fun staleDeleteRejectsAndMissingDeleteIsOutcomeUnknown() = runBlocking {
        val trackId = tracks.create(textTrack())
        val title = requireNotNull(tracks.projection(trackId)).primaryField
        val entryId = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            titleDraft(title.uuid, "Delete carefully"),
        ).entryId
        val staleDelete = requireNotNull(tracks.prepareEntryEdit(entryId))
        val editor = requireNotNull(tracks.prepareEntryEdit(entryId))
        tracks.updateEntry(
            editor.boundary,
            editor.draft.copy(values = editor.draft.values + (title.uuid to TrackValueDraft(textValue = "Changed"))),
        )

        assertEntryConflict(
            TrackEntryConflictKind.EntryChanged,
            runCatching { tracks.deleteEntry(staleDelete.boundary) }.exceptionOrNull(),
        )
        val current = requireNotNull(tracks.prepareEntryEdit(entryId))
        val deleted = tracks.deleteEntry(current.boundary)
        assertTrue(deleted.changed)
        assertNotNull(deleted.deletedEntry)
        assertEntryConflict(
            TrackEntryConflictKind.OutcomeUnknown,
            runCatching { tracks.deleteEntry(current.boundary) }.exceptionOrNull(),
        )
    }

      @Test
    fun restoreRetryRejectsSameEntryIdentityUnderReincarnatedTrack() = runBlocking {
        val trackId = tracks.create(textTrack())
        val title = requireNotNull(tracks.projection(trackId)).primaryField
        val draft = titleDraft(title.uuid, "Old Track history")
        val entryId = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            draft,
        ).entryId
        val deleted = requireNotNull(
            tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(entryId)).boundary).deletedEntry,
        )
        val originalTrack = requireNotNull(database.trackDao().getTrack(trackId))
        database.trackDao().updateTrack(
            originalTrack.copy(
                uuid = "reincarnated-track",
                createdAtMillis = originalTrack.createdAtMillis + 1,
            ),
        )
        val reincarnated = requireNotNull(tracks.prepareEntryCreate(trackId))
        val collidingEntry = tracks.addEntry(
            reincarnated.request.copy(entryUuid = deleted.entry.uuid),
            draft,
        )

        assertEntryConflict(
            TrackEntryConflictKind.IdentityCollision,
            runCatching { tracks.restoreEntry(deleted) }.exceptionOrNull(),
        )
        assertEquals(
            collidingEntry.entryId,
            requireNotNull(database.trackDao().getEntryByUuid(deleted.entry.uuid)).id,
        )
    }

     @Test
    fun undoAllowsUnitRenameAndArchiveButRejectsConversionChange() = runBlocking {
        val custom = UnitDefinitionEntity(
            id = "dose-unit",
            name = "servings",
            symbol = "srv",
            dimension = UnitDimension.Count.name,
            toCanonicalFactor = 2.0,
            toCanonicalOffset = 1.0,
            custom = true,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        database.measurementDao().upsertUnit(custom)
        val trackId = tracks.create(numberTrack())
        val preparation = requireNotNull(tracks.prepareEntryCreate(trackId))
        val fields = preparation.form.fields.associateBy { it.name }
        val entryId = tracks.addEntry(
            preparation.request,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    requireNotNull(fields["Title"]).uuid to TrackValueDraft(textValue = "Dose"),
                    requireNotNull(fields["Amount"]).uuid to TrackValueDraft(enteredNumber = 2.0, enteredUnitId = custom.id),
                ),
            ),
        ).entryId
        val firstSnapshot = requireNotNull(
            tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(entryId)).boundary).deletedEntry,
        )
        database.measurementDao().upsertUnit(
            custom.copy(name = "tablets", symbol = "tab", archived = true, updatedAtMillis = 2),
        )

        val restored = tracks.restoreEntry(firstSnapshot)
        assertTrue(restored.changed)
        val secondSnapshot = requireNotNull(
            tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(restored.entryId)).boundary).deletedEntry,
        )
        database.measurementDao().upsertUnit(
            custom.copy(
                name = "tablets",
                symbol = "tab",
                archived = true,
                toCanonicalFactor = 3.0,
                updatedAtMillis = 3,
            ),
        )

        assertEntryConflict(
            TrackEntryConflictKind.RestoreIncompatible,
            runCatching { tracks.restoreEntry(secondSnapshot) }.exceptionOrNull(),
        )
        assertNull(database.trackDao().getEntryByUuid(secondSnapshot.entry.uuid))
    }

    @Test
    fun everyEntryValueTypeRoundTripsThroughPreparationMutationAndUndo() = runBlocking {
        val trackId = tracks.create(allTypesTrack())
        val preparation = requireNotNull(tracks.prepareEntryCreate(trackId))
        val fields = preparation.form.fields.associateBy { it.name }
        val selectedChoice = preparation.form.options.single { it.label == "Ready" }
        val valueDate = LocalDate.of(2026, 8, 31)
        val draft = TrackEntryDraft(
            entryDate = TestClock.today(),
            values = mapOf(
                requireNotNull(fields["Title"]).uuid to TrackValueDraft(textValue = "  Complete shape  "),
                requireNotNull(fields["Amount"]).uuid to TrackValueDraft(enteredNumber = 3.0),
                requireNotNull(fields["Status"]).uuid to TrackValueDraft(choiceOptionUuid = selectedChoice.uuid),
                requireNotNull(fields["Rating"]).uuid to TrackValueDraft(scaleValue = 3.5),
                requireNotNull(fields["When"]).uuid to TrackValueDraft(dateValue = valueDate),
                requireNotNull(fields["Confirmed"]).uuid to TrackValueDraft(booleanValue = false),
            ),
        )

        val created = tracks.addEntry(preparation.request, draft)
        val edit = requireNotNull(tracks.prepareEntryEdit(created.entryId))
        assertEquals("Complete shape", edit.draft.values.getValue(requireNotNull(fields["Title"]).uuid).textValue)
        assertEquals("count", edit.draft.values.getValue(requireNotNull(fields["Amount"]).uuid).enteredUnitId)
        assertEquals(selectedChoice.uuid, edit.draft.values.getValue(requireNotNull(fields["Status"]).uuid).choiceOptionUuid)
        assertEquals(3.5, edit.draft.values.getValue(requireNotNull(fields["Rating"]).uuid).scaleValue)
        assertEquals(valueDate, edit.draft.values.getValue(requireNotNull(fields["When"]).uuid).dateValue)
        assertEquals(false, edit.draft.values.getValue(requireNotNull(fields["Confirmed"]).uuid).booleanValue)

        val deleted = requireNotNull(tracks.deleteEntry(edit.boundary).deletedEntry)
        val restored = tracks.restoreEntry(deleted)
        assertEquals(6, restored.affectedValueCount)
        assertEquals(draft.entryDate, requireNotNull(tracks.prepareEntryEdit(restored.entryId)).draft.entryDate)
    }

    @Test
    fun createRejectsBlankIdentityMissingOrChangedParentChangedFormAndArchivedTrack() = runBlocking {
        val blankTrack = tracks.create(textTrack().copy(name = "Blank identity"))
        val blank = requireNotNull(tracks.prepareEntryCreate(blankTrack))
        assertTrue(
            runCatching {
                tracks.addEntry(
                    blank.request.copy(entryUuid = " "),
                    titleDraft(blank.form.fields.single { it.primary }.uuid, "No identity"),
                )
            }.exceptionOrNull() is IllegalArgumentException,
        )

        val missingTrack = tracks.create(textTrack().copy(name = "Missing parent"))
        val missing = requireNotNull(tracks.prepareEntryCreate(missingTrack))
        assertEquals(1, database.trackDao().deleteTrack(missingTrack))
        assertEntryConflict(
            TrackEntryConflictKind.ParentMissing,
            runCatching {
                tracks.addEntry(missing.request, titleDraft(missing.form.fields.single { it.primary }.uuid, "Missing"))
            }.exceptionOrNull(),
        )

        val identityTrack = tracks.create(textTrack().copy(name = "Changed identity"))
        val identity = requireNotNull(tracks.prepareEntryCreate(identityTrack))
        val identityRow = requireNotNull(database.trackDao().getTrack(identityTrack))
        database.trackDao().updateTrack(identityRow.copy(uuid = "replacement-parent", createdAtMillis = identityRow.createdAtMillis + 1))
        assertEntryConflict(
            TrackEntryConflictKind.IdentityChanged,
            runCatching {
                tracks.addEntry(identity.request, titleDraft(identity.form.fields.single { it.primary }.uuid, "Identity"))
            }.exceptionOrNull(),
        )

        val changedTrack = tracks.create(textTrack().copy(name = "Changed form"))
        val changed = requireNotNull(tracks.prepareEntryCreate(changedTrack))
        val changedProjection = requireNotNull(tracks.projection(changedTrack))
        tracks.update(
            changedTrack,
            changedProjection.toDraft().copy(
                fields = changedProjection.toDraft().fields + TrackFieldDraft("Added", TrackFieldType.YesNo),
            ),
            requireNotNull(tracks.definitionBoundary(changedTrack)),
        )
        assertEntryConflict(
            TrackEntryConflictKind.FormChanged,
            runCatching {
                tracks.addEntry(changed.request, titleDraft(changed.form.fields.single { it.primary }.uuid, "Stale"))
            }.exceptionOrNull(),
        )

        val archivedTrack = tracks.create(textTrack().copy(name = "Archived"))
        tracks.setArchived(archivedTrack, true)
        val archived = requireNotNull(tracks.prepareEntryCreate(archivedTrack))
        assertEntryConflict(
            TrackEntryConflictKind.FormChanged,
            runCatching {
                tracks.addEntry(archived.request, titleDraft(archived.form.fields.single { it.primary }.uuid, "Archived"))
            }.exceptionOrNull(),
        )
    }

    @Test
    fun updateAndDeleteRejectMissingReidentifiedParentsAndChangedFormsWithoutMutation() = runBlocking {
        val trackId = tracks.create(textTrack().copy(name = "Mutation conflicts"))
        val title = requireNotNull(tracks.projection(trackId)).primaryField
        val firstId = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            titleDraft(title.uuid, "First"),
        ).entryId
        val first = requireNotNull(tracks.prepareEntryEdit(firstId))
        assertEntryConflict(
            TrackEntryConflictKind.IdentityChanged,
            runCatching {
                tracks.updateEntry(first.boundary.copy(entryId = Long.MAX_VALUE), first.draft)
            }.exceptionOrNull(),
        )
        assertEquals(1, database.trackDao().deleteEntry(firstId))
        assertEntryConflict(
            TrackEntryConflictKind.TargetMissing,
            runCatching { tracks.updateEntry(first.boundary, first.draft) }.exceptionOrNull(),
        )

        val secondId = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            titleDraft(title.uuid, "Second"),
        ).entryId
        val staleDelete = requireNotNull(tracks.prepareEntryEdit(secondId))
        val projection = requireNotNull(tracks.projection(trackId))
        tracks.update(
            trackId,
            projection.toDraft().copy(
                fields = projection.toDraft().fields.map { field ->
                    if (field.id == title.id) field.copy(name = "Changed Title") else field
                },
            ),
            requireNotNull(tracks.definitionBoundary(trackId)),
        )
        assertEntryConflict(
            TrackEntryConflictKind.EntryChanged,
            runCatching { tracks.deleteEntry(staleDelete.boundary) }.exceptionOrNull(),
        )
        assertNotNull(database.trackDao().getEntry(secondId))

        val current = requireNotNull(tracks.prepareEntryEdit(secondId))
        val trackRow = requireNotNull(database.trackDao().getTrack(trackId))
        database.trackDao().updateTrack(trackRow.copy(uuid = "reidentified-mutation-parent"))
        assertEntryConflict(
            TrackEntryConflictKind.IdentityChanged,
            runCatching { tracks.updateEntry(current.boundary, current.draft.copy(entryDate = current.draft.entryDate.minusDays(1))) }.exceptionOrNull(),
        )
        assertNotNull(database.trackDao().getEntry(secondId))
    }

    @Test
    fun restoreRejectsMissingReidentifiedParentAndDifferentEntryHistory() = runBlocking {
        val missingTrack = tracks.create(textTrack().copy(name = "Restore missing"))
        val missingTitle = requireNotNull(tracks.projection(missingTrack)).primaryField
        val missingEntry = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(missingTrack)).request,
            titleDraft(missingTitle.uuid, "Missing parent"),
        ).entryId
        val missingSnapshot = requireNotNull(
            tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(missingEntry)).boundary).deletedEntry,
        )
        assertEquals(1, database.trackDao().deleteTrack(missingTrack))
        assertEntryConflict(
            TrackEntryConflictKind.ParentMissing,
            runCatching { tracks.restoreEntry(missingSnapshot) }.exceptionOrNull(),
        )

        val changedTrack = tracks.create(textTrack().copy(name = "Restore identity"))
        val changedTitle = requireNotNull(tracks.projection(changedTrack)).primaryField
        val changedEntry = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(changedTrack)).request,
            titleDraft(changedTitle.uuid, "Changed parent"),
        ).entryId
        val changedSnapshot = requireNotNull(
            tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(changedEntry)).boundary).deletedEntry,
        )
        val changedRow = requireNotNull(database.trackDao().getTrack(changedTrack))
        database.trackDao().updateTrack(changedRow.copy(createdAtMillis = changedRow.createdAtMillis + 1))
        assertEntryConflict(
            TrackEntryConflictKind.IdentityChanged,
            runCatching { tracks.restoreEntry(changedSnapshot) }.exceptionOrNull(),
        )

        val collisionTrack = tracks.create(textTrack().copy(name = "Restore collision"))
        val collisionTitle = requireNotNull(tracks.projection(collisionTrack)).primaryField
        val collisionEntry = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(collisionTrack)).request,
            titleDraft(collisionTitle.uuid, "Original"),
        ).entryId
        val collisionSnapshot = requireNotNull(
            tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(collisionEntry)).boundary).deletedEntry,
        )
        val replacementRequest = requireNotNull(tracks.prepareEntryCreate(collisionTrack)).request
        tracks.addEntry(
            replacementRequest.copy(entryUuid = collisionSnapshot.entry.uuid),
            titleDraft(collisionTitle.uuid, "Replacement"),
        )
        assertEntryConflict(
            TrackEntryConflictKind.IdentityCollision,
            runCatching { tracks.restoreEntry(collisionSnapshot) }.exceptionOrNull(),
        )
    }

    @Test
    fun restoreRejectsMalformedSnapshotAndLiveValueIdentityCollisionAtomically() = runBlocking {
        val trackId = tracks.create(textTrack().copy(name = "Malformed Undo"))
        val fields = requireNotNull(tracks.projection(trackId)).fields.associateBy { it.name }
        val entryId = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    requireNotNull(fields["Title"]).uuid to TrackValueDraft(textValue = "History"),
                    requireNotNull(fields["Notes"]).uuid to TrackValueDraft(textValue = "Details"),
                ),
            ),
        ).entryId
        val snapshot = requireNotNull(
            tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(entryId)).boundary).deletedEntry,
        )
        val first = snapshot.values[0]
        val second = snapshot.values[1]

        assertTrue(
            runCatching {
                tracks.restoreEntry(snapshot.copy(values = listOf(first, second.copy(uuid = first.uuid))))
            }.exceptionOrNull() is IllegalArgumentException,
        )
        assertTrue(
            runCatching {
                tracks.restoreEntry(snapshot.copy(values = listOf(first, second.copy(fieldId = first.fieldId))))
            }.exceptionOrNull() is IllegalArgumentException,
        )
        assertEntryConflict(
            TrackEntryConflictKind.RestoreIncompatible,
            runCatching {
                tracks.restoreEntry(
                    snapshot.copy(
                        openingFormBoundary = snapshot.openingFormBoundary.copy(
                            fieldContracts = snapshot.openingFormBoundary.fieldContracts.filterNot { it.id == first.fieldId },
                        ),
                    ),
                )
            }.exceptionOrNull(),
        )
        assertEntryConflict(
            TrackEntryConflictKind.RestoreIncompatible,
            runCatching {
                tracks.restoreEntry(snapshot.copy(values = listOf(first.copy(booleanValue = true), second)))
            }.exceptionOrNull(),
        )

        val liveEntry = tracks.addEntry(
            requireNotNull(tracks.prepareEntryCreate(trackId)).request,
            titleDraft(requireNotNull(fields["Title"]).uuid, "Live"),
        ).entryId
        val liveValue = database.trackDao().getValues(liveEntry).single()
        assertEntryConflict(
            TrackEntryConflictKind.IdentityCollision,
            runCatching {
                tracks.restoreEntry(snapshot.copy(values = listOf(first.copy(uuid = liveValue.uuid), second)))
            }.exceptionOrNull(),
        )
        assertNull(database.trackDao().getEntryByUuid(snapshot.entry.uuid))
        assertNotNull(database.trackDao().getEntry(liveEntry))
    }

    @Test
    fun restoreValidatesChoiceAndScaleBeforeWriting() = runBlocking {
        val typedTrack = tracks.create(allTypesTrack().copy(name = "Typed Undo"))
        val preparation = requireNotNull(tracks.prepareEntryCreate(typedTrack))
        val fields = preparation.form.fields.associateBy { it.name }
        val choice = preparation.form.options.single { it.label == "Ready" }
        val typedEntry = tracks.addEntry(
            preparation.request,
            TrackEntryDraft(
                TestClock.today(),
                mapOf(
                    requireNotNull(fields["Title"]).uuid to TrackValueDraft(textValue = "Typed"),
                    requireNotNull(fields["Status"]).uuid to TrackValueDraft(choiceOptionUuid = choice.uuid),
                    requireNotNull(fields["Rating"]).uuid to TrackValueDraft(scaleValue = 4.5),
                ),
            ),
        ).entryId
        val typedSnapshot = requireNotNull(
            tracks.deleteEntry(requireNotNull(tracks.prepareEntryEdit(typedEntry)).boundary).deletedEntry,
        )
        val choiceRow = requireNotNull(database.trackDao().getOption(choice.id))
        database.trackDao().updateOption(choiceRow.copy(uuid = "reidentified-choice"))
        assertEntryConflict(
            TrackEntryConflictKind.RestoreIncompatible,
            runCatching { tracks.restoreEntry(typedSnapshot) }.exceptionOrNull(),
        )

        database.trackDao().updateOption(choiceRow)
        val scaleField = requireNotNull(database.trackDao().getField(requireNotNull(fields["Rating"]).id))
        database.trackDao().updateField(scaleField.copy(scaleMax = 4))
        assertEntryConflict(
            TrackEntryConflictKind.RestoreIncompatible,
            runCatching { tracks.restoreEntry(typedSnapshot) }.exceptionOrNull(),
        )
    }

       private fun textTrack() = TrackDraft(
        name = "Entry integrity",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft("Notes", TrackFieldType.LongText),
        ),
    )

    private fun numberTrack() = TrackDraft(
        name = "Number integrity",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft(
                "Amount",
                TrackFieldType.Number,
                dimension = UnitDimension.Count,
                unitId = "count",
            ),
        ),
    )

    private fun allTypesTrack() = TrackDraft(
        name = "All Entry types",
        fields = listOf(
            TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
            TrackFieldDraft(
                "Amount",
                TrackFieldType.Number,
                dimension = UnitDimension.Count,
                unitId = "count",
            ),
            TrackFieldDraft(
                "Status",
                TrackFieldType.SingleChoice,
                options = listOf(TrackChoiceOptionDraft("Ready"), TrackChoiceOptionDraft("Waiting")),
            ),
            TrackFieldDraft("Rating", TrackFieldType.Scale, scaleMin = 1, scaleMax = 5, scaleStep = 0.5),
            TrackFieldDraft("When", TrackFieldType.Date),
            TrackFieldDraft("Confirmed", TrackFieldType.YesNo),
        ),
    )

    private fun titleDraft(fieldUuid: String, title: String) = TrackEntryDraft(
        entryDate = TestClock.today(),
        values = mapOf(fieldUuid to TrackValueDraft(textValue = title)),
    )

    private fun TrackProjection.toDraft() = TrackDraft(
        name = track.name,
        description = track.description,
        icon = track.icon,
        areaId = track.areaId,
        area = track.area,
        tags = track.tags,
        fields = fields.map { field ->
            TrackFieldDraft(
                name = field.name,
                type = field.type,
                required = field.required,
                primary = field.primary,
                showInList = field.showInList,
                dimension = field.dimension,
                unitId = field.unitId,
                precision = field.precision,
                scaleMin = field.scaleMin,
                scaleMax = field.scaleMax,
                scaleLowLabel = field.scaleLowLabel,
                scaleHighLabel = field.scaleHighLabel,
                scaleStep = field.scaleStep,
                options = optionsFor(field.id).map { option ->
                    TrackChoiceOptionDraft(option.label, option.uuid, option.id)
                },
                uuid = field.uuid,
                id = field.id,
            )
        },
    )

    private fun assertEntryConflict(kind: TrackEntryConflictKind, error: Throwable?) {
        assertTrue("Expected $kind but was $error", error is TrackEntryConflictException)
        assertEquals(kind, (error as TrackEntryConflictException).kind)
    }

    private object TestClock : WhipClock {
        var instant: Instant = Instant.parse("2026-09-01T12:00:00Z")
        override fun now(): Instant = instant
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 9, 1)
    }

    private class TestIds : WhipIdGenerator {
        private val next = AtomicInteger()
        override fun nextId(): String = "entry-integrity-${next.incrementAndGet()}"
    }

    companion object {
        private val fixtureIds = AtomicInteger()
    }
}
