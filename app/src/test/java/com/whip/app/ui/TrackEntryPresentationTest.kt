package com.whip.app.ui

import com.whip.app.domain.*
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrackEntryPresentationTest {
    @Test fun numericIdentityResolvesArchivedCustomUnitsWithoutChangingDuplicateKeys() {
        val field = TrackField(1, "field", 1, "Distance", TrackFieldType.Number, 0, true, true, true,
            UnitDimension.Distance, "private-unit-id", 1, null, null, "", "", 1, 1)
        val value = TrackFieldValue(1, "value", 1, 1, enteredNumber = 3.125, canonicalNumber = 6250.0,
            enteredUnitId = "private-unit-id", createdAtMillis = 1, updatedAtMillis = 1)
        val entry = TrackEntryProjection(TrackEntry(1, "entry", 1, LocalDate.of(2026, 9, 28), 1, 1), mapOf(1L to value))
        val projection = TrackProjection(Track(id = 1, uuid = "track", name = "Routes", description = "", icon = "📓",
            areaId = "main", area = "Main", tags = emptyList(), pinned = false, archived = false, position = 0,
            createdAtMillis = 1, updatedAtMillis = 1), listOf(field), emptyList(), listOf(entry))
        val key = projection.identityKey(entry)
        val units = listOf(UnitDefinition("private-unit-id", "trail lengths", "tl", UnitDimension.Distance, 2000.0, archived = true))
        assertEquals("3.125 tl", projection.entryDisplayTitle(entry, units))
        assertFalse(projection.entryDisplayTitle(entry, units).contains("private-unit-id"))
        assertEquals(key, projection.identityKey(entry))
        assertEquals("3.125 trail lengths", projection.entryDisplayTitle(entry, listOf(units.single().copy(symbol = ""))))
        assertEquals("3.125 private-unit-id", projection.entryDisplayTitle(entry, emptyList()))
        val precise = entry.copy(values = mapOf(1L to value.copy(enteredNumber = 0.000000012345678)))
        assertEquals("0.000000012345678 tl", projection.entryDisplayTitle(precise, units))
    }
}
