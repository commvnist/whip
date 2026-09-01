package com.whip.app

import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.startup.StartupRecoveryState
import com.whip.app.startup.StartupBlockReason
import com.whip.app.ui.StartupRecoveryScreen
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupRecoveryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun blockedRecoveryExplainsDataSafetyAndOffersRetry() {
        var retries = 0
        compose.setContent {
            WhipTheme {
                StartupRecoveryScreen(
                    state = StartupRecoveryState.Blocked(StartupBlockReason.Recovery),
                    onRetry = { retries++ },
                )
            }
        }

        compose.onNodeWithTag("startup-recovery-screen").assertIsDisplayed()
        compose.onNodeWithText("Whip Couldn't Safely Open Your Data").assertIsDisplayed()
        compose.onNodeWithText("Retry Recovery").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun checkingRecoveryDoesNotOfferActionsThatCouldMutateData() {
        compose.setContent {
            WhipTheme {
                StartupRecoveryScreen(
                    state = StartupRecoveryState.Checking,
                    onRetry = { error("Retry must not be available while checking") },
                )
            }
        }

        compose.onNodeWithText("Checking Your Data").assertIsDisplayed()
        compose.onAllNodesWithText("Retry Recovery").assertCountEquals(0)
    }

    @Test
    fun runtimeInitializationFailureDoesNotClaimARecoveryCopyStillExists() {
        compose.setContent {
            WhipTheme {
                StartupRecoveryScreen(
                    state = StartupRecoveryState.Blocked(StartupBlockReason.RuntimeInitialization),
                    onRetry = { },
                )
            }
        }

        compose.onNodeWithText("Whip Couldn't Finish Starting").assertIsDisplayed()
        compose.onNodeWithText("Try Again").assertIsDisplayed()
        compose.onAllNodesWithText("private recovery copy", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Retry Recovery").assertCountEquals(0)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun blockedRecoveryPassesComposeAccessibilityChecks() {
        assumeTrue("Compose accessibility checks require Android 14+", Build.VERSION.SDK_INT >= 34)
        compose.setContent {
            WhipTheme {
                StartupRecoveryScreen(
                    state = StartupRecoveryState.Blocked(StartupBlockReason.Recovery),
                    onRetry = { },
                )
            }
        }
        compose.enableAccessibilityChecks()

        compose.onRoot().tryPerformAccessibilityChecks()
    }
}
