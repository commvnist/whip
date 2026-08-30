package com.whip.app.ui

import com.whip.app.domain.AreaScope
import com.whip.app.domain.Area
import org.junit.Assert.assertEquals
import org.junit.Test

class AreaScopeFilterRelationshipsTest {
    @Test
    fun invalidArchivedAndZeroAreaScopesRecoverToAll() {
        val work = Area("work", "Work", null, 0, false, 1, 1)
        val archived = work.copy(archived = true)
        assertEquals(AreaScope.One("work"), AreaScope.One("work").validFor(listOf(work)))
        assertEquals(AreaScope.All, AreaScope.One("missing").validFor(listOf(work)))
        assertEquals(AreaScope.All, AreaScope.One("work").validFor(listOf(archived)))
        assertEquals(AreaScope.All, AreaScope.Unassigned.validFor(emptyList()))
        assertEquals(AreaScope.One("work"), AreaScope.Unassigned.validFor(listOf(work)))
    }
}
