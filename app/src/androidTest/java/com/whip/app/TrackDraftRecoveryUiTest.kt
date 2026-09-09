package com.whip.app

import android.app.Application
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackValueDraft
import com.whip.app.ui.TrackEditorRecoveryNotice
import com.whip.app.ui.TrackEditorRoute
import com.whip.app.ui.TrackEditorSessionViewModel
import com.whip.app.ui.theme.WhipTheme
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDraftRecoveryUiTest {
    private val compose = createComposeRule()
    private val temporary = TemporaryFolder()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(temporary)
        .around(AndroidFontScaleRule()).around(compose)

    @Test fun recoveryNoticesDistinguishLiveAndUnavailableDrafts() = verifyNotices(false)

    @Test @AndroidFontScale
    fun recoveryNoticesRemainReadableAtLargeText() = verifyNotices(true)

    private fun verifyNotices(large: Boolean) {
        fun application(name: String) = object : Application() {
            override fun getNoBackupFilesDir() = File(temporary.root, name).apply { mkdirs() }
        }
        fun prepared(app: Application) = TrackEditorSessionViewModel(app, SavedStateHandle()).apply {
            routeState.value = TrackEditorRoute.Entry(7, openingDataGeneration = 0, sessionId = 1)
            entry.initialize("entry-7-new-1-g0", TrackEntryDraft(
                LocalDate.of(2026, 9, 9), mapOf("name" to TrackValueDraft(textValue = "River trail")),
            ))
        }
        val writeApp = application("write")
        val live = prepared(writeApp)
        File(writeApp.noBackupFilesDir, "track-editor-checkpoints").writeText("unavailable")
        live.saveCheckpoint()
        val readApp = application("read")
        val source = prepared(readApp)
        val reference = source.saveCheckpoint()
        File(readApp.noBackupFilesDir, "track-editor-checkpoints").walkTopDown()
            .single { it.isFile }.writeText("corrupt")
        val unavailable = TrackEditorSessionViewModel(readApp, SavedStateHandle(mapOf(
            TrackEditorSessionViewModel.STATE_KEY to reference,
        )))
        val shown = mutableStateOf(live)
        compose.setContent {
            WhipTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box { TrackEditorRecoveryNotice(shown.value) }
                }
            }
        }
        compose.onNodeWithTag("track-draft-recovery-problem")
            .assertTextContains("Your Track draft is still open", substring = true).assertIsDisplayed()
        if (large) compose.assertDialogFontScale()
        capture("write", large)
        compose.onNodeWithText("Keep Editing").performClick()
        assertEquals("River trail", live.entry.state.value.draft?.values?.get("name")?.textValue)
        compose.runOnIdle { shown.value = unavailable }
        compose.onNodeWithTag("track-draft-recovery-problem")
            .assertTextContains("could not recover the unsaved Track draft", substring = true).assertIsDisplayed()
        if (large) compose.assertDialogFontScale()
        capture("read", large)
        compose.onNodeWithText("Retry").performClick()
        assertNull(unavailable.entry.state.value.draft)
        compose.onNodeWithText("Close").performClick()
        assertNull(unavailable.routeState.value)
        assertNull(unavailable.recoveryProblem)
    }

    private fun capture(kind: String, large: Boolean) {
        val id = "tracks.draft-recovery.$kind.${if (large) "large" else "ordinary"}"
        if (Build.VERSION.SDK_INT >= 29) {
            captureVisualCatalogSurface(id)
        } else {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            compose.waitForIdle()
            instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
            val directory = checkNotNull(instrumentation.targetContext.getExternalFilesDir("track-draft-recovery"))
            check(directory.isDirectory || directory.mkdirs())
            val device = UiDevice.getInstance(instrumentation)
            device.waitForIdle()
            check(device.takeScreenshot(File(directory, "$id.png")))
            device.dumpWindowHierarchy(File(directory, "$id.xml"))
        }
    }
}
