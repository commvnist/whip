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
                fun setAndAwaitScale(command: String, expected: Float, expectAbsent: Boolean = false) {
                    // A busy emulator can acknowledge or overwrite a Settings write
                    // before delivering configuration to the instrumentation process.
                    val deadline = SystemClock.elapsedRealtime() + 30_000
                    var nextWrite = 0L
                    while (true) {
                        val resourcesScale = instrumentation.targetContext.resources.configuration.fontScale
                        if (abs(resourcesScale - expected) < 0.01f) {
                            val setting = device.executeShellCommand("settings get system font_scale").trim()
                            if ((expectAbsent && setting == "null") ||
                                (!expectAbsent && abs((setting.toFloatOrNull() ?: Float.NaN) - expected) < 0.01f)
                            ) return
                        }
                        val now = SystemClock.elapsedRealtime()
                        check(now < deadline) {
                            val setting = device.executeShellCommand("settings get system font_scale").trim()
                            "Android did not apply font scale $expected (setting=$setting, targetResources=$resourcesScale)"
                        }
                        if (now >= nextWrite) {
                            device.executeShellCommand(command)
                            nextWrite = now + 2_000
                        }
                        SystemClock.sleep(50)
                    }
                }
                try {
                    setAndAwaitScale("settings put system font_scale $requested", requested)
                    base.evaluate()
                } finally {
                    setAndAwaitScale(
                        if (original == "null") "settings delete system font_scale" else "settings put system font_scale $original",
                        original.toFloatOrNull() ?: 1f,
                        expectAbsent = original == "null",
                    )
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
