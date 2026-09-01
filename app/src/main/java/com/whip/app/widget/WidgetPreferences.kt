package com.whip.app.widget

import android.content.Context
import androidx.core.content.edit
import com.whip.app.WhipApplication
import com.whip.app.domain.AreaScope

internal enum class WidgetKind {
    TaskAgenda,
    HabitTracking,
}

internal enum class AgendaRange(
    val daysAhead: Long,
    val title: String,
) {
    Today(0, "Today"),
    SevenDays(7, "Next 7 days"),
    ThirtyDays(30, "Next 30 days"),
}

internal data class WidgetPreferences(
    val areaScope: AreaScope = AreaScope.All,
    val transparencyPercent: Int = DEFAULT_WIDGET_TRANSPARENCY,
    val agendaRange: AgendaRange = AgendaRange.SevenDays,
    val showCompletedHabits: Boolean = true,
    /** Null means every matching Habit; an empty set intentionally shows none. */
    val selectedHabitIds: Set<Long>? = null,
    val expandedHabitIds: Set<Long> = emptySet(),
    val expandedTaskKeys: Set<String> = emptySet(),
)

internal const val DEFAULT_WIDGET_TRANSPARENCY = 20
internal const val MAX_WIDGET_TRANSPARENCY = 80

internal object WhipWidgetPreferences {
    private const val PREFS = "whip_widget_areas"

    fun load(
        context: Context,
        appWidgetId: Int,
        dataGeneration: Long = context.currentWidgetDataGeneration(),
    ): WidgetPreferences {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val value = WidgetPreferences(
            areaScope = AreaScope.fromStorageKey(
                prefs.getString(scopeKey(appWidgetId), AreaScope.All.storageKey),
            ),
            transparencyPercent = prefs.getInt(
                transparencyKey(appWidgetId),
                DEFAULT_WIDGET_TRANSPARENCY,
            ).coerceIn(0, MAX_WIDGET_TRANSPARENCY),
            agendaRange = prefs.getString(rangeKey(appWidgetId), null)
                ?.let { stored -> AgendaRange.entries.firstOrNull { it.name == stored } }
                ?: AgendaRange.SevenDays,
            showCompletedHabits = prefs.getBoolean(showCompletedKey(appWidgetId), true),
            selectedHabitIds = if (prefs.contains(selectedHabitsKey(appWidgetId))) {
                prefs.getStringSet(selectedHabitsKey(appWidgetId), emptySet())
                    .orEmpty()
                    .mapNotNull(String::toLongOrNull)
                    .toSet()
            } else {
                null
            },
            expandedHabitIds = prefs.getStringSet(expandedHabitsKey(appWidgetId), emptySet())
                .orEmpty()
                .mapNotNull(String::toLongOrNull)
                .toSet(),
            expandedTaskKeys = prefs.getStringSet(expandedTasksKey(appWidgetId), emptySet()).orEmpty(),
        )
        if (!hasConfiguration(prefs, appWidgetId)) return value

        val storedGeneration = prefs.getLong(dataGenerationKey(appWidgetId), 0L)
        if (storedGeneration == dataGeneration) return value

        // Widget settings live outside the replaceable user-data snapshot.
        // Keep display-only choices, but never let identifiers from the old
        // database generation alias unrelated records restored with the same IDs.
        val reconciled = value.copy(
            areaScope = when (value.areaScope) {
                AreaScope.Unassigned -> AreaScope.Unassigned
                AreaScope.All,
                is AreaScope.One,
                -> AreaScope.All
            },
            selectedHabitIds = if (prefs.contains(selectedHabitsKey(appWidgetId))) emptySet() else null,
            expandedHabitIds = emptySet(),
            expandedTaskKeys = emptySet(),
        )
        write(context, appWidgetId, reconciled, dataGeneration)
        WidgetSnapshotCache.remove(context, intArrayOf(appWidgetId))
        return reconciled
    }

    fun save(
        context: Context,
        appWidgetId: Int,
        value: WidgetPreferences,
        dataGeneration: Long = context.currentWidgetDataGeneration(),
    ) {
        write(context, appWidgetId, value, dataGeneration)
        // A display snapshot describes the previous scope/range/selection and
        // must never be relabeled under newly saved configuration.
        WidgetSnapshotCache.remove(context, intArrayOf(appWidgetId))
    }

    private fun write(
        context: Context,
        appWidgetId: Int,
        value: WidgetPreferences,
        dataGeneration: Long,
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(scopeKey(appWidgetId), value.areaScope.storageKey)
            putInt(
                transparencyKey(appWidgetId),
                value.transparencyPercent.coerceIn(0, MAX_WIDGET_TRANSPARENCY),
            )
            putString(rangeKey(appWidgetId), value.agendaRange.name)
            putBoolean(showCompletedKey(appWidgetId), value.showCompletedHabits)
            if (value.selectedHabitIds == null) {
                remove(selectedHabitsKey(appWidgetId))
            } else {
                putStringSet(selectedHabitsKey(appWidgetId), value.selectedHabitIds.map(Long::toString).toSet())
            }
            putStringSet(expandedHabitsKey(appWidgetId), value.expandedHabitIds.map(Long::toString).toSet())
            putStringSet(expandedTasksKey(appWidgetId), value.expandedTaskKeys)
            putLong(dataGenerationKey(appWidgetId), dataGeneration)
        }
    }

    fun saveScope(context: Context, appWidgetId: Int, scope: AreaScope) {
        val current = load(context, appWidgetId)
        save(context, appWidgetId, current.copy(areaScope = scope))
    }

    fun setHabitExpanded(
        context: Context,
        appWidgetId: Int,
        habitId: Long,
        expanded: Boolean,
    ) {
        val current = load(context, appWidgetId)
        val updated = if (expanded) {
            current.expandedHabitIds + habitId
        } else {
            current.expandedHabitIds - habitId
        }
        save(context, appWidgetId, current.copy(expandedHabitIds = updated))
    }

    fun setTaskExpanded(
        context: Context,
        appWidgetId: Int,
        taskKey: String,
        expanded: Boolean,
    ) {
        val current = load(context, appWidgetId)
        val updated = if (expanded) {
            current.expandedTaskKeys + taskKey
        } else {
            current.expandedTaskKeys - taskKey
        }
        save(context, appWidgetId, current.copy(expandedTaskKeys = updated))
    }

    fun pruneHabitExpansions(
        context: Context,
        appWidgetId: Int,
        eligibleHabitIds: Set<Long>,
    ): WidgetPreferences {
        val current = load(context, appWidgetId)
        val retained = current.expandedHabitIds.intersect(eligibleHabitIds)
        if (retained == current.expandedHabitIds) return current
        return current.copy(expandedHabitIds = retained).also { save(context, appWidgetId, it) }
    }

    fun pruneTaskExpansions(
        context: Context,
        appWidgetId: Int,
        eligibleTaskKeys: Set<String>,
    ): WidgetPreferences {
        val current = load(context, appWidgetId)
        val retained = current.expandedTaskKeys.intersect(eligibleTaskKeys)
        if (retained == current.expandedTaskKeys) return current
        return current.copy(expandedTaskKeys = retained).also { save(context, appWidgetId, it) }
    }

    fun remove(context: Context, appWidgetIds: IntArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            appWidgetIds.forEach { id ->
                remove(scopeKey(id))
                remove(transparencyKey(id))
                remove(rangeKey(id))
                remove(showCompletedKey(id))
                remove(selectedHabitsKey(id))
                remove(expandedHabitsKey(id))
                remove(expandedTasksKey(id))
                remove(dataGenerationKey(id))
            }
        }
    }

    private fun hasConfiguration(
        prefs: android.content.SharedPreferences,
        appWidgetId: Int,
    ): Boolean = listOf(
        scopeKey(appWidgetId),
        transparencyKey(appWidgetId),
        rangeKey(appWidgetId),
        showCompletedKey(appWidgetId),
        selectedHabitsKey(appWidgetId),
        expandedHabitsKey(appWidgetId),
        expandedTasksKey(appWidgetId),
        dataGenerationKey(appWidgetId),
    ).any(prefs::contains)

    private fun scopeKey(appWidgetId: Int) = "scope_$appWidgetId"
    private fun transparencyKey(appWidgetId: Int) = "transparency_$appWidgetId"
    private fun rangeKey(appWidgetId: Int) = "range_$appWidgetId"
    private fun showCompletedKey(appWidgetId: Int) = "show_completed_$appWidgetId"
    private fun selectedHabitsKey(appWidgetId: Int) = "selected_habits_$appWidgetId"
    private fun expandedHabitsKey(appWidgetId: Int) = "expanded_habits_$appWidgetId"
    private fun expandedTasksKey(appWidgetId: Int) = "expanded_tasks_$appWidgetId"
    private fun dataGenerationKey(appWidgetId: Int) = "data_generation_$appWidgetId"
}

internal fun Context.currentWidgetDataGeneration(): Long =
    (applicationContext as? WhipApplication)?.currentUserDataGeneration() ?: 0L

internal fun widgetBackgroundAlpha(transparencyPercent: Int): Int =
    (((100 - transparencyPercent.coerceIn(0, MAX_WIDGET_TRANSPARENCY)) / 100f) * 255f)
        .toInt()
        .coerceIn(0, 255)

internal fun widgetRowCapacity(availableHeightDp: Int, fontScale: Float): Int {
    if (availableHeightDp <= 0) return 3
    val headerHeight = if (fontScale >= 1.3f) 84 else 72
    val rowHeight = if (fontScale >= 1.3f) 62 else 52
    return ((availableHeightDp - headerHeight) / rowHeight).coerceIn(1, 6)
}

internal fun availableWidgetHeight(
    minHeightDp: Int,
    maxHeightDp: Int,
    landscape: Boolean,
): Int = if (landscape) {
    minHeightDp.takeIf { it > 0 } ?: maxHeightDp
} else {
    maxHeightDp.takeIf { it > 0 } ?: minHeightDp
}

internal fun useCompactWidgetHeader(availableHeightDp: Int, fontScale: Float): Boolean =
    availableHeightDp in 1 until 200 || fontScale >= 1.5f

/** At extreme scaling the secondary line would consume the minimum widget's collection viewport. */
internal fun useSingleLineWidgetRows(fontScale: Float): Boolean = fontScale >= 2f
