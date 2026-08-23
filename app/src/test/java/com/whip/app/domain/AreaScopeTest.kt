package com.whip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaScopeTest {
    @Test
    fun scopesMatchAllAssignedAndUnassignedRecords() {
        assertTrue(AreaScope.All.matches(null))
        assertTrue(AreaScope.All.matches("work"))
        assertTrue(AreaScope.Unassigned.matches(null))
        assertFalse(AreaScope.Unassigned.matches("work"))
        assertTrue(AreaScope.One("work").matches("work"))
        assertFalse(AreaScope.One("work").matches("personal"))
    }

    @Test
    fun persistedScopeRoundTripsAndRejectsInvalidValues() {
        val scope = AreaScope.One("area-123")
        assertEquals(scope, AreaScope.fromStorageKey(scope.storageKey))
        assertEquals(AreaScope.Unassigned, AreaScope.fromStorageKey("unassigned"))
        assertEquals(AreaScope.All, AreaScope.fromStorageKey("area:"))
        assertEquals(AreaScope.All, AreaScope.fromStorageKey("unexpected"))
    }

    @Test(timeout = 1_000)
    fun switchingScopeAcrossTenThousandAssignmentsStaysLinearAndComplete() {
        val assignments = List(10_000) { index -> if (index % 4 == 0) null else "area-${index % 3}" }
        assertEquals(2_500, assignments.count(AreaScope.Unassigned::matches))
        assertEquals(2_500, assignments.count(AreaScope.One("area-1")::matches))
        assertEquals(10_000, assignments.count(AreaScope.All::matches))
    }
}
