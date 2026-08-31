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

    @Test
    fun allAreasAndTheOnlyActiveAreaAreTheSameVisibleWidgetScope() {
        val main = Area("main", "Main", null, 0, false, 1, 1)
        val work = Area("work", "Work", null, 1, false, 1, 1)

        assertEquals(true, AreaScope.All.hasSameVisibleAreaAs(AreaScope.One("main"), listOf(main)))
        assertEquals(false, AreaScope.All.hasSameVisibleAreaAs(AreaScope.One("main"), listOf(main, work)))
        assertEquals(false, AreaScope.One("main").hasSameVisibleAreaAs(AreaScope.One("work"), listOf(main, work)))
    }
}
