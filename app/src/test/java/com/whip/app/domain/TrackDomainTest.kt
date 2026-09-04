package com.whip.app.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackDomainTest {
    @Test fun definitionMutationContractsRemainProcessSaveableAndReportExactImpact() {
        val boundary = TrackDefinitionBoundary(8, "track-8", 11, "definition-revision")
        val fieldImpact = TrackFieldRemovalImpact(
            fieldId = 21,
            fieldUuid = "field-21",
            fieldName = "Notes",
            savedValueCount = 3,
            childChoiceCount = 2,
            connectedLinkSourceCount = 1,
            connectedLinkConditionCount = 2,
            connectedTriggerConditionCount = 3,
            connectedTriggerMappingCount = 4,
        )
        val choiceImpact = TrackChoiceRemovalImpact(
            optionId = 31,
            optionUuid = "choice-31",
            fieldId = 22,
            fieldName = "Genre",
            optionLabel = "History",
            savedValueCount = 5,
            replacementOptionId = 32,
            replacementOptionLabel = "Fiction",
            connectedLinkConditionCount = 2,
            connectedTriggerConditionCount = 3,
            connectedTriggerMappingCount = 4,
            removedWithField = false,
        )
        val review = TrackDefinitionRemovalReview(
            trackId = boundary.trackId,
            definitionRevisionToken = boundary.semanticRevisionToken,
            removalRevisionToken = "removal-revision",
            removedFields = listOf(fieldImpact),
            removedChoices = listOf(choiceImpact),
            choiceReplacementIds = mapOf(choiceImpact.optionId to requireNotNull(choiceImpact.replacementOptionId)),
        )
        val restoredReview = serializedRoundTrip(review)

        assertEquals("track-8", boundary.trackUuid)
        assertEquals(11, boundary.trackCreatedAtMillis)
        assertTrue(restoredReview.hasRemovals)
        assertEquals("removal-revision", restoredReview.removalRevisionToken)
        assertEquals(3, restoredReview.removedFields.single().connectedLinkReferenceCount)
        assertEquals(7, restoredReview.removedFields.single().connectedTriggerReferenceCount)
        assertTrue(restoredReview.removedChoices.single().replacesSavedValues)
        assertEquals(2, restoredReview.removedChoices.single().connectedLinkReferenceCount)
        assertEquals(7, restoredReview.removedChoices.single().connectedTriggerReferenceCount)
        assertEquals("Fiction", restoredReview.removedChoices.single().replacementOptionLabel)
        assertFalse(review.copy(removedFields = emptyList(), removedChoices = emptyList()).hasRemovals)

        val receipt = serializedRoundTrip(
            TrackDefinitionSaveReceipt(
                trackId = 8,
                schemaChanged = true,
                removedFieldCount = 1,
                removedChoiceCount = 1,
                deletedValueCount = 3,
                replacedValueCount = 5,
                connectedLinkReferenceCount = 4,
                connectedTriggerReferenceCount = 7,
                warnings = listOf("Tag suggestions did not refresh"),
            ),
        )
        assertTrue(receipt.schemaChanged)
        assertEquals(1, receipt.removedFieldCount)
        assertEquals(1, receipt.removedChoiceCount)
        assertEquals(3, receipt.deletedValueCount)
        assertEquals(5, receipt.replacedValueCount)
        assertEquals(4, receipt.connectedLinkReferenceCount)
        assertEquals(7, receipt.connectedTriggerReferenceCount)
        assertEquals(1, receipt.warnings.size)

        val conflict = TrackDefinitionConflictException(
            TrackDefinitionConflictKind.RemovalImpactChanged,
            "Review again",
        )
        assertEquals(TrackDefinitionConflictKind.RemovalImpactChanged, conflict.kind)
        assertEquals("Review again", conflict.message)
    }

    @Test fun scaleSupportsUserDefinedFractionalIncrementsWithoutRoundingEntries() {
        assertEquals(
            listOf(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0),
            trackScaleValues(minimum = 1, maximum = 5, increment = 0.5),
        )
        assertEquals(3.5, normalizeTrackScaleValue(3.5, 1, 5, 0.5))
        assertNull(normalizeTrackScaleValue(3.6, 1, 5, 0.5))
        assertEquals(
            listOf(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()),
            trackScaleValues(Int.MIN_VALUE, Int.MAX_VALUE, 4_294_967_295.0),
        )

        val validated = TrackDraft(
            name = "Films",
            fields = listOf(
                TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true),
                TrackFieldDraft("Rating", TrackFieldType.Scale, scaleMin = 1, scaleMax = 5, scaleStep = 0.5),
            ),
        ).validated()
        assertEquals(1, validated.fields.single { it.name == "Rating" }.precision)

        assertThrows(IllegalArgumentException::class.java) {
            trackScaleValues(minimum = 1, maximum = 5, increment = 0.3)
        }
    }

    @Test fun schemaSupportsCompositeTypedEntryIdentityWithoutAFieldLimit() {
        val fields = buildList {
            add(TrackFieldDraft("Opening", TrackFieldType.ShortText, required = true, primary = true))
            add(TrackFieldDraft("Variation", TrackFieldType.LongText, primary = true, showInList = true))
            repeat(98) { add(TrackFieldDraft("Field ${it + 3}", TrackFieldType.LongText)) }
        }
        val validated = TrackDraft("Chess Openings", fields = fields).validated()
        assertEquals(100, validated.fields.size)
        assertEquals(2, validated.fields.count(TrackFieldDraft::primary))
        assertTrue(validated.fields[1].required)
        assertTrue(validated.fields[1].showInList)
        assertThrows(IllegalArgumentException::class.java) {
            TrackDraft("Broken", fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, required = true))).validated()
        }
    }

    @Test fun typedConditionsRespectAndOrBlankAndEntryDateSemantics() {
        val track = sampleProjection()
        val genre = track.fields.first { it.name == "Genre" }
        val rating = track.fields.first { it.name == "Rating" }
        val notes = track.fields.first { it.name == "Notes" }
        val recommend = track.fields.first { it.name == "Recommend" }
        val history = track.options.first { it.label == "History" }

        val conditions = listOf(
            TrackCondition(genre.uuid, TrackConditionOperator.Is, choiceOptionUuids = setOf(history.uuid)),
            TrackCondition(rating.uuid, TrackConditionOperator.AtLeast, numberValue = 4.0),
            TrackCondition(TRACK_ENTRY_DATE_CONDITION_UUID, TrackConditionOperator.OnOrAfter, dateValue = LocalDate.of(2026, 1, 1)),
        )
        assertEquals(listOf("A"), track.matchingEntries(conditions).map(track::primaryText))
        assertEquals(2, track.matchingEntries(conditions, TrackConditionMode.MatchAny).size)

        assertEquals(2, track.matchingEntries(listOf(TrackCondition(notes.uuid, TrackConditionOperator.IsBlank))).size)
        assertEquals(1, track.matchingEntries(listOf(TrackCondition(notes.uuid, TrackConditionOperator.Contains, textValue = "great"))).size)
        assertEquals(1, track.matchingEntries(listOf(TrackCondition(recommend.uuid, TrackConditionOperator.IsYes))).size)
        assertEquals(1, track.matchingEntries(listOf(TrackCondition(recommend.uuid, TrackConditionOperator.IsNo))).size)
        assertEquals(1, track.matchingEntries(listOf(TrackCondition(recommend.uuid, TrackConditionOperator.IsUnanswered))).size)
        assertEquals(2, track.matchingEntries(listOf(TrackCondition(rating.uuid, TrackConditionOperator.Between, numberValue = 3.0, secondNumberValue = 5.0))).size)
        assertEquals(1, track.matchingEntries(listOf(TrackCondition(rating.uuid, TrackConditionOperator.LessThan, numberValue = 3.0))).size)
    }

    @Test fun compositeIdentityUsesAllSelectedFieldsInStableOrder() {
        val base = sampleProjection()
        val composite = base.copy(fields = base.fields.map { field ->
            if (field.name == "Genre") field.copy(primary = true, required = true, showInList = true) else field
        })
        assertEquals("A · History", composite.primaryText(composite.entries.first()))
        assertEquals("a\u001fhistory", composite.identityKey(composite.entries.first()))
        assertThrows(IllegalArgumentException::class.java) {
            previewTrackCsvImport(
                projection = composite.copy(entries = emptyList()),
                csv = "Title\nA",
                mapping = TrackCsvMapping(fieldColumns = mapOf(composite.primaryFields.first().uuid to "Title")),
                today = LocalDate.of(2026, 8, 23),
            )
        }
    }

    @Test fun everyAggregationHandlesMissingValuesWithoutInventingZero() {
        val track = sampleProjection()
        val rating = track.fields.first { it.name == "Rating" }
        assertEquals(3.0, track.aggregate(TrackAggregation.CountEntries).value)
        assertEquals(9.0, track.aggregate(TrackAggregation.Sum, rating.uuid).value)
        assertEquals(3.0, track.aggregate(TrackAggregation.Average, rating.uuid).value)
        assertEquals(2.0, track.aggregate(TrackAggregation.Latest, rating.uuid).value)
        assertEquals(2.0, track.aggregate(TrackAggregation.Minimum, rating.uuid).value)
        assertEquals(4.0, track.aggregate(TrackAggregation.Maximum, rating.uuid).value)
        assertEquals(7.5, track.aggregate(TrackAggregation.FixedAmount, fixedCanonicalValue = 2.5).value)
        val noValues = track.copy(entries = track.entries.map { it.copy(values = it.values - rating.id) })
        assertNull(noValues.aggregate(TrackAggregation.Sum, rating.uuid).value)
        assertEquals(3, noValues.aggregate(TrackAggregation.Sum, rating.uuid).skippedEntryCount)
    }

    @Test fun csvParserAndImportPreviewHandleQuotesChoicesTypesAndInvalidRows() {
        val track = sampleProjection().copy(entries = emptyList())
        val csv = """Title,Genre,Rating,Notes,Recommend,Entry Date
"A, quoted",History,4,"A ""great"" book",Yes,2026-01-02
B,Unknown,nope,,Maybe,wrong
""".trimIndent()
        val mapping = TrackCsvMapping(
            entryDateColumn = "Entry Date",
            fieldColumns = track.fields.associate { it.uuid to it.name.replace("Title", "Title") },
        )
        val preview = previewTrackCsvImport(track, csv, mapping, LocalDate.of(2026, 8, 23))
        assertEquals(2, preview.totalRows)
        assertEquals(1, preview.validRows)
        assertEquals(1, preview.invalidRows)
        assertEquals("A, quoted", preview.validDrafts.single().values.getValue(track.primaryField.uuid).textValue)
        assertTrue(preview.issues.any { it.message.contains("choose one of") })
        assertTrue(preview.issues.any { it.message.contains("enter a number") })
    }

    @Test fun csvRoundTripMappingPreservesCompatibleEnteredNumberUnits() {
        val base = sampleProjection().copy(entries = emptyList())
        val rating = base.fields.first { it.name == "Rating" }
        val track = base.copy(fields = base.fields.map { field ->
            if (field.id == rating.id) field.copy(dimension = UnitDimension.Mass, unitId = "kilogram") else field
        })
        val csv = "Title,Rating (Entered),Rating (Unit)\nLoaded carry,22,pound"
        val preview = previewTrackCsvImport(
            projection = track,
            csv = csv,
            mapping = TrackCsvMapping(
                fieldColumns = mapOf(track.primaryField.uuid to "Title", rating.uuid to "Rating (Entered)"),
                numberUnitColumns = mapOf(rating.uuid to "Rating (Unit)"),
            ),
            today = LocalDate.of(2026, 8, 23),
        )
        assertEquals(1, preview.validRows)
        assertEquals("pound", preview.validDrafts.single().values.getValue(rating.uuid).enteredUnitId)
    }

    @Test fun tenThousandEntriesRemainDeterministic() {
        val base = sampleProjection().copy(entries = emptyList())
        val rating = base.fields.first { it.name == "Rating" }
        val entries = (1..10_000).map { index ->
            TrackEntryProjection(
                TrackEntry(index.toLong(), "entry-$index", 1, LocalDate.of(2026, 1, 1).plusDays((index % 365).toLong()), createdAtMillis = index.toLong(), updatedAtMillis = index.toLong()),
                mapOf(rating.id to TrackFieldValue(index.toLong(), "value-$index", index.toLong(), rating.id, canonicalNumber = (index % 5 + 1).toDouble(), enteredNumber = (index % 5 + 1).toDouble(), enteredUnitId = "unitless", createdAtMillis = index.toLong(), updatedAtMillis = index.toLong())),
            )
        }
        val projection = base.copy(entries = entries)
        assertEquals(10_000.0, projection.aggregate(TrackAggregation.CountEntries).value)
        assertEquals(30_000.0, projection.aggregate(TrackAggregation.Sum, rating.uuid).value)
        assertEquals(2_000, projection.matchingEntries(listOf(TrackCondition(rating.uuid, TrackConditionOperator.Equals, numberValue = 5.0))).size)
    }

    private fun sampleProjection(): TrackProjection {
        val title = field(1, "title", "Title", TrackFieldType.ShortText, primary = true)
        val genre = field(2, "genre", "Genre", TrackFieldType.SingleChoice)
        val rating = field(3, "rating", "Rating", TrackFieldType.Number, dimension = UnitDimension.Unitless, unitId = "unitless")
        val notes = field(4, "notes", "Notes", TrackFieldType.LongText)
        val recommend = field(5, "recommend", "Recommend", TrackFieldType.YesNo)
        val history = TrackChoiceOption(1, "history", genre.id, "History", 0, 1, 1)
        val fiction = TrackChoiceOption(2, "fiction", genre.id, "Fiction", 1, 1, 1)
        fun entry(id: Long, titleValue: String, option: TrackChoiceOption, score: Double, date: LocalDate, note: String?, yes: Boolean?) =
            TrackEntryProjection(
                TrackEntry(id, "entry-$id", 1, date, createdAtMillis = id, updatedAtMillis = id),
                buildMap {
                    put(title.id, TrackFieldValue(id * 10, "title-$id", id, title.id, textValue = titleValue, createdAtMillis = id, updatedAtMillis = id))
                    put(genre.id, TrackFieldValue(id * 10 + 1, "genre-$id", id, genre.id, choiceOptionId = option.id, createdAtMillis = id, updatedAtMillis = id))
                    put(rating.id, TrackFieldValue(id * 10 + 2, "rating-$id", id, rating.id, enteredNumber = score, canonicalNumber = score, enteredUnitId = "unitless", createdAtMillis = id, updatedAtMillis = id))
                    note?.let { put(notes.id, TrackFieldValue(id * 10 + 3, "notes-$id", id, notes.id, textValue = it, createdAtMillis = id, updatedAtMillis = id)) }
                    yes?.let { put(recommend.id, TrackFieldValue(id * 10 + 4, "yes-$id", id, recommend.id, booleanValue = it, createdAtMillis = id, updatedAtMillis = id)) }
                },
            )
        return TrackProjection(
            Track(1, "track", "Books", "", "▤", "main", "Main", emptyList(), false, false, 0, 1, 1),
            listOf(title, genre, rating, notes, recommend),
            listOf(history, fiction),
            listOf(
                entry(1, "A", history, 4.0, LocalDate.of(2026, 1, 2), "A great book", true),
                entry(2, "B", fiction, 3.0, LocalDate.of(2025, 12, 1), null, false),
                entry(3, "C", history, 2.0, LocalDate.of(2026, 2, 1), null, null),
            ),
        )
    }

    private fun field(
        id: Long,
        uuid: String,
        name: String,
        type: TrackFieldType,
        primary: Boolean = false,
        dimension: UnitDimension? = null,
        unitId: String? = null,
    ) = TrackField(id, uuid, 1, name, type, id.toInt(), required = primary, primary = primary, showInList = false, dimension = dimension, unitId = unitId, precision = 1, scaleMin = null, scaleMax = null, scaleLowLabel = "", scaleHighLabel = "", createdAtMillis = 1, updatedAtMillis = 1)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> serializedRoundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(value) }
            output.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
    }
}
