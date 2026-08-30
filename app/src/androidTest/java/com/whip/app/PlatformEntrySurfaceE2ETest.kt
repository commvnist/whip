package com.whip.app

import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.AreaScope
import com.whip.app.health.HealthPermissionsRationaleActivity
import com.whip.app.widget.WhipWidgetConfigureActivity
import com.whip.app.widget.WhipWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformEntrySurfaceE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var app: WhipApplication

    @Before
    fun prepare() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true) }
    }

    @Test
    fun sharedPlainTextOpensAPrefilledTaskAndCancelDoesNotPersistIt() {
        ActivityScenario.launch<MainActivity>(sharedTextIntent("Capture this from another app")).use {
            waitForTaskEditor()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Capture this from another app")
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Discard Changes").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Discard Changes").performClick()
            compose.onAllNodesWithTag("task-editor-surface").assertCountEquals(0)
            assertEquals(emptyList<Any>(), runBlocking { app.taskRepository.tasks.first() })
        }
    }

    @Test
    fun repeatedSharedTextIntentReplacesTheDraftWithoutReplayingTheOldDelivery() {
        ActivityScenario.launch<MainActivity>(sharedTextIntent("First shared draft")).use {
            waitForTaskEditor()
            compose.onNodeWithTag("task-editor-title").assertTextContains("First shared draft")
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Discard Changes").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Discard Changes").performClick()

            app.startActivity(
                sharedTextIntent("Second shared draft")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )

            waitForTaskEditor()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Second shared draft")
        }
    }

    @Test
    fun unsupportedSharedPayloadDoesNotOpenAnEditor() {
        val intent = Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_SEND)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TEXT, "Do not capture this")

        ActivityScenario.launch<MainActivity>(intent).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("home-destination-links").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithTag("task-editor-surface").assertCountEquals(0)
        }
    }

    @Test
    fun widgetTaskActionAppliesItsAreaTransientlyAndRestoresItAfterRecreation() = runBlocking {
        val workAreaId = app.areaRepository.create("Work")
        val persistedScope = AreaScope.All.storageKey
        app.settingsRepository.update { it.copy(activeAreaScope = persistedScope) }
        val intent = Intent(app, MainActivity::class.java)
            .setAction(WhipWidgetProvider.ACTION_ADD_TASK)
            .putExtra(WhipWidgetProvider.EXTRA_AREA_SCOPE, AreaScope.One(workAreaId).storageKey)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            waitForTaskEditor()
            compose.onNodeWithContentDescription("Area selection: Work").performScrollTo().assertIsDisplayed()
            assertEquals(persistedScope, app.settingsRepository.current().activeAreaScope)

            scenario.recreate()
            waitForTaskEditor()
            compose.onNodeWithContentDescription("Area selection: Work").performScrollTo().assertIsDisplayed()
            assertEquals(persistedScope, app.settingsRepository.current().activeAreaScope)
        }
    }

    @Test
    fun widgetQuickActionResumesAfterFirstRunSetup() = runBlocking {
        app.settingsRepository.update { it.copy(setupCompleted = false) }
        val intent = Intent(app, MainActivity::class.java)
            .setAction(WhipWidgetProvider.ACTION_ADD_TASK)

        ActivityScenario.launch<MainActivity>(intent).use {
            compose.onNodeWithText("Welcome to Whip").assertIsDisplayed()
            compose.onNodeWithText("Use Recommended").performClick()
            waitForTaskEditor()
        }
    }

    @Test
    fun widgetConfigurationPersistsTheChosenAreaAndReturnsSuccess() = runBlocking {
        val widgetId = 73_041
        app.areaRepository.create("Work")
        WhipWidgetProvider().onDeleted(app, intArrayOf(widgetId))
        val intent = Intent(app, WhipWidgetConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

        ActivityScenario.launch<WhipWidgetConfigureActivity>(intent).use { scenario ->
            compose.onNodeWithText("Task Agenda Widget").assertIsDisplayed()
            compose.onNodeWithContentDescription("Area selection: All areas").performClick()
            compose.onNodeWithContentDescription("Area Work").performClick()
            compose.onNodeWithText("Save Widget").assertIsDisplayed().performClick()
            compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }

            val scope = WhipWidgetProvider.loadScope(app, widgetId)
            val workAreaId = app.areaRepository.areas.first().single { it.name == "Work" }.id
            assertEquals(AreaScope.One(workAreaId), scope)
        }
        WhipWidgetProvider().onDeleted(app, intArrayOf(widgetId))
    }

    @Test
    fun widgetReconfigurationKeepsItsExistingAreaUnlessTheUserChangesIt() = runBlocking {
        val widgetId = 73_042
        val workAreaId = app.areaRepository.create("Work")
        WhipWidgetProvider.saveScope(app, widgetId, AreaScope.One(workAreaId))
        val intent = Intent(app, WhipWidgetConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

        ActivityScenario.launch<WhipWidgetConfigureActivity>(intent).use { scenario ->
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Work").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Area selection: Work").assertIsDisplayed()
            compose.onNodeWithText("Save Widget").assertIsDisplayed().performClick()
            compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
        }

        assertEquals(AreaScope.One(workAreaId), WhipWidgetProvider.loadScope(app, widgetId))
        WhipWidgetProvider().onDeleted(app, intArrayOf(widgetId))
    }

    @Test
    fun widgetConfigurationPersistsUnassignedAsADistinctAreaScope() {
        val widgetId = 73_043
        WhipWidgetProvider().onDeleted(app, intArrayOf(widgetId))
        val intent = Intent(app, WhipWidgetConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

        ActivityScenario.launch<WhipWidgetConfigureActivity>(intent).use { scenario ->
            compose.onNodeWithTag("widget-config-area-unassigned").performClick()
            compose.onNodeWithText("Save Widget").performClick()
            compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
        }

        assertEquals(AreaScope.Unassigned, WhipWidgetProvider.loadScope(app, widgetId))
        WhipWidgetProvider().onDeleted(app, intArrayOf(widgetId))
    }

    @Test
    fun healthPermissionRationaleStatesTheReadOnlyLocalDataPolicyAndCloses() {
        ActivityScenario.launch<HealthPermissionsRationaleActivity>(Intent(app, HealthPermissionsRationaleActivity::class.java)).use { scenario ->
            compose.onNodeWithText("Health Connect & Whip").assertIsDisplayed()
            compose.onNodeWithText(
                "Whip only reads the health categories you select. It never writes to Health Connect.",
            ).assertIsDisplayed()
            compose.onNodeWithText(
                "Turning off sync stops future reads. It does not delete records already imported into Whip. You remain in control of those records through Whip’s data controls.",
            ).assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()
            compose.waitUntil(5_000) { scenario.state == Lifecycle.State.DESTROYED }
        }
    }

    private fun sharedTextIntent(text: String) = Intent(app, MainActivity::class.java)
        .setAction(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)

    private fun waitForTaskEditor() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("task-editor-title").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("task-editor-surface").assertIsDisplayed()
    }
}
