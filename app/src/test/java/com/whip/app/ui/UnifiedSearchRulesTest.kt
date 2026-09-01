package com.whip.app.ui

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.unit.dp
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

    @Test
    fun boundedHistoryProjectionKeepsNewestValuesRegardlessOfInputOrder() {
        val history = (1..150).toList()

        assertEquals((150 downTo 51).toList(), newestSearchValues(history, 100) { it })
        assertEquals((150 downTo 51).toList(), newestSearchValues(history.reversed(), 100) { it })
    }

    @Test
    fun adaptiveWorkspaceRequiresBothWideWidthAndAdequateHeight() {
        assertEquals(
            UnifiedSearchWorkspaceLayout.Compact,
            unifiedSearchWorkspaceLayout(width = 320.dp, height = 480.dp),
        )
        assertEquals(
            UnifiedSearchWorkspaceLayout.Compact,
            unifiedSearchWorkspaceLayout(width = 900.dp, height = 439.dp),
        )
        assertEquals(
            UnifiedSearchWorkspaceLayout.Wide,
            unifiedSearchWorkspaceLayout(width = 720.dp, height = 440.dp),
        )
    }

    @Test
    fun readinessOnlyIncludesSourcesOwnedBySelectedDomains() {
        val status = unifiedSearchDataStatus(
            domains = setOf(SearchDomain.Task),
            taskState = TaskUiState(loading = false),
            habitState = HabitUiState(loading = true),
            goalState = GoalUiState(loading = false, errorMessage = "Goals unavailable"),
            trackState = TrackUiState(loading = true),
            gymState = GymUiState(loading = false, errorMessage = "Gym unavailable"),
        )

        assertTrue(status.complete)
        assertTrue(status.loadingSources.isEmpty())
        assertTrue(status.failedSources.isEmpty())
    }

    @Test
    fun readinessGroupsSharedSourcesAndTreatsFailureAsIncomplete() {
        val status = unifiedSearchDataStatus(
            domains = setOf(SearchDomain.Track, SearchDomain.TrackEntry, SearchDomain.Habit, SearchDomain.Workout),
            taskState = TaskUiState(loading = true),
            habitState = HabitUiState(loading = true, errorMessage = "Offline"),
            goalState = GoalUiState(loading = false),
            trackState = TrackUiState(loading = true),
            gymState = GymUiState(loading = true),
        )

        assertFalse(status.complete)
        assertEquals(listOf("Tracks", "Gym"), status.loadingSources)
        assertEquals(listOf("Habits"), status.failedSources)
    }

    @Test
    fun perDomainIndexLimitKeepsLaterDomainsVisibleAndReportsPartialSources() {
        val tasks = (1L..5L).map { id ->
            task.copy(id = id, title = "Task $id")
        }
        val track = task.copy(domain = SearchDomain.Track, id = 99, title = "Medication")

        val index = boundSearchIndex(tasks + track, maxResultsPerDomain = 2)

        assertEquals(listOf(1L, 2L, 99L), index.results.map(WhipSearchResult::id))
        assertEquals(setOf(SearchDomain.Task), index.limitedDomains)
        assertTrue(UnifiedSearchDataStatus(limitedSources = listOf("Tasks")).complete.not())
    }

    @Test
    fun completedTaskDatesUseTheActiveWhipZoneNearMidnight() {
        val completedAt = Instant.parse("2026-09-01T02:00:00Z").toEpochMilli()
        val torontoDate = completedTaskSearchDate(completedAt, ZoneId.of("America/Toronto"))
        val tokyoDate = completedTaskSearchDate(completedAt, ZoneId.of("Asia/Tokyo"))

        assertEquals(LocalDate.of(2026, 8, 31), torontoDate)
        assertEquals(LocalDate.of(2026, 9, 1), tokyoDate)
        assertTrue(task.copy(date = torontoDate).matchesQuery("before:2026-09-01"))
        assertTrue(task.copy(date = tokyoDate).matchesQuery("after:2026-08-31"))
    }
}
