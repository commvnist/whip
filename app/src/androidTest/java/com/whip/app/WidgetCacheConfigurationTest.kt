package com.whip.app

import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.whip.app.widget.HabitWidgetRemoteViewsFactory
import com.whip.app.widget.TaskWidgetRemoteViewsFactory
import com.whip.app.widget.WidgetActionStatus
import com.whip.app.widget.performWidgetMutation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.AreaScope
import com.whip.app.widget.AgendaRange
import com.whip.app.widget.CachedWidgetRow
import com.whip.app.widget.WhipWidgetPreferences
import com.whip.app.widget.WidgetPreferences
import com.whip.app.widget.WidgetSnapshotCache
import com.whip.app.widget.WidgetSnapshotKind
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetCacheConfigurationTest {
    @Test
    fun failedWidgetWriteAndSavedReminderWarningRemainDistinctAndNeverRepeatTheWrite() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val widgetId = 91_205
        val kind = WidgetSnapshotKind.HabitTracking
        val scaled = app.createConfigurationContext(Configuration(app.resources.configuration).apply { fontScale = 3.2f })
        var writes = 0
        var reminderAttempts = 0
        try {
            performWidgetMutation(app, kind, widgetId, mutate = { error("Injected write failure") },
                synchronize = { reminderAttempts++ })
            assertEquals(WidgetActionStatus.Failed, WidgetSnapshotCache.actionStatus(app, kind, widgetId))
            assertEquals(0, reminderAttempts)
            val factory = HabitWidgetRemoteViewsFactory(scaled, widgetId)
            factory.onDataSetChanged()
            val failedRow = requireNotNull(factory.getViewAt(0)).apply(scaled, FrameLayout(scaled))
            assertEquals("Action Not Saved", failedRow.findViewById<TextView>(R.id.widget_row_title).text.toString())
            assertTrue(failedRow.findViewById<View>(R.id.widget_row).contentDescription.toString().contains("try again"))
            performWidgetMutation(app, kind, widgetId, mutate = { writes++; true },
                synchronize = { reminderAttempts++; error("Injected reminder failure") })
            assertEquals(WidgetActionStatus.SavedWithWarning, WidgetSnapshotCache.actionStatus(app, kind, widgetId))
            factory.onDataSetChanged()
            val savedRow = requireNotNull(factory.getViewAt(0)).apply(scaled, FrameLayout(scaled))
            assertEquals("Action Saved", savedRow.findViewById<TextView>(R.id.widget_row_title).text.toString())
            assertTrue(savedRow.findViewById<View>(R.id.widget_row).contentDescription.toString().contains("Your change is saved"))
            // The status action only acknowledges/refreshes; it cannot reapply the mutation.
            WidgetSnapshotCache.setActionStatus(app, kind, widgetId, null)
            factory.onDataSetChanged()
            assertEquals(1, writes)
            assertEquals(1, reminderAttempts)
        } finally {
            WidgetSnapshotCache.remove(app, intArrayOf(widgetId))
        }
    }

    @Test
    fun staleWidgetActionsAndReplacedDataNeverReuseAnOldActionResult() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val widgetId = 91_206
        val kind = WidgetSnapshotKind.TaskAgenda
        try {
            performWidgetMutation(app, kind, widgetId, mutate = { false }, synchronize = { error("Must not synchronize stale action") })
            assertEquals(WidgetActionStatus.Stale, WidgetSnapshotCache.actionStatus(app, kind, widgetId))
            val factory = TaskWidgetRemoteViewsFactory(app, widgetId)
            factory.onDataSetChanged()
            val row = requireNotNull(factory.getViewAt(0)).apply(app, FrameLayout(app))
            assertEquals("Previous Action Not Applied", row.findViewById<TextView>(R.id.widget_row_title).text.toString())
            assertNull(WidgetSnapshotCache.actionStatus(app, kind, widgetId, app.currentUserDataGeneration() + 1))
            assertNull(WidgetSnapshotCache.actionStatus(app, kind, widgetId))
        } finally {
            WidgetSnapshotCache.remove(app, intArrayOf(widgetId))
        }
    }

    @Test
    fun savingWidgetConfigurationInvalidatesDisplayDataFromThePreviousScope() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val widgetId = 91_204
        WidgetSnapshotCache.save(
            context,
            WidgetSnapshotKind.TaskAgenda,
            widgetId,
            listOf(CachedWidgetRow("Old scoped task", "Today", isChild = false, completed = false)),
            savedAtMillis = 42,
        )

        WhipWidgetPreferences.save(
            context,
            widgetId,
            WidgetPreferences(
                areaScope = AreaScope.One("work"),
                agendaRange = AgendaRange.ThirtyDays,
            ),
        )

        assertNull(WidgetSnapshotCache.load(context, WidgetSnapshotKind.TaskAgenda, widgetId))
        WhipWidgetPreferences.remove(context, intArrayOf(widgetId))
    }
}
