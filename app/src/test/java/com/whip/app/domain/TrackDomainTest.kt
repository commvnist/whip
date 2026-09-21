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
    @Test fun choiceDuplicateFeedbackMatchesCanonicalValidationAndKeepsIdentities() {
        val options = listOf(
            TrackChoiceOptionDraft(" River   trail ", uuid = "one"),
            TrackChoiceOptionDraft("RIVER trail", uuid = "two"),
            TrackChoiceOptionDraft("Street", uuid = "three"),
        )
        assertEquals(setOf(0, 1), duplicateTrackChoiceLabelIndices(options))
        val draft = TrackDraft("Walks", fields = listOf(TrackFieldDraft("Terrain", TrackFieldType.SingleChoice, primary = true, options = options)))
        assertThrows(IllegalArgumentException::class.java) { draft.validated() }
        val corrected = options.toMutableList().also { it[1] = it[1].copy(label = "Hill trail") }
        assertTrue(duplicateTrackChoiceLabelIndices(corrected).isEmpty())
        val valid = draft.copy(fields = listOf(draft.fields.single().copy(options = corrected))).validated()
        assertEquals(listOf("River trail", "Hill trail", "Street"), valid.fields.single().options.map { it.label })
        assertEquals(options.map { it.uuid }, valid.fields.single().options.map { it.uuid })
        assertTrue(duplicateTrackChoiceLabelIndices(listOf(TrackChoiceOptionDraft(""), TrackChoiceOptionDraft(" "))).isEmpty())
    }

    @Test fun definitionMutationContractsRemainProcessSaveableAndReportExactImpact() {
        val boundary = TrackDefinitionBoundary(8, "track-8", 11, "definition-revision")
        val fieldImpact = TrackFieldRemovalImpact(
            fieldId = 21,
            fieldUuid = "field-21",
            fieldName = "Notes",
            savedValueCount = 3,
            childChoiceCount = 2,
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
        assertTrue(restoredReview.removedChoices.single().replacesSavedValues)
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
                warnings = listOf("Tag suggestions did not refresh"),
            ),
        )
        assertTrue(receipt.schemaChanged)
        assertEquals(1, receipt.removedFieldCount)
        assertEquals(1, receipt.removedChoiceCount)
        assertEquals(3, receipt.deletedValueCount)
        assertEquals(5, receipt.replacedValueCount)
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
        val savedValues = listOf(1.0, 2.5, 3.5, 5.0)
        assertNull(incompatibleTrackScaleValue(savedValues, -2, 5, 0.25))
        assertEquals(2.5, incompatibleTrackScaleValue(savedValues, 1, 5, 1.0))
        assertEquals(5.0, incompatibleTrackScaleValue(savedValues, 1, 4, 0.5))
        assertEquals(1.0, incompatibleTrackScaleValue(savedValues, 2, 5, 0.5))
        assertNull(incompatibleTrackScaleValue(listOf(1.0 - 1e-8, 5.0 + 1e-8), 1, 5, 0.5))
        assertEquals(1.0 - 1e-4, incompatibleTrackScaleValue(listOf(1.0 - 1e-4), 1, 5, 0.5))
        assertNull(incompatibleTrackScaleValue(emptyList(), 1, 5, 0.5))
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

    @Test fun everyTypedConditionOperatorHandlesBoundariesMissingValuesAndReverseRanges() {
        val track = sampleProjection()
        val title = track.fields.first { it.name == "Title" }
        val genre = track.fields.first { it.name == "Genre" }
        val rating = track.fields.first { it.name == "Rating" }
        val notes = track.fields.first { it.name == "Notes" }
        val recommend = track.fields.first { it.name == "Recommend" }
        val finished = track.fields.first { it.name == "Finished" }
        val effort = track.fields.first { it.name == "Effort" }
        val history = track.options.first { it.label == "History" }
        val fiction = track.options.first { it.label == "Fiction" }
        fun names(condition: TrackCondition, projection: TrackProjection = track) =
            projection.matchingEntries(listOf(condition)).map(projection::primaryText)

        assertEquals(listOf("A"), names(TrackCondition(title.uuid, TrackConditionOperator.Is, textValue = "a")))
        assertEquals(listOf("B", "C"), names(TrackCondition(title.uuid, TrackConditionOperator.IsNot, textValue = "A")))
        assertEquals(listOf("A"), names(TrackCondition(notes.uuid, TrackConditionOperator.Contains, textValue = "GREAT")))
        assertEquals(listOf("B", "C"), names(TrackCondition(title.uuid, TrackConditionOperator.DoesNotContain, textValue = "a")))
        assertEquals(listOf("B", "C"), names(TrackCondition(notes.uuid, TrackConditionOperator.IsBlank)))
        assertEquals(listOf("A"), names(TrackCondition(notes.uuid, TrackConditionOperator.IsNotBlank)))

        assertEquals(listOf("A", "C"), names(TrackCondition(genre.uuid, TrackConditionOperator.Is, choiceOptionUuids = setOf(history.uuid))))
        assertEquals(listOf("B"), names(TrackCondition(genre.uuid, TrackConditionOperator.IsNot, choiceOptionUuids = setOf(history.uuid))))
        assertEquals(listOf("A", "B", "C"), names(TrackCondition(genre.uuid, TrackConditionOperator.IsOneOf, choiceOptionUuids = setOf(history.uuid, fiction.uuid))))

        assertEquals(listOf("B"), names(TrackCondition(rating.uuid, TrackConditionOperator.Equals, numberValue = 3.0)))
        assertEquals(listOf("A", "C"), names(TrackCondition(rating.uuid, TrackConditionOperator.NotEqual, numberValue = 3.0)))
        assertEquals(listOf("A"), names(TrackCondition(rating.uuid, TrackConditionOperator.GreaterThan, numberValue = 3.0)))
        assertEquals(listOf("A", "B"), names(TrackCondition(rating.uuid, TrackConditionOperator.AtLeast, numberValue = 3.0)))
        assertEquals(listOf("C"), names(TrackCondition(rating.uuid, TrackConditionOperator.LessThan, numberValue = 3.0)))
        assertEquals(listOf("B", "C"), names(TrackCondition(rating.uuid, TrackConditionOperator.AtMost, numberValue = 3.0)))
        assertEquals(listOf("A", "B", "C"), names(TrackCondition(rating.uuid, TrackConditionOperator.Between, numberValue = 4.0, secondNumberValue = 2.0)))
        assertEquals(emptyList<String>(), names(TrackCondition(rating.uuid, TrackConditionOperator.Equals)))

        assertEquals(listOf("A"), names(TrackCondition(effort.uuid, TrackConditionOperator.Equals, numberValue = 4.5)))
        assertEquals(listOf("A", "B"), names(TrackCondition(effort.uuid, TrackConditionOperator.Between, numberValue = 4.5, secondNumberValue = 2.0)))
        assertEquals(listOf("C"), names(TrackCondition(effort.uuid, TrackConditionOperator.IsBlank)))

        val firstFinished = LocalDate.of(2026, 1, 10)
        assertEquals(listOf("A"), names(TrackCondition(finished.uuid, TrackConditionOperator.On, dateValue = firstFinished)))
        assertEquals(listOf("B"), names(TrackCondition(finished.uuid, TrackConditionOperator.Before, dateValue = firstFinished)))
        assertEquals(listOf("A", "B"), names(TrackCondition(finished.uuid, TrackConditionOperator.OnOrBefore, dateValue = firstFinished)))
        assertEquals(listOf("A"), names(TrackCondition(finished.uuid, TrackConditionOperator.After, dateValue = LocalDate.of(2026, 1, 1))))
        assertEquals(listOf("A"), names(TrackCondition(finished.uuid, TrackConditionOperator.OnOrAfter, dateValue = firstFinished)))
        assertEquals(listOf("A", "B"), names(TrackCondition(finished.uuid, TrackConditionOperator.Between, dateValue = firstFinished, secondDateValue = LocalDate.of(2025, 12, 10))))
        assertEquals(listOf("C"), names(TrackCondition(finished.uuid, TrackConditionOperator.IsBlank)))

        assertEquals(listOf("A"), names(TrackCondition(recommend.uuid, TrackConditionOperator.IsYes)))
        assertEquals(listOf("B"), names(TrackCondition(recommend.uuid, TrackConditionOperator.IsNo)))
        assertEquals(listOf("C"), names(TrackCondition(recommend.uuid, TrackConditionOperator.IsUnanswered)))
        assertEquals(listOf("A", "B"), names(TrackCondition(recommend.uuid, TrackConditionOperator.IsAnswered)))

        assertEquals(listOf("A"), names(TrackCondition(TRACK_ENTRY_DATE_CONDITION_UUID, TrackConditionOperator.On, dateValue = LocalDate.of(2026, 1, 2))))
        assertEquals(listOf("B"), names(TrackCondition(TRACK_ENTRY_DATE_CONDITION_UUID, TrackConditionOperator.Before, dateValue = LocalDate.of(2026, 1, 1))))
        assertEquals(listOf("A", "C"), names(TrackCondition(TRACK_ENTRY_DATE_CONDITION_UUID, TrackConditionOperator.OnOrAfter, dateValue = LocalDate.of(2026, 1, 1))))
        assertEquals(listOf("A", "B", "C"), names(TrackCondition(TRACK_ENTRY_DATE_CONDITION_UUID, TrackConditionOperator.Between, dateValue = LocalDate.of(2026, 2, 1), secondDateValue = LocalDate.of(2025, 12, 1))))
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
            fieldColumns = track.fields
                .filter { it.name in setOf("Title", "Genre", "Rating", "Notes", "Recommend") }
                .associate { it.uuid to it.name },
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
        val finished = field(6, "finished", "Finished", TrackFieldType.Date)
        val effort = field(7, "effort", "Effort", TrackFieldType.Scale)
        val history = TrackChoiceOption(1, "history", genre.id, "History", 0, 1, 1)
        val fiction = TrackChoiceOption(2, "fiction", genre.id, "Fiction", 1, 1, 1)
        fun entry(
            id: Long,
            titleValue: String,
            option: TrackChoiceOption,
            score: Double,
            date: LocalDate,
            note: String?,
            yes: Boolean?,
            finishedDate: LocalDate?,
            effortValue: Double?,
        ) =
            TrackEntryProjection(
                TrackEntry(id, "entry-$id", 1, date, createdAtMillis = id, updatedAtMillis = id),
                buildMap {
                    put(title.id, TrackFieldValue(id * 10, "title-$id", id, title.id, textValue = titleValue, createdAtMillis = id, updatedAtMillis = id))
                    put(genre.id, TrackFieldValue(id * 10 + 1, "genre-$id", id, genre.id, choiceOptionId = option.id, createdAtMillis = id, updatedAtMillis = id))
                    put(rating.id, TrackFieldValue(id * 10 + 2, "rating-$id", id, rating.id, enteredNumber = score, canonicalNumber = score, enteredUnitId = "unitless", createdAtMillis = id, updatedAtMillis = id))
                    note?.let { put(notes.id, TrackFieldValue(id * 10 + 3, "notes-$id", id, notes.id, textValue = it, createdAtMillis = id, updatedAtMillis = id)) }
                    yes?.let { put(recommend.id, TrackFieldValue(id * 10 + 4, "yes-$id", id, recommend.id, booleanValue = it, createdAtMillis = id, updatedAtMillis = id)) }
                    finishedDate?.let { put(finished.id, TrackFieldValue(id * 10 + 5, "finished-$id", id, finished.id, dateValue = it, createdAtMillis = id, updatedAtMillis = id)) }
                    effortValue?.let { put(effort.id, TrackFieldValue(id * 10 + 6, "effort-$id", id, effort.id, scaleValue = it, createdAtMillis = id, updatedAtMillis = id)) }
                },
            )
        return TrackProjection(
            Track(1, "track", "Books", "", "▤", "main", "Main", emptyList(), false, false, 0, 1, 1),
            listOf(title, genre, rating, notes, recommend, finished, effort),
            listOf(history, fiction),
            listOf(
                entry(1, "A", history, 4.0, LocalDate.of(2026, 1, 2), "A great book", true, LocalDate.of(2026, 1, 10), 4.5),
                entry(2, "B", fiction, 3.0, LocalDate.of(2025, 12, 1), null, false, LocalDate.of(2025, 12, 10), 2.0),
                entry(3, "C", history, 2.0, LocalDate.of(2026, 2, 1), null, null, null, null),
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
