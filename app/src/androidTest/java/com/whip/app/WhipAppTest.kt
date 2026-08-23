package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test

class WhipAppTest {
    @Test
    fun appLaunchesAndRendersComposeContent() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        )
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            it.onActivity { activity ->
                val root = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue(root.isAttachedToWindow)
                assertTrue(root.containsComposeView())
            }
        }
    }
}

private fun View.containsComposeView(): Boolean {
    if (javaClass.name.endsWith("AndroidComposeView")) return true
    if (this !is ViewGroup) return false
    return (0 until childCount).any { getChildAt(it).containsComposeView() }
}
