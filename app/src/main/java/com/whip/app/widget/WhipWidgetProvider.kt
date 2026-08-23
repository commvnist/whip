package com.whip.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.edit
import com.whip.app.MainActivity
import com.whip.app.R
import com.whip.app.WhipApplication
import com.whip.app.domain.Habit
import com.whip.app.domain.WhipTask
import com.whip.app.domain.isScheduledOn
import com.whip.app.domain.hasEnded
import com.whip.app.domain.AreaScope
import com.whip.app.domain.matches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WhipWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext as WhipApplication
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val today = app.clock.today()
                val tasks = app.taskRepository.tasks.first()
                val habitLogs = app.habitRepository.logs.first()
                val units = app.measurementRepository.customUnits.first()
                val habits = app.habitRepository.habits.first()
                val areas = app.areaRepository.areas.first()
                ids.forEach { id ->
                    val areaScope = loadScope(context, id)
                    val openTasks = tasks.count { !it.archived && it.completedAtMillis == null && areaScope.matches(it.areaId) }
                    val dueHabits = habits.count { habit ->
                        areaScope.matches(habit.areaId) && !habit.archived && habit.isScheduledOn(today) &&
                            !habit.hasEnded(habitLogs.filter { it.habitId == habit.id }, today, customUnits = units)
                    }
                    val scopeLabel = when (areaScope) {
                        AreaScope.All -> "All areas"
                        AreaScope.Unassigned -> "No area"
                        is AreaScope.One -> areas.firstOrNull { it.id == areaScope.areaId }?.name ?: "All areas"
                    }
                    val views = RemoteViews(context.packageName, R.layout.whip_widget).apply {
                        setTextViewText(R.id.widget_date, "$today · $scopeLabel")
                        setTextViewText(R.id.widget_summary, "$openTasks open tasks · $dueHabits habits due")
                        setOnClickPendingIntent(R.id.widget_root, launch(context, ACTION_OPEN, id * 10 + 1, areaScope))
                        setOnClickPendingIntent(R.id.widget_add_task, launch(context, ACTION_ADD_TASK, id * 10 + 2, areaScope))
                        setOnClickPendingIntent(R.id.widget_add_habit, launch(context, ACTION_ADD_HABIT, id * 10 + 3, areaScope))
                    }
                    manager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            appWidgetIds.forEach { remove(scopeKey(it)) }
        }
    }

    companion object {
        const val ACTION_OPEN = "commvne.com.whip.app.widget.OPEN"
        const val ACTION_ADD_TASK = "commvne.com.whip.app.widget.ADD_TASK"
        const val ACTION_ADD_HABIT = "commvne.com.whip.app.widget.ADD_HABIT"
        const val EXTRA_AREA_SCOPE = "commvne.com.whip.app.widget.AREA_SCOPE"
        private const val PREFS = "whip_widget_areas"

        fun saveScope(context: Context, appWidgetId: Int, scope: AreaScope) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(scopeKey(appWidgetId), scope.storageKey) }
        }

        fun loadScope(context: Context, appWidgetId: Int): AreaScope = AreaScope.fromStorageKey(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(scopeKey(appWidgetId), AreaScope.All.storageKey),
        )

        private fun scopeKey(appWidgetId: Int) = "scope_$appWidgetId"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WhipWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) WhipWidgetProvider().onUpdate(context, manager, ids)
        }

        fun update(context: Context, appWidgetId: Int) {
            WhipWidgetProvider().onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(appWidgetId))
        }

        fun clearAreaScope(
            context: Context,
            areaId: String,
            appWidgetIds: IntArray? = null,
        ) {
            val resolvedIds = appWidgetIds ?: AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, WhipWidgetProvider::class.java))
            resolvedIds.forEach { appWidgetId ->
                if (loadScope(context, appWidgetId) == AreaScope.One(areaId)) {
                    saveScope(context, appWidgetId, AreaScope.All)
                }
            }
            if (appWidgetIds == null && resolvedIds.isNotEmpty()) {
                WhipWidgetProvider().onUpdate(context, AppWidgetManager.getInstance(context), resolvedIds)
            }
        }

        private fun launch(context: Context, action: String, requestCode: Int, areaScope: AreaScope): PendingIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                Intent(context, MainActivity::class.java).setAction(action)
                    .putExtra(EXTRA_AREA_SCOPE, areaScope.storageKey)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
