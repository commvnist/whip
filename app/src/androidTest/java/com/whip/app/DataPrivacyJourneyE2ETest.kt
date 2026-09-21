package com.whip.app

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.lifecycle.ViewModelProvider
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.TaskDraft
import com.whip.app.data.EncryptedBackupCodec
import com.whip.app.ui.SettingsViewModel
import org.json.JSONObject
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
    private val encryptedFileName = "WhipEncryptedPickerRecreation.whip.enc.json"
    private val plainFileName = "WhipPlainPickerRegression.whip.json"
    private val csvFileName = "WhipTasksPickerRegression.csv"

    @After fun clean() {
        runBlocking { app.backupRepository.deleteAllData() }
        device.executeShellCommand("rm -f /sdcard/Download/$fileName")
        device.executeShellCommand("rm -f /sdcard/Download/$encryptedFileName")
        device.executeShellCommand("rm -f /sdcard/Download/$plainFileName")
        device.executeShellCommand("rm -f /sdcard/Download/$csvFileName")
    }

    @Test fun backupChoicesPreserveRecordsAndSettingsAfterReopening() = journey(AppThemeMode.Dark)
    @Test fun backupChoicesRemainClearInLightTheme() = journey(AppThemeMode.Light)

    @Test fun encryptedExportSurvivesActivityRecreationWhileDocumentPickerIsOpen() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
            app.taskRepository.create(TaskDraft(title = "Picker recreation keeps this task"))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            lateinit var settingsViewModel: SettingsViewModel
            lateinit var originalActivity: MainActivity
            scenario.onActivity {
                originalActivity = it
                settingsViewModel = ViewModelProvider(it)[SettingsViewModel::class.java]
            }
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.openSettingsCategory("Data & Privacy")
            scroll(hasText("Save Passphrase-Encrypted Backup")).performClick()
            compose.onNodeWithText("Passphrase", substring = false).performTextReplacement("test-only-passphrase")
            compose.onNodeWithText("Confirm passphrase").performTextReplacement("test-only-passphrase")
            compose.onNodeWithText("Choose Location").performClick()
            awaitDocuments()
            InstrumentationRegistry.getInstrumentation().runOnMainSync { originalActivity.recreate() }
            compose.waitUntil(10_000) { originalActivity.isDestroyed }
            saveCreatedDocument(encryptedFileName)
            compose.waitUntil(10_000) { settingsViewModel.uiState.value.message != null }
            assertEquals("Encrypted backup saved", settingsViewModel.uiState.value.message)
            capture("encrypted-export.saved")
            val encrypted = device.executeShellCommand("cat /sdcard/Download/$encryptedFileName")
            assertTrue(EncryptedBackupCodec.isEncrypted(encrypted))
            val plaintext = EncryptedBackupCodec.decrypt(encrypted, "test-only-passphrase".toCharArray())
            assertTrue(plaintext.contains("Picker recreation keeps this task"))
        }
    }

    @Test fun plainAndCsvExportsKeepTheirRequestedFormats() {
        runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
            app.taskRepository.create(TaskDraft(title = "Export format survives picker"))
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            lateinit var settingsViewModel: SettingsViewModel
            scenario.onActivity { settingsViewModel = ViewModelProvider(it)[SettingsViewModel::class.java] }
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.openSettingsCategory("Data & Privacy")
            scroll(hasText("Save Plain JSON Backup")).performClick()
            saveCreatedDocument(plainFileName)
            compose.waitUntil(10_000) { settingsViewModel.uiState.value.message == "Plain JSON backup saved" }
            capture("plain-export.saved")
            val plain = device.executeShellCommand("cat /sdcard/Download/$plainFileName")
            assertFalse(EncryptedBackupCodec.isEncrypted(plain))
            assertEquals("whip-backup", JSONObject(plain).getString("format"))
            assertTrue(plain.contains("Export format survives picker"))

            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Export CSV"))
            compose.onNode(hasText("Tasks", substring = false) and hasAnyAncestor(hasTestTag("settings-list"))).performClick()
            saveCreatedDocument(csvFileName)
            compose.waitUntil(10_000) { settingsViewModel.uiState.value.message == "CSV saved" }
            capture("csv-export.saved")
            val csv = device.executeShellCommand("cat /sdcard/Download/$csvFileName")
            assertTrue(csv.contains("Export format survives picker"))
            assertFalse(EncryptedBackupCodec.isEncrypted(csv))
        }
    }

    @Test fun cancelledOrLostDocumentRequestCannotWriteAnExport() {
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            lateinit var settingsViewModel: SettingsViewModel
            scenario.onActivity {
                settingsViewModel = ViewModelProvider(it)[SettingsViewModel::class.java]
                settingsViewModel.prepareDocumentExport(com.whip.app.ui.ExportKind.EncryptedBackup, "test-only-passphrase")
                settingsViewModel.completeDocumentExport(null)
                settingsViewModel.completeDocumentExport(Uri.parse("content://whip.invalid/export"))
            }
            compose.waitUntil(10_000) {
                settingsViewModel.uiState.value.message ==
                    "Export was interrupted. The selected file may be empty; choose a location again."
            }
            assertFalse(settingsViewModel.uiState.value.busy)
        }
    }

    @Test fun invalidBackupIsReportedAsFailureAndCanBeReplaced() {
        val valid = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false) }
            app.taskRepository.create(TaskDraft(title = "Keep this original record", notes = "An invalid backup must not change this"))
            app.backupRepository.exportBackup()
        }
        val before = tasks()
        writeFixture(JSONObject(valid).put("checksumSha256", "invalid").toString())
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            lateinit var settingsViewModel: SettingsViewModel
            scenario.onActivity { settingsViewModel = ViewModelProvider(it)[SettingsViewModel::class.java] }
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.openSettingsCategory("Data & Privacy")
            scroll(hasText("Preview and Restore Backup")).performClick()
            selectFixture()
            compose.waitUntil(10_000) { settingsViewModel.uiState.value.message == "Backup checksum does not match" }
            capture("invalid-return")
            scroll(hasText("Backup checksum does not match")).assertIsDisplayed()
            assertEquals(before, tasks())
            compose.onNodeWithText("Action Not Completed").assertIsDisplayed()
            compose.onNodeWithText("Dismiss").performClick()
            writeFixture(valid)
            scroll(hasText("Preview and Restore Backup")).performClick()
            selectFixture()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("merge-new-data").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("merge-new-data").assertIsEnabled()
            assertNull(settingsViewModel.uiState.value.message)
            capture("corrected-preview")
            compose.onNodeWithText("Cancel").performClick()
            scenario.recreate()
            assertEquals(before, tasks())
            compose.onAllNodesWithText("Backup checksum does not match").assertCountEquals(0)
        }
    }

    @Test fun wrongBackupPassphraseExplainsFailureInsideDialogAndRetries() {
        val encrypted = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { AppSettings(setupCompleted = true, themeMode = AppThemeMode.Light, dynamicColor = false) }
            app.taskRepository.create(TaskDraft(title = "Keep encrypted history", notes = "Original encrypted record"))
            EncryptedBackupCodec.encrypt(app.backupRepository.exportBackup(), "correct-test-passphrase".toCharArray())
        }
        val before = tasks()
        writeFixture(encrypted)
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            lateinit var settingsViewModel: SettingsViewModel
            scenario.onActivity { settingsViewModel = ViewModelProvider(it)[SettingsViewModel::class.java] }
            compose.onNodeWithContentDescription("Open Settings").performClick()
            compose.openSettingsCategory("Data & Privacy")
            scroll(hasText("Preview and Restore Backup")).performClick()
            selectFixture()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Unlock Encrypted Backup").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Passphrase").performTextReplacement("wrong-test-passphrase")
            compose.onNodeWithText("Unlock and Preview").performClick()
            val failure = "Wrong passphrase or encrypted backup was modified"
            compose.waitUntil(10_000) { settingsViewModel.uiState.value.message == failure }
            capture("unlock-failure")
            assertEquals(before, tasks())
            compose.onNode(hasText(failure) and hasAnyAncestor(isDialog())).assertIsDisplayed()
            compose.onNodeWithText("Passphrase").performTextReplacement("correct-test-passphrase")
            compose.onNodeWithText("Unlock and Preview").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("merge-new-data").fetchSemanticsNodes().isNotEmpty() }
            assertNull(settingsViewModel.uiState.value.message)
            capture("unlocked-preview")
            compose.onNodeWithTag("merge-new-data").performScrollTo().performClick()
            compose.waitUntil(10_000) { settingsViewModel.uiState.value.message?.startsWith("Imported ") == true }
            scenario.recreate()
            assertEquals(before, tasks())
            scroll(hasText("Action Completed")).assertIsDisplayed()
        }
    }

    private fun writeFixture(json: String) {
        val encoded = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
        device.executeShellCommand("mkdir -p /sdcard/Download")
        device.executeShellCommand("sh -c echo\${IFS}$encoded|base64\${IFS}-d>/sdcard/Download/$fileName")
        assertEquals(json, device.executeShellCommand("cat /sdcard/Download/$fileName"))
    }

    private fun selectFixture() {
        awaitDocuments()
        assertTrue(device.wait(Until.hasObject(By.desc("Show roots")), 10_000))
        device.findObject(By.desc("Show roots")).click()
        assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10_000))
        device.findObjects(By.text("Downloads")).last().click()
        assertTrue(device.wait(Until.hasObject(By.text(fileName)), 10_000))
        device.findObject(By.text(fileName)).click()
    }

    private fun saveCreatedDocument(name: String) {
        awaitDocuments()
        assertTrue(device.wait(Until.hasObject(By.desc("Show roots")), 10_000))
        device.findObject(By.desc("Show roots")).click()
        assertTrue(device.wait(Until.hasObject(By.text("Downloads")), 10_000))
        device.findObjects(By.text("Downloads")).last().click()
        requireNotNull(device.findObject(By.clazz("android.widget.EditText"))) {
            "Document name field should be present"
        }.text = name
        val save = device.wait(Until.findObject(By.text("Save")), 10_000)
            ?: device.wait(Until.findObject(By.text("SAVE")), 10_000)
        requireNotNull(save) { "Native Save action should be available" }.click()
    }

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
