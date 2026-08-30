package com.whip.app

import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.graphics.ColorUtils
import com.whip.app.domain.AreaScope
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.widget.AgendaRange
import com.whip.app.widget.TaskWidgetRemoteViewsFactory
import com.whip.app.widget.HabitWidgetRemoteViewsFactory
import com.whip.app.widget.WhipWidgetPreferences
import com.whip.app.widget.WidgetPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetExtremeTextTest {
    @Test
    fun widgetResourcesProvideDistinctHighContrastDayAndNightPalettes() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        fun themedContext(night: Boolean): android.content.Context {
            val configuration = Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            return app.createConfigurationContext(configuration)
        }

        val day = themedContext(false)
        val night = themedContext(true)
        val daySurface = day.getColor(R.color.widget_surface)
        val dayText = day.getColor(R.color.widget_primary_text)
        val nightSurface = night.getColor(R.color.widget_surface)
        val nightText = night.getColor(R.color.widget_primary_text)

        assertNotEquals(daySurface, nightSurface)
        assertTrue(ColorUtils.calculateContrast(dayText, daySurface) >= 4.5)
        assertTrue(ColorUtils.calculateContrast(nightText, nightSurface) >= 4.5)
    }

    @Test
    fun minimumHabitWidgetRetainsPrimaryLabelAndTrailingActionAtExtremeTextScale() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val habitId = app.habitRepository.create(
            HabitDraft(
                name = "A long but quickly checkable daily habit",
                trackingMode = HabitTrackingMode.CheckOff,
                startDate = app.clock.today(),
            ),
        )
        val configuration = Configuration(app.resources.configuration).apply { fontScale = 3.2f }
        val scaledContext = app.createConfigurationContext(configuration)
        val widgetId = 83_202
        WhipWidgetPreferences.save(
            app,
            widgetId,
            WidgetPreferences(areaScope = AreaScope.All, selectedHabitIds = setOf(habitId)),
        )
        val factory = HabitWidgetRemoteViewsFactory(scaledContext, widgetId)

        factory.onDataSetChanged()

        assertTrue(factory.getCount() >= 1)
        val row = requireNotNull(factory.getViewAt(0)).apply(scaledContext, FrameLayout(scaledContext))
        val title = row.findViewById<TextView>(R.id.widget_row_title)
        val meta = row.findViewById<TextView>(R.id.widget_row_meta)
        val action = row.findViewById<View>(R.id.widget_row_action)
        row.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        row.layout(0, 0, row.measuredWidth, row.measuredHeight)

        assertEquals(1, title.maxLines)
        assertEquals(View.GONE, meta.visibility)
        assertTrue("Extreme text must retain the 48 dp habit target", action.width >= 48 * scaledContext.resources.displayMetrics.density)
        WhipWidgetPreferences.remove(app, intArrayOf(widgetId))
    }

    @Test
    fun minimumTaskWidgetRetainsOnePrimaryRowAndTrailingActionAtExtremeTextScale() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val today = app.clock.today()
        app.taskRepository.create(
            TaskDraft(
                title = "A long but actionable agenda item",
                scheduleKind = ScheduleKind.Once,
                date = today,
                inbox = false,
            ),
        )
        val configuration = Configuration(app.resources.configuration).apply { fontScale = 3.2f }
        val scaledContext = app.createConfigurationContext(configuration)
        val widgetId = 83_201
        WhipWidgetPreferences.save(
            app,
            widgetId,
            WidgetPreferences(areaScope = AreaScope.All, agendaRange = AgendaRange.Today),
        )
        val factory = TaskWidgetRemoteViewsFactory(scaledContext, widgetId)

        factory.onDataSetChanged()

        assertTrue(factory.getCount() >= 1)
        val row = requireNotNull(factory.getViewAt(0)).apply(scaledContext, FrameLayout(scaledContext))
        val title = row.findViewById<TextView>(R.id.widget_row_title)
        val meta = row.findViewById<TextView>(R.id.widget_row_meta)
        val action = row.findViewById<View>(R.id.widget_row_action)
        row.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        row.layout(0, 0, row.measuredWidth, row.measuredHeight)

        assertEquals(1, title.maxLines)
        assertEquals(View.GONE, meta.visibility)
        assertTrue("Extreme text must retain the 48 dp completion target", action.width >= 48 * scaledContext.resources.displayMetrics.density)
        WhipWidgetPreferences.remove(app, intArrayOf(widgetId))
    }

    @Test
    fun expandableHabitMovesDisclosureBelowInsteadOfCrushingTextAtExtremeScale() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        val habitId = app.habitRepository.create(
            HabitDraft(
                name = "A long checklist Habit whose identity must stay readable",
                trackingMode = HabitTrackingMode.Checklist,
                checklistItems = listOf(HabitChecklistItemDraft("First item", 0)),
                startDate = app.clock.today(),
            ),
        )
        val configuration = Configuration(app.resources.configuration).apply { fontScale = 3.2f }
        val scaledContext = app.createConfigurationContext(configuration)
        val widgetId = 83_203
        WhipWidgetPreferences.save(
            app,
            widgetId,
            WidgetPreferences(
                selectedHabitIds = setOf(habitId),
                expandedHabitIds = setOf(habitId),
            ),
        )
        val factory = HabitWidgetRemoteViewsFactory(scaledContext, widgetId)
        factory.onDataSetChanged()

        val row = requireNotNull(factory.getViewAt(0)).apply(scaledContext, FrameLayout(scaledContext))
        row.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        row.layout(0, 0, row.measuredWidth, row.measuredHeight)
        val body = row.findViewById<View>(R.id.widget_row_body)
        val action = row.findViewById<View>(R.id.widget_row_action)
        val disclosure = row.findViewById<View>(R.id.widget_row_expand)

        assertTrue(action.left >= body.right)
        assertTrue("Large-text disclosure belongs below the identity row", disclosure.top >= action.bottom)
        assertTrue(disclosure.width >= row.width)
        factory.onDestroy()
        WhipWidgetPreferences.remove(app, intArrayOf(widgetId))
    }
}
