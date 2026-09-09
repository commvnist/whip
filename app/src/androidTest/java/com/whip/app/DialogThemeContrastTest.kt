package com.whip.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogThemeContrastTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var originalNightMode = "no"
    private var nativeStatusHeight = 0
    private val contrastFailures = mutableListOf<String>()

    @Before
    fun seed() = runBlocking {
        originalNightMode = device.executeShellCommand("cmd uimode night").substringAfter(":").trim()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(dynamicColor = false) }
        app.taskRepository.create(TaskDraft("Theme boundary task", scheduleKind = ScheduleKind.Once, date = app.clock.today()))
        Unit
    }

    @After
    fun restore() = runBlocking {
        device.executeShellCommand("cmd uimode night $originalNightMode")
        app.backupRepository.deleteAllData()
    }

    @Test
    fun darkDialogsOnLightAndroidFollowThemeAcrossEveryWindowOwner() = verifyWindows(systemDark = false)

    @Test
    fun lightDialogsOnDarkAndroidFollowThemeAcrossEveryWindowOwner() = verifyWindows(systemDark = true)

    private fun verifyWindows(systemDark: Boolean) {
        device.executeShellCommand("cmd uimode night ${if (systemDark) "yes" else "no"}")
        compose.waitUntil(10_000) {
            (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) == systemDark
        }
        val dark = !systemDark
        setAppTheme(dark)
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            waitForWelcome()
            scenario.onActivity {
                nativeStatusHeight = checkNotNull(ViewCompat.getRootWindowInsets(it.window.decorView))
                    .getInsets(WindowInsetsCompat.Type.statusBars()).top
            }
            capture("platform.dialog.${if (dark) "dark-on-light" else "light-on-dark"}")
            checkDialogBars("First run", dark)
            scenario.recreate()
            waitForWelcome()
            checkDialogBars("Recreated first run", dark)
            setAppTheme(!dark)
            checkDialogBars("Live first-run change", !dark)
            setAppTheme(dark)
            checkDialogBars("Restored first-run theme", dark)

            compose.onNodeWithText("Use Recommended").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Welcome to Whip").fetchSemanticsNodes().isEmpty() }
            waitForTag("home-tasks-today-record")
            scenario.onActivity { checkBars("Home after setup", it.window, dark) }

            compose.onNodeWithContentDescription("Open task details for Theme boundary task").performClick()
            waitForTag("entity-inspector-header")
            capture("platform.dialog.inspector-${if (dark) "dark" else "light"}")
            checkDialogBars("Task inspector", dark)
            compose.onNodeWithTag("entity-inspector-edit").performClick()
            waitForTag("task-editor-surface")
            capture("platform.dialog.task-editor-${if (dark) "dark" else "light"}")
            checkDialogBars("Task editor", dark)
            compose.onNodeWithContentDescription("Cancel Task editing").performClick()
            waitForTag("home-tasks-today-record")

            compose.onNodeWithTag("workspace-search-action").performClick()
            waitForTag("unified-search-surface")
            capture("platform.dialog.search-${if (dark) "dark" else "light"}")
            checkDialogBars("Unified Search", dark, statusBarUsesContentBackground = true)
            compose.onNodeWithTag("unified-search-close-action").performClick()
            waitForTag("home-tasks-today-record")

            compose.onNodeWithTag("workspace-add-action").performClick()
            compose.onNodeWithText("New Habit").performClick()
            waitForTag("habit-editor-surface")
            capture("platform.dialog.primary-editor-${if (dark) "dark" else "light"}")
            checkDialogBars("Shared primary editor", dark)
            compose.onNodeWithContentDescription("Cancel Habit editing").performClick()
            compose.waitForIdle()
            scenario.onActivity { checkBars("Activity after dialog dismissal", it.window, dark) }
        }
        assertTrue(contrastFailures.joinToString("\n"), contrastFailures.isEmpty())
    }

    private fun setAppTheme(dark: Boolean) {
        runBlocking { app.settingsRepository.update { it.copy(themeMode = if (dark) AppThemeMode.Dark else AppThemeMode.Light) } }
        compose.waitForIdle()
    }

    private fun waitForWelcome() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Welcome to Whip").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun checkDialogBars(label: String, dark: Boolean, statusBarUsesContentBackground: Boolean = false) {
        compose.waitForIdle()
        var dimAmount = 0f
        onView(isRoot()).inRoot(isDialog()).check { root, error ->
            if (error != null) throw error
            val window = checkNotNull(findDialogWindow(root)) { "Expected a Compose dialog window for $label" }
            checkBars(label, window, dark, darkStatusBarBackdrop = dark || !statusBarUsesContentBackground)
            dimAmount = window.attributes.dimAmount
        }
        // Fitted dialog decor has already consumed these insets. The underlying
        // edge-to-edge Activity retains the actual screen status-band bounds.
        assertTrue("Expected native status-bar bounds for $label", nativeStatusHeight > 0)
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(750L, 5_000L)
        device.waitForIdle()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            // These disposable AOSP devices have flat, solid status backdrops.
            // Compare their predominant painted foreground and background, rather
            // than treating the requested icon flags as proof of readable pixels.
            val colors = mutableMapOf<Int, Int>()
            for (y in 0 until nativeStatusHeight.coerceAtMost(bitmap.height)) {
                for (x in 0 until bitmap.width) {
                    val color = bitmap.getPixel(x, y)
                    colors[color] = (colors[color] ?: 0) + 1
                }
            }
            val background = colors.maxBy { it.value }.key
            val foreground = checkNotNull(colors.filterKeys {
                // Android's colored charging/privacy indicators are independent
                // of the light/dark clock and icon appearance controlled here.
                val channels = listOf(Color.red(it), Color.green(it), Color.blue(it))
                channels.max() - channels.min() <= 12 && ColorUtils.calculateContrast(it, background) > 1.2
            }.maxByOrNull { it.value }) { "Expected painted status text/icons for $label" }.key
            val contrast = ColorUtils.calculateContrast(foreground, background)
            if (contrast < 4.5) contrastFailures += "$label: native status contrast $contrast is below 4.5 (dim=$dimAmount)"
        } finally {
            bitmap.recycle()
        }
    }

    private fun findDialogWindow(view: View): Window? {
        if (view is DialogWindowProvider) return view.window
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) findDialogWindow(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun checkBars(label: String, window: Window, dark: Boolean, darkStatusBarBackdrop: Boolean = dark) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (controller.isAppearanceLightStatusBars != !darkStatusBarBackdrop) {
            contrastFailures += "$label: status icons do not match dark backdrop=$darkStatusBarBackdrop"
        }
        if (controller.isAppearanceLightNavigationBars != !dark) contrastFailures += "$label: navigation icons do not match dark=$dark"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            val lightScrim = ColorUtils.calculateLuminance(window.navigationBarColor) > 0.5
            if (lightScrim != !dark) contrastFailures += "$label: legacy navigation scrim does not match dark=$dark"
        }
    }

    private fun capture(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            captureVisualCatalogSurface(id)
        } else {
            compose.waitForIdle()
            device.waitForIdle()
            val directory = checkNotNull(app.getExternalFilesDir("dialog-theme"))
            check(directory.isDirectory || directory.mkdirs())
            check(device.takeScreenshot(File(directory, "$id.png")))
        }
    }
}
