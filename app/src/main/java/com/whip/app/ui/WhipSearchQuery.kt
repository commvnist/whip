package com.whip.app.ui

import com.whip.app.domain.Exercise
import com.whip.app.domain.GymMachine
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineExercise
import com.whip.app.domain.WorkoutSession
import java.time.LocalDate

/** Only persisted content consumed by the search index, excluding the workout clock/UI. */
internal data class GymSearchContent(
    val exercises: List<Exercise>,
    val archivedExercises: List<Exercise>,
    val machines: List<GymMachine>,
    val archivedMachines: List<GymMachine>,
    val sessions: List<WorkoutSession>,
    val routines: List<GymRoutine>,
    val archivedRoutines: List<GymRoutine>,
    val routineDays: List<RoutineDay>,
    val routineExercises: List<RoutineExercise>,
)

internal fun GymUiState.searchContent() = GymSearchContent(
    exercises, archivedExercises, machines, archivedMachines, allSessions, routines, archivedRoutines,
    routineDays, routineExercises,
)

/** Compile query constraints once for a search, rather than once for every row/comparison. */
internal class WhipSearchQuery(query: String) {
    private val tokens = query.trim().split(Regex("\\s+")).filter(String::isNotBlank).map(String::lowercase)
    private val structuredKeys = setOf("tag", "area", "status", "domain", "before", "after", "deadline")
    private val structured = tokens.filter { it.substringBefore(':', "") in structuredKeys }
    private val plain = tokens.filterNot { it.substringBefore(':', "") in structuredKeys }
    private val titleQuery = tokens.filterNot { ':' in it }.joinToString(" ")
    val explicitAreaOverride = structured.any { it.startsWith("area:") }
    private val constraints: List<(WhipSearchResult) -> Boolean> = structured.map { token ->
        val key = token.substringBefore(':')
        val value = token.substringAfter(':')
        when (key) {
            "tag" -> { result -> result.tags.any { it.equals(value, true) } }
            "area" -> { result -> result.area.contains(value, true) }
            "status" -> { result -> result.status.equals(value, true) }
            "domain" -> { result -> result.domain.name.equals(value, true) ||
                result.domain.uiLabel().replace(" ", "").equals(value.replace(" ", ""), true) }
            "before", "after" -> {
                val boundary = runCatching { LocalDate.parse(value) }.getOrNull()
                ({ result: WhipSearchResult -> boundary != null && result.date?.let {
                    if (key == "before") it.isBefore(boundary) else it.isAfter(boundary)
                } == true })
            }
            else -> {
                val required = value.toBooleanStrictOrNull()
                ({ result: WhipSearchResult -> required != null && (result.deadline != null) == required })
            }
        }
    }

    fun matches(result: WhipSearchResult, requireAllTerms: Boolean = true): Boolean {
        if (!constraints.all { it(result) }) return false
        if (plain.isEmpty()) return constraints.isNotEmpty()
        val haystack = with(result) {
            listOf(domain.uiLabel(), title, detail, searchText, area, tags.joinToString(" "), status,
                date?.toString().orEmpty(), deadline?.toString().orEmpty()).joinToString(" ").lowercase()
        }
        return if (requireAllTerms) plain.all(haystack::contains) else plain.any(haystack::contains)
    }

    /** Stable ordering: exact title, prefix, title match, then detail match. */
    fun rank(result: WhipSearchResult): Int {
        if (titleQuery.isBlank()) return 3
        val title = result.title.trim().lowercase()
        return when {
            title == titleQuery -> 0
            title.startsWith(titleQuery) -> 1
            title.contains(titleQuery) -> 2
            else -> 3
        }
    }
}
