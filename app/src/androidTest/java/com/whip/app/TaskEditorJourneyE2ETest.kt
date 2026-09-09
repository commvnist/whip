package com.whip.app

import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskPriority
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskEditorJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)

    @Before
    fun seed() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
        }
        app.taskRepository.create(TaskDraft(
            title = "Review release notes",
            notes = "Keep the existing acceptance checklist.",
            scheduleKind = ScheduleKind.Once,
            date = app.clock.today(),
            priority = TaskPriority.High,
        ))
        Unit
    }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun existingTaskCanBeReadEditedRecreatedSavedAndReopened() = verifyExistingTask(large = false)

    @Test
    @AndroidFontScale
    fun existingTaskCanBeReadEditedRecreatedSavedAndReopenedAtLargeText() = verifyExistingTask(large = true)

    private fun verifyExistingTask(large: Boolean) {
        val before = runBlocking { app.taskRepository.tasks.first().single() }
        val suffix = if (large) "large" else "ordinary"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            openExistingTask("Review release notes")
            if (large) compose.assertDialogFontScale()
            capture("tasks.editor.existing-open.$suffix")
            assertTitleVisible()

            focusTitle()
            compose.onNodeWithTag("task-editor-title").performTextReplacement("Review the revised notes")
            capture("tasks.editor.existing-input.$suffix")
            assertFocusedTitleVisible()

            scenario.recreate()
            waitForEditor()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Review the revised notes")
            focusTitle()
            capture("tasks.editor.existing-recreated.$suffix")
            assertFocusedTitleVisible()
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("task-editor-surface").fetchSemanticsNodes().isEmpty() }
            val after = runBlocking { app.taskRepository.tasks.first().single() }
            assertEquals("Review the revised notes", after.title)
            assertEquals(before.copy(title = after.title, updatedAtMillis = after.updatedAtMillis), after)

            scenario.recreate()
            openExistingTask("Review the revised notes")
            compose.onNodeWithTag("task-editor-title").assertTextContains("Review the revised notes")
            capture("tasks.editor.existing-reopened.$suffix")
            assertTitleVisible()
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()
        }
    }

    private fun openExistingTask(title: String) {
        val label = "Open task details for $title"
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home-list").performScrollToNode(hasContentDescription(label))
        compose.onNodeWithContentDescription(label).performClick()
        compose.onNodeWithTag("entity-inspector-edit").performClick()
        waitForEditor()
    }

    private fun waitForEditor() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("task-editor-surface").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun focusTitle() {
        compose.onNodeWithTag("task-editor-title").performScrollTo().performClick()
        compose.waitUntil(10_000) { keyboardVisible() }
        instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
        compose.waitForIdle()
    }

    private fun keyboardVisible() = instrumentation.uiAutomation.windows.any {
        it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
    }

    private fun assertFocusedTitleVisible() {
        compose.assertEditorHeaderVisibleWithKeyboard("Edit Task", "Cancel Task editing")
        assertTitleVisible()
    }

    private fun assertTitleVisible() {
        val density = app.resources.displayMetrics.density
        val labelLayout = compose.onNodeWithText("Task *", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val labelBounds = device.findObject(By.text("Task *"))?.visibleBounds
        assertTrue("Complete focused Task label: $labelBounds versus $labelLayout",
            labelBounds != null && labelBounds.height() >= (labelLayout.bottom - labelLayout.top).value * density - 1f)
        val field = device.findObject(By.clazz("android.widget.EditText"))
        val fieldLayout = compose.onNodeWithTag("task-editor-title").getUnclippedBoundsInRoot()
        assertTrue("The complete Task field must be visible: ${field?.visibleBounds} versus $fieldLayout",
            field != null && field.visibleBounds.height() >= (fieldLayout.bottom - fieldLayout.top).value * density - 1f)
    }

    private fun capture(id: String) {
        compose.waitForIdle()
        instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
        device.waitForIdle()
        val focused = compose.onNodeWithTag("task-editor-title").fetchSemanticsNode().config[SemanticsProperties.Focused]
        Log.i("TaskEditorJourney", "$id: focused=$focused keyboard=${keyboardVisible()}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            captureVisualCatalogSurface(id)
        } else {
            val directory = checkNotNull(app.getExternalFilesDir("task-edit-journey"))
            check(directory.isDirectory || directory.mkdirs())
            check(device.takeScreenshot(File(directory, "$id.png")))
            device.dumpWindowHierarchy(File(directory, "$id.xml"))
        }
    }
}
