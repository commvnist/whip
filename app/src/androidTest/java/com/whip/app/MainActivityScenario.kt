package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.view.Display
import androidx.test.core.app.ActivityScenario

/** Launches on the default display without passing unsupported options on API 26–27. */
internal fun launchMainActivity(intent: Intent): ActivityScenario<MainActivity> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ActivityScenario.launch(
            intent,
            ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle(),
        )
    } else {
        ActivityScenario.launch(intent)
    }
