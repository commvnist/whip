package com.whip.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun criticalUserJourneys() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
        // AndroidX dependencies already ship their own profiles. Recording only
        // Whip descriptors keeps the app profile focused and avoids packaging
        // a multi-megabyte snapshot of framework/library implementation detail.
        filterPredicate = { rule -> rule.contains("Lcom/whip/app/") },
    ) {
        // Profile generation must not inherit a dense-data benchmark fixture;
        // that makes navigation timing nondeterministic and can leave stale nodes.
        device.executeShellCommand("pm clear $PACKAGE_NAME")
        pressHome()
        startWhipActivityAndWait()
        device.completeOnboardingIfNeeded()
        listOf("Tasks", "Habits", "Gym", "Goals", "Home").forEach { label ->
            device.clickPrimaryDestination(label)
            device.waitForIdle(1_000)
        }
    }
}

internal const val PACKAGE_NAME = "commvne.com.whip.app.benchmark"
