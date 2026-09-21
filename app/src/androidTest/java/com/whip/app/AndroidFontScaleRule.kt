package com.whip.app

import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AndroidFontScale(val value: Float = 2f)

/** Place outside the Compose rule so configuration changes precede activity creation. */
class AndroidFontScaleRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        val requested = description.getAnnotation(AndroidFontScale::class.java)?.value ?: return base
        require(requested.isFinite() && requested > 0f)
        return object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val device = UiDevice.getInstance(instrumentation)
                val original = device.executeShellCommand("settings get system font_scale").trim()
                fun awaitScale(expected: Float) {
                    // A busy emulator can acknowledge Settings before delivering the
                    // configuration change to the instrumentation process. Keep the
                    // actual resource assertion, but allow that dispatch to settle.
                    val deadline = SystemClock.elapsedRealtime() + 30_000
                    while (abs(instrumentation.targetContext.resources.configuration.fontScale - expected) >= 0.01f) {
                        check(SystemClock.elapsedRealtime() < deadline) {
                            val setting = device.executeShellCommand("settings get system font_scale").trim()
                            "Android did not apply font scale $expected (setting=$setting, " +
                                "targetResources=${instrumentation.targetContext.resources.configuration.fontScale})"
                        }
                        SystemClock.sleep(50)
                    }
                }
                try {
                    device.executeShellCommand("settings put system font_scale $requested")
                    awaitScale(requested)
                    base.evaluate()
                } finally {
                    device.executeShellCommand(
                        if (original == "null") "settings delete system font_scale" else "settings put system font_scale $original",
                    )
                    awaitScale(original.toFloatOrNull() ?: 1f)
                }
            }
        }
    }
}

/** Fails if no actual dialog text was checked, including when a fixture only rendered inline. */
fun SemanticsNodeInteractionsProvider.assertDialogFontScale(expected: Float = 2f) {
    val text = onAllNodes(
        hasAnyAncestor(isDialog()) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
        useUnmergedTree = true,
    )
    val count = text.fetchSemanticsNodes().size
    assertTrue("Expected rendered text inside a dialog", count > 0)
    repeat(count) { index ->
        text[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
            val results = mutableListOf<TextLayoutResult>()
            getResults(results)
            assertTrue("Expected a text layout from the dialog", results.isNotEmpty())
            results.forEach { result ->
                assertEquals(
                    "Dialog text must use the requested scale: ${result.layoutInput.text.text.take(80)}",
                    expected,
                    result.layoutInput.density.fontScale,
                    0.01f,
                )
            }
        }
    }
}
