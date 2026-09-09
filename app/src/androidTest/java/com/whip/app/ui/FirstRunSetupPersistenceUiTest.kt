package com.whip.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.assertDialogFontScale
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.core.HomeSection
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class FirstRunSetupPersistenceUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val restoration by lazy { StateRestorationTester(compose) }
    private var state by mutableStateOf<PersistenceRequestState<SettingsMutationReceipt>>(PersistenceRequestState.Idle)
    private var completed by mutableStateOf(false)
    private var rejectAdmission = false
    private var permissionRequests = 0
    private val requests = mutableListOf<Pair<String, FirstRunSetupDraft>>()
    private val consumed = mutableListOf<String>()

    private fun show(rtl: Boolean = false) {
        restoration.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = if (rtl) 320.dp else 380.dp),
            ) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    Surface(Modifier.fillMaxSize()) {
                        FirstRunSetupHost(
                            setupCompleted = completed,
                            mutation = TypedSettingMutation(
                                state = state,
                                consume = { id ->
                                    consumed += id
                                    if ((state as? PersistenceRequestState.Finished)?.requestId == id) {
                                        state = PersistenceRequestState.Idle
                                    }
                                },
                                submit = { id, draft ->
                                    if (rejectAdmission) false else {
                                        requests += id to draft
                                        state = PersistenceRequestState.Running(id)
                                        true
                                    }
                                },
                            ),
                            onRequestNotificationPermission = { permissionRequests++ },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun failedCommitRetainsCustomizedDraftThroughRecreationAndOnlyConfirmedRetryRequestsPermission() {
        show()
        compose.onNodeWithText("Customize").performClick()
        compose.onNodeWithText("Tasks").performClick()
        compose.onNodeWithText("Habits").performClick()
        compose.onNodeWithText("Tracks").performClick()
        compose.onNodeWithText("Gym").performClick()
        compose.onNodeWithContentDescription("Show advanced controls by default").performClick()
        compose.onNodeWithText("lb").performScrollTo().performClick()
        compose.onNodeWithText("Optional Preferences").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Use low-pressure presentation").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ask for reminder notifications").performScrollTo().performClick()
        compose.onNodeWithText("Save and Start").performTouchInput {
            down(center)
            up()
            advanceEventTime(20)
            down(center)
            up()
        }
        compose.runOnIdle {
            assertEquals(1, requests.size)
            assertEquals(0, permissionRequests)
            completed = true // SharedPreferences may publish before commit returns.
        }
        compose.onNodeWithContentDescription("Saving setup. Editing is temporarily unavailable.").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("shared.first-run.saving")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Saving setup. Editing is temporarily unavailable.").assertIsDisplayed()
        fail("Setup could not be saved. Your choices are still here; try again.")
        compose.onNodeWithText("Setup could not be saved. Your choices are still here; try again.").assertIsDisplayed()
        compose.onNodeWithText("Tracks").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Gym").assertIsSelected()
        compose.onNodeWithText("lb").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Setup could not be saved. Your choices are still here; try again.").performScrollTo()
        compose.waitForIdle()
        captureVisualCatalogSurface("shared.first-run.save-error", visuallyDistinctFrom = "shared.first-run.saving")
        compose.runOnIdle { assertEquals(0, permissionRequests) }
        compose.onNodeWithText("Save and Start").performClick()
        compose.runOnIdle {
            assertEquals(2, requests.size)
            assertNotEquals(requests[0].first, requests[1].first)
            assertEquals(requests[0].second, requests[1].second)
            assertEquals(FirstRunSetupDraft(setOf(HomeSection.Tracks, HomeSection.Gym), true, true, true), requests[1].second)
        }
        succeed()
        waitForClosed("Customize Whip")
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(1, permissionRequests)
            assertEquals(requests.map { it.first }, consumed)
        }
    }

    @Test
    fun recommendedSetupCanRetryAdmissionAndStorageFailuresWithoutRequestingPermission() {
        rejectAdmission = true
        show()
        compose.onNodeWithText("Use Recommended").performClick()
        compose.onNodeWithText("Whip is busy saving another setting. Your choices are still here; try again.").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, requests.size)
            rejectAdmission = false
        }
        compose.onNodeWithText("Use Recommended").performClick()
        fail("Local storage did not confirm the settings change.")
        compose.onNodeWithText("Local storage did not confirm the settings change.").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Use Recommended").performClick()
        succeed()
        waitForClosed("Welcome to Whip")
        compose.runOnIdle {
            assertEquals(listOf(FirstRunSetupDraft(), FirstRunSetupDraft()), requests.map { it.second })
            assertEquals(0, permissionRequests)
        }
    }

    @Test
    fun restoredDraftWithLostRequestExplainsInterruptionAndCanFinish() {
        show()
        compose.onNodeWithText("Customize").performClick()
        compose.onNodeWithText("lb").performScrollTo().performClick()
        compose.onNodeWithText("Save and Start").performClick()
        compose.runOnIdle {
            completed = true
            state = PersistenceRequestState.Idle // New process has no ViewModel request.
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Setup was interrupted. Your choices are still here. Save again to finish.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("lb").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Save and Start").performClick()
        succeed()
        waitForClosed("Customize Whip")
        compose.runOnIdle {
            assertEquals(requests[0].second, requests[1].second)
            assertEquals(0, permissionRequests)
        }
    }

    @Test
    fun completedSetupLeavesAnotherSettingsEditorsResultOwnedByThatEditor() {
        completed = true
        state = PersistenceRequestState.Finished("another-editor:request", WhipResult.Success(SettingsMutationReceipt()))
        show()
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle {
            assertEquals(emptyList<String>(), consumed)
            assertEquals(0, permissionRequests)
        }
    }

    @Test
    @AndroidFontScale
    fun customizationAndSaveFailureStayReachableAtLargeTextInRtl() {
        show(rtl = true)
        compose.onNodeWithText("Customize").performClick()
        compose.assertDialogFontScale()
        compose.onNodeWithText("Tasks").performScrollTo().performClick()
        compose.onNodeWithText("Habits").performScrollTo().performClick()
        compose.onNodeWithText("Choose at least one Home section.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save and Start").assertIsNotEnabled()
        compose.onNodeWithText("Tracks").performScrollTo().performClick()
        compose.onNodeWithText("Optional Preferences").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ask for reminder notifications").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save and Start").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("shared.first-run.optional-large-rtl")
        compose.onNodeWithText("Save and Start").performClick()
        fail("Setup could not be saved. Your choices are still here; try again.")
        compose.onNodeWithText("Setup could not be saved. Your choices are still here; try again.").assertIsDisplayed()
        compose.onNodeWithText("Save and Start").assertIsDisplayed()
        compose.onNodeWithText("Back").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("shared.first-run.error-large-rtl", visuallyDistinctFrom = "shared.first-run.optional-large-rtl")
    }

    private fun fail(message: String) = compose.runOnIdle {
        state = PersistenceRequestState.Finished(requests.last().first, WhipResult.Failure(message))
    }

    private fun succeed() = compose.runOnIdle {
        completed = true
        state = PersistenceRequestState.Finished(requests.last().first, WhipResult.Success(SettingsMutationReceipt()))
    }

    private fun waitForClosed(title: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(title).fetchSemanticsNodes().isEmpty()
    }
}
