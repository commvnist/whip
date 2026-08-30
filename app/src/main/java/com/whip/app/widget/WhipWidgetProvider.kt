package com.whip.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.core.WhipLaunchActions
import com.whip.app.core.zoneId
import com.whip.app.domain.AreaScope
import com.whip.app.domain.HabitTrackingMode
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The original provider identity is retained so existing installed widgets are
 * upgraded in place to Task Agenda instead of being removed by the launcher.
 */
class WhipWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateTaskAgendaWidgets(context, manager, ids)
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val collectionClick = intent.action == ACTION_TASK_COLLECTION_CLICK
        val resolvedAction = if (collectionClick) {
            intent.getStringExtra(EXTRA_TASK_COLLECTION_ACTION)
        } else {
            intent.action
        }
        if (resolvedAction == COLLECTION_SET_TASK_EXPANDED) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            val taskKey = intent.getStringExtra(EXTRA_TASK_KEY).orEmpty()
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && taskKey.isNotBlank()) {
                WhipWidgetPreferences.setTaskExpanded(
                    context = context,
                    appWidgetId = appWidgetId,
                    taskKey = taskKey,
                    expanded = intent.getBooleanExtra(EXTRA_EXPANDED, false),
                )
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.widget_task_list,
                )
            }
            return
        }
        if (resolvedAction == COLLECTION_REFRESH_TASKS) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.widget_task_list,
                )
            }
            return
        }
        if (resolvedAction == COLLECTION_OPEN_TASK) {
            openTaskFromCollection(context, intent)
            return
        }
        if (resolvedAction !in TASK_MUTATION_ACTIONS) {
            super.onReceive(context, intent)
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as WhipApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val occurrenceEpochDay = intent.getLongExtra(EXTRA_OCCURRENCE_EPOCH_DAY, NO_DATE)
                if (taskId >= 0L) {
                    val originalDate = occurrenceEpochDay.takeUnless { it == NO_DATE }?.let(LocalDate::ofEpochDay)
                    val renderedDate = intent.getLongExtra(EXTRA_RENDERED_DATE_EPOCH_DAY, NO_DATE)
                        .takeUnless { it == NO_DATE }
                        ?.let(LocalDate::ofEpochDay)
                    val appWidgetId = intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                    )
                    if (!collectionClick || renderedDate == app.clock.today()) runCatching {
                        when (resolvedAction) {
                            ACTION_COMPLETE_TASK -> {
                                val currentItem = if (collectionClick) currentTaskAgendaItem(
                                    context = context,
                                    appWidgetId = appWidgetId,
                                    taskId = taskId,
                                    originalDate = originalDate,
                                ) else null
                                val stillVisible = !collectionClick || currentItem != null
                                val hasUnfinishedSubtasks = currentItem?.subtasks?.any { !it.completed } == true
                                if (stillVisible && !hasUnfinishedSubtasks) {
                                    app.taskRepository.completeOccurrence(taskId, originalDate)
                                } else if (
                                    hasUnfinishedSubtasks &&
                                    appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                                ) {
                                    WhipWidgetPreferences.setTaskExpanded(
                                        context = context,
                                        appWidgetId = appWidgetId,
                                        taskKey = requireNotNull(currentItem).stableKey,
                                        expanded = true,
                                    )
                                }
                            }
                            ACTION_TOGGLE_SUBTASK -> {
                                val item = currentTaskAgendaItem(
                                    context = context,
                                    appWidgetId = appWidgetId,
                                    taskId = taskId,
                                    originalDate = originalDate,
                                ) ?: return@runCatching
                                val stepId = intent.getLongExtra(EXTRA_STEP_ID, -1L)
                                if (stepId >= 0L && item.subtasks.any { it.step.id == stepId }) {
                                    app.taskRepository.setStepCompleted(
                                        item = item,
                                        stepId = stepId,
                                        completed = intent.getBooleanExtra(EXTRA_COMPLETED, true),
                                    )
                                }
                            }
                        }
                        app.reminderScheduler.syncTask(taskId)
                    }
                }
                updateAll(context)
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WhipWidgetPreferences.remove(context, appWidgetIds)
        WidgetSnapshotCache.remove(context, appWidgetIds)
    }

    companion object {
        const val ACTION_OPEN = "commvne.com.whip.app.widget.OPEN"
        const val ACTION_OPEN_TASK_AGENDA = "commvne.com.whip.app.widget.OPEN_TASK_AGENDA"
        const val ACTION_OPEN_HABIT_TRACKING = "commvne.com.whip.app.widget.OPEN_HABIT_TRACKING"
        const val ACTION_ADD_TASK = "commvne.com.whip.app.widget.ADD_TASK"
        const val ACTION_ADD_HABIT = "commvne.com.whip.app.widget.ADD_HABIT"
        const val EXTRA_AREA_SCOPE = "commvne.com.whip.app.widget.AREA_SCOPE"

        internal const val ACTION_COMPLETE_TASK = "commvne.com.whip.app.widget.COMPLETE_TASK"
        internal const val ACTION_TOGGLE_SUBTASK = "commvne.com.whip.app.widget.TOGGLE_SUBTASK"
        internal const val ACTION_TASK_COLLECTION_CLICK = "commvne.com.whip.app.widget.TASK_COLLECTION_CLICK"
        internal const val COLLECTION_OPEN_TASK = "open_task"
        internal const val COLLECTION_SET_TASK_EXPANDED = "set_task_expanded"
        internal const val COLLECTION_REFRESH_TASKS = "refresh_tasks"
        internal const val EXTRA_TASK_ID = "commvne.com.whip.app.widget.TASK_ID"
        internal const val EXTRA_OCCURRENCE_EPOCH_DAY = "commvne.com.whip.app.widget.OCCURRENCE_EPOCH_DAY"
        internal const val EXTRA_RENDERED_DATE_EPOCH_DAY = "commvne.com.whip.app.widget.RENDERED_DATE_EPOCH_DAY"
        internal const val EXTRA_STEP_ID = "commvne.com.whip.app.widget.STEP_ID"
        internal const val EXTRA_COMPLETED = "commvne.com.whip.app.widget.TASK_COMPLETED"
        internal const val EXTRA_EXPANDED = "commvne.com.whip.app.widget.TASK_EXPANDED"
        internal const val EXTRA_TASK_KEY = "commvne.com.whip.app.widget.TASK_KEY"
        internal const val EXTRA_TASK_COLLECTION_ACTION = "commvne.com.whip.app.widget.TASK_COLLECTION_ACTION"
        internal const val NO_DATE = Long.MIN_VALUE

        private val TASK_MUTATION_ACTIONS = setOf(ACTION_COMPLETE_TASK, ACTION_TOGGLE_SUBTASK)

        fun saveScope(context: Context, appWidgetId: Int, scope: AreaScope) =
            WhipWidgetPreferences.saveScope(context, appWidgetId, scope)

        fun loadScope(context: Context, appWidgetId: Int): AreaScope =
            WhipWidgetPreferences.load(context, appWidgetId).areaScope

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val taskIds = manager.getAppWidgetIds(ComponentName(context, WhipWidgetProvider::class.java))
            if (taskIds.isNotEmpty()) WhipWidgetProvider().onUpdate(context, manager, taskIds)
            val habitIds = manager.getAppWidgetIds(ComponentName(context, HabitTrackingWidgetProvider::class.java))
            if (habitIds.isNotEmpty()) HabitTrackingWidgetProvider().onUpdate(context, manager, habitIds)
        }

        fun update(context: Context, appWidgetId: Int) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = manager.getAppWidgetInfo(appWidgetId)?.provider?.className
            if (provider == HabitTrackingWidgetProvider::class.java.name) {
                HabitTrackingWidgetProvider().onUpdate(context, manager, intArrayOf(appWidgetId))
            } else {
                WhipWidgetProvider().onUpdate(context, manager, intArrayOf(appWidgetId))
            }
        }

        fun clearAreaScope(
            context: Context,
            areaId: String,
            appWidgetIds: IntArray? = null,
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val resolvedIds = appWidgetIds ?: (
                manager.getAppWidgetIds(ComponentName(context, WhipWidgetProvider::class.java)) +
                    manager.getAppWidgetIds(ComponentName(context, HabitTrackingWidgetProvider::class.java))
                )
            resolvedIds.forEach { appWidgetId ->
                if (loadScope(context, appWidgetId) == AreaScope.One(areaId)) {
                    saveScope(context, appWidgetId, AreaScope.All)
                }
            }
            if (appWidgetIds == null && resolvedIds.isNotEmpty()) updateAll(context)
        }
    }
}

class HabitTrackingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateHabitTrackingWidgets(context, manager, ids)
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val resolvedAction = if (intent.action == ACTION_COLLECTION_CLICK) {
            intent.getStringExtra(EXTRA_COLLECTION_ACTION)
        } else {
            intent.action
        }
        if (resolvedAction == COLLECTION_SET_EXPANDED) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && habitId >= 0L) {
                WhipWidgetPreferences.setHabitExpanded(
                    context = context,
                    appWidgetId = appWidgetId,
                    habitId = habitId,
                    expanded = intent.getBooleanExtra(EXTRA_EXPANDED, false),
                )
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.widget_habit_list,
                )
            }
            return
        }
        if (resolvedAction == COLLECTION_REFRESH_HABITS) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.widget_habit_list,
                )
            }
            return
        }
        if (resolvedAction == COLLECTION_OPEN_HABIT) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
            if (habitId >= 0L) {
                val scope = WhipWidgetPreferences.load(context, appWidgetId).areaScope
                runCatching {
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .setAction(WhipLaunchActions.ACTION_OPEN_HABIT)
                            .setData(Uri.parse("whip://widget/$appWidgetId/habit/$habitId"))
                            .putExtra(WhipWidgetProvider.EXTRA_AREA_SCOPE, scope.storageKey)
                            .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, habitId)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    )
                }
            }
            return
        }
        if (resolvedAction !in HABIT_ACTIONS) {
            super.onReceive(context, intent)
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as WhipApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                val renderedDate = intent.getLongExtra(EXTRA_DATE_EPOCH_DAY, WhipWidgetProvider.NO_DATE)
                    .takeUnless { it == WhipWidgetProvider.NO_DATE }
                    ?.let(LocalDate::ofEpochDay)
                val today = app.clock.today()
                if (habitId >= 0L && renderedDate == today) {
                    runCatching {
                        val habit = app.habitRepository.get(habitId) ?: return@runCatching
                        if (habit.archived || habit.sourceMetricId != null) return@runCatching
                        when (resolvedAction) {
                            ACTION_TOGGLE_HABIT -> app.habitRepository.setCheckOff(
                                habitId,
                                today,
                                intent.getBooleanExtra(EXTRA_COMPLETED, true),
                            )
                            ACTION_TOGGLE_CHECKLIST_ITEM -> {
                                val itemId = intent.getLongExtra(EXTRA_CHECKLIST_ITEM_ID, -1L)
                                if (itemId >= 0L) {
                                    app.habitRepository.toggleChecklistItem(
                                        habitId,
                                        itemId,
                                        today,
                                        intent.getBooleanExtra(EXTRA_COMPLETED, true),
                                    )
                                }
                            }
                            ACTION_INCREMENT_HABIT -> if (
                                habit.trackingMode in setOf(HabitTrackingMode.Count, HabitTrackingMode.Decimal)
                            ) {
                                app.habitRepository.log(habitId, habit.quickIncrement, date = today)
                            }
                            ACTION_START_HABIT -> if (
                                habit.trackingMode == HabitTrackingMode.Duration && habit.timerStartedAtMillis == null
                            ) app.habitRepository.startTimer(habitId)
                            ACTION_STOP_HABIT -> if (
                                habit.trackingMode == HabitTrackingMode.Duration && habit.timerStartedAtMillis != null
                            ) app.habitRepository.stopTimer(habitId, today)
                        }
                        app.habitReminderScheduler.syncHabit(habitId)
                    }
                }
                WhipWidgetProvider.updateAll(context)
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WhipWidgetPreferences.remove(context, appWidgetIds)
        WidgetSnapshotCache.remove(context, appWidgetIds)
    }

    companion object {
        internal const val ACTION_TOGGLE_HABIT = "commvne.com.whip.app.widget.TOGGLE_HABIT"
        internal const val ACTION_TOGGLE_CHECKLIST_ITEM = "commvne.com.whip.app.widget.TOGGLE_CHECKLIST_ITEM"
        internal const val ACTION_INCREMENT_HABIT = "commvne.com.whip.app.widget.INCREMENT_HABIT"
        internal const val ACTION_START_HABIT = "commvne.com.whip.app.widget.START_HABIT"
        internal const val ACTION_STOP_HABIT = "commvne.com.whip.app.widget.STOP_HABIT"
        internal const val ACTION_COLLECTION_CLICK = "commvne.com.whip.app.widget.HABIT_COLLECTION_CLICK"
        internal const val COLLECTION_OPEN_HABIT = "open_habit"
        internal const val COLLECTION_SET_EXPANDED = "set_expanded"
        internal const val COLLECTION_REFRESH_HABITS = "refresh_habits"
        internal const val EXTRA_HABIT_ID = "commvne.com.whip.app.widget.HABIT_ID"
        internal const val EXTRA_CHECKLIST_ITEM_ID = "commvne.com.whip.app.widget.CHECKLIST_ITEM_ID"
        internal const val EXTRA_DATE_EPOCH_DAY = "commvne.com.whip.app.widget.DATE_EPOCH_DAY"
        internal const val EXTRA_COMPLETED = "commvne.com.whip.app.widget.COMPLETED"
        internal const val EXTRA_EXPANDED = "commvne.com.whip.app.widget.EXPANDED"
        internal const val EXTRA_COLLECTION_ACTION = "commvne.com.whip.app.widget.COLLECTION_ACTION"

        private val HABIT_ACTIONS = setOf(
            ACTION_TOGGLE_HABIT,
            ACTION_TOGGLE_CHECKLIST_ITEM,
            ACTION_INCREMENT_HABIT,
            ACTION_START_HABIT,
            ACTION_STOP_HABIT,
        )
    }
}

private suspend fun updateTaskAgendaWidgets(
    context: Context,
    manager: AppWidgetManager,
    ids: IntArray,
) {
    val app = context.applicationContext as WhipApplication
    val today = app.clock.today()
    // Header metadata is supplemental. Always bind and notify the collection so
    // its factory can show current rows, a saved snapshot, or a retry row.
    val areas = runCatching { app.areaRepository.areas.first() }.getOrNull()
    ids.forEach { id ->
        val preferences = WhipWidgetPreferences.load(context, id)
        val scopeLabel = scopeLabel(context, preferences.areaScope, areas.orEmpty())
        val openAgenda = openSectionIntent(
            context,
            WhipWidgetProvider.ACTION_OPEN_TASK_AGENDA,
            id,
            preferences.areaScope,
            "task-agenda",
        )
        val views = RemoteViews(context.packageName, R.layout.widget_task_agenda).apply {
            applyResponsiveWidgetHeader(context, manager, id, this)
            setInt(android.R.id.background, "setImageAlpha", widgetBackgroundAlpha(preferences.transparencyPercent))
            setTextViewText(
                R.id.widget_subtitle,
                context.getString(R.string.widget_agenda_subtitle, preferences.agendaRange.title, scopeLabel),
            )
            setOnClickPendingIntent(R.id.widget_header, openAgenda)
            setContentDescription(
                R.id.widget_header,
                context.getString(R.string.widget_open_task_agenda_for, scopeLabel),
            )
            setOnClickPendingIntent(
                R.id.widget_add,
                openSectionIntent(
                    context,
                    WhipWidgetProvider.ACTION_ADD_TASK,
                    id,
                    preferences.areaScope,
                    "add-task",
                    occurrenceEpochDay = today.toEpochDay(),
                ),
            )
            setContentDescription(R.id.widget_add, context.getString(R.string.widget_add_task_to, scopeLabel))
            setRemoteAdapter(R.id.widget_task_list, taskCollectionServiceIntent(context, id))
            setPendingIntentTemplate(R.id.widget_task_list, taskCollectionPendingIntent(context, id))
            setEmptyView(R.id.widget_task_list, R.id.widget_empty)
            setOnClickPendingIntent(R.id.widget_empty, openAgenda)
        }
        manager.updateAppWidget(id, views)
        manager.notifyAppWidgetViewDataChanged(id, R.id.widget_task_list)
    }
}

private suspend fun updateHabitTrackingWidgets(
    context: Context,
    manager: AppWidgetManager,
    ids: IntArray,
) {
    val app = context.applicationContext as WhipApplication
    val today = app.clock.today()
    // Keep progress/header loading isolated from the RemoteViews collection.
    // A repository failure must not prevent the adapter from displaying its
    // cache-aware failure state and retry action.
    val calculateContent = runCatching {
        val habits = app.habitRepository.habits.first()
        val logs = app.habitRepository.logs.first()
        val checklistItems = app.habitRepository.checklistItems.first()
        val checklistStates = app.habitRepository.checklistStates.first()
        val pauses = app.habitRepository.pauses.first()
        val skips = app.habitRepository.skips.first()
        val units = app.measurementRepository.customUnits.first()
        val metricEntries = app.measurementRepository.entries.first()
        val contentCalculator: (WidgetPreferences) -> HabitTrackingContent = { preferences ->
            calculateHabitTrackingContent(
                habits = habits,
                habitLogs = logs,
                habitChecklistItems = checklistItems,
                habitChecklistStates = checklistStates,
                habitPauses = pauses,
                habitSkips = skips,
                metricEntries = metricEntries,
                customUnits = units,
                today = today,
                areaScope = preferences.areaScope,
                showCompleted = preferences.showCompletedHabits,
                selectedHabitIds = preferences.selectedHabitIds,
                expandedHabitIds = preferences.expandedHabitIds,
            )
        }
        contentCalculator
    }.getOrNull()
    val areas = runCatching { app.areaRepository.areas.first() }.getOrNull()
    ids.forEach { id ->
        val preferences = WhipWidgetPreferences.load(context, id)
        val content = calculateContent?.invoke(preferences)
        val scopeLabel = scopeLabel(context, preferences.areaScope, areas.orEmpty())
        val openHabits = openSectionIntent(
            context,
            WhipWidgetProvider.ACTION_OPEN_HABIT_TRACKING,
            id,
            preferences.areaScope,
            "habit-tracking",
        )
        val views = RemoteViews(context.packageName, R.layout.widget_habit_tracking).apply {
            applyResponsiveWidgetHeader(context, manager, id, this)
            setInt(android.R.id.background, "setImageAlpha", widgetBackgroundAlpha(preferences.transparencyPercent))
            setTextViewText(
                R.id.widget_subtitle,
                if (content == null) {
                    context.getString(R.string.widget_refresh_needed, scopeLabel)
                } else if (content.scheduledHabits == 0) {
                    context.getString(R.string.widget_today_scope, scopeLabel)
                } else {
                    context.getString(
                        R.string.widget_habit_subtitle,
                        content.completedHabits,
                        content.scheduledHabits,
                        scopeLabel,
                    )
                },
            )
            setOnClickPendingIntent(R.id.widget_header, openHabits)
            setContentDescription(
                R.id.widget_header,
                context.getString(R.string.widget_open_habit_tracking_for, scopeLabel),
            )
            setOnClickPendingIntent(
                R.id.widget_add,
                openSectionIntent(context, WhipWidgetProvider.ACTION_ADD_HABIT, id, preferences.areaScope, "add-habit"),
            )
            setContentDescription(R.id.widget_add, context.getString(R.string.widget_add_habit_to, scopeLabel))
            setRemoteAdapter(R.id.widget_habit_list, habitCollectionServiceIntent(context, id))
            setPendingIntentTemplate(R.id.widget_habit_list, habitCollectionPendingIntent(context, id))
            setEmptyView(R.id.widget_habit_list, R.id.widget_empty)
            setTextViewText(
                R.id.widget_empty,
                when {
                    preferences.selectedHabitIds?.isEmpty() == true ->
                        context.getString(R.string.widget_habits_none_selected)
                    content != null && content.scheduledHabits > 0 && content.completedHabits == content.scheduledHabits ->
                        context.getString(R.string.widget_habits_complete)
                    content == null -> context.getString(R.string.widget_refresh_failed)
                    else -> context.getString(R.string.widget_habit_empty)
                },
            )
            setOnClickPendingIntent(R.id.widget_empty, openHabits)
        }
        manager.updateAppWidget(id, views)
        manager.notifyAppWidgetViewDataChanged(id, R.id.widget_habit_list)
    }
}

/**
 * Preserve one complete collection row at the launcher's supported minimum
 * height. The title and 48 dp Add target are essential; brand and subtitle are
 * progressive detail when height or scaled text makes the header compete with
 * the collection.
 */
private fun applyResponsiveWidgetHeader(
    context: Context,
    manager: AppWidgetManager,
    appWidgetId: Int,
    views: RemoteViews,
) {
    val options = manager.getAppWidgetOptions(appWidgetId)
    val configuration = context.resources.configuration
    val availableHeightDp = availableWidgetHeight(
        minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160),
        maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160),
        landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
    )
    val compactHeader = useCompactWidgetHeader(availableHeightDp, configuration.fontScale)
    val detailVisibility = if (compactHeader) android.view.View.GONE else android.view.View.VISIBLE
    views.setViewVisibility(R.id.widget_brand, detailVisibility)
    views.setViewVisibility(R.id.widget_subtitle, detailVisibility)
}

private fun scopeLabel(
    context: Context,
    areaScope: AreaScope,
    areas: List<com.whip.app.domain.Area>,
): String = when (areaScope) {
    AreaScope.All -> context.getString(R.string.widget_scope_all)
    AreaScope.Unassigned -> context.getString(R.string.widget_scope_unassigned)
    is AreaScope.One -> areas.firstOrNull { it.id == areaScope.areaId }?.let { area ->
        area.name + if (area.archived) context.getString(R.string.widget_scope_archived_suffix) else ""
    } ?: context.getString(R.string.widget_scope_unavailable)
}

private fun openSectionIntent(
    context: Context,
    action: String,
    widgetId: Int,
    areaScope: AreaScope,
    path: String,
    occurrenceEpochDay: Long? = null,
): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .setAction(action)
        .setData(Uri.parse("whip://widget/$widgetId/$path"))
        .putExtra(WhipWidgetProvider.EXTRA_AREA_SCOPE, areaScope.storageKey)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    occurrenceEpochDay?.let { intent.putExtra(WhipLaunchActions.EXTRA_OCCURRENCE_EPOCH_DAY, it) }
    return PendingIntent.getActivity(
        context,
        stableRequestCode(widgetId, path),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun stableRequestCode(widgetId: Int, key: String): Int = 31 * widgetId + key.hashCode()

private fun taskCollectionServiceIntent(context: Context, widgetId: Int): Intent =
    Intent(context, TaskWidgetRemoteViewsService::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        .setData(Uri.parse("whip://widget/$widgetId/task-collection"))

private fun taskCollectionPendingIntent(context: Context, widgetId: Int): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        stableRequestCode(widgetId, "task-collection"),
        Intent(context, WhipWidgetProvider::class.java)
            .setAction(WhipWidgetProvider.ACTION_TASK_COLLECTION_CLICK)
            .setData(Uri.parse("whip://widget/$widgetId/task-collection-click"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

private fun habitCollectionServiceIntent(context: Context, widgetId: Int): Intent =
    Intent(context, HabitWidgetRemoteViewsService::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        .setData(Uri.parse("whip://widget/$widgetId/habit-collection"))

private fun habitCollectionPendingIntent(context: Context, widgetId: Int): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        stableRequestCode(widgetId, "habit-collection"),
        Intent(context, HabitTrackingWidgetProvider::class.java)
            .setAction(HabitTrackingWidgetProvider.ACTION_COLLECTION_CLICK)
            .setData(Uri.parse("whip://widget/$widgetId/habit-collection-click"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

private fun openTaskFromCollection(context: Context, intent: Intent) {
    val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID,
    )
    val taskId = intent.getLongExtra(WhipWidgetProvider.EXTRA_TASK_ID, -1L)
    if (taskId < 0L) return
    val occurrence = intent.getLongExtra(
        WhipWidgetProvider.EXTRA_OCCURRENCE_EPOCH_DAY,
        WhipWidgetProvider.NO_DATE,
    )
    val path = "task/$taskId/${occurrence.takeUnless { it == WhipWidgetProvider.NO_DATE } ?: "task"}"
    val scope = WhipWidgetPreferences.load(context, appWidgetId).areaScope
    runCatching {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .setAction(WhipLaunchActions.ACTION_OPEN_TASK)
                .setData(Uri.parse("whip://widget/$appWidgetId/$path"))
                .putExtra(WhipWidgetProvider.EXTRA_AREA_SCOPE, scope.storageKey)
                .putExtra(WhipLaunchActions.EXTRA_ENTITY_ID, taskId)
                .apply {
                    occurrence.takeUnless { it == WhipWidgetProvider.NO_DATE }?.let {
                        putExtra(WhipLaunchActions.EXTRA_OCCURRENCE_EPOCH_DAY, it)
                    }
                }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }
}

private suspend fun currentTaskAgendaItem(
    context: Context,
    appWidgetId: Int,
    taskId: Long,
    originalDate: LocalDate?,
) = if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
    null
} else {
    val app = context.applicationContext as WhipApplication
    val preferences = WhipWidgetPreferences.load(context, appWidgetId)
    calculateTaskAgendaContent(
        tasks = app.taskRepository.tasks.first(),
        taskOccurrences = app.taskRepository.occurrences.first(),
        taskSteps = app.taskRepository.steps.first(),
        taskStepStates = app.taskRepository.stepStates.first(),
        taskStepSnapshots = app.taskRepository.stepSnapshots.first(),
        today = app.clock.today(),
        areaScope = preferences.areaScope,
        range = preferences.agendaRange,
        zoneId = app.settingsRepository.current().zoneId(),
    ).items.firstOrNull { it.task.id == taskId && it.originalDate == originalDate }
}
