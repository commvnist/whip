package com.whip.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.StaleObjectException

/** Launch the product Activity, never whichever benchmark fixture One UI last kept in Recents. */
internal fun MacrobenchmarkScope.startWhipActivityAndWait() {
    startActivityAndWait(
        Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(PACKAGE_NAME, "com.whip.app.MainActivity")
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        },
    )
}

internal fun UiDevice.requireObject(
    selector: BySelector,
    description: String,
    timeoutMillis: Long = 10_000,
): UiObject2 =
    checkNotNull(wait(Until.findObject(selector), timeoutMillis)) {
        "Benchmark UI did not show $description"
    }

internal fun UiDevice.clickObject(
    selector: BySelector,
    description: String,
    timeoutMillis: Long = 10_000,
) {
    val deadline = android.os.SystemClock.elapsedRealtime() + timeoutMillis
    var lastStale: StaleObjectException? = null
    while (android.os.SystemClock.elapsedRealtime() < deadline) {
        val candidate = wait(Until.findObject(selector), 1_000) ?: continue
        try {
            val center = candidate.visibleCenter
            executeShellCommand("input tap ${center.x} ${center.y}")
            return
        } catch (stale: StaleObjectException) {
            lastStale = stale
            waitForIdle(1_000)
        }
    }
    throw IllegalStateException("Benchmark UI did not provide a stable $description", lastStale)
}

internal fun UiDevice.completeOnboardingIfNeeded() {
    wait(Until.findObject(By.text("Skip · simple mode")), 3_000)?.let { setup ->
        val center = setup.visibleCenter
        executeShellCommand("input tap ${center.x} ${center.y}")
    }
    waitForIdle(1_000)
}
