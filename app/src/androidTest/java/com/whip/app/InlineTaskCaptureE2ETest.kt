package com.whip.app

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InlineTaskCaptureE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun inlineTaskDraftSurvivesRecreationAndSavesWithTheNativeKeyboard() = verifyCapture(false)

    @Test @AndroidFontScale
    fun inlineTaskDraftSurvivesRecreationAndSavesWithTheNativeKeyboardAtLargeText() = verifyCapture(true)

    private fun verifyCapture(large: Boolean) {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
            }
        }
        val title = "Keyboard capture after interruption"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            focusQuery()
            compose.onNodeWithTag("task-quick-capture").performTextReplacement(title)
            assertWholeQueryVisible(if (large) 2f else 1f)
            scenario.recreate()
            waitForWorkspace()
            compose.onNodeWithTag("task-quick-capture").performScrollTo().assertTextContains(title)
            focusQuery()
            assertWholeQueryVisible(if (large) 2f else 1f)
            capture("tasks.quick-capture.native.${if (large) "large" else "ordinary"}")
            compose.onNodeWithTag("task-quick-capture").performImeAction()
            compose.waitUntil(15_000) {
                runBlocking { app.taskRepository.tasks.first().count { it.title == title } == 1 }
            }
            compose.waitUntil(10_000) {
                runCatching {
                    compose.onNodeWithTag("task-quick-capture").fetchSemanticsNode()
                        .config[SemanticsProperties.EditableText].text.isEmpty()
                }.getOrDefault(false)
            }
            compose.waitForIdle()
            instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
            device.waitForIdle()
            if (keyboardVisible()) device.pressBack()
            compose.waitUntil(10_000) { !keyboardVisible() }
            compose.onNodeWithContentDescription("Tasks tab").assertIsSelected()
            compose.onNodeWithContentDescription("Tracks tab").assertIsDisplayed()
            scenario.recreate()
            waitForWorkspace()
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasText(title))
            compose.onNodeWithText(title).assertIsDisplayed()
            val saved = runBlocking { app.taskRepository.tasks.first().single { it.title == title } }
            assertEquals(app.clock.today(), saved.date)
        }
    }

    private fun keyboardVisible() = instrumentation.uiAutomation.windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }

    private fun waitForWorkspace() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("task-workspace-list").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun focusQuery() {
        compose.onNodeWithTag("task-quick-capture").performScrollTo().performClick()
        compose.waitUntil(10_000) { keyboardVisible() }
        instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
        compose.waitForIdle()
    }

    private fun assertWholeQueryVisible(expectedFontScale: Float) {
        compose.waitForIdle()
        instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
        device.waitForIdle()
        refreshAccessibilityHierarchy("Task Quick Capture")
        compose.onNodeWithTag("task-quick-capture").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
            val results = mutableListOf<TextLayoutResult>()
            getResults(results)
            assertTrue("Expected actual inline text layout", results.isNotEmpty())
            results.forEach { assertEquals(expectedFontScale, it.layoutInput.density.fontScale, 0.01f) }
        }
        val ime = Rect()
        checkNotNull(instrumentation.uiAutomation.windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
        }).getBoundsInScreen(ime)
        val native = device.findObject(By.res("task-quick-capture"))?.visibleBounds
        val layout = compose.onNodeWithTag("task-quick-capture").getUnclippedBoundsInRoot()
        val density = app.resources.displayMetrics.density
        assertTrue("Complete native Task capture above keyboard: $native versus $ime and $layout",
            native != null && native.bottom <= ime.top && native.height() >= (layout.bottom - layout.top).value * density - 1f)
        val labelLayout = compose.onNodeWithText("Task for Today", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val label = device.findObject(By.text("Task for Today"))?.visibleBounds
        assertTrue("Complete Task capture label: $label versus $labelLayout",
            label != null && label.height() >= (labelLayout.bottom - labelLayout.top).value * density - 1f)
    }

    private fun capture(id: String) {
        compose.waitForIdle()
        instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
        device.waitForIdle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            captureVisualCatalogSurface(id)
        } else {
            val directory = checkNotNull(app.getExternalFilesDir("inline-task-capture"))
            check(directory.isDirectory || directory.mkdirs())
            check(device.takeScreenshot(File(directory, "$id.png")))
            device.dumpWindowHierarchy(File(directory, "$id.xml"))
        }
    }
}
