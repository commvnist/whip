package com.whip.app

import android.content.Intent
import android.appwidget.AppWidgetManager
import android.text.SpannedString
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.AreaScope
import com.whip.app.core.AreaOpeningMode
import com.whip.app.core.SharedTaskCapturePolicy
import com.whip.app.core.WhipLaunchActions
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
    fun styledSharedTextUsesTheStandardCharSequenceContract() {
        ActivityScenario.launch<MainActivity>(sharedTextIntent(SpannedString("Styled shared task"))).use {
            waitForTaskEditor()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Styled shared task")
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
    fun incomingShareCannotSilentlyReplaceAnOpenTaskDraft() {
        ActivityScenario.launch<MainActivity>(sharedTextIntent("First unsaved shared draft")).use { scenario ->
            waitForTaskEditor()

            app.startActivity(
                sharedTextIntent("Second shared draft")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )

            compose.onNodeWithText("Review New Task Request?").assertIsDisplayed()
            compose.onNodeWithTag("task-editor-title").assertTextContains("First unsaved shared draft")
            scenario.recreate()
            compose.onNodeWithText("Review New Task Request?").assertIsDisplayed()
            compose.onNodeWithText("Keep Editing").performClick()
            compose.onNodeWithTag("task-editor-title").assertTextContains("First unsaved shared draft")
            compose.onNodeWithTag("pending-task-request-waiting").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("task-editor-title").fetchSemanticsNodes().any { node ->
                    node.config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text ==
                        "Second shared draft"
                }
            }
        }
    }

    @Test
    fun queuedWidgetAddTaskPreservesRequestedAreaAfterCurrentDraftSave() {
        val requestedDate = java.time.LocalDate.now().plusDays(3)
        val workAreaId = runBlocking { app.areaRepository.create("Work") }
        runBlocking {
            app.settingsRepository.update { it.copy(activeAreaScope = AreaScope.Unassigned.storageKey) }
        }
        val scenario = ActivityScenario.launch<MainActivity>(sharedTextIntent("Keep this shared draft"))
        try {
            waitForTaskEditor()

            scenario.onActivity { activity ->
                activity.acceptLaunchIntent(
                    Intent(activity, MainActivity::class.java)
                        .setAction(WhipWidgetProvider.ACTION_ADD_TASK)
                        .putExtra(WhipLaunchActions.EXTRA_OCCURRENCE_EPOCH_DAY, requestedDate.toEpochDay())
                        .putExtra(
                            WhipWidgetProvider.EXTRA_AREA_SCOPE,
                            AreaScope.One(workAreaId).storageKey,
                        ),
                )
            }

            compose.onNodeWithText("Review New Task Request?").assertIsDisplayed()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Keep this shared draft")

            compose.onNodeWithText("Keep Editing").performClick()
            compose.onNodeWithTag("pending-task-request-waiting").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("0/200").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Scheduled Date").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Area selection: Work").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()
            compose.onAllNodesWithTag("task-editor-surface").assertCountEquals(0)
        } finally {
            runCatching { scenario.onActivity { it.finishAndRemoveTask() } }
        }
    }

    @Test
    fun fourthShareCannotOverwriteTheUnadmittedThirdShare() {
        ActivityScenario.launch<MainActivity>(sharedTextIntent("First open draft")).use {
            waitForTaskEditor()

            app.startActivity(
                sharedTextIntent("Second waiting share")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
            compose.onNodeWithText("Review New Task Request?").assertIsDisplayed()

            app.startActivity(
                sharedTextIntent("Third waiting share")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
            app.startActivity(
                sharedTextIntent("Fourth waiting share")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
            compose.onNodeWithText("Review New Task Request?").assertIsDisplayed()
            compose.onNodeWithText("Replace Open Draft").performClick()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Second waiting share")

            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Review New Task Request?").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Replace Open Draft").performClick()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Third waiting share")

            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Review New Task Request?").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Replace Open Draft").performClick()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Fourth waiting share")
        }
    }

    @Test
    fun shareQueueOverflowAcknowledgementSurvivesRecreation() {
        ActivityScenario.launch<MainActivity>(sharedTextIntent("Open draft")).use { scenario ->
            waitForTaskEditor()
            listOf("Waiting B", "Queued C", "Queued D", "Queued E", "Rejected F").forEach { text ->
                app.startActivity(
                    sharedTextIntent(text)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
            }
            repeat(4) {
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithText("Replace Open Draft").fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithText("Replace Open Draft").performClick()
            }

            compose.onNodeWithText("Share Queue Full").assertIsDisplayed()
            compose.onNodeWithText("1 additional Task share wasn't added", substring = true)
                .assertIsDisplayed()

            scenario.recreate()

            compose.onNodeWithText("Share Queue Full").assertIsDisplayed()
            compose.onNodeWithText("1 additional Task share wasn't added", substring = true)
                .assertIsDisplayed()
            compose.onNodeWithText("Got It").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Share Queue Full").fetchSemanticsNodes().isEmpty()
            }
        }
    }

    @Test
    fun oversizedSharedTextIsBoundedBeforeRecreationAndExplainsTheShortenedDraft() {
        val oversized = buildString {
            append("😀".repeat(500))
            repeat(2_000) { index ->
                append('\n').append("Shared subtask ").append(index).append(' ').append("x".repeat(20))
            }
        }

        ActivityScenario.launch<MainActivity>(sharedTextIntent(oversized)).use { scenario ->
            waitForTaskEditor()
            compose.onNodeWithTag("shared-task-capture-shortened").assertIsDisplayed()
            compose.onNodeWithTag("task-editor-title").assertTextContains("200/200")

            scenario.recreate()

            waitForTaskEditor()
            compose.onNodeWithTag("shared-task-capture-shortened").assertIsDisplayed()
            compose.onNodeWithTag("task-editor-title").assertTextContains("200/200")
        }
    }

    @Test
    fun editedSharedDraftSurvivesRecreationWithoutLaunchReplay() {
        ActivityScenario.launch<MainActivity>(sharedTextIntent("Original shared draft")).use { scenario ->
            waitForTaskEditor()
            compose.onNodeWithTag("task-editor-title").performTextReplacement("Edited locally")

            scenario.recreate()

            waitForTaskEditor()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Edited locally")
            compose.onAllNodesWithText("Original shared draft").assertCountEquals(0)
            compose.onAllNodesWithText("Review New Task Request?").assertCountEquals(0)
            compose.onAllNodesWithTag("pending-task-request-waiting").assertCountEquals(0)
        }
    }

    @Test
    fun saveAndNewClearsTheShortenedShareWarningFromTheNewDraft() = runBlocking {
        app.settingsRepository.update { it.copy(powerMode = true) }
        val oversizedTitle = "x".repeat(SharedTaskCapturePolicy.MAX_TITLE_CODE_POINTS + 1)

        ActivityScenario.launch<MainActivity>(sharedTextIntent(oversizedTitle)).use {
            waitForTaskEditor()
            compose.onNodeWithTag("shared-task-capture-shortened").assertIsDisplayed()
            compose.onNodeWithText("Save & New").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("0/200").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithTag("shared-task-capture-shortened").assertCountEquals(0)
        }
        Unit
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
    fun blankSharedPayloadDoesNotOpenAnEditor() {
        ActivityScenario.launch<MainActivity>(sharedTextIntent(" \r\n\t ")).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("home-destination-links").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithTag("task-editor-surface").assertCountEquals(0)
        }
    }

    @Test
    fun widgetTaskActionSelectsAndPersistsItsAreaAcrossRecreation() = runBlocking {
        val workAreaId = app.areaRepository.create("Work")
        val selectedScope = AreaScope.One(workAreaId).storageKey
        app.settingsRepository.update { it.copy(activeAreaScope = AreaScope.All.storageKey) }
        val intent = Intent(app, MainActivity::class.java)
            .setAction(WhipWidgetProvider.ACTION_ADD_TASK)
            .putExtra(WhipWidgetProvider.EXTRA_AREA_SCOPE, AreaScope.One(workAreaId).storageKey)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            waitForTaskEditor()
            compose.onNodeWithContentDescription("Area selection: Work").performScrollTo().assertIsDisplayed()
            assertEquals(selectedScope, app.settingsRepository.current().activeAreaScope)
            compose.onNodeWithTag("task-editor-title").performTextReplacement("Widget draft")

            scenario.recreate()
            waitForTaskEditor()
            compose.onNodeWithTag("task-editor-title").assertTextContains("Widget draft")
            compose.onNodeWithContentDescription("Area selection: Work").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Review New Task Request?").assertCountEquals(0)
            assertEquals(selectedScope, app.settingsRepository.current().activeAreaScope)
        }
    }

    @Test
    fun widgetDoesNotShowATemporaryBannerForTheOnlyVisibleArea() = runBlocking {
        val mainAreaId = app.areaRepository.create("Main")
        app.settingsRepository.update { it.copy(activeAreaScope = AreaScope.All.storageKey) }
        val intent = Intent(app, MainActivity::class.java)
            .setAction(WhipWidgetProvider.ACTION_OPEN_HABIT_TRACKING)
            .putExtra(WhipWidgetProvider.EXTRA_AREA_SCOPE, AreaScope.One(mainAreaId).storageKey)

        ActivityScenario.launch<MainActivity>(intent).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("habit-list-Today").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText("Temporarily showing Main").assertCountEquals(0)
            assertEquals(AreaScope.One(mainAreaId).storageKey, app.settingsRepository.current().activeAreaScope)
        }
    }

    @Test
    fun widgetSwitchRespectsChosenOrLastUsedAreaOnTheNextSession() = runBlocking {
        val mainAreaId = app.areaRepository.create("Main")
        val workAreaId = app.areaRepository.create("Work")
        app.settingsRepository.update {
            it.copy(
                activeAreaScope = AreaScope.One(mainAreaId).storageKey,
                areaOpeningMode = AreaOpeningMode.Chosen,
                chosenOpeningAreaScope = AreaScope.One(mainAreaId).storageKey,
            )
        }
        val widgetIntent = Intent(app, MainActivity::class.java)
            .setAction(WhipWidgetProvider.ACTION_OPEN_HABIT_TRACKING)
            .putExtra(WhipWidgetProvider.EXTRA_AREA_SCOPE, AreaScope.One(workAreaId).storageKey)

        ActivityScenario.launch<MainActivity>(widgetIntent).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Area scope: Work").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText("Temporarily showing Work").assertCountEquals(0)
            assertEquals(AreaScope.One(workAreaId).storageKey, app.settingsRepository.current().activeAreaScope)
        }

        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Area scope: Main").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(AreaScope.One(workAreaId).storageKey, app.settingsRepository.current().activeAreaScope)
        }

        app.settingsRepository.update { it.copy(areaOpeningMode = AreaOpeningMode.LastUsed) }
        ActivityScenario.launch<MainActivity>(Intent(app, MainActivity::class.java)).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Area scope: Work").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(AreaScope.One(workAreaId).storageKey, app.settingsRepository.current().activeAreaScope)
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

    private fun sharedTextIntent(text: CharSequence) = Intent(app, MainActivity::class.java)
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
