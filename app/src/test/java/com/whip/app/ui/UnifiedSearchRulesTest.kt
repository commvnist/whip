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
    @Test
    fun searchContentIgnoresClockButInvalidatesChangedRoutineContent() {
        val state = GymUiState(loading = false)
        assertEquals(state.searchContent(), state.copy(nowMillis = 2000, restSecondsRemaining = 10,
            errorMessage = "Unavailable").searchContent())
        val routine = com.whip.app.domain.GymRoutine(1, "routine", "Strength", "", 0, false, false, 1, 1)
        assertFalse(state.searchContent() == state.copy(routines = listOf(routine)).searchContent())
        assertFalse(state.searchContent() == state.copy(archivedRoutines = listOf(routine)).searchContent())
        val day = com.whip.app.domain.RoutineDay(1, "day", routine.id, "Saturday", 0, 1, 1)
        val placement = com.whip.app.domain.RoutineExercise(1, "placement", day.id, 1, 0, "", null, false, 1, 1)
        assertFalse(state.searchContent() == state.copy(routineDays = listOf(day)).searchContent())
        assertFalse(state.searchContent() == state.copy(routineExercises = listOf(placement)).searchContent())
    }

    @Test
    fun compiledSearchPreservesConstraintsRankingAndReducesRepeatedParsing() {
        val rows = (1L..2_000L).map { task.copy(id = it, title = if (it % 3 == 0L) "Report" else "Quarterly report $it") }
        val queries = listOf("report tag:finance area:WORK", "report missing deadline:true", "before:invalid",
            "domain:task after:2026-08-01 before:2026-09-01", "status:active", "deadline:invalid", "unknown:report", "  ")
        queries.forEach { query ->
            val compiled = WhipSearchQuery(query)
            listOf(true, false).forEach { all ->
                assertEquals(rows.filter { it.legacyMatchesQuery(query, all) }.map { it.id },
                    rows.filter { compiled.matches(it, all) }.map { it.id })
            }
        }
        assertFalse(WhipSearchQuery("before:2026-08-20").matches(task))
        assertFalse(WhipSearchQuery("after:2026-08-20").matches(task))
        assertFalse(WhipSearchQuery("missing tag:home").matches(task, false))
        assertTrue(WhipSearchQuery("AREA:work").explicitAreaOverride)
        val machineResult = task.copy(domain = SearchDomain.Machine, searchText = "North room v3 cable fly")
        assertTrue(WhipSearchQuery("north v3 cable").matches(machineResult))
        assertFalse(WhipSearchQuery("north v2 cable").matches(machineResult))
        val query = "report tag:finance area:work deadline:true before:2026-09-01"
        val compiled = WhipSearchQuery(query)
        fun repeated() = rows.filter { it.legacyMatchesQuery(query) }.sortedWith(compareBy { it.legacySearchRank(query) }).map { it.id }
        fun prepared() = rows.filter { compiled.matches(it) }.sortedWith(compareBy { compiled.rank(it) }).map { it.id }
        assertEquals(repeated(), prepared())
        repeat(2) { repeated(); prepared() }
        val before = (1..5).map { kotlin.system.measureNanoTime { repeated() } }.sorted()[2]
        val after = (1..5).map { kotlin.system.measureNanoTime { prepared() } }.sorted()[2]
        println("Search 2000 rows: median per-row parse=$before ns; compiled query=$after ns")
    }
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
    fun omittedEntityHistoryMarksItsDomainPartialEvenBelowResultLimit() {
        val builder = BoundedSearchIndexBuilder(2_000)
        assertEquals(listOf(3, 2), builder.history(SearchDomain.Habit, listOf(1, 3, 2), 2) { it })
        builder.history(SearchDomain.Goal, listOf(1, 2), 2) { it }
        builder.history(SearchDomain.TrackEntry, listOf(1), 2) { it }
        assertEquals(setOf(SearchDomain.Habit), builder.build().limitedDomains)
        builder.history(SearchDomain.TrackEntry, (1..501).toList(), 500) { it }
        assertEquals(setOf(SearchDomain.Habit, SearchDomain.TrackEntry), builder.build().limitedDomains)
    }

    @Test
    fun tenThousandHistoryValuesUseOneBoundedSelectorPass() {
        val history = (0 until 10_000).map { index -> (index * 7_919) % 10_000 }
        var selectorCalls = 0

        val newest = newestSearchValues(history, 100) { value ->
            selectorCalls++
            value
        }

        assertEquals(10_000, selectorCalls)
        assertEquals((9_999 downTo 9_900).toList(), newest)
    }

    @Test
    fun tenThousandResultsPerDomainStayIndependentlyBounded() {
        val tasks = (1L..10_000L).map { id -> task.copy(id = id, title = "Task $id") }
        val habits = (1L..10_000L).map { id ->
            task.copy(domain = SearchDomain.Habit, id = id, title = "Habit $id")
        }

        val index = boundSearchIndex(tasks + habits, maxResultsPerDomain = 2_000)

        assertEquals(4_000, index.results.size)
        assertEquals(setOf(SearchDomain.Task, SearchDomain.Habit), index.limitedDomains)
        assertEquals(2_000, index.results.count { it.domain == SearchDomain.Task })
        assertEquals(2_000, index.results.count { it.domain == SearchDomain.Habit })
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
    fun sharedUnitReadinessBelongsOnlyToSelectedMeasurementSources() {
        fun status(domain: SearchDomain, loaded: Boolean) = unifiedSearchDataStatus(
            setOf(domain), TaskUiState(loading = false), HabitUiState(loading = false, errorMessage = "Unavailable"),
            GoalUiState(loading = false), TrackUiState(loading = false), GymUiState(loading = false),
            customUnitsLoaded = loaded,
        )
        assertEquals(listOf("Measurement units"), status(SearchDomain.TrackEntry, false).loadingSources)
        assertTrue(status(SearchDomain.TrackEntry, true).complete)
        assertTrue(status(SearchDomain.Task, false).complete)
        assertTrue(status(SearchDomain.Track, false).complete)
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

// Previous production implementation retained as the behavior and CPU-work reference.
private fun WhipSearchResult.legacyMatchesQuery(query: String, requireAllTerms: Boolean = true): Boolean {
    val tokens = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val plain = mutableListOf<String>()
    var structuredCount = 0
    tokens.forEach { raw ->
        val token = raw.lowercase()
        val key = token.substringBefore(':', "")
        val value = token.substringAfter(':', "")
        when (key) {
            "tag" -> { structuredCount++; if (tags.none { it.equals(value, true) }) return false }
            "area" -> { structuredCount++; if (!area.contains(value, true)) return false }
            "status" -> { structuredCount++; if (!status.equals(value, true)) return false }
            "domain" -> { structuredCount++; if (!domain.name.equals(value, true) && !domain.uiLabel().replace(" ", "").equals(value.replace(" ", ""), true)) return false }
            "before" -> {
                structuredCount++
                val boundary = parseDateOrNull(value) ?: return false
                if (date?.isBefore(boundary) != true) return false
            }
            "after" -> {
                structuredCount++
                val boundary = parseDateOrNull(value) ?: return false
                if (date?.isAfter(boundary) != true) return false
            }
            "deadline" -> {
                structuredCount++
                val required = value.toBooleanStrictOrNull() ?: return false
                if ((deadline != null) != required) return false
            }
            else -> {
                plain += token
            }
        }
    }
    if (plain.isEmpty()) return structuredCount > 0
    val haystack = listOf(domain.uiLabel(), title, detail, area, tags.joinToString(" "), status, date?.toString().orEmpty(), deadline?.toString().orEmpty())
        .joinToString(" ").lowercase()
    return if (requireAllTerms) plain.all(haystack::contains) else plain.any(haystack::contains)
}

/** Stable user-facing rank: exact titles, title prefixes, title matches, then detail matches. */
private fun WhipSearchResult.legacySearchRank(query: String): Int {
    val plainQuery = query.trim().split(Regex("\\s+")).filterNot { ':' in it }.joinToString(" ").lowercase()
    if (plainQuery.isBlank()) return 3
    val normalizedTitle = title.trim().lowercase()
    return when {
        normalizedTitle == plainQuery -> 0
        normalizedTitle.startsWith(plainQuery) -> 1
        normalizedTitle.contains(plainQuery) -> 2
        else -> 3
    }
}

private fun parseDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
