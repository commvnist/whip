package com.whip.app.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackCsvBatchIdentityTest {
    @Test
    fun canonicalRequestIsStableAcrossMapOrderAndEquivalentNormalizedDrafts() {
        val boundary = boundary()
        val payload = trackCsvPayloadFingerprint("Name,Weight\nA,100")
        val first = prepareTrackCsvImportRequest(
            batchUuid = BATCH_UUID,
            openingFormBoundary = boundary,
            payloadFingerprint = payload,
            mapping = TrackCsvMapping(
                fieldColumns = linkedMapOf("weight" to "Weight", "name" to "Name"),
                numberUnitColumns = linkedMapOf("weight" to "Unit"),
            ),
            defaultEntryDate = DATE,
            drafts = listOf(
                TrackEntryDraft(
                    DATE,
                    linkedMapOf(
                        "weight" to TrackValueDraft(enteredNumber = 100.0),
                        "name" to TrackValueDraft(textValue = "  A  "),
                    ),
                ),
            ),
        )
        val reordered = prepareTrackCsvImportRequest(
            batchUuid = BATCH_UUID,
            openingFormBoundary = boundary,
            payloadFingerprint = payload,
            mapping = TrackCsvMapping(
                fieldColumns = linkedMapOf("name" to "Name", "weight" to "Weight"),
                numberUnitColumns = linkedMapOf("weight" to "Unit"),
            ),
            defaultEntryDate = DATE,
            drafts = listOf(
                TrackEntryDraft(
                    DATE,
                    linkedMapOf(
                        "name" to TrackValueDraft(textValue = "A"),
                        "weight" to TrackValueDraft(enteredNumber = 100.0, enteredUnitId = "kilogram"),
                    ),
                ),
            ),
        )

        assertEquals(first.requestFingerprint, reordered.requestFingerprint)
        assertEquals(first.entryUuids, reordered.entryUuids)
        assertEquals(first.entryIdentityDigest, reordered.entryIdentityDigest)
        assertEquals(1, first.rowCount)
        assertEquals(8, UUID.fromString(first.entryUuids.single()).version())
    }

    @Test
    fun canonicalRequestPreservesRawNumbersNullsRowOrderAndBatchIntent() {
        fun request(
            batch: String = BATCH_UUID,
            mapping: TrackCsvMapping = TrackCsvMapping(fieldColumns = mapOf("name" to "Name")),
            values: List<Double> = listOf(0.0, 1.0),
        ) = prepareTrackCsvImportRequest(
            batchUuid = batch,
            openingFormBoundary = boundary(),
            payloadFingerprint = trackCsvPayloadFingerprint("same exact payload"),
            mapping = mapping,
            defaultEntryDate = DATE,
            drafts = values.mapIndexed { index, number ->
                TrackEntryDraft(
                    DATE.plusDays(index.toLong()),
                    mapOf(
                        "name" to TrackValueDraft(textValue = "Row $index"),
                        "weight" to TrackValueDraft(enteredNumber = number, enteredUnitId = "kilogram"),
                    ),
                )
            },
        )

        val base = request()
        assertNotEquals(base.requestFingerprint, request(values = listOf(-0.0, 1.0)).requestFingerprint)
        assertNotEquals(base.requestFingerprint, request(values = listOf(1.0, 0.0)).requestFingerprint)
        assertNotEquals(
            base.requestFingerprint,
            request(mapping = TrackCsvMapping(entryDateColumn = "Date", fieldColumns = mapOf("name" to "Name"))).requestFingerprint,
        )
        val newBatch = request(batch = "22222222-2222-4222-8222-222222222222")
        assertEquals(base.requestFingerprint, newBatch.requestFingerprint)
        assertNotEquals(base.entryUuids, newBatch.entryUuids)
        assertNotEquals(base.entryIdentityDigest, newBatch.entryIdentityDigest)
    }

    @Test
    fun deterministicEntryIdentityChangesByOrdinalAndRejectsMalformedInputs() {
        val fingerprint = trackCsvPayloadFingerprint("request")
        val first = deterministicTrackCsvEntryUuid(BATCH_UUID, fingerprint, 0)
        assertEquals(first, deterministicTrackCsvEntryUuid(BATCH_UUID, fingerprint, 0))
        assertNotEquals(first, deterministicTrackCsvEntryUuid(BATCH_UUID, fingerprint, 1))
        assertEquals(
            trackCsvEntryIdentityDigest(listOf(first)),
            trackCsvEntryIdentityDigest(listOf(first)),
        )
        assertThrows(IllegalArgumentException::class.java) {
            deterministicTrackCsvEntryUuid("not-a-uuid", fingerprint, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            deterministicTrackCsvEntryUuid(BATCH_UUID, fingerprint, -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            prepareTrackCsvImportRequest(
                BATCH_UUID,
                boundary(),
                fingerprint,
                TrackCsvMapping(fieldColumns = mapOf("name" to "Name")),
                DATE,
                emptyList(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            prepareTrackCsvImportRequest(
                BATCH_UUID,
                boundary(),
                fingerprint,
                TrackCsvMapping(fieldColumns = mapOf("name" to "Name")),
                DATE,
                List(TRACK_CSV_MAX_IMPORT_ROWS + 1) {
                    TrackEntryDraft(DATE, mapOf("name" to TrackValueDraft(textValue = "A")))
                },
            )
        }
    }

    @Test
    fun batchContractsRemainProcessSaveableWithoutRawCsv() {
        val request = prepareTrackCsvImportRequest(
            BATCH_UUID,
            boundary(),
            trackCsvPayloadFingerprint("Name\nA"),
            TrackCsvMapping(fieldColumns = mapOf("name" to "Name")),
            DATE,
            listOf(TrackEntryDraft(DATE, mapOf("name" to TrackValueDraft(textValue = "A")))),
        )
        val restored = serializedRoundTrip(request)
        assertEquals(request, restored)
        val receipt = TrackCsvImportReceipt(
            request.batchUuid,
            request.openingFormBoundary.trackId,
            request.openingFormBoundary.trackUuid,
            request.openingFormBoundary.trackCreatedAtMillis,
            request.requestFingerprint,
            request.entryIdentityDigest,
            request.rowCount,
            request.fingerprintVersion,
            request.identityVersion,
            99,
            changed = true,
            alreadyApplied = false,
        )
        assertEquals(receipt, serializedRoundTrip(receipt))
        val envelope = serializedRoundTrip(request.receiptEnvelope())
        assertTrue(receipt.matches(envelope))
        assertTrue(!receipt.matches(envelope.copy(trackUuid = "different-track")))
        assertEquals(
            TrackCsvImportReceiptVerification.Exact(receipt),
            serializedRoundTrip(TrackCsvImportReceiptVerification.Exact(receipt)),
        )
        val conflict = TrackCsvImportConflictException(TrackCsvImportConflictKind.BatchIdentityCollision, "collision")
        assertEquals(TrackCsvImportConflictKind.BatchIdentityCollision, conflict.kind)
    }

    @Test
    fun canonicalBoundaryFailsClosedForSameIdentityRenamesAndUnitSemanticDrift() {
        fun request(formBoundary: TrackEntryFormBoundary) = prepareTrackCsvImportRequest(
            BATCH_UUID,
            formBoundary,
            trackCsvPayloadFingerprint("Name\nA"),
            TrackCsvMapping(fieldColumns = mapOf("name" to "Name")),
            DATE,
            listOf(TrackEntryDraft(DATE, mapOf("name" to TrackValueDraft(textValue = "A")))),
        )

        val baseBoundary = boundaryWithChoice()
        val base = request(baseBoundary)
        val fieldRenamed = baseBoundary.copy(
            fieldContracts = baseBoundary.fieldContracts.map { field ->
                if (field.uuid == "weight") field.copy(name = "Load") else field
            },
        )
        val choiceRenamed = baseBoundary.copy(
            choiceContracts = baseBoundary.choiceContracts.map { it.copy(label = "Great") },
        )
        val unitRenamed = baseBoundary.copy(
            unitContracts = baseBoundary.unitContracts.map { it.copy(name = "kilos") },
        )
        val unitFactorChanged = baseBoundary.copy(
            unitContracts = baseBoundary.unitContracts.map { it.copy(toCanonicalFactor = 1.000_001) },
        )

        assertNotEquals(base.requestFingerprint, request(fieldRenamed).requestFingerprint)
        assertNotEquals(base.requestFingerprint, request(choiceRenamed).requestFingerprint)
        assertNotEquals(base.requestFingerprint, request(unitRenamed).requestFingerprint)
        assertNotEquals(base.requestFingerprint, request(unitFactorChanged).requestFingerprint)
    }

    @Test
    fun atomicFormSnapshotOwnsPreviewFieldsChoicesAndCustomUnits() {
        val form = formSnapshot()
        assertTrue(form.hasExactCsvBoundary())
        assertTrue(!form.copy(fields = form.fields.map { field ->
            if (field.uuid == "name") field.copy(name = "Renamed") else field
        }).hasExactCsvBoundary())
        assertTrue(!form.copy(options = form.options.map { it.copy(label = "Renamed") }).hasExactCsvBoundary())
        assertTrue(!form.copy(units = form.units.map { it.copy(toCanonicalFactor = 2.0) }).hasExactCsvBoundary())

        val preview = previewTrackCsvImport(
            form = form,
            csv = "Name,Weight,Unit,Mood\nA,3,kg,Good",
            mapping = TrackCsvMapping(
                fieldColumns = mapOf("name" to "Name", "weight" to "Weight", "mood" to "Mood"),
                numberUnitColumns = mapOf("weight" to "Unit"),
            ),
            today = DATE,
        )
        assertEquals(1, preview.validRows)
        assertEquals("kilogram", preview.validDrafts.single().values.getValue("weight").enteredUnitId)
        assertEquals("good", preview.validDrafts.single().values.getValue("mood").choiceOptionUuid)
    }

    @Test
    fun strictParserPreservesValidRfcQuotingCrLfAndTrailingEmptyCells() {
        assertEquals(
            listOf(
                listOf("Name", "Notes", "Blank"),
                listOf("A, quoted", "line 1\nline 2 with \"quote\"", ""),
                listOf("", "", ""),
            ),
            parseCsv("Name,Notes,Blank\r\n\"A, quoted\",\"line 1\nline 2 with \"\"quote\"\"\",\r\n\"\",,\r\n"),
        )
        assertThrows(IllegalStateException::class.java) { parseCsv("Name\na\"b") }
        assertThrows(IllegalStateException::class.java) { parseCsv("Name\n\"a\"x") }
        assertThrows(IllegalArgumentException::class.java) { parseCsv("Name\n\"a") }
    }

    @Test
    fun previewToleratesBomButRejectsAmbiguousHeadersWidthsAndUnits() {
        val projection = projection()
        assertEquals(listOf("Name", "Weight"), trackCsvHeaders("\uFEFFName,Weight\nA,1"))
        assertEquals(
            1,
            previewTrackCsvImport(
                projection,
                "\uFEFFName,Weight\nA,1",
                TrackCsvMapping(fieldColumns = mapOf("name" to "Name", "weight" to "Weight")),
                DATE,
            ).validRows,
        )
        assertThrows(IllegalArgumentException::class.java) {
            previewTrackCsvImport(projection, "Name, name \nA,B", TrackCsvMapping(), DATE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            previewTrackCsvImport(
                projection,
                "Name\nA,unexpected",
                TrackCsvMapping(fieldColumns = mapOf("name" to "Name")),
                DATE,
            )
        }
        val ambiguousUnits = listOf(
            UnitDefinition("custom-a", "alpha", "x", UnitDimension.Mass, 1.0),
            UnitDefinition("custom-b", "beta", "x", UnitDimension.Mass, 2.0),
        )
        val ambiguous = previewTrackCsvImport(
            projection,
            "Name,Weight,Unit\nA,1,x",
            TrackCsvMapping(
                fieldColumns = mapOf("name" to "Name", "weight" to "Weight"),
                numberUnitColumns = mapOf("weight" to "Unit"),
            ),
            DATE,
            ambiguousUnits,
        )
        assertEquals(1, ambiguous.invalidRows)
        assertTrue(ambiguous.issues.single().message.contains("ambiguous"))
    }

    @Test
    fun archivedUnitsAreAllowedOnlyForTheFieldsRetainedDefault() {
        val unitContracts = listOf(
            TrackEntryUnitContract("kilogram", "kilograms", "kg", UnitDimension.Mass, 1.0, 0.0, true),
            TrackEntryUnitContract("pound", "pounds", "lb", UnitDimension.Mass, 0.45359237, 0.0, false),
            TrackEntryUnitContract("ounce", "ounces", "oz", UnitDimension.Mass, 0.028349523125, 0.0, true),
        )
        val formBoundary = boundary().copy(unitContracts = unitContracts)
        val mapping = TrackCsvMapping(fieldColumns = mapOf("name" to "Name", "weight" to "Weight"))
        fun request(unitId: String?) = prepareTrackCsvImportRequest(
            BATCH_UUID,
            formBoundary,
            trackCsvPayloadFingerprint("Name,Weight\nA,1"),
            mapping,
            DATE,
            listOf(
                TrackEntryDraft(
                    DATE,
                    mapOf(
                        "name" to TrackValueDraft(textValue = "A"),
                        "weight" to TrackValueDraft(enteredNumber = 1.0, enteredUnitId = unitId),
                    ),
                ),
            ),
        )

        assertEquals("kilogram", request(null).openingFormBoundary.fieldContracts.single { it.uuid == "weight" }.unitId)
        assertEquals(1, request("pound").rowCount)
        val archivedNonDefault = assertThrows(IllegalArgumentException::class.java) { request("ounce") }
        assertTrue(archivedNonDefault.message.orEmpty().contains("archived"))

        val availableUnits = unitContracts.map { unit ->
            UnitDefinition(
                unit.id,
                unit.name,
                unit.symbol,
                unit.dimension,
                unit.toCanonicalFactor,
                unit.toCanonicalOffset,
                archived = unit.archived,
            )
        }
        fun preview(unitLabel: String) = previewTrackCsvImport(
            projection(),
            "Name,Weight,Unit\nA,1,$unitLabel",
            TrackCsvMapping(
                fieldColumns = mapOf("name" to "Name", "weight" to "Weight"),
                numberUnitColumns = mapOf("weight" to "Unit"),
            ),
            DATE,
            availableUnits,
        )
        assertEquals(1, preview("kg").validRows)
        assertEquals(1, preview("lb").validRows)
        val rejectedPreview = preview("oz")
        assertEquals(1, rejectedPreview.invalidRows)
        assertTrue(rejectedPreview.issues.single().message.contains("archived"))
    }

    private fun boundary() = TrackEntryFormBoundary(
        trackId = 7,
        trackUuid = "track-seven",
        trackCreatedAtMillis = 10,
        writable = true,
        semanticRevisionToken = "form-revision",
        fieldContracts = listOf(
            TrackEntryFieldContract(1, "name", 7, "Name", TrackFieldType.ShortText, true, true, null, null, 0, null, null, "", "", 1.0),
            TrackEntryFieldContract(2, "weight", 7, "Weight", TrackFieldType.Number, false, false, UnitDimension.Mass, "kilogram", 1, null, null, "", "", 1.0),
        ),
        unitContracts = listOf(
            TrackEntryUnitContract("kilogram", "kilograms", "kg", UnitDimension.Mass, 1.0, 0.0, false),
        ),
    )

    private fun boundaryWithChoice(): TrackEntryFormBoundary {
        val base = boundary()
        return base.copy(
            fieldContracts = base.fieldContracts + TrackEntryFieldContract(
                3,
                "mood",
                7,
                "Mood",
                TrackFieldType.SingleChoice,
                false,
                false,
                null,
                null,
                0,
                null,
                null,
                "",
                "",
                1.0,
            ),
            choiceContracts = listOf(TrackEntryChoiceContract(4, "good", 3, "Good")),
        )
    }

    private fun formSnapshot(): TrackEntryFormSnapshot {
        val boundary = boundaryWithChoice()
        val projection = projection()
        val mood = TrackField(3, "mood", 7, "Mood", TrackFieldType.SingleChoice, 2, false, false, true, null, null, 0, null, null, "", "", 1, 1)
        return TrackEntryFormSnapshot(
            boundary = boundary,
            track = projection.track,
            fields = projection.fields + mood,
            options = listOf(TrackChoiceOption(4, "good", 3, "Good", 0, 1, 1)),
            units = boundary.unitContracts,
        )
    }

    private fun projection(): TrackProjection {
        val name = TrackField(1, "name", 7, "Name", TrackFieldType.ShortText, 0, true, true, true, null, null, 0, null, null, "", "", 1, 1)
        val weight = TrackField(2, "weight", 7, "Weight", TrackFieldType.Number, 1, false, false, true, UnitDimension.Mass, "kilogram", 1, null, null, "", "", 1, 1)
        return TrackProjection(
            Track(7, "track-seven", "Exercises", "", "▤", "main", "Main", emptyList(), false, false, 0, 10, 10),
            listOf(name, weight),
            emptyList(),
            emptyList(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> serializedRoundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(value) }
            output.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
    }

    private companion object {
        const val BATCH_UUID = "11111111-1111-4111-8111-111111111111"
        val DATE: LocalDate = LocalDate.of(2026, 9, 1)
    }
}
