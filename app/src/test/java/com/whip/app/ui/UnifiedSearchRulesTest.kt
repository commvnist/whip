package com.whip.app.ui

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.whip.app.domain.AreaScope

class UnifiedSearchRulesTest {
    private val task = WhipSearchResult(
        domain = SearchDomain.Task,
        id = 1,
        title = "Submit quarterly report",
        detail = "Send final numbers",
        area = "Work",
        tags = setOf("finance", "urgent"),
        date = LocalDate.of(2026, 8, 20),
        deadline = LocalDate.of(2026, 8, 21),
        status = "active",
    )

    @Test
    fun structuredFiltersCombineAndPlainTermsCanUseAllOrAny() {
        assertTrue(task.matchesQuery("report tag:finance area:work deadline:true before:2026-09-01"))
        assertFalse(task.matchesQuery("report tag:home"))
        assertFalse(task.matchesQuery("report missing", requireAllTerms = true))
        assertTrue(task.matchesQuery("report missing", requireAllTerms = false))
    }

    @Test
    fun statusDomainAndDateBoundsAreExplicit() {
        assertTrue(task.matchesQuery("domain:task status:active after:2026-08-01"))
        assertFalse(task.matchesQuery("domain:habit"))
        assertFalse(task.matchesQuery("before:not-a-date"))
    }

    @Test
    fun exactAndPrefixTitlesRankAheadOfBroadDetailMatches() {
        assertEquals(0, task.copy(title = "Report").searchRank("report"))
        assertEquals(1, task.searchRank("submit"))
        assertEquals(2, task.searchRank("quarterly"))
        assertEquals(3, task.searchRank("numbers"))
        assertEquals(0, task.copy(title = "Report").searchRank("report tag:finance"))
    }

    @Test
    fun globalAreaScopeIsDefaultButExplicitAreaQueryCanOverrideItLocally() {
        val workTask = task.copy(areaId = "work")
        assertTrue(workTask.isVisibleInAreaScope(AreaScope.One("work"), explicitAreaOverride = false))
        assertFalse(workTask.isVisibleInAreaScope(AreaScope.One("personal"), explicitAreaOverride = false))
        assertTrue(workTask.isVisibleInAreaScope(AreaScope.One("personal"), explicitAreaOverride = true))
        assertTrue(workTask.copy(domain = SearchDomain.Workout).isVisibleInAreaScope(AreaScope.One("personal"), false))
    }
}
