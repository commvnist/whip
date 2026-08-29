package com.whip.app

import android.content.Intent
import android.appwidget.AppWidgetManager
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.AreaScope
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.widget.HabitTrackingWidgetProvider
import com.whip.app.widget.HabitWidgetRemoteViewsFactory
import com.whip.app.widget.TaskWidgetRemoteViewsFactory
import com.whip.app.widget.WhipWidgetProvider
import com.whip.app.widget.WhipWidgetPreferences
import com.whip.app.widget.WidgetPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipWidgetAreaScopeTest {
    @Test
    fun widgetLayoutUsesOnlyRemoteViewsSafeClasses() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val taskAgenda = RemoteViews(context.packageName, R.layout.widget_task_agenda)
            .apply(context, FrameLayout(context))
        val habitTracking = RemoteViews(context.packageName, R.layout.widget_habit_tracking)
            .apply(context, FrameLayout(context))
        val taskRow = RemoteViews(context.packageName, R.layout.widget_task_row)
            .apply(context, FrameLayout(context))
        val habitRow = RemoteViews(context.packageName, R.layout.widget_habit_row)
            .apply(context, FrameLayout(context))
        val childRow = RemoteViews(context.packageName, R.layout.widget_child_row)
            .apply(context, FrameLayout(context))
        assertNotNull(taskAgenda.findViewById<android.view.View>(R.id.widget_header))
        assertNotNull(taskAgenda.findViewById<android.view.View>(R.id.widget_add))
        assertNotNull(taskAgenda.findViewById<android.view.View>(R.id.widget_task_list))
        assertNotNull(habitTracking.findViewById<android.view.View>(R.id.widget_header))
        assertNotNull(habitTracking.findViewById<android.view.View>(R.id.widget_add))
        assertNotNull(habitTracking.findViewById<android.view.View>(R.id.widget_habit_list))
        assertNotNull(taskRow.findViewById<android.view.View>(R.id.widget_row_action))
        assertNotNull(taskRow.findViewById<android.view.View>(R.id.widget_row_action_icon))
        assertNotNull(taskRow.findViewById<android.view.View>(R.id.widget_row_action_label))
        assertNotNull(taskRow.findViewById<android.view.View>(R.id.widget_row_expand))
        assertNotNull(habitRow.findViewById<android.view.View>(R.id.widget_row_action_icon))
        assertNotNull(habitRow.findViewById<android.view.View>(R.id.widget_row_action_label))
        assertNotNull(habitRow.findViewById<android.view.View>(R.id.widget_row_expand))
        assertNotNull(childRow.findViewById<android.view.View>(R.id.widget_row_action_icon))
        assertNotNull(childRow.findViewById<android.view.View>(R.id.widget_row_body))
        childRow.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        childRow.layout(0, 0, childRow.measuredWidth, childRow.measuredHeight)
        val childBody = childRow.findViewById<View>(R.id.widget_row_body)
        val childAction = childRow.findViewById<View>(R.id.widget_row_action_icon)
        assertTrue("Widget child completion must trail its text", childAction.left >= childBody.right)
        assertTrue("Widget child completion target must remain 48 dp", childAction.width >= 48 * context.resources.displayMetrics.density)
    }

    @Test
    fun habitWidgetPersistsSelectionAndExpansionIndependentlyPerWidget() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WhipWidgetPreferences.save(
            context,
            303,
            WidgetPreferences(
                selectedHabitIds = setOf(11L, 22L),
                expandedHabitIds = setOf(22L),
                expandedTaskKeys = setOf("1:20000"),
            ),
        )

        assertEquals(setOf(11L, 22L), WhipWidgetPreferences.load(context, 303).selectedHabitIds)
        assertEquals(setOf(22L), WhipWidgetPreferences.load(context, 303).expandedHabitIds)
        assertEquals(setOf("1:20000"), WhipWidgetPreferences.load(context, 303).expandedTaskKeys)

        WhipWidgetPreferences.setHabitExpanded(context, 303, 11, expanded = true)
        assertEquals(setOf(11L, 22L), WhipWidgetPreferences.load(context, 303).expandedHabitIds)
        WhipWidgetPreferences.setTaskExpanded(context, 303, "2:20001", expanded = true)
        assertEquals(
            setOf("1:20000", "2:20001"),
            WhipWidgetPreferences.load(context, 303).expandedTaskKeys,
        )
        HabitTrackingWidgetProvider().onDeleted(context, intArrayOf(303))
        assertEquals(null, WhipWidgetPreferences.load(context, 303).selectedHabitIds)
        assertTrue(WhipWidgetPreferences.load(context, 303).expandedHabitIds.isEmpty())
        assertTrue(WhipWidgetPreferences.load(context, 303).expandedTaskKeys.isEmpty())
    }

    @Test
    fun taskCollectionReturnsEveryAgendaTaskAndExpandsSubtaskRows() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        val expandableId = app.taskRepository.create(
            TaskDraft(
                title = "Expandable task",
                scheduleKind = ScheduleKind.Once,
                date = today,
                inbox = false,
                steps = listOf(
                    TaskStepDraft(title = "First step", position = 0),
                    TaskStepDraft(title = "Second step", position = 1),
                ),
            ),
        )
        repeat(8) { index ->
            app.taskRepository.create(
                TaskDraft(
                    title = "Scrollable Task ${index + 1}",
                    scheduleKind = ScheduleKind.Once,
                    date = today,
                    inbox = false,
                ),
            )
        }
        val widgetId = 405
        WhipWidgetPreferences.save(app, widgetId, WidgetPreferences(agendaRange = com.whip.app.widget.AgendaRange.Today))
        val factory = TaskWidgetRemoteViewsFactory(app, widgetId)

        factory.onDataSetChanged()
        assertEquals(2, factory.getViewTypeCount())
        assertEquals(9, factory.getCount())

        val taskKey = "$expandableId:${today.toEpochDay()}"
        WhipWidgetProvider().onReceive(
            app,
            Intent(app, WhipWidgetProvider::class.java)
                .setAction(WhipWidgetProvider.ACTION_TASK_COLLECTION_CLICK)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(
                    WhipWidgetProvider.EXTRA_TASK_COLLECTION_ACTION,
                    WhipWidgetProvider.COLLECTION_SET_TASK_EXPANDED,
                )
                .putExtra(WhipWidgetProvider.EXTRA_TASK_KEY, taskKey)
                .putExtra(WhipWidgetProvider.EXTRA_EXPANDED, true),
        )
        factory.onDataSetChanged()
        assertEquals(11, factory.getCount())
        factory.onDestroy()
        WhipWidgetProvider().onDeleted(app, intArrayOf(widgetId))
    }

    @Test
    fun habitCollectionReturnsEverySelectedHabitAndExpandsChecklistRows() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        val selectedId = app.habitRepository.create(
            HabitDraft(
                name = "Selected checklist",
                trackingMode = HabitTrackingMode.Checklist,
                checklistItems = listOf(HabitChecklistItemDraft("Water", 0)),
                startDate = today,
            ),
        )
        val selectedIds = buildSet {
            add(selectedId)
            repeat(8) { index ->
                add(
                    app.habitRepository.create(
                        HabitDraft(
                            name = "Scrollable Habit ${index + 1}",
                            trackingMode = HabitTrackingMode.CheckOff,
                            startDate = today,
                        ),
                    ),
                )
            }
        }
        app.habitRepository.create(
            HabitDraft(
                name = "Not on this widget",
                trackingMode = HabitTrackingMode.CheckOff,
                startDate = today,
            ),
        )
        val widgetId = 404
        WhipWidgetPreferences.save(
            app,
            widgetId,
            WidgetPreferences(selectedHabitIds = selectedIds),
        )
        val factory = HabitWidgetRemoteViewsFactory(app, widgetId)

        factory.onDataSetChanged()
        assertEquals(2, factory.getViewTypeCount())
        assertEquals(9, factory.getCount())

        HabitTrackingWidgetProvider().onReceive(
            app,
            Intent(app, HabitTrackingWidgetProvider::class.java)
                .setAction(HabitTrackingWidgetProvider.ACTION_COLLECTION_CLICK)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(
                    HabitTrackingWidgetProvider.EXTRA_COLLECTION_ACTION,
                    HabitTrackingWidgetProvider.COLLECTION_SET_EXPANDED,
                )
                .putExtra(HabitTrackingWidgetProvider.EXTRA_HABIT_ID, selectedId)
                .putExtra(HabitTrackingWidgetProvider.EXTRA_EXPANDED, true),
        )
        factory.onDataSetChanged()
        assertEquals(10, factory.getCount())
        factory.onDestroy()
        HabitTrackingWidgetProvider().onDeleted(app, intArrayOf(widgetId))
    }

    @Test
    fun eachWidgetKeepsAnIndependentAreaScopeAndDeletionCleansItUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WhipWidgetProvider.saveScope(context, 101, AreaScope.One("work"))
        WhipWidgetProvider.saveScope(context, 202, AreaScope.One("health"))

        assertEquals(AreaScope.One("work"), WhipWidgetProvider.loadScope(context, 101))
        assertEquals(AreaScope.One("health"), WhipWidgetProvider.loadScope(context, 202))

        WhipWidgetProvider.clearAreaScope(context, "work", intArrayOf(101, 202))
        assertEquals(AreaScope.All, WhipWidgetProvider.loadScope(context, 101))
        assertEquals(AreaScope.One("health"), WhipWidgetProvider.loadScope(context, 202))

        WhipWidgetProvider().onDeleted(context, intArrayOf(101))
        assertEquals(AreaScope.All, WhipWidgetProvider.loadScope(context, 101))
        assertEquals(AreaScope.One("health"), WhipWidgetProvider.loadScope(context, 202))
        HabitTrackingWidgetProvider().onDeleted(context, intArrayOf(202))
    }

    @Test
    fun taskAgendaCompletionActionCompletesTheExactOccurrence() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        val taskId = app.taskRepository.create(
            TaskDraft(
                title = "Complete from widget",
                scheduleKind = ScheduleKind.Once,
                date = today,
                inbox = false,
            ),
        )

        WhipWidgetProvider().onReceive(
            app,
            Intent(app, WhipWidgetProvider::class.java)
                .setAction(WhipWidgetProvider.ACTION_COMPLETE_TASK)
                .putExtra(WhipWidgetProvider.EXTRA_TASK_ID, taskId)
                .putExtra(WhipWidgetProvider.EXTRA_OCCURRENCE_EPOCH_DAY, today.toEpochDay()),
        )

        val completed = withTimeout(5_000) {
            app.taskRepository.tasks.first { tasks ->
                tasks.firstOrNull { it.id == taskId }?.completedAtMillis != null
            }
        }
        assertNotNull(completed.single { it.id == taskId }.completedAtMillis)
    }

    @Test
    fun taskWidgetSubtaskActionChecksTheExactChild() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        val taskId = app.taskRepository.create(
            TaskDraft(
                title = "Check child from widget",
                scheduleKind = ScheduleKind.Once,
                date = today,
                inbox = false,
                autoCompleteFromSteps = false,
                steps = listOf(TaskStepDraft(title = "Widget child", position = 0)),
            ),
        )
        val stepId = app.taskRepository.steps.first().single { it.taskId == taskId }.id
        val widgetId = 406
        WhipWidgetPreferences.save(app, widgetId, WidgetPreferences(agendaRange = com.whip.app.widget.AgendaRange.Today))

        WhipWidgetProvider().onReceive(
            app,
            Intent(app, WhipWidgetProvider::class.java)
                .setAction(WhipWidgetProvider.ACTION_TASK_COLLECTION_CLICK)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(
                    WhipWidgetProvider.EXTRA_TASK_COLLECTION_ACTION,
                    WhipWidgetProvider.ACTION_TOGGLE_SUBTASK,
                )
                .putExtra(WhipWidgetProvider.EXTRA_TASK_ID, taskId)
                .putExtra(WhipWidgetProvider.EXTRA_OCCURRENCE_EPOCH_DAY, today.toEpochDay())
                .putExtra(WhipWidgetProvider.EXTRA_RENDERED_DATE_EPOCH_DAY, today.toEpochDay())
                .putExtra(WhipWidgetProvider.EXTRA_STEP_ID, stepId)
                .putExtra(WhipWidgetProvider.EXTRA_COMPLETED, true),
        )

        val state = withTimeout(5_000) {
            app.taskRepository.stepStates.first { states ->
                states.any { it.taskId == taskId && it.stepId == stepId && it.completed }
            }
        }
        assertTrue(state.single { it.taskId == taskId && it.stepId == stepId }.completed)
        WhipWidgetProvider().onDeleted(app, intArrayOf(widgetId))
    }

    @Test
    fun taskWidgetParentCompletionExpandsInsteadOfBypassingUnfinishedSubtasks() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        val taskId = app.taskRepository.create(
            TaskDraft(
                title = "Review children before completion",
                scheduleKind = ScheduleKind.Once,
                date = today,
                inbox = false,
                autoCompleteFromSteps = false,
                steps = listOf(TaskStepDraft(title = "Still unfinished", position = 0)),
            ),
        )
        val widgetId = 407
        WhipWidgetPreferences.save(app, widgetId, WidgetPreferences(agendaRange = com.whip.app.widget.AgendaRange.Today))
        val taskKey = "$taskId:${today.toEpochDay()}"

        WhipWidgetProvider().onReceive(
            app,
            Intent(app, WhipWidgetProvider::class.java)
                .setAction(WhipWidgetProvider.ACTION_TASK_COLLECTION_CLICK)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(
                    WhipWidgetProvider.EXTRA_TASK_COLLECTION_ACTION,
                    WhipWidgetProvider.ACTION_COMPLETE_TASK,
                )
                .putExtra(WhipWidgetProvider.EXTRA_TASK_ID, taskId)
                .putExtra(WhipWidgetProvider.EXTRA_OCCURRENCE_EPOCH_DAY, today.toEpochDay())
                .putExtra(WhipWidgetProvider.EXTRA_RENDERED_DATE_EPOCH_DAY, today.toEpochDay()),
        )

        withTimeout(5_000) {
            while (taskKey !in WhipWidgetPreferences.load(app, widgetId).expandedTaskKeys) delay(20)
        }
        assertEquals(null, app.taskRepository.tasks.first().single { it.id == taskId }.completedAtMillis)
        WhipWidgetProvider().onDeleted(app, intArrayOf(widgetId))
    }

    @Test
    fun habitWidgetChecklistActionChecksTheChildAndAutoCompletesTheHabit() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        val habitId = app.habitRepository.create(
            HabitDraft(
                name = "Morning routine",
                trackingMode = HabitTrackingMode.Checklist,
                checklistItems = listOf(HabitChecklistItemDraft("Water", 0)),
                startDate = today,
            ),
        )
        val itemId = app.habitRepository.checklistItems.first().single { it.habitId == habitId }.id

        HabitTrackingWidgetProvider().onReceive(
            app,
            Intent(app, HabitTrackingWidgetProvider::class.java)
                .setAction(HabitTrackingWidgetProvider.ACTION_COLLECTION_CLICK)
                .putExtra(
                    HabitTrackingWidgetProvider.EXTRA_COLLECTION_ACTION,
                    HabitTrackingWidgetProvider.ACTION_TOGGLE_CHECKLIST_ITEM,
                )
                .putExtra(HabitTrackingWidgetProvider.EXTRA_HABIT_ID, habitId)
                .putExtra(HabitTrackingWidgetProvider.EXTRA_CHECKLIST_ITEM_ID, itemId)
                .putExtra(HabitTrackingWidgetProvider.EXTRA_DATE_EPOCH_DAY, today.toEpochDay())
                .putExtra(HabitTrackingWidgetProvider.EXTRA_COMPLETED, true),
        )

        val state = withTimeout(5_000) {
            app.habitRepository.checklistStates.first { states ->
                states.any { it.habitId == habitId && it.itemId == itemId && it.completed }
            }
        }
        assertTrue(state.single { it.habitId == habitId && it.itemId == itemId }.completed)
        val logs = withTimeout(5_000) {
            app.habitRepository.logs.first { logs -> logs.any { it.habitId == habitId } }
        }
        assertTrue(logs.any { it.habitId == habitId && (it.value ?: 0.0) > 0.0 })
    }
}
