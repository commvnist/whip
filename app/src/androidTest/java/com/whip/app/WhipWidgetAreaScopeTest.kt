package com.whip.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.AreaScope
import com.whip.app.widget.WhipWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipWidgetAreaScopeTest {
    @Test
    fun eachWidgetKeepsAnIndependentAreaScopeAndDeletionCleansItUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WhipWidgetProvider.saveScope(context, 101, AreaScope.One("work"))
        WhipWidgetProvider.saveScope(context, 202, AreaScope.One("health"))

        assertEquals(AreaScope.One("work"), WhipWidgetProvider.loadScope(context, 101))
        assertEquals(AreaScope.One("health"), WhipWidgetProvider.loadScope(context, 202))

        WhipWidgetProvider().onDeleted(context, intArrayOf(101))
        assertEquals(AreaScope.All, WhipWidgetProvider.loadScope(context, 101))
        assertEquals(AreaScope.One("health"), WhipWidgetProvider.loadScope(context, 202))
        WhipWidgetProvider().onDeleted(context, intArrayOf(202))
    }
}
