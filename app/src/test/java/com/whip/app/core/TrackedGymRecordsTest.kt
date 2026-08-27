package com.whip.app.core

import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.PersonalRecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackedGymRecordsTest {
    @Test
    fun normalizationDeduplicatesSelectionsAndRemovesSpecificTargets() {
        val normalized = normalizeTrackedGymRecords(
            listOf(
                TrackedGymRecord("bench", PersonalRecordType.EstimatedOneRepMax, position = 8),
                TrackedGymRecord("bench", PersonalRecordType.EstimatedOneRepMax, position = 3),
                TrackedGymRecord("bench", PersonalRecordType.MaxWeight, secondaryValue = 5.0),
                TrackedGymRecord("bench", PersonalRecordType.BestWeightForRepCount, secondaryValue = 5.0, position = 4),
                TrackedGymRecord("bench", PersonalRecordType.MaxRepetitionsForWeight, secondaryValue = 100.0, position = 5),
                TrackedGymRecord("bench", PersonalRecordType.MaxWeight, position = 6),
            ),
        )

        assertEquals(2, normalized.size)
        assertEquals(listOf(0, 1), normalized.map(TrackedGymRecord::position))
        assertEquals(PersonalRecordType.EstimatedOneRepMax, normalized.first().type)
        assertEquals(PersonalRecordType.MaxWeight, normalized.last().type)
    }

    @Test
    fun storedBestWeightForRepCountTargetIsDiscarded() {
        val decoded = listOf(
            TrackedGymRecord("bench", PersonalRecordType.EstimatedOneRepMax),
            TrackedGymRecord("bench", PersonalRecordType.BestWeightForRepCount, secondaryValue = 5.0, position = 1),
        ).encodeTrackedGymRecords().decodeTrackedGymRecords()

        assertEquals(listOf(PersonalRecordType.EstimatedOneRepMax), decoded.map(TrackedGymRecord::type))
    }

    @Test
    fun storedMinimumWeightRepTargetIsDiscarded() {
        val normalized = normalizeTrackedGymRecords(
            listOf(TrackedGymRecord("bench", PersonalRecordType.MaxRepetitionsForWeight, secondaryValue = 102.0)),
        )

        assertEquals(emptyList<TrackedGymRecord>(), normalized)
    }

    @Test
    fun regularSelectionResolvesOnlyItsEquipmentScope() {
        val records = listOf(
            record("weight-a", PersonalRecordType.MaxWeight, null, "machine-a", 100.0),
            record("weight-b", PersonalRecordType.MaxWeight, null, "machine-b", 200.0),
        )
        val selection = TrackedGymRecord(
            exerciseUuid = "bench",
            type = PersonalRecordType.MaxWeight,
            machineProfileUuid = "machine-a",
        )

        assertEquals("weight-a", selection.resolveForExercise(7, records)?.uuid)
    }

    @Test
    fun regularSelectionDoesNotBindSpecificTargetRecords() {
        val selection = TrackedGymRecord("bench", PersonalRecordType.MaxWeight)
        val record = record("qualified", PersonalRecordType.MaxWeight, 5.0, null, 100.0)

        assertNull(selection.resolveForExercise(7, listOf(record)))
    }

    @Test
    fun unscopedRecommendationBindsOneEquipmentContextButNeverMergesSeveral() {
        val selection = TrackedGymRecord("bench", PersonalRecordType.MaxWeight)
        assertEquals(
            "only",
            selection.resolveForExercise(7, listOf(record("only", PersonalRecordType.MaxWeight, null, "machine-a", 100.0)))?.uuid,
        )
        assertNull(
            selection.resolveForExercise(
                7,
                listOf(
                    record("a", PersonalRecordType.MaxWeight, null, "machine-a", 100.0),
                    record("b", PersonalRecordType.MaxWeight, null, "machine-b", 200.0),
                ),
            ),
        )
    }

    @Test
    fun preferencesEncodingRoundTripsScopesAndOrder() {
        val expected = listOf(
            TrackedGymRecord("bench", PersonalRecordType.EstimatedOneRepMax, machineProfileUuid = "rack-a"),
            TrackedGymRecord("curl", PersonalRecordType.MaxWeight, position = 1),
        )

        assertEquals(expected, expected.encodeTrackedGymRecords().decodeTrackedGymRecords())
    }

    private fun record(
        uuid: String,
        type: PersonalRecordType,
        secondary: Double?,
        machine: String?,
        achieved: Double,
    ) = PersonalRecord(
        uuid = uuid,
        exerciseId = 7,
        type = type,
        value = achieved,
        secondaryValue = secondary,
        unitId = "kilogram",
        sourceSetId = 1,
        sourceSessionId = 2,
        achievedAtMillis = achieved.toLong(),
        current = true,
        imported = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        machineProfileUuidSnapshot = machine,
    )
}
