package com.whip.app

import android.content.Intent
import android.content.res.Configuration
import android.appwidget.AppWidgetManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.core.view.WindowCompat
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.startup.StartupRecoveryState
import com.whip.app.widget.WhipWidgetConfigureActivity
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityThemeContrastTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var originalNightMode = "no"

    @Before
    fun seed() = runBlocking {
        originalNightMode = device.executeShellCommand("cmd uimode night").substringAfter(":").trim()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false) }
        app.taskRepository.create(TaskDraft("Plan the day", scheduleKind = ScheduleKind.Once, date = app.clock.today()))
        Unit
    }

    @After
    fun restore() = runBlocking {
        device.executeShellCommand("cmd uimode night $originalNightMode")
        app.backupRepository.deleteAllData()
    }

    @Test
    fun darkWhipOnLightAndroidKeepsBarsReadableThroughLiveChangesAndRecreation() {
        verifyOppositeTheme(systemDark = false)
    }

    @Test
    fun lightWhipOnDarkAndroidKeepsBarsReadableThroughLiveChangesAndRecreation() {
        verifyOppositeTheme(systemDark = true)
    }

    @Test
    fun widgetConfigurationChromeFollowsThemeChangesAndRecovery() {
        setSystemDark(false)
        setAppTheme(AppThemeMode.Dark)
        ActivityScenario.launch<WhipWidgetConfigureActivity>(
            Intent(app, WhipWidgetConfigureActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 73_044),
        ).use { scenario ->
            verifyExternalTheme(scenario, "widget-config-header", "widget")
        }
    }

    private fun verifyOppositeTheme(systemDark: Boolean) {
        setSystemDark(systemDark)
        val appDark = !systemDark
        setAppTheme(if (appDark) AppThemeMode.Dark else AppThemeMode.Light)
        launchMainActivity(
            Intent(app, MainActivity::class.java)
                .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true),
        ).use { scenario ->
            waitForHome()
            captureThemeSurface(if (appDark) "platform.theme.dark-on-light" else "platform.theme.light-on-dark")
            assertBars(scenario, appDark)
            scenario.recreate()
            waitForHome()
            awaitBars(scenario, appDark)

            setAppTheme(if (systemDark) AppThemeMode.Dark else AppThemeMode.Light)
            awaitBars(scenario, systemDark)
            setAppTheme(AppThemeMode.System)
            awaitBars(scenario, systemDark)
            setSystemDark(!systemDark)
            waitForHome()
            awaitBars(scenario, !systemDark)
            if (appDark) verifyRecovery(scenario, "home-tasks-today-record", "main")
        }
    }

    private fun <T : ComponentActivity> verifyExternalTheme(scenario: ActivityScenario<T>, tag: String, id: String) {
        waitForTag(tag)
        awaitBars(scenario, dark = true)
        captureThemeSurface("platform.theme.$id-dark-on-light")
        setAppTheme(AppThemeMode.Light)
        awaitBars(scenario, dark = false)
        runBlocking { app.settingsRepository.update { it.copy(dynamicColor = true) } }
        scenario.recreate()
        waitForTag(tag)
        awaitBars(scenario, dark = false)
        verifyRecovery(scenario, tag, id)
    }

    private fun <T : ComponentActivity> verifyRecovery(scenario: ActivityScenario<T>, tag: String, id: String) {
        setSystemDark(false)
        setAppTheme(AppThemeMode.Dark)
        waitForTag(tag)
        awaitBars(scenario, dark = true)
        val marker = File(app.noBackupFilesDir, "restore-recovery.whip.json")
        check(!marker.exists()) { "A pending recovery must never be overwritten by a test" }
        try {
            marker.writeText("intentionally corrupt recovery snapshot")
            runBlocking { app.blockForPendingRecovery() }
            waitForTag("startup-recovery-screen")
            awaitBars(scenario, dark = false)
            if (id == "main") captureThemeSurface("platform.theme.main-recovery-light")
        } finally {
            marker.delete()
            app.retryStartupRecovery()
            runBlocking {
                withTimeout(10_000) {
                    while (app.startupRecoveryState.value != StartupRecoveryState.Ready) delay(20)
                }
            }
        }
        waitForTag(tag)
        awaitBars(scenario, dark = true)
    }

    private fun setAppTheme(mode: AppThemeMode) = runBlocking {
        app.settingsRepository.update { it.copy(themeMode = mode) }
    }

    private fun captureThemeSurface(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            captureVisualCatalogSurface(id)
        } else {
            // The catalog exporter requires MediaStore Downloads (Android 10+).
            // Retain the same window evidence privately on older test devices.
            compose.waitForIdle()
            device.waitForIdle()
            val directory = checkNotNull(app.getExternalFilesDir("theme-contrast"))
            check(directory.isDirectory || directory.mkdirs())
            check(device.takeScreenshot(File(directory, "$id.png")))
        }
    }

    private fun setSystemDark(dark: Boolean) {
        device.executeShellCommand("cmd uimode night ${if (dark) "yes" else "no"}")
        compose.waitUntil(10_000) {
            (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) == dark
        }
    }

    private fun waitForHome() = waitForTag("home-tasks-today-record")

    private fun waitForTag(tag: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun <T : ComponentActivity> awaitBars(scenario: ActivityScenario<T>, dark: Boolean) {
        compose.waitUntil(10_000) {
            var matches = false
            scenario.onActivity { activity ->
                val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                matches = controller.isAppearanceLightStatusBars == !dark &&
                    controller.isAppearanceLightNavigationBars == !dark
            }
            matches
        }
        assertBars(scenario, dark)
    }

    private fun <T : ComponentActivity> assertBars(scenario: ActivityScenario<T>, dark: Boolean) {
        scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertEquals("Status icons must contrast with the displayed Whip theme", !dark, controller.isAppearanceLightStatusBars)
            assertEquals("Navigation icons must contrast with the displayed Whip theme", !dark, controller.isAppearanceLightNavigationBars)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val lightScrim = ColorUtils.calculateLuminance(activity.window.navigationBarColor) > 0.5
                assertTrue("Legacy navigation scrim must follow the displayed theme", lightScrim == !dark)
            }
        }
    }
}
