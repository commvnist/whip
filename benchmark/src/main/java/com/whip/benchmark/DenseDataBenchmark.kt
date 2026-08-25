package com.whip.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DenseDataBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    private val fixtureDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun homeWithTenThousandTasksAndHabitLogs() {
        seed("home")
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
            startupMode = StartupMode.WARM,
            iterations = 3,
            setupBlock = { pressHome(); startWhipActivityAndWait() },
        ) {
            repeat(4) {
                device.swipe(device.displayWidth / 2, device.displayHeight * 4 / 5, device.displayWidth / 2, device.displayHeight / 5, 20)
                device.waitForIdle()
            }
        }
    }

    @Test
    fun goalAndGymGraphsWithOneHundredThousandPoints() {
        seed("graphs")
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
            iterations = 3,
            setupBlock = { pressHome(); startWhipActivityAndWait() },
        ) {
            device.requireObject(By.desc("Goals tab"), "Goals navigation item").click()
            device.waitForIdle()
            device.requireObject(By.text("100k point goal"), "100k point goal", 60_000).click()
            device.waitForIdle()
            device.pressBack()
            device.requireObject(By.desc("Gym tab"), "Gym navigation item").click()
            device.waitForIdle()
            device.requireObject(By.text("Progress"), "Gym Progress destination").click()
            device.waitForIdle()
        }
    }

    @Test
    fun activeWorkoutInlineInputLatency() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
            iterations = 3,
            setupBlock = {
                // Saving a set intentionally mutates the active workout. Restore
                // the same dense fixture before every measurement so later
                // iterations never depend on the prior iteration's next-set or
                // rest-timer state.
                seed("graphs")
                pressHome()
                startWhipActivityAndWait()
                device.requireObject(By.desc("Gym tab"), "Gym navigation item").click()
                device.waitForIdle()
                device.requireObject(
                    By.res(Pattern.compile(".*next-set-focus")),
                    "next set shortcut",
                    60_000,
                ).click()
                device.waitForIdle()
            },
        ) {
            device.requireObject(By.res(Pattern.compile(".*quick-set-load-.*")), "inline weight field").text = "62.5"
            device.requireObject(By.res(Pattern.compile(".*quick-set-reps-.*")), "inline repetitions field").text = "6"
            if (device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) {
                device.pressBack()
                device.waitForIdle(1_000)
            }
            // The focused inline fields can leave the action row just below the
            // composed viewport on large/fold layouts. Exercise the same short
            // upward reveal a user performs instead of querying an off-screen node.
            repeat(2) {
                val x = device.displayWidth * 3 / 4
                device.executeShellCommand(
                    "input swipe $x ${device.displayHeight / 2} $x ${device.displayHeight / 4} 180",
                )
                device.waitForIdle(1_000)
            }
            // Compose merges this action row without exporting either the test tag
            // or label to UIAutomator on the Fold. The preceding reveal leaves the
            // primary action at a stable proportional position in the right pane.
            device.executeShellCommand(
                "input tap ${device.displayWidth * 69 / 100} ${device.displayHeight * 55 / 100}",
            )
            device.waitForIdle(1_000)
        }
    }

    private fun seed(mode: String) {
        val device = fixtureDevice
        device.executeShellCommand("am force-stop $PACKAGE_NAME")
        // Permission prompts obscure the target UI and turn a performance run
        // into a race with human input. Runtime-permission behavior is covered
        // by instrumentation tests; benchmarks run in a deterministic allowed
        // state instead.
        device.executeShellCommand(
            "pm grant $PACKAGE_NAME android.permission.POST_NOTIFICATIONS",
        )
        val launch = device.executeShellCommand(
            "am start -W -n $PACKAGE_NAME/com.whip.app.BenchmarkDataActivity --es mode $mode",
        )
        check("Status: ok" in launch) { "Benchmark fixture activity did not launch: $launch" }
        val ready = device.wait(Until.findObject(By.text("Seed ready: $mode")), 180_000)
        check(ready != null) {
            val failure = device.findObject(By.textStartsWith("Seed failed:"))?.text
            "Benchmark fixture failed to seed: $mode${failure?.let { " ($it)" }.orEmpty()}"
        }
        // Remove the fixture Activity from the app task before Macrobenchmark
        // launches the normal MAIN/LAUNCHER entry point. Merely pressing Home can
        // cause Android to restore BenchmarkDataActivity as the task's top
        // Activity, so the benchmark waits for UI that was never launched.
        device.executeShellCommand("am force-stop $PACKAGE_NAME")
    }
}
