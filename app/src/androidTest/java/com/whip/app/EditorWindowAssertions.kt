package com.whip.app

import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue

/** Compare the visible Android hierarchy with Compose's complete layout after rendering settles. */
internal fun SemanticsNodeInteractionsProvider.assertEditorHeaderVisibleWithKeyboard(title: String, exitLabel: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val device = UiDevice.getInstance(instrumentation)
    val density = instrumentation.targetContext.resources.displayMetrics.density
    val titleLayout = onNodeWithText(title).getUnclippedBoundsInRoot()
    val titleBounds = device.findObject(By.text(title))?.visibleBounds
    val exitLayout = onNodeWithContentDescription(exitLabel, useUnmergedTree = true).getUnclippedBoundsInRoot()
    val exitBounds = device.findObject(By.desc(exitLabel))?.visibleBounds
    assertTrue("Expected the actual software keyboard", instrumentation.uiAutomation.windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    })
    assertTrue(
        "The complete editor title must remain visible: $titleBounds versus $titleLayout",
        titleBounds != null && titleBounds.height() >= (titleLayout.bottom - titleLayout.top).value * density - 1f,
    )
    assertTrue(
        "The complete editor exit must remain visible: $exitBounds versus $exitLayout",
        exitBounds != null && exitBounds.height() >= (exitLayout.bottom - exitLayout.top).value * density - 1f,
    )
}
