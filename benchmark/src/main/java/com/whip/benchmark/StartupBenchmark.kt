package com.whip.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupWithProfile() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() },
    ) {
        startWhipActivityAndWait()
    }

    @Test
    fun coldStartupWithoutCompilation() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() },
    ) {
        startWhipActivityAndWait()
    }

    @Test
    fun warmStartupWithProfile() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { pressHome() },
    ) {
        startWhipActivityAndWait()
    }

    @Test
    fun primaryNavigationFrameTiming() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
        iterations = 5,
        setupBlock = {
            pressHome()
            startWhipActivityAndWait()
            device.completeOnboardingIfNeeded()
        },
    ) {
        listOf("Tasks", "Habits", "Gym", "Goals", "Home").forEach { label ->
            device.clickPrimaryDestination(label)
            device.waitForIdle()
        }
    }

    @Test
    fun expandedResizeFrameTiming() {
        try {
            benchmarkRule.measureRepeated(
                packageName = PACKAGE_NAME,
                metrics = listOf(FrameTimingMetric()),
                compilationMode = CompilationMode.Partial(BaselineProfileMode.UseIfAvailable),
                iterations = 5,
                setupBlock = {
                    device.executeShellCommand("wm size 1800x2200")
                    pressHome()
                    startWhipActivityAndWait()
                    device.completeOnboardingIfNeeded()
                },
            ) {
                device.executeShellCommand("wm size 2200x1800")
                device.waitForIdle()
                device.clickPrimaryDestination("Tasks")
                device.waitForIdle()
                device.executeShellCommand("wm size 1800x2200")
                device.waitForIdle()
                device.clickPrimaryDestination("Goals")
                device.waitForIdle()
            }
        } finally {
            androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("wm size reset")
        }
    }
}
