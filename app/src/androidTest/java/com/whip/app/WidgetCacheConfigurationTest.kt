package com.whip.app

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
