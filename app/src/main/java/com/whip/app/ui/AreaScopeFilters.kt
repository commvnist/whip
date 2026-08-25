package com.whip.app.ui

import com.whip.app.domain.AreaScope
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.HabitDayProgress
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.matches
import com.whip.app.domain.Area

internal fun AreaScope.validFor(areas: List<Area>): AreaScope {
    val active = areas.filterNot(Area::archived)
    return when {
        active.isEmpty() -> AreaScope.All
        this == AreaScope.Unassigned -> AreaScope.One(active.first().id)
        this is AreaScope.One && active.none { it.id == areaId } -> AreaScope.All
        else -> this
    }
}

internal fun TaskUiState.forArea(scope: AreaScope): TaskUiState {
    if (scope == AreaScope.All) return this
    fun List<ScheduledTask>.visible() = filter { scope.matches(it.task.areaId) }
    val visibleTaskIds = (inbox + today + upcoming + completed + archived + planning)
        .asSequence()
        .filter { scope.matches(it.task.areaId) }
        .map { it.task.id }
        .toSet()
    return copy(
        inbox = inbox.visible(),
        today = today.visible(),
        upcoming = upcoming.visible(),
        completed = completed.visible(),
        archived = archived.visible(),
        planning = planning.visible(),
        occurrences = occurrences.filter { it.taskId in visibleTaskIds },
    )
}

internal fun HabitUiState.forArea(scope: AreaScope): HabitUiState {
    if (scope == AreaScope.All) return this
    fun List<HabitDayProgress>.visible() = filter { scope.matches(it.habit.areaId) }
    val visibleIds = (today + all).asSequence()
        .filter { scope.matches(it.habit.areaId) }
        .map { it.habit.id }
        .toSet()
    return copy(
        today = today.visible(),
        all = all.visible(),
        archived = archived.filter { scope.matches(it.areaId) },
        logs = logs.filter { it.habitId in visibleIds },
        pauses = pauses.filter { it.habitId in visibleIds },
    )
}

internal fun GoalUiState.forArea(scope: AreaScope): GoalUiState {
    if (scope == AreaScope.All) return this
    fun List<GoalProjection>.visible() = filter { scope.matches(it.goal.areaId) }
    return copy(
        active = active.visible(),
        completed = completed.visible(),
        archived = archived.visible(),
        // A visible habit/task can contribute to a Goal in another Area. Keep
        // relationship context intact even while the primary Goal collection is scoped.
        linkRules = linkRules,
        contributions = contributions,
    )
}
