package com.whip.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.WhipTask
import com.whip.app.ui.TaskActionsDialog
import com.whip.app.ui.TaskTimeSettings
import com.whip.app.ui.RestDurationDialog
import com.whip.app.ui.PaneAwareAlertDialog
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain

@RunWith(AndroidJUnit4::class)
class FocusTimeSelectionUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val started = mutableListOf<Int>()
    private val busy = mutableStateOf(false)
    private val error = mutableStateOf<String?>(null)
    private val item = ScheduledTask(
        task = WhipTask(
            id = 901, title = "Read a chapter", notes = "", scheduleKind = ScheduleKind.Once,
            date = LocalDate.of(2026, 9, 28), recurrence = null, timeMinutes = null,
            reminderEnabled = false, archived = false, completedAtMillis = null,
            createdAtMillis = 1, updatedAtMillis = 1, area = "Personal", icon = "📖",
        ),
        originalDate = LocalDate.of(2026, 9, 28), scheduledDate = LocalDate.of(2026, 9, 28),
    )

    @Composable
    private fun Content(active: Boolean = false, enlarged: Boolean = false) {
        WhipTheme(dynamicColor = false) {
            TaskActionsDialog(
                item = item, onDismiss = {}, onComplete = {}, onEdit = {}, onReschedule = {},
                onSkip = {}, onArchive = {}, onDeletePermanently = {}, onPin = {},
                onDuplicate = {}, onStartFocus = { started += it }, onToggleSubtask = { _, _ -> },
                onPromoteSubtask = {}, onReopenOccurrence = {}, onResetOccurrence = {},
                focusBusy = busy.value, focusError = error.value,
                activeFocusTaskTitle = if (active) "Current reading session" else null,
                activeFocusDeadlineMillis = if (active) System.currentTimeMillis() + 60_000L else null,
                modifier = Modifier.width(if (enlarged) 320.dp else 360.dp),
            )
        }
    }

    private fun more() = compose.onNodeWithTag("task-detail-section-Options").performClick()
    private fun capture(id: String) {
        closeSoftKeyboard()
        compose.waitForIdle()
        captureVisualCatalogSurface(id)
    }
    private fun custom() {
        more()
        compose.onNodeWithTag("focus-custom-time").performScrollTo().performClick()
    }

    @Test fun presetsStartExactly15_30_45_60Minutes() {
        compose.setContent { Content() }
        more()
        compose.onNodeWithText("25 min").assertDoesNotExist()
        compose.onNodeWithTag("focus-custom-time").performScrollTo().assertIsDisplayed()
        capture("focus.duration.presets")
        listOf(15, 30, 45, 60).forEach { minutes ->
            compose.onNodeWithTag("focus-preset-$minutes").performScrollTo().performClick()
        }
        compose.runOnIdle { assertEquals(listOf(15, 30, 45, 60), started) }
    }

    @Test fun customRejectsInvalidInputAndStartsExact37MinutesWithoutClosingDraft() {
        compose.setContent { Content() }
        custom()
        listOf("", "0", "241", "1.5", "-5", "bad").forEach { invalid ->
            compose.onNodeWithTag("focus-custom-minutes").performTextReplacement(invalid)
            compose.onNodeWithTag("focus-custom-start").assertIsNotEnabled()
            compose.onNodeWithTag("focus-custom-minutes").assertTextContains(invalid)
            compose.onNodeWithTag("focus-custom-minutes").assert(
                SemanticsMatcher("announces the allowed minute range") { node ->
                    node.config.contains(SemanticsProperties.Error) &&
                        node.config[SemanticsProperties.Error].contains("1–240 minutes")
                },
            )
            if (invalid == "241") capture("focus.duration.custom-error")
        }
        compose.onNodeWithTag("focus-custom-minutes").performTextReplacement("37")
        capture("focus.duration.custom")
        compose.onNodeWithTag("focus-custom-start").performClick()
        compose.onNodeWithTag("focus-custom-minutes").assertTextContains("37")
        compose.runOnIdle { assertEquals(listOf(37), started) }
    }

    @Test fun cancelStartsNothingAndCustomDraftSurvivesRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        custom()
        compose.onNodeWithTag("focus-custom-minutes").performTextReplacement("37")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("focus-custom-minutes").assertTextContains("37")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("focus-custom-time").performScrollTo().performClick()
        compose.onNodeWithTag("focus-custom-minutes").assertTextContains("37")
        compose.runOnIdle { assertTrue(started.isEmpty()) }
    }

    @Test fun replacementNamesCurrentTimerAndKeepRestoresCustomDraft() {
        compose.setContent { Content(active = true) }
        custom()
        compose.onNodeWithTag("focus-custom-minutes").performTextReplacement("37")
        compose.onNodeWithTag("focus-custom-start").performClick()
        compose.onNodeWithTag("focus-custom-minutes").assertDoesNotExist()
        compose.onNodeWithText("“Current reading session” has an active timer. Replace it with a 37 minute timer for “Read a chapter”?").assertIsDisplayed()
        capture("focus.duration.replacement")
        compose.onNodeWithText("Keep Timer").performClick()
        compose.onNodeWithTag("focus-custom-minutes").assertTextContains("37")
        capture("focus.duration.custom-restored")
        compose.runOnIdle { assertTrue(started.isEmpty()) }
        compose.onNodeWithTag("focus-custom-start").performClick()
        compose.onNodeWithTag("focus-replace").performClick()
        compose.runOnIdle { assertEquals(listOf(37), started) }
    }

    @Test fun pendingStartBlocksControlsAndFailurePreservesDraftForRetry() {
        compose.setContent { Content() }
        custom()
        compose.onNodeWithTag("focus-custom-minutes").performTextReplacement("37")
        compose.onNodeWithTag("focus-custom-start").performClick()
        compose.runOnIdle { busy.value = true }
        fun waitForStartingOverlay() {
            compose.waitUntil(5_000) {
                runCatching {
                    compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
                        .assertContentDescriptionContains("Starting…", substring = true)
                }.isSuccess
            }
        }
        waitForStartingOverlay()
        pressBack()
        waitForStartingOverlay()
        compose.onNodeWithTag("focus-custom-start").assertDoesNotExist()
        compose.onNodeWithText("Cancel").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(37), started) }
        compose.runOnIdle { busy.value = false; error.value = "Could not save the Focus timer. Retry." }
        compose.onNodeWithTag("focus-start-error").assertIsDisplayed()
        compose.onNodeWithTag("focus-custom-minutes").assertTextContains("37")
        compose.onNodeWithTag("focus-custom-start").performClick()
        compose.runOnIdle { assertEquals(listOf(37, 37), started) }
    }

    @AndroidFontScale
    @Test fun enlargedPresetsAndCustomControlRemainReachableWith48DpTargets() {
        compose.setContent { Content(enlarged = true) }
        compose.assertDialogFontScale()
        more()
        listOf("focus-preset-15", "focus-preset-30", "focus-preset-45", "focus-preset-60", "focus-custom-time").forEach { tag ->
            val bounds = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("$tag keeps a 48 dp target", bounds.bottom - bounds.top >= 48.dp)
            if (tag == "focus-preset-15") capture("focus.duration.presets.large-top")
        }
        capture("focus.duration.presets.large")
        compose.onNodeWithTag("focus-custom-time").performClick()
        compose.assertDialogFontScale()
        compose.onNodeWithTag("focus-custom-minutes").assertIsDisplayed()
        compose.onNodeWithTag("focus-custom-start").assertIsDisplayed()
        capture("focus.duration.custom.large")
    }

    @Test fun presetReplacementRequiresConsentBeforeStarting() {
        compose.setContent { Content(active = true) }
        more()
        compose.onNodeWithTag("focus-preset-30").performScrollTo().performClick()
        compose.onNodeWithText("Keep Timer").performClick()
        compose.runOnIdle { assertTrue(started.isEmpty()) }
        compose.onNodeWithTag("focus-preset-45").performScrollTo().performClick()
        compose.onNodeWithTag("focus-replace").performClick()
        compose.runOnIdle { assertEquals(listOf(45), started) }
    }

    @AndroidFontScale
    @Test fun reminderCustomTimePreservesInvalidInputAndAddsExactOffsetAtLargeText() {
        val draft = mutableStateOf("")
        val offsets = mutableStateOf(setOf(0))
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                PaneAwareAlertDialog(
                    modifier = Modifier.width(320.dp), onDismissRequest = {},
                    title = { Text("Reminder Times") },
                    confirmButton = {}, dismissButton = {}, text = {
                        androidx.compose.foundation.layout.Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                            TaskTimeSettings(
                                hasTime = true, timeMinutes = 480, reminderEnabled = true,
                                reminderOffsets = offsets.value, customReminderText = draft.value,
                                onHasTimeChange = {}, onChangeTime = {}, onReminderEnabledChange = {},
                                onReminderOffsetsChange = { offsets.value = it },
                                onCustomReminderTextChange = { draft.value = it },
                            )
                        }
                    },
                )
            }
        }
        compose.assertDialogFontScale()
        compose.onNodeWithTag("task-reminder-custom-minutes").assertDoesNotExist()
        compose.onNodeWithTag("task-reminder-custom-time").performScrollTo().performClick()
        compose.onNodeWithTag("task-reminder-custom-minutes").performScrollTo().performTextReplacement("-5")
        compose.onNodeWithTag("task-reminder-custom-add").assertIsNotEnabled()
        compose.runOnIdle { assertEquals("-5", draft.value) }
        compose.onNodeWithTag("task-reminder-custom-minutes").performTextReplacement("43201")
        compose.onNodeWithTag("task-reminder-custom-add").assertIsNotEnabled()
        compose.onNodeWithTag("task-reminder-custom-minutes").performTextReplacement("37")
        compose.onNodeWithTag("task-reminder-custom-add").performScrollTo().assertIsDisplayed()
        capture("focus.duration.reminder.large")
        compose.onNodeWithTag("task-reminder-custom-add").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(setOf(0, 37), offsets.value) }
        compose.onNodeWithTag("task-reminder-custom-minutes").performScrollTo().performTextReplacement("37")
        compose.onNodeWithTag("task-reminder-custom-add").assertIsNotEnabled()
        compose.onNodeWithText("This reminder already exists. Enter 0–43,200 minutes.").performScrollTo().assertIsDisplayed()
        capture("focus.duration.reminder-duplicate.large")
    }

    @Test fun gymCustomRestKeepsSecondsAndRejectsInvalidDuration() {
        val chosen = mutableListOf<Int?>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                RestDurationDialog(
                    initialSeconds = 120, isWorkoutOverride = false, presetSeconds = listOf(60, 120),
                    onDismiss = {}, onPresetSecondsChange = {}, onConfirm = { chosen += it },
                )
            }
        }
        capture("focus.duration.gym-rest")
        compose.onNodeWithTag("rest-custom-seconds").performTextReplacement("14")
        compose.onNodeWithText("Use for This Workout").assertIsNotEnabled()
        capture("focus.duration.gym-rest-error")
        compose.onNodeWithTag("rest-custom-seconds").performTextReplacement("135")
        compose.onNodeWithText("Use for This Workout").performClick()
        compose.runOnIdle { assertEquals(listOf(135), chosen) }
    }

    @Test fun taskClockLabelFollowsAndroid12And24HourPreference() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val original = device.executeShellCommand("settings get system time_12_24").trim()
        val minutes = mutableStateOf(16 * 60 + 5)
        try {
            device.executeShellCommand("settings put system time_12_24 24")
            assertTrue(android.text.format.DateFormat.is24HourFormat(context))
            compose.setContent {
                WhipTheme(dynamicColor = false) {
                    androidx.compose.foundation.layout.Column(Modifier.width(320.dp)) {
                        TaskTimeSettings(
                            hasTime = true, timeMinutes = minutes.value, reminderEnabled = false,
                            reminderOffsets = emptySet(), customReminderText = "",
                            onHasTimeChange = {}, onChangeTime = {}, onReminderEnabledChange = {},
                            onReminderOffsetsChange = {}, onCustomReminderTextChange = {},
                        )
                    }
                }
            }
            compose.onNodeWithText("16:05").assertIsDisplayed()
            device.executeShellCommand("settings put system time_12_24 12")
            assertTrue(!android.text.format.DateFormat.is24HourFormat(context))
            compose.runOnIdle { minutes.value = 16 * 60 + 6 }
            val time = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 16)
                set(Calendar.MINUTE, 6)
                set(Calendar.SECOND, 0)
            }.time
            val expected12Hour = android.text.format.DateFormat.getTimeFormat(context).format(time)
            compose.onNodeWithText(expected12Hour).assertIsDisplayed()
            compose.onNodeWithText("16:06").assertDoesNotExist()
        } finally {
            device.executeShellCommand(
                if (original == "null") "settings delete system time_12_24"
                else "settings put system time_12_24 $original",
            )
        }
    }
}
