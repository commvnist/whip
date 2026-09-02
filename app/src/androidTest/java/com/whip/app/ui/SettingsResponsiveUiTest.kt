package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import com.whip.app.WhipApplication
import com.whip.app.core.AppSettings
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.ui.theme.WhipTheme
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsResponsiveUiTest {
    @get:Rule val compose = createComposeRule()

    @androidx.compose.runtime.Composable
    private fun <T> immediateMutation(onPersist: (T) -> Unit): TypedSettingMutation<T> {
        var state by remember {
            mutableStateOf<PersistenceRequestState<SettingsMutationReceipt>>(PersistenceRequestState.Idle)
        }
        return TypedSettingMutation(
            state = state,
            consume = { requestId ->
                if ((state as? PersistenceRequestState.Finished)?.requestId == requestId) {
                    state = PersistenceRequestState.Idle
                }
            },
            submit = { requestId, value ->
                if (state !is PersistenceRequestState.Idle) {
                    false
                } else {
                    state = PersistenceRequestState.Running(requestId)
                    onPersist(value)
                    state = PersistenceRequestState.Finished(
                        requestId,
                        WhipResult.Success(SettingsMutationReceipt()),
                    )
                    true
                }
            },
        )
    }

    @Test
    fun numberSettingKeepsARecreatedDraftOutOfPersistenceUntilDone() {
        val restoration = StateRestorationTester(compose)
        var committed by mutableIntStateOf(120)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                NumberSetting(
                    label = "Default rest time (seconds)",
                    current = committed,
                    mutation = immediateMutation { committed = it },
                    validRange = 15..3_600,
                )
            }
        }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("300")
        compose.runOnIdle { assertEquals(120, committed) }

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("settings-field-default-rest-time-seconds-editor").assertIsDisplayed()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .assertTextContains("300")
        compose.runOnIdle { assertEquals(120, committed) }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performImeAction()
        compose.waitUntil {
            compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-editor")
                .fetchSemanticsNodes().isEmpty()
        }
        compose.runOnIdle { assertEquals(300, committed) }
        compose.onNodeWithTag("settings-field-default-rest-time-seconds")
            .assertTextContains("Current: 300")
    }

    @Test
    fun unchangedAndNormalizationEquivalentDoneCloseWithoutDispatchingAWrite() {
        var committed by mutableIntStateOf(120)
        var submissions by mutableIntStateOf(0)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                NumberSetting(
                    label = "Default rest time (seconds)",
                    current = committed,
                    mutation = immediateMutation {
                        submissions++
                        committed = it
                    },
                    validRange = 15..3_600,
                )
            }
        }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").assertIsNotEnabled()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performImeAction()
        compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-editor").assertCountEquals(0)
        compose.runOnIdle { assertEquals(0, submissions) }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("0120")
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performImeAction()
        compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-editor").assertCountEquals(0)
        compose.runOnIdle {
            assertEquals(0, submissions)
            assertEquals(120, committed)
        }
    }

    @Test
    fun rapidRepeatedSaveOwnsExactlyOnePersistenceRequest() {
        var committed by mutableIntStateOf(120)
        var submissions by mutableIntStateOf(0)
        var requestId: String? = null
        var mutationState by mutableStateOf<PersistenceRequestState<SettingsMutationReceipt>>(
            PersistenceRequestState.Idle,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                NumberSetting(
                    label = "Default rest time (seconds)",
                    current = committed,
                    mutation = TypedSettingMutation(
                        state = mutationState,
                        consume = { mutationState = PersistenceRequestState.Idle },
                        submit = { id, value ->
                            submissions++
                            requestId = id
                            committed = value
                            mutationState = PersistenceRequestState.Running(id)
                            true
                        },
                    ),
                    validRange = 15..3_600,
                )
            }
        }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("300")
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").performTouchInput {
            down(center)
            up()
            advanceEventTime(20)
            down(center)
            up()
        }
        compose.runOnIdle { assertEquals(1, submissions) }

        compose.runOnIdle {
            mutationState = PersistenceRequestState.Finished(
                requireNotNull(requestId),
                WhipResult.Success(SettingsMutationReceipt()),
            )
        }
        compose.waitUntil {
            compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-editor")
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun editorWaitsForItsRequestReceiptAndRetainsAFailedDraftForRetry() {
        var committed by mutableIntStateOf(120)
        var submitted: Int? = null
        var requestId: String? = null
        var mutationState by mutableStateOf<PersistenceRequestState<SettingsMutationReceipt>>(
            PersistenceRequestState.Idle,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                NumberSetting(
                    label = "Default rest time (seconds)",
                    current = committed,
                    mutation = TypedSettingMutation(
                        state = mutationState,
                        consume = { consumed ->
                            if ((mutationState as? PersistenceRequestState.Finished)?.requestId == consumed) {
                                mutationState = PersistenceRequestState.Idle
                            }
                        },
                        submit = { id, value ->
                            if (mutationState !is PersistenceRequestState.Idle) {
                                false
                            } else {
                                requestId = id
                                submitted = value
                                mutationState = PersistenceRequestState.Running(id)
                                true
                            }
                        },
                    ),
                    validRange = 15..3_600,
                )
            }
        }

        assertEquals(
            "Saved value 120. Activate to edit.",
            compose.onNodeWithTag("settings-field-default-rest-time-seconds")
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription],
        )
        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("300")
        assertEquals(
            "Edited, not saved",
            compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
                .fetchSemanticsNode().config[SemanticsProperties.StateDescription],
        )
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performImeAction()

        compose.runOnIdle {
            assertEquals(300, submitted)
            assertEquals(120, committed)
            // SharedPreferences can publish process memory before commit()
            // reports that disk rejected the write.
            committed = 300
        }
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-editor").assertIsDisplayed()
        compose.runOnIdle {
            mutationState = PersistenceRequestState.Finished(
                requireNotNull(requestId),
                WhipResult.Failure("Local storage rejected this save."),
            )
        }
        compose.waitUntil {
            compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-save-error")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save-error")
            .assertTextContains("Local storage rejected", substring = true)
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .assertTextContains("300")
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").assertIsEnabled()

        compose.waitUntil { mutationState is PersistenceRequestState.Idle }
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").performClick()
        compose.runOnIdle {
            committed = 300
            mutationState = PersistenceRequestState.Finished(
                requireNotNull(requestId),
                WhipResult.Success(SettingsMutationReceipt()),
            )
        }
        compose.waitUntil {
            compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-editor")
                .fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("settings-field-default-rest-time-seconds")
            .assertTextContains("Current: 300")
    }

    @Test
    fun lateProcessMemoryPublicationAfterFailureStillRequiresAndAllowsDurableRetry() {
        var committed by mutableIntStateOf(120)
        var requestId: String? = null
        var submissions by mutableIntStateOf(0)
        var mutationState by mutableStateOf<PersistenceRequestState<SettingsMutationReceipt>>(
            PersistenceRequestState.Idle,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                NumberSetting(
                    label = "Default rest time (seconds)",
                    current = committed,
                    mutation = TypedSettingMutation(
                        state = mutationState,
                        consume = { consumed ->
                            if ((mutationState as? PersistenceRequestState.Finished)?.requestId == consumed) {
                                mutationState = PersistenceRequestState.Idle
                            }
                        },
                        submit = { id, _ ->
                            submissions++
                            requestId = id
                            mutationState = PersistenceRequestState.Running(id)
                            true
                        },
                    ),
                    validRange = 15..3_600,
                )
            }
        }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("300")
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").performClick()
        compose.runOnIdle {
            mutationState = PersistenceRequestState.Finished(
                requireNotNull(requestId),
                WhipResult.Failure("Disk rejected this save."),
            )
        }
        compose.waitUntil { mutationState is PersistenceRequestState.Idle }

        // Exercise the inverse ordering: the failure receipt is observed first,
        // then the failed transaction's process-local value is published.
        compose.runOnIdle { committed = 300 }
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").assertIsEnabled()
        repeat(2) {
            if (compose.onAllNodesWithText("Discard Unsaved Changes?").fetchSemanticsNodes().isEmpty()) {
                pressBack()
                compose.waitForIdle()
            }
        }
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep Editing").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").performClick()
        compose.runOnIdle { assertEquals(2, submissions) }

        compose.runOnIdle {
            mutationState = PersistenceRequestState.Finished(
                requireNotNull(requestId),
                WhipResult.Success(SettingsMutationReceipt()),
            )
        }
        compose.waitUntil {
            compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-editor")
                .fetchSemanticsNodes().isEmpty()
        }
        compose.runOnIdle { assertEquals(300, committed) }
    }

    @Test
    fun externalSameFieldChangeKeepsTheDraftButRequiresAnInformedResave() {
        var committed by mutableIntStateOf(120)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                NumberSetting(
                    label = "Default rest time (seconds)",
                    current = committed,
                    mutation = immediateMutation { committed = it },
                    validRange = 15..3_600,
                )
            }
        }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("300")
        compose.runOnIdle { committed = 180 }

        compose.onNodeWithText(
            "This setting or its mode changed elsewhere. Your draft is still here; review it before saving.",
        ).assertIsDisplayed()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .assertTextContains("300")
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").performClick()
        compose.waitUntil {
            compose.onAllNodesWithTag("settings-field-default-rest-time-seconds-editor")
                .fetchSemanticsNodes().isEmpty()
        }
        compose.runOnIdle { assertEquals(300, committed) }
    }

    @Test
    fun boundedLargeTextEditorAnnouncesErrorsAndGuardsHardwareDismissal() {
        var committed by mutableIntStateOf(120)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.size(width = 320.dp, height = 320.dp)) {
                        NumberSetting(
                            label = "Default rest time (seconds)",
                            current = committed,
                            mutation = immediateMutation { committed = it },
                            validRange = 15..3_600,
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("9".repeat(20_000))
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performImeAction()
        compose.onNodeWithText("Use at most 10 characters.").assertIsDisplayed()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").assertIsNotEnabled()
        assertEquals(
            LiveRegionMode.Polite,
            compose.onNodeWithText("Use at most 10 characters.")
                .fetchSemanticsNode().config[SemanticsProperties.LiveRegion],
        )
        compose.runOnIdle { assertEquals(120, committed) }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("3")
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performImeAction()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .assertTextContains("3")
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-save").assertIsNotEnabled()
        assertEquals(
            LiveRegionMode.Polite,
            compose.onNodeWithText("Enter 15–3600.")
                .fetchSemanticsNode().config[SemanticsProperties.LiveRegion],
        )
        compose.runOnIdle { assertEquals(120, committed) }

        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performKeyInput {
            keyDown(Key.Escape)
            keyUp(Key.Escape)
        }
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep Editing").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").assertIsFocused()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input").performKeyInput {
            keyDown(Key.Escape)
            keyUp(Key.Escape)
        }
        compose.onNodeWithText("Discard Changes").assertIsDisplayed().performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds")
            .assertTextContains("Current: 120")
        compose.runOnIdle { assertEquals(120, committed) }
    }

    @Test
    fun timeZoneSettingDoesNotPersistAValidPrefixOrDraftBeforeDone() {
        var committed by mutableStateOf("America/Toronto")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TimeZoneSetting(
                    current = committed,
                    mutation = immediateMutation { committed = it },
                )
            }
        }

        compose.onNodeWithTag("settings-field-time-zone").performClick()
        compose.onNodeWithTag("settings-field-time-zone-input")
            .performTextReplacement("America")
        compose.onNodeWithTag("settings-field-time-zone-input").performImeAction()
        compose.onNodeWithText("Enter a valid region or UTC-offset time zone.").assertIsDisplayed()
        compose.runOnIdle { assertEquals("America/Toronto", committed) }

        compose.onNodeWithTag("settings-field-time-zone-input")
            .performTextReplacement("+02:00")
        compose.runOnIdle { assertEquals("America/Toronto", committed) }
        compose.onNodeWithTag("settings-field-time-zone-input").performImeAction()
        compose.waitUntil { committed == "+02:00" }
        compose.onNodeWithTag("settings-field-time-zone")
            .assertTextContains("Current: +02:00", substring = true)
    }

    @Test
    fun timeZoneModeChangeIsNotHiddenWhenTheEffectiveValueStaysTheSame() {
        var current: String? by mutableStateOf(ZoneId.systemDefault().id)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TimeZoneSetting(
                    current = current,
                    mutation = immediateMutation { current = it },
                )
            }
        }

        compose.onNodeWithTag("settings-field-time-zone").performClick()
        compose.runOnIdle { current = null }

        compose.onNodeWithText(
            "This setting or its mode changed elsewhere. Your draft is still here; review it before saving.",
        ).assertIsDisplayed()
        compose.onNodeWithTag("settings-field-time-zone-cancel").performClick()
        compose.runOnIdle { assertEquals(null, current) }
    }

    @Test
    fun coupledQuietHoursDisableIsNotHiddenByTheDefaultDisplayValue() {
        var current by mutableIntStateOf(22 * 60)
        var contextIdentity by mutableStateOf("1320:420")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                ClockSetting(
                    label = "Quiet hours start",
                    currentMinutes = current,
                    sourceIdentity = contextIdentity,
                    mutation = immediateMutation { current = it },
                )
            }
        }

        compose.onNodeWithTag("settings-field-quiet-hours-start").performClick()
        compose.runOnIdle { contextIdentity = "null:null" }

        compose.onNodeWithText(
            "This setting or its mode changed elsewhere. Your draft is still here; review it before saving.",
        ).assertIsDisplayed()
        compose.onNodeWithTag("settings-field-quiet-hours-start-cancel").performClick()
        compose.runOnIdle { assertEquals(22 * 60, current) }
    }

    @Test
    fun wideSettingsCannotSwitchCategoriesBehindATypedEditor() {
        val app: WhipApplication = ApplicationProvider.getApplicationContext()
        val viewModel = SettingsViewModel(app)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 1f)) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.size(width = 900.dp, height = 800.dp)) {
                        SettingsContent(
                            state = SettingsUiState(
                                settings = AppSettings(
                                    setupCompleted = true,
                                    timeZoneId = "America/Toronto",
                                ),
                            ),
                            innerPadding = PaddingValues(),
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("settings-section-Planning & Units").performClick()
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-field-default-rest-time-seconds"))
        compose.onNodeWithTag("settings-field-default-rest-time-seconds").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-input")
            .performTextReplacement("300")

        // Even a direct semantics invocation cannot bypass the modal editor and
        // change the section behind it.
        compose.onNodeWithTag("settings-section-Appearance & Home").performClick()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-editor").assertIsDisplayed()
        compose.onNodeWithTag("settings-field-default-rest-time-seconds-cancel").performClick()

        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-field-default-rest-time-seconds"))
        compose.onNodeWithTag("settings-field-default-rest-time-seconds").assertIsDisplayed()

        compose.onNodeWithTag("settings-wide-section-list")
            .performScrollToNode(hasTestTag("settings-section-Appearance & Home"))
        compose.onNodeWithTag("settings-section-Appearance & Home").performClick()
        compose.onNodeWithTag("settings-list")
            .performScrollToNode(hasTestTag("settings-compact-item-layout"))
        compose.onNodeWithTag("settings-compact-item-layout").assertIsDisplayed()
    }

    @Test
    fun wideSectionSidebarScrollsAtLargeTextAndKeepsSelectionAndFocusSemantics() {
        var selected by mutableStateOf(SettingsSection.Appearance)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.size(width = 240.dp, height = 220.dp)) {
                        WideSettingsSectionSidebar(
                            selectedSection = selected,
                            onSectionSelected = { selected = it },
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("settings-section-Appearance & Home").assertIsSelected()
        compose.onNodeWithTag("settings-wide-section-list")
            .performScrollToNode(hasTestTag("settings-section-About Whip"))

        val about = compose.onNodeWithTag("settings-section-About Whip").assertIsDisplayed()
        about.performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        about.performClick().assertIsSelected().assertIsFocused()
        assertTrue(about.fetchSemanticsNode().boundsInRoot.height >= with(compose.density) { 48.dp.toPx() })
    }

    @Test
    fun sharedNavigationRowsRemainReachableAndClickableInRtl() {
        var clicks = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhipTheme(dynamicColor = false) {
                    Column(Modifier.width(320.dp)) {
                        NavigationRow(
                            title = "Machine configurations",
                            onClick = { clicks++ },
                            modifier = Modifier.testTag("rtl-navigation-row"),
                        )
                        WhipActionRow(
                            title = "Planning and units",
                            onClick = { clicks++ },
                            modifier = Modifier.testTag("rtl-action-row"),
                        )
                    }
                }
            }
        }

        val minimumTargetPx = with(compose.density) { 48.dp.toPx() }
        listOf("rtl-navigation-row", "rtl-action-row").forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
            assertTrue(compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.height >= minimumTargetPx)
        }
        assertEquals(2, clicks)
    }

    @Test
    fun settingsActionPairsStackAtCompactLargeTextWithoutShrinkingTargets() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp)) {
                        ResponsiveSettingsActions(
                            first = { modifier ->
                                WhipOutlinedButton(
                                    onClick = {},
                                    modifier = modifier.testTag("settings-action-first"),
                                ) { androidx.compose.material3.Text("Review Access") }
                            },
                            second = { modifier ->
                                WhipButton(
                                    onClick = {},
                                    modifier = modifier.testTag("settings-action-second"),
                                ) { androidx.compose.material3.Text("Sync Now") }
                            },
                        )
                    }
                }
            }
        }

        val first = compose.onNodeWithTag("settings-action-first").assertIsDisplayed().getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("settings-action-second").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(first.bottom <= second.top)
        assertTrue(first.let { it.bottom - it.top } >= 48.dp)
        assertTrue(second.let { it.bottom - it.top } >= 48.dp)
    }
}
