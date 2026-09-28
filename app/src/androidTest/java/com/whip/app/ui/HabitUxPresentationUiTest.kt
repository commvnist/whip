package com.whip.app.ui

import android.view.accessibility.AccessibilityWindowInfo
import android.graphics.Rect
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.domain.*
import com.whip.app.ui.theme.WhipTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitUxPresentationUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test fun isolatedWeeklyObservationsRenderVisiblePointsAcrossGaps() {
        var pointColor = Color.Transparent
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                pointColor = MaterialTheme.colorScheme.primary
                Surface {
                    Column(Modifier.padding(20.dp)) {
                        HabitRateChart("Sparse practice", listOf(null, 0.5, null, null, 1.0, null, null, null), (0L..7L).map { LocalDate.of(2026, 8, 3).plusWeeks(it) })
                    }
                }
            }
        }
        val pixels = compose.onNodeWithTag("habit-rate-chart").captureToImage().toPixelMap()
        var coloredPixels = 0
        for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
            val c = pixels[x, y]
            if (abs(c.red - pointColor.red) < 0.03f && abs(c.green - pointColor.green) < 0.03f && abs(c.blue - pointColor.blue) < 0.03f) coloredPixels++
        }
        check(coloredPixels > 12) { "Isolated observations disappeared: $coloredPixels chart pixels" }
        compose.onNodeWithTag("habit-rate-chart").assertContentDescriptionContains("Aug 10, 2026: 50 percent", substring = true)
        captureVisualCatalogSurface("ux-upgrades.habits-isolated-chart")
    }

    @Test @AndroidFontScale
    fun timerReviewKeepsEditableEstimateAndActionsReachableWithKeyboard() {
        var logged: Double? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                HabitTimerReviewDialog(
                    HabitTimerReviewPrompt(HabitTimerBoundary(1, "habit", "session"), "Evening mobility and recovery", 600.0),
                    onDismiss = {}, onStopAndLog = { logged = it }, onContinue = {}, onDiscard = {},
                )
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        compose.onNodeWithTag("habit-timer-review-minutes").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            instrumentation.uiAutomation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        }
        compose.onNodeWithTag("habit-timer-review-minutes").performTextReplacement("12")
        instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
        compose.waitForIdle()
        captureVisualCatalogSurface("ux-upgrades.habits-timer-review.large")
        val keyboard = Rect().also { bounds ->
            instrumentation.uiAutomation.windows.first { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }.getBoundsInScreen(bounds)
        }
        val stopBounds = device.findObject(By.text("Stop & Log"))?.visibleBounds
        val inputBounds = device.findObject(By.clazz("android.widget.EditText"))?.visibleBounds
        check(inputBounds != null && inputBounds.height() > 0 && inputBounds.bottom <= keyboard.top) {
            "Editable estimate must remain visible above the actual keyboard: $inputBounds versus $keyboard"
        }
        check(stopBounds != null && stopBounds.height() > 0 && stopBounds.bottom <= keyboard.top) {
            "Stop & Log must remain above the actual keyboard: $stopBounds versus $keyboard"
        }
        compose.onNodeWithTag("habit-timer-review-stop").assertIsDisplayed().performClick()
        check(logged == 720.0)
    }

    @Test fun completedTaskShowsActualCompletionInWhipTimeZone() {
        val instant = Instant.parse("2026-09-27T01:30:00Z")
        val zone = ZoneId.of("America/Toronto")
        val date = LocalDate.of(2026, 9, 20)
        val item = ScheduledTask(
            task = WhipTask(7, "Finished later", "", ScheduleKind.Once, date, null, null, false, false, instant.toEpochMilli(), 1, 1),
            originalDate = date, scheduledDate = date, completedAtMillis = instant.toEpochMilli(),
        )
        compose.setContent {
            CompositionLocalProvider(LocalWhipZone provides zone) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    CompletedTaskDialog(item, {}, {}, {}, {}, onReopenOccurrence = {}, onResetOccurrence = {})
                }
            }
        }
        compose.onNodeWithText(instant.atZone(zone).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)))
            .performScrollTo().assertIsDisplayed()
        captureVisualCatalogSurface("ux-upgrades.tasks-completed-date")
    }
}
