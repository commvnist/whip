package com.whip.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.domain.HabitTrackingMode
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Supplies the launcher with a scrollable, independently refreshable Habit collection. */
class HabitWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        HabitWidgetRemoteViewsFactory(
            context = applicationContext,
            appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ),
        )
}

internal class HabitWidgetRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<HabitWidgetRow> = emptyList()
    private var renderedDate: LocalDate = LocalDate.MIN

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val app = context.applicationContext as WhipApplication
        val snapshot = runCatching {
            runBlocking(Dispatchers.IO) {
                val preferences = WhipWidgetPreferences.load(context, appWidgetId)
                val today = app.clock.today()
                val content = calculateHabitTrackingContent(
                    habits = app.habitRepository.habits.first(),
                    habitLogs = app.habitRepository.logs.first(),
                    habitChecklistItems = app.habitRepository.checklistItems.first(),
                    habitChecklistStates = app.habitRepository.checklistStates.first(),
                    habitPauses = app.habitRepository.pauses.first(),
                    habitSkips = app.habitRepository.skips.first(),
                    metricEntries = app.measurementRepository.entries.first(),
                    customUnits = app.measurementRepository.customUnits.first(),
                    today = today,
                    areaScope = preferences.areaScope,
                    showCompleted = preferences.showCompletedHabits,
                    selectedHabitIds = preferences.selectedHabitIds,
                    expandedHabitIds = preferences.expandedHabitIds,
                )
                HabitWidgetSnapshot(content.rows, today)
            }
        }.getOrElse { HabitWidgetSnapshot(emptyList(), app.clock.today()) }
        rows = snapshot.rows
        renderedDate = snapshot.date
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews? = rows.getOrNull(position)?.let { row ->
        habitCollectionRow(context, row, renderedDate)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.let { row ->
        "${row.habit.id}:${row.checklistItem?.id ?: "habit"}".hashCode().toLong()
    } ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}

private data class HabitWidgetSnapshot(
    val rows: List<HabitWidgetRow>,
    val date: LocalDate,
)

private fun habitCollectionRow(
    context: Context,
    row: HabitWidgetRow,
    today: LocalDate,
): RemoteViews {
    val views = RemoteViews(
        context.packageName,
        if (row.isChecklistItem) R.layout.widget_child_row else R.layout.widget_habit_row,
    )
    val title = row.checklistItem?.name ?: "${row.habit.icon} ${row.habit.name}"
    views.setTextViewText(
        R.id.widget_row_title,
        if (row.completed && row.isChecklistItem) SpannableString(title).apply {
            setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else title,
    )
    views.setTextViewText(R.id.widget_row_meta, habitMeta(context, row))

    if (row.isChecklistItem) {
        views.setImageViewResource(
            R.id.widget_row_action_icon,
            if (row.completed) R.drawable.widget_ic_checkbox_checked
            else R.drawable.widget_ic_checkbox_unchecked,
        )
        views.setTextColor(
            R.id.widget_row_title,
            context.getColor(
                if (row.completed) R.color.widget_secondary_text else R.color.widget_primary_text,
            ),
        )
        collectionFillInIntent(row.action, row, today)?.let { fillIn ->
            views.setOnClickFillInIntent(R.id.widget_row, fillIn)
        }
        views.setContentDescription(R.id.widget_row, habitActionDescription(context, row))
    } else {
        collectionFillInIntent(HabitWidgetAction.Open, row, today)?.let { fillIn ->
            views.setOnClickFillInIntent(R.id.widget_row_body, fillIn)
        }
        views.setContentDescription(
            R.id.widget_row_body,
            context.getString(R.string.widget_open_habit, row.habit.name),
        )

        bindHabitAction(views, row)
        views.setContentDescription(R.id.widget_row_action, habitActionDescription(context, row))
        collectionFillInIntent(row.action, row, today)?.let { fillIn ->
            views.setOnClickFillInIntent(R.id.widget_row_action, fillIn)
        }

        views.setInt(
            R.id.widget_row,
            "setBackgroundResource",
            if (row.expanded) R.drawable.widget_row_expanded_surface else R.drawable.widget_row_surface,
        )
        views.setViewVisibility(R.id.widget_row_expand, if (row.expandable) View.VISIBLE else View.GONE)
        if (row.expandable) {
            views.setImageViewResource(
                R.id.widget_row_expand,
                if (row.expanded) R.drawable.widget_ic_expand_less else R.drawable.widget_ic_expand_more,
            )
            views.setContentDescription(
                R.id.widget_row_expand,
                context.getString(
                    if (row.expanded) R.string.widget_collapse_habit_named else R.string.widget_expand_habit_named,
                    row.habit.name,
                ),
            )
            views.setOnClickFillInIntent(
                R.id.widget_row_expand,
                Intent()
                    .putExtra(
                        HabitTrackingWidgetProvider.EXTRA_COLLECTION_ACTION,
                        HabitTrackingWidgetProvider.COLLECTION_SET_EXPANDED,
                    )
                    .putExtra(HabitTrackingWidgetProvider.EXTRA_HABIT_ID, row.habit.id)
                    .putExtra(HabitTrackingWidgetProvider.EXTRA_EXPANDED, !row.expanded),
            )
        }
    }
    return views
}

private fun bindHabitAction(views: RemoteViews, row: HabitWidgetRow) {
    val icon = when (row.action) {
        HabitWidgetAction.ToggleHabit,
        HabitWidgetAction.ToggleChecklistItem,
        -> if (row.completed) R.drawable.widget_ic_checkbox_checked
        else R.drawable.widget_ic_checkbox_unchecked
        HabitWidgetAction.ReadOnly -> R.drawable.widget_ic_sync
        else -> null
    }
    val label = when (row.action) {
        HabitWidgetAction.Increment -> "+${formatWidgetNumber(row.habit.quickIncrement)}"
        HabitWidgetAction.StartTimer -> "Start"
        HabitWidgetAction.StopTimer -> "Stop"
        HabitWidgetAction.Open -> if (row.habit.trackingMode == HabitTrackingMode.Rating) "Rate" else "Log"
        else -> null
    }
    views.setViewVisibility(R.id.widget_row_action_icon, if (icon != null) View.VISIBLE else View.GONE)
    icon?.let { views.setImageViewResource(R.id.widget_row_action_icon, it) }
    views.setViewVisibility(R.id.widget_row_action_label, if (label != null) View.VISIBLE else View.GONE)
    label?.let { views.setTextViewText(R.id.widget_row_action_label, it) }
}

private fun collectionFillInIntent(
    action: HabitWidgetAction,
    row: HabitWidgetRow,
    today: LocalDate,
): Intent? {
    val actionName = when (action) {
        HabitWidgetAction.ToggleHabit -> HabitTrackingWidgetProvider.ACTION_TOGGLE_HABIT
        HabitWidgetAction.ToggleChecklistItem -> HabitTrackingWidgetProvider.ACTION_TOGGLE_CHECKLIST_ITEM
        HabitWidgetAction.Increment -> HabitTrackingWidgetProvider.ACTION_INCREMENT_HABIT
        HabitWidgetAction.StartTimer -> HabitTrackingWidgetProvider.ACTION_START_HABIT
        HabitWidgetAction.StopTimer -> HabitTrackingWidgetProvider.ACTION_STOP_HABIT
        HabitWidgetAction.Open -> HabitTrackingWidgetProvider.COLLECTION_OPEN_HABIT
        HabitWidgetAction.ReadOnly -> return null
    }
    return Intent()
        .putExtra(HabitTrackingWidgetProvider.EXTRA_COLLECTION_ACTION, actionName)
        .putExtra(HabitTrackingWidgetProvider.EXTRA_HABIT_ID, row.habit.id)
        .putExtra(HabitTrackingWidgetProvider.EXTRA_DATE_EPOCH_DAY, today.toEpochDay())
        .apply {
            row.checklistItem?.let {
                putExtra(HabitTrackingWidgetProvider.EXTRA_CHECKLIST_ITEM_ID, it.id)
            }
            if (action in setOf(HabitWidgetAction.ToggleHabit, HabitWidgetAction.ToggleChecklistItem)) {
                putExtra(HabitTrackingWidgetProvider.EXTRA_COMPLETED, !row.completed)
            }
        }
}

private fun habitActionDescription(context: Context, row: HabitWidgetRow): String {
    val nextState = context.getString(
        if (row.completed) R.string.widget_completed_action else R.string.widget_incomplete_action,
    )
    return when (row.action) {
        HabitWidgetAction.ToggleHabit -> context.getString(R.string.widget_toggle_habit, row.habit.name, nextState)
        HabitWidgetAction.ToggleChecklistItem -> context.getString(
            R.string.widget_toggle_checklist_item,
            requireNotNull(row.checklistItem).name,
            nextState,
            row.habit.name,
        )
        HabitWidgetAction.Increment -> context.getString(
            R.string.widget_increment_habit,
            formatWidgetNumber(row.habit.quickIncrement),
            row.habit.name,
        )
        HabitWidgetAction.StartTimer -> context.getString(R.string.widget_start_habit, row.habit.name)
        HabitWidgetAction.StopTimer -> context.getString(R.string.widget_stop_habit, row.habit.name)
        HabitWidgetAction.Open -> context.getString(R.string.widget_open_habit, row.habit.name)
        HabitWidgetAction.ReadOnly -> if (row.habit.sourceMetricId != null) {
            "${row.habit.name} is synced and read-only"
        } else {
            "${row.habit.name}: ${row.completedChecklistItems} of ${row.checklistItemCount} checklist items complete"
        }
    }
}

private fun habitMeta(context: Context, row: HabitWidgetRow): String = when {
    row.isChecklistItem -> row.habit.name
    row.habit.sourceMetricId != null -> context.getString(R.string.widget_synced)
    row.habit.trackingMode == HabitTrackingMode.Checklist -> context.getString(
        R.string.widget_items_progress,
        row.completedChecklistItems,
        row.checklistItemCount,
    )
    row.habit.trackingMode == HabitTrackingMode.CheckOff -> if (row.completed) "Completed" else "Due today"
    row.habit.trackingMode == HabitTrackingMode.Duration -> if (row.action == HabitWidgetAction.StopTimer) {
        context.getString(R.string.widget_stop)
    } else context.getString(R.string.widget_start)
    row.habit.trackingMode in setOf(HabitTrackingMode.Rating, HabitTrackingMode.LogOnly) ->
        context.getString(R.string.widget_log)
    else -> listOfNotNull(
        formatWidgetNumber(row.value),
        row.habit.targetMin?.let { "of ${formatWidgetNumber(it)}" },
    ).joinToString(" ")
}

private fun formatWidgetNumber(value: Double): String = if (value % 1.0 == 0.0) {
    value.toLong().toString()
} else {
    String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
}
