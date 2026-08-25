package com.whip.app

import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.whip.app.core.AppSettings
import com.whip.app.core.HomeSection
import com.whip.app.domain.CustomIdentityEmoji
import kotlinx.coroutines.runBlocking
import org.junit.Before
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
    fun compactItemLayoutCanBeEnabledFromAppearance() {
        openAppearanceSettings()
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-compact-item-layout"))
        compose.onNodeWithTag("settings-compact-item-layout").performClick()

        compose.waitUntil { app.settingsRepository.current().compactItemLayout }
    }

    @Test
    fun hidingGoalsRemovesItsHomeSectionAndEmptyDayShortcut() {
        openAppearanceSettings()
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("home-section-Goals"))
        compose.onNodeWithTag("home-section-Goals").performClick()
        compose.waitUntil {
            HomeSection.Goals in app.settingsRepository.current().hiddenHomeSections
        }

        compose.onNodeWithContentDescription("Close Settings").performClick()

        compose.onAllNodesWithTag("home-destination-goals").assertCountEquals(0)
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
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithTag("custom-emoji-editor-name").performTextReplacement("Outdoor Projects")
        compose.onNodeWithTag("custom-emoji-editor-save").performClick()
        compose.waitUntil {
            app.settingsRepository.current().customIdentityEmojis.singleOrNull()?.name == "Outdoor Projects"
        }

        compose.onNodeWithText("Remove").performClick()
        compose.waitUntil { app.settingsRepository.current().customIdentityEmojis.isEmpty() }

        compose.onNodeWithTag("custom-emoji-add").performClick()
        compose.onNodeWithTag("custom-emoji-editor-glyph").performTextReplacement("✅")
        compose.onNodeWithTag("custom-emoji-editor-name").performTextReplacement("Changed Default")
        compose.onNodeWithTag("custom-emoji-editor-save").assertIsNotEnabled()
        compose.onNodeWithText("This is a built-in emoji and is already always available.").assertIsDisplayed()
    }

    @Test
    fun resettingWhipReturnsHomeBeforeFirstRunSetupCompletes() {
        openSettingsSection("Data & Privacy")
        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("reset-whip-action"))
        compose.onNodeWithTag("reset-whip-action").performClick()
        compose.onNodeWithTag("confirm-reset-whip").performClick()

        compose.waitUntil { !app.settingsRepository.current().setupCompleted }
        compose.onNodeWithText("Set Up Whip").assertIsDisplayed()
        compose.onNodeWithText("Use Defaults").performClick()
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

        if (compose.onAllNodesWithContentDescription("Open Settings").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Open Settings").performClick()
        } else {
            compose.onNodeWithContentDescription("App actions").performClick()
            compose.onNodeWithText("Open Settings").performClick()
        }
        expectedControl.forEach { (section, control) ->
            selectSettingsCategory(section)
            compose.onNodeWithTag("settings-list").performScrollToNode(androidx.compose.ui.test.hasText(control))
            compose.onNodeWithText(control).assertIsDisplayed()
            if (compose.onAllNodesWithText("All Settings").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText("All Settings").performClick()
            }
        }
    }

    private fun openAppearanceSettings() {
        openSettingsSection("Appearance & Home")
    }

    private fun openSettingsSection(label: String) {
        if (compose.onAllNodesWithContentDescription("Open Settings").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithContentDescription("Open Settings").performClick()
        } else {
            compose.onNodeWithContentDescription("App actions").performClick()
            compose.onNodeWithText("Open Settings").performClick()
        }
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
