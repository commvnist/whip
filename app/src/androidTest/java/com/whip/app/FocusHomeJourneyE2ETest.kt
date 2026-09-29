package com.whip.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.whip.app.core.AppSettings
import com.whip.app.core.HomeSection
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskPriority
import com.whip.app.reminders.FocusTimerNotifications
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/** Home uses the real ViewModel, settings store, Task repository and timer scheduler. */
class FocusHomeJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app get() = ApplicationProvider.getApplicationContext<WhipApplication>()

    @Before fun prepare() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(setupCompleted = true, activeAreaScope = "all", homeSections = listOf(HomeSection.Tasks),
                hiddenHomeSections = HomeSection.entries.filterNot { it == HomeSection.Tasks }.toSet(),
                notificationPermissionRequested = true, dynamicColor = false)
        }
        grantNotifications()
    }

    @After fun clean() = runBlocking {
        app.backupRepository.deleteAllData()
        grantNotifications()
    }

    private fun grantNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private fun launch() = launchMainActivity(Intent(app, MainActivity::class.java)
        .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true))
    private fun awaitTag(tag: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    private fun task(title: String, priority: TaskPriority = TaskPriority.None) = runBlocking {
        app.taskRepository.create(TaskDraft(title, scheduleKind = ScheduleKind.Once,
            date = app.clock.today(), inbox = false, priority = priority))
    }
    private fun focusChannelEnabled(): Boolean = app.getSystemService(NotificationManager::class.java)
        .getNotificationChannel(FocusTimerNotifications.channelId).importance != NotificationManager.IMPORTANCE_NONE
    private fun openFocusChannelSettings() {
        app.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, FocusTimerNotifications.channelId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    private fun setFocusChannelAndReturn(enabled: Boolean) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")), 5_000))
        val switch = device.wait(Until.findObject(By.clazz("android.widget.Switch")), 5_000)
            ?: error("Android Focus settings did not expose its notification switch")
        if (switch.isChecked != enabled) switch.click()
        compose.waitUntil(5_000) { focusChannelEnabled() == enabled }
        device.pressBack()
        assertTrue(device.wait(Until.hasObject(By.pkg(app.packageName)), 5_000))
        compose.waitForIdle()
    }

    @Test fun lowHomeTaskStartPinsCountdownAndOpenStopSurviveRecreationAndAreaChange() {
        repeat(12) { task("Earlier priority work ${it + 1}", TaskPriority.High) }
        val title = "Focus on the last Home Task"
        val taskId = task(title)
        val quietArea = runBlocking { app.areaRepository.create("Quiet area") }
        launch().use { scenario ->
            awaitTag("home-list")
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-view-all-Tasks"))
            compose.onNodeWithTag("home-view-all-Tasks").performClick()
            compose.onNodeWithTag("task-workspace-list").performScrollToNode(hasTestTag("task-card-$taskId"))
            compose.onNodeWithTag("task-card-$taskId").assertIsDisplayed().performClick()
            compose.onNodeWithTag("task-detail-section-Overview").performClick()
            compose.onNodeWithTag("focus-preset-30").performScrollTo().performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().focusTimerTaskId == taskId }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("entity-inspector").fetchSemanticsNodes().isEmpty() }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Focus started · 30 min", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("primary-navigation-Home").performClick()
            awaitTag("active-focus-card")
            compose.onNodeWithTag("active-focus-card").assertIsDisplayed()
            compose.onNodeWithTag("active-focus-task").assertTextEquals(title)
            captureVisualCatalogSurface("focus.home.started")
            val pair = app.settingsRepository.current().let { it.focusTimerTaskId to it.focusTimerDeadlineMillis }
            compose.onNodeWithTag("home-list").performScrollToNode(hasText("Earlier priority work 1"))
            compose.onNodeWithTag("active-focus-card").assertIsDisplayed()
            captureVisualCatalogSurface("focus.home.pinned")
            scenario.recreate()
            awaitTag("active-focus-card")
            assertEquals(pair, app.settingsRepository.current().let { it.focusTimerTaskId to it.focusTimerDeadlineMillis })
            compose.onNodeWithContentDescription("Area scope: All Areas").performClick()
            compose.onNodeWithText("Quiet area ·", substring = true).performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().activeAreaScope == "area:$quietArea" }
            compose.onNodeWithTag("active-focus-task").assertTextEquals(title)
            compose.onNodeWithTag("focus-open-task").performClick()
            compose.onNodeWithTag("entity-inspector-title").assertTextEquals(title)
            compose.onNodeWithContentDescription("Home").assertIsSelected()
            compose.onNodeWithTag("entity-inspector-close").performClick()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            awaitTag("active-focus-card")
            compose.onNodeWithTag("active-focus-task").assertTextEquals(title)
            compose.onNodeWithTag("focus-stop").performClick()
            compose.waitUntil(10_000) { app.settingsRepository.current().focusTimerDeadlineMillis == null }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Focus timer stopped.").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("active-focus-card").assertDoesNotExist()
            assertNull(runBlocking { app.taskRepository.getTask(taskId) }?.completedAtMillis)
        }
    }

    @Test @AndroidFontScale fun blockedChannelAndLongTitleKeepHomeControlsAndListUsableAtShortHeight() {
        val title = "Read the complete planning notes for a long project and decide which next Task needs a focused session"
        val taskId = task(title)
        repeat(8) { task("Other Home work ${it + 1}") }
        FocusTimerNotifications.createChannel(app)
        val originallyEnabled = focusChannelEnabled()
        runBlocking {
            val deadline = System.currentTimeMillis() + 30 * 60_000L
            assertTrue(app.settingsRepository.updateAndConfirm {
                it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = deadline)
            })
            app.focusTimerScheduler.schedule(taskId, deadline)
        }
        launch().use {
            try {
            awaitTag("active-focus-card")
            openFocusChannelSettings()
            setFocusChannelAndReturn(false)
            assertFalse(focusChannelEnabled())
            awaitTag("focus-alert-unavailable")
            compose.onNodeWithTag("active-focus-task").assertTextEquals(title)
            compose.onNodeWithTag("focus-alert-unavailable").assertTextEquals("Alerts off in Android").assertIsDisplayed()
            compose.onNodeWithTag("active-focus-finish-time").assertTextContains("Finishes at", substring = true).assertIsDisplayed()
            if (InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.screenHeightDp < 650) {
                compose.onNodeWithTag("active-focus-metadata").assertIsDisplayed()
            }
            captureVisualCatalogSurface("focus.home.channel-blocked.short-large.before-layout-assertions")
            val layouts = mutableListOf<Pair<String, TextLayoutResult>>()
            listOf("active-focus-task", "active-focus-countdown", "active-focus-finish-time").forEach { tag ->
                compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
                    val results = mutableListOf<TextLayoutResult>()
                    getResults(results)
                    assertTrue(results.isNotEmpty())
                    results.forEach { result ->
                        layouts += tag to result
                        Log.i("FocusHomeTextLayout", focusTextLayoutDiagnostic(tag, result))
                    }
                }
            }
            layouts.forEach { (tag, result) ->
                assertEquals(2f, result.layoutInput.density.fontScale, 0.01f)
                if (tag != "active-focus-task") {
                    val diagnostic = focusTextLayoutDiagnostic(tag, result)
                    assertFalse("Text must include every line: $diagnostic", result.multiParagraph.didExceedMaxLines)
                    assertEquals("Text must include every character: $diagnostic",
                        result.layoutInput.text.length, result.getLineEnd(result.lineCount - 1))
                    repeat(result.lineCount) { line ->
                        assertFalse("Text must not be ellipsized: $diagnostic", result.isLineEllipsized(line))
                        // Compose's integer measured size can round a fractional line edge by one pixel.
                        assertTrue("Line left must fit measured text: $diagnostic", result.getLineLeft(line) >= -1f)
                        assertTrue("Line right must fit measured text: $diagnostic", result.getLineRight(line) <= result.size.width + 1f)
                        assertTrue("Line top must fit measured text: $diagnostic", result.getLineTop(line) >= -1f)
                        assertTrue("Line bottom must fit measured text: $diagnostic", result.getLineBottom(line) <= result.size.height + 1f)
                    }
                }
            }
            listOf("focus-open-task", "focus-stop", "focus-notification-settings").forEach { tag ->
                compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(48.dp)
            }
            compose.onNodeWithTag("home-list").assertHeightIsAtLeast(160.dp)
            compose.onNodeWithTag("home-list").performScrollToNode(hasTestTag("home-view-all-Tasks"))
            compose.onNodeWithTag("home-view-all-Tasks").assertIsDisplayed()
            compose.onNodeWithTag("active-focus-card").assertIsDisplayed()
            assertEquals(2f, InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.fontScale, 0.01f)
            captureVisualCatalogSurface("focus.home.channel-blocked.short-large")
            } finally {
                openFocusChannelSettings()
                setFocusChannelAndReturn(originallyEnabled)
            }
        }
    }

    private fun focusTextLayoutDiagnostic(tag: String, result: TextLayoutResult): String =
        "$tag text=${result.layoutInput.text.text}; size=${result.size}; " +
            "constraints=${result.layoutInput.constraints}; paragraphWidth=${result.multiParagraph.width}; " +
            "paragraphHeight=${result.multiParagraph.height}; " +
            "widthOverflow=${result.didOverflowWidth}; heightOverflow=${result.didOverflowHeight}; " +
            "visualOverflow=${result.hasVisualOverflow}; fontScale=${result.layoutInput.density.fontScale}; " +
            "lines=${result.lineCount}; " +
            "lineBounds=${(0 until result.lineCount).map { line -> listOf(result.getLineLeft(line), result.getLineTop(line), result.getLineRight(line), result.getLineBottom(line)) }}"

    @Test fun androidFocusChannelRecoveryRefreshesTheCardAndRepublishesTheCountdown() {
        val taskId = task("Focus channel recovery")
        val manager = app.getSystemService(NotificationManager::class.java)
        FocusTimerNotifications.createChannel(app)
        val originallyEnabled = focusChannelEnabled()
        val receipt = runBlocking { app.focusTimerScheduler.start(taskId, 30) }
        launch().use {
            try {
                awaitTag("active-focus-card")
                openFocusChannelSettings()
                setFocusChannelAndReturn(true)
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("focus-alert-unavailable").fetchSemanticsNodes().isEmpty() }
                openFocusChannelSettings()
                setFocusChannelAndReturn(false)
                awaitTag("focus-alert-unavailable")
                compose.onNodeWithTag("focus-alert-unavailable").assertTextEquals("Alerts off in Android")
                captureVisualCatalogSurface("focus.home.channel-off")
                compose.onNodeWithTag("focus-notification-settings").performClick()
                setFocusChannelAndReturn(true)
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("focus-alert-unavailable").fetchSemanticsNodes().isEmpty() }
                compose.waitUntil(5_000) { manager.activeNotifications.any { it.id == FocusTimerNotifications.notificationId } }
                assertEquals(receipt.deadlineMillis, app.settingsRepository.current().focusTimerDeadlineMillis)
                val notification = manager.activeNotifications.single { it.id == FocusTimerNotifications.notificationId }.notification
                assertEquals(receipt.deadlineMillis, notification.`when`)
                compose.onNodeWithTag("active-focus-card").assertIsDisplayed()
                captureVisualCatalogSurface("focus.home.channel-repaired")
            } finally {
                openFocusChannelSettings()
                setFocusChannelAndReturn(originallyEnabled)
            }
        }
    }

    @Test fun matchingNativeCompletionAcknowledgesForegroundOnceAndLeavesTaskOpen() {
        val title = "Review the focused work"
        val taskId = task(title)
        runBlocking {
            app.settingsRepository.update { it.copy(homeSections = HomeSection.entries, hiddenHomeSections = emptySet()) }
        }
        launch().use { scenario ->
        awaitTag("home-list")
        runBlocking {
            val deadline = System.currentTimeMillis() + 3_000L
            assertTrue(app.settingsRepository.updateAndConfirm {
                it.copy(focusTimerTaskId = taskId, focusTimerDeadlineMillis = deadline)
            })
            app.focusTimerScheduler.schedule(taskId, deadline)
        }
            awaitTag("active-focus-card")
            compose.waitUntil(25_000) {
                compose.onAllNodesWithText("Focus complete for $title.", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            assertNull(app.settingsRepository.current().focusTimerDeadlineMillis)
            assertNull(runBlocking { app.taskRepository.getTask(taskId) }?.completedAtMillis)
            val gymHeading = compose.onNode(hasContentDescription("Gym") and hasAnyAncestor(hasTestTag("home-list")))
            gymHeading.assertIsDisplayed().assertHasClickAction()
            assertTrue(gymHeading.fetchSemanticsNode().config.contains(SemanticsProperties.Heading))
            assertEquals(Role.Button, gymHeading.fetchSemanticsNode().config[SemanticsProperties.Role])
            val gymStatus = compose.onNodeWithContentDescription("No Active Workout, Start and resume workouts from the Gym screen.")
            gymStatus.assertIsDisplayed().assertHasClickAction()
            assertEquals(Role.Button, gymStatus.fetchSemanticsNode().config[SemanticsProperties.Role])
            val visibleBounds = gymStatus.fetchSemanticsNode().boundsInRoot
            val fullBounds = gymStatus.getUnclippedBoundsInRoot()
            val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
            assertTrue("Completion fixture must retain the partly clipped Gym card: visible=$visibleBounds full=$fullBounds",
                visibleBounds.height > 0f && visibleBounds.height < (fullBounds.bottom - fullBounds.top).value * density)
            captureVisualCatalogSurface("focus.home.native-complete")
            scenario.recreate()
            awaitTag("home-list")
            compose.onNodeWithTag("active-focus-card").assertDoesNotExist()
            compose.onAllNodesWithText("Focus complete for $title.", substring = true).assertCountEquals(0)
        }
    }
}
