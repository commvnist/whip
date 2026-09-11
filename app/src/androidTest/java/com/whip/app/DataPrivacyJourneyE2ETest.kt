package com.whip.app

import android.content.Intent
import android.util.Base64
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.TaskDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real document selection and consequential choices, with persisted data checked after reopening. */
class DataPrivacyJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app = ApplicationProvider.getApplicationContext<WhipApplication>()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val fileName = "WhipDataPrivacyJourney.json"

    @After fun clean() {
        runBlocking { app.backupRepository.deleteAllData() }
        device.executeShellCommand("rm -f /sdcard/Download/$fileName")
    }

    @Test fun backupChoicesPreserveRecordsAndSettingsAfterReopening() = journey(AppThemeMode.Dark)
    @Test fun backupChoicesRemainClearInLightTheme() = journey(AppThemeMode.Light)

    private fun journey(theme: AppThemeMode) {
        val backup = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = theme, dynamicColor = false) }
            app.taskRepository.create(TaskDraft(title = "From the saved backup", notes = "Preserve this original note"))
            app.backupRepository.exportBackup()
        }
        val importedBefore = tasks().single()
        val localBefore = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = theme, dynamicColor = false, defaultRestSeconds = 75) }
            app.taskRepository.create(TaskDraft(title = "Keep my current task", notes = "Local work must survive"))
            app.taskRepository.tasks.first().single()
        }
        val encoded = Base64.encodeToString(backup.toByteArray(), Base64.NO_WRAP)
        device.executeShellCommand("mkdir -p /sdcard/Download")
        device.executeShellCommand("sh -c echo\${IFS}$encoded|base64\${IFS}-d>/sdcard/Download/$fileName")
        assertEquals(backup, device.executeShellCommand("cat /sdcard/Download/$fileName"))
        val suffix = theme.name.lowercase()
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.openSettingsCategory("Data & Privacy")
            compose.onNodeWithText("Backup & Export").assertIsDisplayed()
            capture("overview.$suffix")
            scroll(hasText("Save Passphrase-Encrypted Backup")).performClick()
            compose.onNodeWithText("Choose Location").assertIsNotEnabled()
            compose.onNodeWithText("Passphrase", substring = false).performTextReplacement("test-only-passphrase")
            compose.onNodeWithText("Confirm passphrase").performTextReplacement("does-not-match")
            compose.onNodeWithText("Choose Location").assertIsNotEnabled()
            compose.onNodeWithText("Confirm passphrase").performTextReplacement("test-only-passphrase")
            compose.onNodeWithText("Choose Location").assertIsEnabled().performClick()
            awaitDocuments()
            device.pressBack()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("settings-list").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(listOf(localBefore), tasks())

            scroll(hasText("Preview and Restore Backup")).performClick()
            awaitDocuments()
            assertTrue(device.wait(Until.hasObject(By.desc("Show roots")), 10_000))
            device.findObject(By.desc("Show roots")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10_000))
            device.findObjects(By.text("Downloads")).last().click()
            assertTrue(device.wait(Until.hasObject(By.text(fileName)), 10_000))
            device.findObject(By.text(fileName)).click()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("merge-new-data").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("merge-new-data").assertIsEnabled()
            capture("preview.$suffix")
            compose.onNodeWithTag("request-replace-everything").performScrollTo().performClick()
            compose.onNodeWithTag("confirm-replace-everything").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()
            assertEquals(listOf(localBefore), tasks())
            compose.onNodeWithTag("merge-new-data").performScrollTo().performClick()
            compose.waitUntil(10_000) { tasks().size == 2 && compose.onAllNodesWithTag("merge-new-data").fetchSemanticsNodes().isEmpty() }
            assertEquals(localBefore, tasks().single { it.uuid == localBefore.uuid })
            val importedAfter = tasks().single { it.uuid == importedBefore.uuid }
            assertEquals(importedBefore.copy(id = importedAfter.id), importedAfter)
            assertEquals(75, app.settingsRepository.current().defaultRestSeconds)
            scenario.recreate()
            scroll(hasTestTag("reset-whip-action")).assertIsDisplayed()
            capture("cleanup.$suffix")
            compose.onNodeWithTag("reset-whip-action").performClick()
            compose.onNodeWithTag("confirm-reset-whip").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performClick()
            scenario.recreate()
            assertEquals(setOf(localBefore, importedAfter), tasks().toSet())
            assertEquals(75, app.settingsRepository.current().defaultRestSeconds)
            compose.onNodeWithContentDescription("Close Settings").performClick()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithText("Inbox", substring = false).performClick()
            compose.onNodeWithText("Keep my current task").assertIsDisplayed()
            compose.onNodeWithText("From the saved backup").assertIsDisplayed()
        }
    }

    private fun tasks() = runBlocking { app.taskRepository.tasks.first() }
    private fun scroll(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag("settings-list").performScrollToNode(matcher)
        return compose.onNode(matcher)
    }
    private fun awaitDocuments() = assertTrue(
        "Native document picker should open",
        device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")), 3_000) ||
            device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 3_000),
    )
    private fun capture(state: String) {
        compose.waitForIdle()
        captureVisualCatalogSurface("settings.data-controls.$state")
    }
}
