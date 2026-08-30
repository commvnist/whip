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
import com.whip.app.core.zoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Supplies the launcher with a scrollable Task Agenda and expandable subtask rows. */
class TaskWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TaskWidgetRemoteViewsFactory(
            context = applicationContext,
            appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ),
        )
}

internal class TaskWidgetRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int,
    private val snapshotLoaderOverride: (() -> TaskWidgetSnapshot)? = null,
) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<TaskCollectionEntry> = emptyList()
    private var renderedDate: LocalDate = LocalDate.MIN

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val app = context.applicationContext as WhipApplication
        val result = runCatching {
            snapshotLoaderOverride?.invoke() ?: runBlocking(Dispatchers.IO) {
                val preferences = WhipWidgetPreferences.load(context, appWidgetId)
                val today = app.clock.today()
                val content = calculateTaskAgendaContent(
                    tasks = app.taskRepository.tasks.first(),
                    taskOccurrences = app.taskRepository.occurrences.first(),
                    taskSteps = app.taskRepository.steps.first(),
                    taskStepStates = app.taskRepository.stepStates.first(),
                    taskStepSnapshots = app.taskRepository.stepSnapshots.first(),
                    today = today,
                    areaScope = preferences.areaScope,
                    range = preferences.agendaRange,
                    zoneId = app.settingsRepository.current().zoneId(),
                )
                TaskWidgetSnapshot(
                    rows = taskWidgetRows(content.items, preferences.expandedTaskKeys),
                    date = today,
                )
            }
        }
        val snapshot = result.getOrNull()
        if (snapshot != null) {
            rows = snapshot.rows.map(TaskCollectionEntry::Current)
            renderedDate = snapshot.date
            WidgetSnapshotCache.save(
                context = context,
                kind = WidgetSnapshotKind.TaskAgenda,
                appWidgetId = appWidgetId,
                rows = snapshot.rows.map { it.toCachedRow(context, snapshot.date) },
            )
        } else {
            val cached = WidgetSnapshotCache.load(context, WidgetSnapshotKind.TaskAgenda, appWidgetId)
            rows = buildList {
                add(TaskCollectionEntry.RefreshError(hasCachedRows = cached?.rows?.isNotEmpty() == true))
                cached?.rows?.mapTo(this, TaskCollectionEntry::Cached)
            }
            renderedDate = app.clock.today()
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews? = rows.getOrNull(position)?.let { entry ->
        when (entry) {
            is TaskCollectionEntry.Current -> taskCollectionRow(context, entry.row, renderedDate)
            is TaskCollectionEntry.Cached -> cachedCollectionRow(context, entry.row)
            is TaskCollectionEntry.RefreshError -> refreshErrorRow(
                context = context,
                hasCachedRows = entry.hasCachedRows,
                retryActionKey = WhipWidgetProvider.EXTRA_TASK_COLLECTION_ACTION,
                retryAction = WhipWidgetProvider.COLLECTION_REFRESH_TASKS,
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 3

    override fun getItemId(position: Int): Long = when (val entry = rows.getOrNull(position)) {
        is TaskCollectionEntry.Current ->
            "${entry.row.item.stableKey}:${entry.row.subtask?.step?.id ?: "task"}".hashCode().toLong()
        is TaskCollectionEntry.Cached -> "cached:${entry.row.title}:${entry.row.meta}".hashCode().toLong()
        is TaskCollectionEntry.RefreshError -> Long.MIN_VALUE
        null -> position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}

private sealed interface TaskCollectionEntry {
    data class Current(val row: TaskWidgetRow) : TaskCollectionEntry
    data class Cached(val row: CachedWidgetRow) : TaskCollectionEntry
    data class RefreshError(val hasCachedRows: Boolean) : TaskCollectionEntry
}

internal data class TaskWidgetSnapshot(
    val rows: List<TaskWidgetRow>,
    val date: LocalDate,
)

private fun TaskWidgetRow.toCachedRow(context: Context, today: LocalDate): CachedWidgetRow =
    CachedWidgetRow(
        title = subtask?.title ?: "${item.task.icon} ${item.task.title}",
        meta = subtask?.let { item.task.title } ?: taskMeta(context, this, today),
        isChild = isSubtask,
        completed = subtask?.completed == true,
    )

private fun taskCollectionRow(
    context: Context,
    row: TaskWidgetRow,
    today: LocalDate,
): RemoteViews {
    val views = RemoteViews(
        context.packageName,
        if (row.isSubtask) R.layout.widget_child_row else R.layout.widget_task_row,
    )
    val subtask = row.subtask
    val title = subtask?.title ?: "${row.item.task.icon} ${row.item.task.title}"
    views.setTextViewText(
        R.id.widget_row_title,
        if (subtask?.completed == true) SpannableString(title).apply {
            setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else title,
    )
    views.setTextViewText(
        R.id.widget_row_meta,
        subtask?.let { row.item.task.title } ?: taskMeta(context, row, today),
    )

    if (subtask != null) {
        views.setImageViewResource(
            R.id.widget_row_action_icon,
            if (subtask.completed) R.drawable.widget_ic_checkbox_checked
            else R.drawable.widget_ic_checkbox_unchecked,
        )
        views.setTextColor(
            R.id.widget_row_title,
            context.getColor(
                if (subtask.completed) R.color.widget_secondary_text else R.color.widget_primary_text,
            ),
        )
        views.setOnClickFillInIntent(R.id.widget_row, subtaskToggleIntent(row, today))
        views.setContentDescription(R.id.widget_row, subtaskActionDescription(context, row))
    } else {
        val requiresSubtaskReview = row.requiresSubtaskReview
        views.setOnClickFillInIntent(R.id.widget_row_body, openTaskFillInIntent(row, today))
        views.setContentDescription(
            R.id.widget_row_body,
            context.getString(R.string.widget_open_task, row.item.task.title),
        )
        views.setImageViewResource(
            R.id.widget_row_action_icon,
            if (requiresSubtaskReview) {
                R.drawable.widget_ic_checkbox_indeterminate
            } else R.drawable.widget_ic_checkbox_unchecked,
        )
        views.setContentDescription(
            R.id.widget_row_action,
            if (requiresSubtaskReview) {
                context.resources.getQuantityString(
                    R.plurals.widget_review_task,
                    row.unfinishedSubtaskCount,
                    row.item.task.title,
                    row.unfinishedSubtaskCount,
                )
            } else context.getString(R.string.widget_complete_task, row.item.task.title),
        )
        views.setOnClickFillInIntent(
            R.id.widget_row_action,
            if (requiresSubtaskReview && !row.expanded) {
                taskActionIntent(WhipWidgetProvider.COLLECTION_SET_TASK_EXPANDED, row, today)
                    .putExtra(WhipWidgetProvider.EXTRA_TASK_KEY, row.item.stableKey)
                    .putExtra(WhipWidgetProvider.EXTRA_EXPANDED, true)
            } else if (requiresSubtaskReview) {
                openTaskFillInIntent(row, today)
            } else {
                taskActionIntent(WhipWidgetProvider.ACTION_COMPLETE_TASK, row, today)
            },
        )
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
                    if (row.expanded) R.string.widget_collapse_task_named else R.string.widget_expand_task_named,
                    row.item.task.title,
                ),
            )
            views.setOnClickFillInIntent(
                R.id.widget_row_expand,
                taskActionIntent(WhipWidgetProvider.COLLECTION_SET_TASK_EXPANDED, row, today)
                    .putExtra(WhipWidgetProvider.EXTRA_TASK_KEY, row.item.stableKey)
                    .putExtra(WhipWidgetProvider.EXTRA_EXPANDED, !row.expanded),
            )
        }
    }
    applyResponsiveCollectionRow(context, views, row.isSubtask)
    return views
}

private fun openTaskFillInIntent(row: TaskWidgetRow, today: LocalDate): Intent =
    taskActionIntent(WhipWidgetProvider.COLLECTION_OPEN_TASK, row, today)

private fun subtaskToggleIntent(row: TaskWidgetRow, today: LocalDate): Intent =
    taskActionIntent(WhipWidgetProvider.ACTION_TOGGLE_SUBTASK, row, today)
        .putExtra(WhipWidgetProvider.EXTRA_STEP_ID, requireNotNull(row.subtask).step.id)
        .putExtra(WhipWidgetProvider.EXTRA_COMPLETED, !row.subtask.completed)

private fun taskActionIntent(action: String, row: TaskWidgetRow, today: LocalDate): Intent = Intent()
    .putExtra(WhipWidgetProvider.EXTRA_TASK_COLLECTION_ACTION, action)
    .putExtra(WhipWidgetProvider.EXTRA_TASK_ID, row.item.task.id)
    .putExtra(
        WhipWidgetProvider.EXTRA_OCCURRENCE_EPOCH_DAY,
        row.item.originalDate?.toEpochDay() ?: WhipWidgetProvider.NO_DATE,
    )
    .putExtra(WhipWidgetProvider.EXTRA_RENDERED_DATE_EPOCH_DAY, today.toEpochDay())

private fun subtaskActionDescription(context: Context, row: TaskWidgetRow): String {
    val subtask = requireNotNull(row.subtask)
    return context.getString(
        R.string.widget_toggle_subtask,
        subtask.title,
        context.getString(
            if (subtask.completed) R.string.widget_completed_action else R.string.widget_incomplete_action,
        ),
        row.item.task.title,
    )
}

private fun taskMeta(context: Context, row: TaskWidgetRow, today: LocalDate): String {
    val item = row.item
    val date = item.scheduledDate ?: item.originalDate
    val dateText = when {
        date == null -> context.getString(R.string.widget_today)
        date.isBefore(today) -> context.getString(R.string.widget_overdue)
        date == today -> context.getString(R.string.widget_today)
        date == today.plusDays(1) -> context.getString(R.string.widget_tomorrow)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    val timeText = item.task.timeMinutes?.let { minutes ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
        }
        android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
    }
    val unfinished = item.subtasks.count { !it.completed }
    val subtaskText = unfinished.takeIf { it > 0 }?.let {
        context.resources.getQuantityString(R.plurals.widget_subtasks_left_plural, it, it)
    }
    return listOfNotNull(dateText, timeText, subtaskText).joinToString(" · ")
}
