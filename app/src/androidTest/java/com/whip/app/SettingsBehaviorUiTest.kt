package com.whip.app

import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.whip.app.core.AppSettings
import com.whip.app.core.AreaOpeningMode
import com.whip.app.core.HomeSection
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.AreaScope
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsBehaviorUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetSettings() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(setupCompleted = true, hiddenHomeSections = emptySet())
        }
        compose.waitForIdle()
    }

    @Test
    fun appearanceHasNoDensityChoiceAndTasksUseSummaryDisclosure() {
        val taskId = runBlocking {
            app.taskRepository.create(
                TaskDraft(
                    title = "Summary-first task",
                    notes = "Details revealed on request",
                    scheduleKind = ScheduleKind.Once,
                    date = app.clock.today(),
                    inbox = false,
                ),
            )
        }
        compose.waitUntil {
            compose.onAllNodesWithTag("task-expand-$taskId")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Details revealed on request").assertCountEquals(0)
        compose.onNodeWithTag("task-expand-$taskId").performClick()
        compose.onNodeWithText("Details revealed on request").assertIsDisplayed()

        openAppearanceSettings()
        compose.onAllNodesWithText("Use compact item rows").assertCountEquals(0)
        compose.onNodeWithText("Show advanced controls by default").assertIsDisplayed()
    }

    @Test
    fun openingAreaBehaviorAndChosenAreaCanBeConfiguredFromAppearance() = runBlocking {
        val mainAreaId = app.areaRepository.create("Main")
        val workAreaId = app.areaRepository.create("Work")
        app.settingsRepository.update {
            it.copy(activeAreaScope = AreaScope.One(mainAreaId).storageKey)
        }
        compose.waitForIdle()

        openAppearanceSettings()
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(androidx.compose.ui.test.hasText("When Whip opens"))
        compose.onNodeWithContentDescription("When Whip opens: Last used area").performClick()
        compose.onNodeWithText("Chosen area").performClick()
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(androidx.compose.ui.test.hasText("Opening area"))
        compose.onNodeWithContentDescription("Opening area: Main").performClick()
        compose.onNodeWithText("Work").performClick()

        compose.waitUntil {
            val saved = app.settingsRepository.current()
            saved.areaOpeningMode == AreaOpeningMode.Chosen &&
                saved.chosenOpeningAreaScope == AreaScope.One(workAreaId).storageKey
        }
        assertTrue(app.settingsRepository.current().activeAreaScope == AreaScope.One(mainAreaId).storageKey)
    }

    @Test
    fun hidingGoalsKeepsItDiscoverableInTheEmptyHomeGuide() {
        openAppearanceSettings()
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("home-section-Goals"))
        compose.onNodeWithTag("home-section-Goals").performClick()
        compose.waitUntil {
            HomeSection.Goals in app.settingsRepository.current().hiddenHomeSections
        }

        compose.onNodeWithContentDescription("Close Settings").performClick()

        // Home-section visibility controls the populated dashboard. The empty
        // state still teaches every available component so a new user can
        // understand and reach tools they have not enabled on Home yet.
        compose.onNodeWithTag("home-destination-goals").fetchSemanticsNode()
        compose.onNodeWithTag("home-destination-tasks").fetchSemanticsNode()
        compose.onNodeWithTag("home-destination-habits").fetchSemanticsNode()
    }

    @Test
    fun finalVisibleHomeSectionExplainsWhyItCannotBeDisabled() {
        runBlocking {
            app.settingsRepository.update {
                it.copy(hiddenHomeSections = HomeSection.entries.toSet() - HomeSection.Goals)
            }
        }
        compose.waitForIdle()

        openAppearanceSettings()
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("home-section-Goals"))

        compose.onNodeWithTag("home-section-Goals").assertIsNotEnabled()
        compose.onNodeWithText("At least one Home section must remain visible.").fetchSemanticsNode()
    }

    @Test
    fun organizationSettingsAddsRenamesAndRemovesNamedCustomEmojis() {
        openSettingsSection("Organization")
        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("custom-emoji-add"))
        compose.onNodeWithTag("custom-emoji-add").performClick()
        compose.onNodeWithTag("custom-emoji-editor").assertIsDisplayed()
        compose.onNodeWithTag("custom-emoji-editor-glyph").performTextReplacement("🦊")
        compose.onNodeWithTag("custom-emoji-editor-name").performTextReplacement("Forest Work")
        compose.onNodeWithTag("custom-emoji-editor-save").performClick()

        compose.waitUntil {
            app.settingsRepository.current().customIdentityEmojis ==
                listOf(CustomIdentityEmoji("🦊", "Forest Work"))
        }
        compose.onNodeWithTag("custom-emoji-🦊").assertIsDisplayed()
        compose.onNodeWithTag("custom-emoji-menu-🦊").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithTag("custom-emoji-editor-name").performTextReplacement("Outdoor Projects")
        compose.onNodeWithTag("custom-emoji-editor-save").performClick()
        compose.waitUntil {
            app.settingsRepository.current().customIdentityEmojis.singleOrNull()?.name == "Outdoor Projects"
        }

        compose.onNodeWithTag("custom-emoji-menu-🦊").performClick()
        compose.onNodeWithText("Remove").performClick()
        compose.waitUntil { app.settingsRepository.current().customIdentityEmojis.isEmpty() }

        compose.onNodeWithTag("custom-emoji-add").performClick()
        compose.onNodeWithTag("custom-emoji-editor-glyph").performTextReplacement("✅")
        compose.onNodeWithTag("custom-emoji-editor-name").performTextReplacement("Changed Default")
        compose.onNodeWithTag("custom-emoji-editor-save").assertIsNotEnabled()
        compose.onNodeWithText("This is a built-in emoji and is already always available.").assertIsDisplayed()
    }

    @Test
    fun rapidDoubleTapOnResetSubmitsOnlyOneDestructiveOperationAndReturnsHome() {
        openSettingsSection("Data & Privacy")
        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("reset-whip-action"))
        compose.onNodeWithTag("reset-whip-action").performClick()
        compose.onNodeWithTag("confirm-reset-whip").performTouchInput {
            down(center)
            up()
            advanceEventTime(40)
            down(center)
            up()
        }

        compose.waitUntil(timeoutMillis = 15_000) {
            !app.settingsRepository.current().setupCompleted &&
                app.startupRecoveryState.value == com.whip.app.startup.StartupRecoveryState.Ready
        }
        compose.waitForIdle()
        compose.onNodeWithText("Welcome to Whip").assertIsDisplayed()
        compose.onNodeWithText("Use Recommended").performClick()
        compose.waitUntil { app.settingsRepository.current().setupCompleted }

        compose.onNodeWithTag("home-destination-links").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Close Settings").assertCountEquals(0)
    }

    @Test
    fun plainJsonBackupUsesAndroidsDocumentProviderAndReturnsSafelyOnCancel() {
        openSettingsSection("Data & Privacy")
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(androidx.compose.ui.test.hasText("Save Plain JSON Backup"))
        compose.onNodeWithText("Save Plain JSON Backup").performClick()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue(
            "Android's document provider did not open for a user-selected export destination",
            device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")), 2_500) ||
                device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 2_500),
        )
        device.pressBack()
        device.wait(Until.gone(By.pkg("com.google.android.documentsui")), 5_000)
        device.wait(Until.gone(By.pkg("com.android.documentsui")), 5_000)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            // This API 26 reference DocumentsUI build returns to Launcher after
            // cancellation instead of its caller. The modern-API lane below
            // remains the authoritative cancellation/resume assertion.
            return
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("settings-list").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("page-title").assertTextContains("Data & Privacy").assertIsDisplayed()
    }

    @Test
    fun everySettingsCategoryIsReachableAndOwnsItsExpectedControls() {
        val expectedControl = linkedMapOf(
            "Appearance & Home" to "Show advanced controls by default",
            "Planning & Units" to "First day of week",
            "Organization" to "Manage Areas",
            "Reminders" to "Reminder Delivery",
            "Data & Privacy" to "Portable Backup Folder",
            "About Whip" to "Your data stays on this device unless you explicitly export or sync it.",
        )

        compose.onNodeWithTag("workspace-settings-action").performClick()
        expectedControl.forEach { (section, control) ->
            selectSettingsCategory(section)
            compose.onNodeWithTag("settings-list").performScrollToNode(androidx.compose.ui.test.hasText(control))
            compose.onNodeWithText(control).assertIsDisplayed()
            if (compose.onAllNodesWithContentDescription("Back to Settings").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Back to Settings").performClick()
            }
        }
    }

    @Test
    fun validGymDefaultDraftSurvivesRecreationWithoutPersistingUntilDone() {
        openSettingsSection("Planning & Units")
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-field-default-rest-time-seconds"))
        val initial = app.settingsRepository.current().defaultRestSeconds

        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("300")
        compose.runOnIdle {
            assertEquals(initial, app.settingsRepository.current().defaultRestSeconds)
        }

        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-input")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .assertTextContains("300")
        compose.runOnIdle {
            assertEquals(initial, app.settingsRepository.current().defaultRestSeconds)
        }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performImeAction()
        compose.waitUntil {
            app.settingsRepository.current().defaultRestSeconds == 300
        }
    }

    @Test
    fun malformedClockCannotChangeTheCutoffButCompleteClockCommits() {
        openSettingsSection("Planning & Units")
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-field-late-night-day-cutoff"))
        val initial = app.settingsRepository.current().dayCutoffMinutes

        compose.onNodeWithTag("settings-field-late-night-day-cutoff").performClick()
        compose.onNodeWithTag("settings-field-late-night-day-cutoff-input")
            .performTextReplacement("12:30:99")
        compose.onNodeWithTag("settings-field-late-night-day-cutoff-input").performImeAction()
        compose.onNodeWithText("Use at most 5 characters.")
            .assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(initial, app.settingsRepository.current().dayCutoffMinutes)
        }

        compose.onNodeWithTag("settings-field-late-night-day-cutoff-input")
            .performTextReplacement("03:15")
        compose.onNodeWithTag("settings-field-late-night-day-cutoff-input").performImeAction()
        compose.waitUntil {
            app.settingsRepository.current().dayCutoffMinutes == 3 * 60 + 15
        }
    }

    @Test
    fun hardwareBackRequiresExplicitDiscardAndCannotNavigateBehindATypedEditor() {
        openSettingsSection("Planning & Units")
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-field-default-rest-time-seconds"))
        val initial = app.settingsRepository.current().defaultRestSeconds
        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("300")

        repeat(2) {
            if (compose.onAllNodesWithText("Discard Unsaved Changes?").fetchSemanticsNodes().isEmpty()) {
                pressBack()
                compose.waitForIdle()
            }
        }
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(initial, app.settingsRepository.current().defaultRestSeconds) }
        compose.onNodeWithText("Keep Editing").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .assertTextContains("300")

        repeat(2) {
            if (compose.onAllNodesWithText("Discard Unsaved Changes?").fetchSemanticsNodes().isEmpty()) {
                pressBack()
                compose.waitForIdle()
            }
        }
        compose.onNodeWithText("Discard Changes").performClick()
        compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-editor")
            .assertCountEquals(0)
        compose.runOnIdle { assertEquals(initial, app.settingsRepository.current().defaultRestSeconds) }
        compose.onNodeWithContentDescription("Back to Settings").performClick()
        compose.onNodeWithTag("settings-category-list").assertIsDisplayed()
    }

    @Test
    fun conditionalParentControlsCannotRemoveAnOpenTypedEditor() {
        runBlocking {
            app.settingsRepository.update {
                it.copy(
                    timeZoneId = "America/Toronto",
                    quietStartMinutes = 22 * 60,
                    quietEndMinutes = 7 * 60,
                )
            }
        }
        compose.waitForIdle()

        openSettingsSection("Planning & Units")
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-field-time-zone"))
        compose.onNodeWithTag("settings-field-time-zone").performClick()
        compose.onNodeWithTag("settings-follow-device-time-zone").assertIsNotEnabled()
        compose.onNodeWithTag("settings-field-time-zone-cancel").performClick()
        compose.onNodeWithTag("settings-follow-device-time-zone").assertIsEnabled()

        compose.onNodeWithContentDescription("Back to Settings").performClick()
        selectSettingsCategory("Reminders")
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-field-quiet-hours-start"))
        compose.onNodeWithTag("settings-field-quiet-hours-start").performClick()
        compose.onNodeWithTag("settings-enable-quiet-hours").assertIsNotEnabled()
        compose.onNodeWithTag("settings-field-quiet-hours-start-cancel").performClick()
        compose.onNodeWithTag("settings-enable-quiet-hours").assertIsEnabled()
    }

    private fun openAppearanceSettings() {
        openSettingsSection("Appearance & Home")
    }

    private fun openSettingsSection(label: String) {
        compose.onNodeWithTag("workspace-settings-action").performClick()
        selectSettingsCategory(label)
    }

    private fun selectSettingsCategory(label: String) {
        when {
            compose.onAllNodesWithTag("settings-category-list").fetchSemanticsNodes().isNotEmpty() -> {
                compose.onNodeWithTag("settings-category-list")
                    .performScrollToNode(hasTestTag("settings-section-$label"))
                compose.onNodeWithTag("settings-section-$label").performClick()
            }
            compose.onAllNodesWithTag("settings-support-list").fetchSemanticsNodes().isNotEmpty() -> {
                compose.onNodeWithTag("settings-support-list")
                    .performScrollToNode(hasTestTag("settings-support-section-$label"))
                compose.onNodeWithTag("settings-support-section-$label").performClick()
            }
            else -> compose.onNodeWithTag("settings-section-$label").performClick()
        }
        compose.waitForIdle()
    }
}
