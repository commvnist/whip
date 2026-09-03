package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.EntitySaveReceipt
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.TaskDraft
import com.whip.app.ui.theme.WhipTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntitySaveCoordinatorUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun secondBeginCannotReplaceTheOwnedRequest() {
        val requestState = mutableStateOf<String?>(null)
        val errorState = mutableStateOf<String?>(null)
        val coordinator = EntitySaveCoordinator(requestState, errorState)

        val accepted = coordinator.begin()
        assertNotNull(accepted)
        assertNull(coordinator.begin())
        assertEquals(accepted, coordinator.requestId)
    }

    @Test
    fun matchingSuccessSettlesTheRestoredEditorExactlyOnce() {
        var state by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        var persistedCount = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                val coordinator = rememberEntitySaveCoordinator(
                    state = state,
                    consume = { state = PersistenceRequestState.Idle },
                    onPersisted = { persistedCount++ },
                )
                Column {
                    WhipButton(
                        enabled = !coordinator.saving,
                        onClick = {
                            coordinator.begin()?.let { state = PersistenceRequestState.Running(it) }
                        },
                    ) { Text(if (coordinator.saving) "Saving…" else "Save") }
                    Text(coordinator.requestId.orEmpty(), modifier = androidx.compose.ui.Modifier.testTag("owned-request"))
                }
            }
        }

        compose.onNodeWithText("Save").performClick()
        val requestId = compose.onNodeWithTag("owned-request").fetchSemanticsNode().config[SemanticsProperties.Text]
            .joinToString("") { it.text }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Saving…").assertIsDisplayed()

        state = PersistenceRequestState.Finished(
            requestId,
            WhipResult.Success(EntitySaveReceipt(41, "work")),
        )

        compose.waitUntil(2_000) { persistedCount == 1 }
        compose.onNodeWithText("Save").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle { assertEquals(1, persistedCount) }
    }

    @Test
    fun failureStaysInsideTheEditorAsALiveRetryableMessage() {
        var state by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        var requestId = ""
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val coordinator = rememberEntitySaveCoordinator(
                    state = state,
                    consume = { state = PersistenceRequestState.Idle },
                    onPersisted = {},
                )
                Column {
                    PersistenceFailureNotice(coordinator.errorMessage, testTag = "save-failure")
                    WhipButton(
                        enabled = !coordinator.saving,
                        onClick = {
                            coordinator.begin()?.let {
                                requestId = it
                                state = PersistenceRequestState.Running(it)
                            }
                        },
                    ) { Text(if (coordinator.saving) "Saving…" else "Save") }
                }
            }
        }

        compose.onNodeWithText("Save").performClick()
        state = PersistenceRequestState.Finished(requestId, WhipResult.Failure("Repository unavailable"))

        compose.onNodeWithTag("save-failure").assertIsDisplayed()
        compose.onNodeWithTag("save-failure")
            .assertContentDescriptionContains("Save did not finish", substring = true)
        val liveRegion = compose.onNodeWithTag("save-failure").fetchSemanticsNode().config[SemanticsProperties.LiveRegion]
        assertEquals(LiveRegionMode.Polite, liveRegion)
        compose.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun restoredRequestWithoutALiveViewModelCannotRemainStuckSaving() {
        var state by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                val coordinator = rememberEntitySaveCoordinator(
                    state = state,
                    consume = {},
                    onPersisted = {},
                )
                Column {
                    PersistenceFailureNotice(coordinator.errorMessage, testTag = "save-failure")
                    WhipButton(
                        enabled = !coordinator.saving,
                        onClick = {
                            coordinator.begin()?.let { state = PersistenceRequestState.Running(it) }
                        },
                    ) { Text(if (coordinator.saving) "Saving…" else "Save") }
                }
            }
        }

        compose.onNodeWithText("Save").performClick()
        restoration.emulateSavedInstanceStateRestore()
        state = PersistenceRequestState.Idle

        compose.waitUntil(2_000) {
            compose.onAllNodesWithTag("save-failure").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        compose.onNodeWithTag("save-failure").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun unknownTrackCreateOutcomeWarnsAgainstRetryUntilTheListIsVerified() {
        var state by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                val coordinator = rememberEntitySaveCoordinator(
                    state = state,
                    consume = {},
                    orphanedMessage =
                        "The previous Track save was interrupted and its outcome is unknown. " +
                            "Verify the Track list and do not retry until you know whether it was saved.",
                    onPersisted = {},
                )
                Column {
                    PersistenceFailureNotice(coordinator.errorMessage, testTag = "track-unknown-outcome")
                    WhipButton(onClick = {
                        coordinator.begin()?.let { state = PersistenceRequestState.Running(it) }
                    }) { Text(if (coordinator.saving) "Saving…" else "Save") }
                }
            }
        }

        compose.onNodeWithText("Save").performClick()
        restoration.emulateSavedInstanceStateRestore()
        state = PersistenceRequestState.Idle

        compose.waitUntil(2_000) {
            compose.onAllNodesWithTag("track-unknown-outcome")
                .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        compose.onNodeWithTag("track-unknown-outcome")
            .assertContentDescriptionContains("Verify the Track list", substring = true)
            .assertContentDescriptionContains("do not retry", substring = true)
    }

    @Test
    fun userDataGenerationChangeDropsStaleIdentityStateAndRestoresOnlyTheNewGeneration() {
        var generation by mutableStateOf(0L)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                UserDataGenerationBoundary(generation) {
                    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
                    Column {
                        Text(selectedId?.toString() ?: "No selection", modifier = androidx.compose.ui.Modifier.testTag("selected-id"))
                        WhipButton(onClick = { selectedId = if (generation == 0L) 41L else 72L }) {
                            Text("Select identity")
                        }
                    }
                }
            }
        }

        compose.onNodeWithText("Select identity").performClick()
        compose.onNodeWithText("41").assertIsDisplayed()
        compose.runOnIdle { generation = 1L }
        compose.onNodeWithText("No selection").assertIsDisplayed()
        compose.onNodeWithText("Select identity").performClick()
        compose.onNodeWithText("72").assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("72").assertIsDisplayed()
        compose.onAllNodesWithText("41").assertCountEquals(0)
    }

    @Test
    fun terminalResultFromAnEditorThatLeftCompositionIsReclaimed() {
        var state by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(
            PersistenceRequestState.Finished(
                "previous-editor",
                WhipResult.Success(EntitySaveReceipt(39, "work")),
            ),
        )
        var consumedRequest: String? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val coordinator = rememberEntitySaveCoordinator(
                    state = state,
                    consume = { requestId ->
                        consumedRequest = requestId
                        state = PersistenceRequestState.Idle
                    },
                    onPersisted = {},
                )
                WhipButton(
                    enabled = !coordinator.saving,
                    onClick = {
                        coordinator.begin()?.let { state = PersistenceRequestState.Running(it) }
                    },
                ) { Text(if (coordinator.saving) "Saving…" else "Save") }
            }
        }

        compose.waitUntil(2_000) { consumedRequest == "previous-editor" }
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Saving…").assertIsDisplayed()
        compose.runOnIdle { assertTrue(state is PersistenceRequestState.Running) }
    }

    @Test
    fun abandonedTerminalResultsAreReclaimedAcrossNamespacedSurfacesInBothDirections() {
        var state by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        var showHome by mutableStateOf(true)
        var showWorkspace by mutableStateOf(false)
        val consumed = mutableListOf<String>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    if (showHome) {
                        val home = rememberEntitySaveCoordinator(
                            state = state,
                            consume = { requestId ->
                                consumed += requestId
                                state = PersistenceRequestState.Idle
                            },
                            requestNamespace = "home-habit-quick",
                            onPersisted = {},
                        )
                        WhipButton(
                            onClick = {
                                home.begin()?.let { state = PersistenceRequestState.Running(it) }
                            },
                        ) { Text("Home save") }
                    }
                    if (showWorkspace) {
                        val workspace = rememberEntitySaveCoordinator(
                            state = state,
                            consume = { requestId ->
                                consumed += requestId
                                state = PersistenceRequestState.Idle
                            },
                            requestNamespace = "habit-workspace",
                            onPersisted = {},
                        )
                        WhipButton(
                            onClick = {
                                workspace.begin()?.let { state = PersistenceRequestState.Running(it) }
                            },
                        ) { Text("Workspace save") }
                    }
                }
            }
        }

        compose.onNodeWithText("Home save").performClick()
        val homeRequest = (state as PersistenceRequestState.Running).requestId
        compose.runOnIdle {
            showHome = false
            showWorkspace = true
            state = PersistenceRequestState.Finished(
                homeRequest,
                WhipResult.Success(EntitySaveReceipt(1, null)),
            )
        }
        compose.waitUntil(2_000) { state is PersistenceRequestState.Idle }
        compose.runOnIdle { assertEquals(listOf(homeRequest), consumed) }

        compose.onNodeWithText("Workspace save").performClick()
        val workspaceRequest = (state as PersistenceRequestState.Running).requestId
        compose.runOnIdle {
            showWorkspace = false
            showHome = true
            state = PersistenceRequestState.Finished(
                workspaceRequest,
                WhipResult.Success(EntitySaveReceipt(2, null)),
            )
        }
        compose.waitUntil(2_000) { state is PersistenceRequestState.Idle }
        compose.runOnIdle { assertEquals(listOf(homeRequest, workspaceRequest), consumed) }
    }

    @Test
    fun nonOwningNamespaceCannotStealTerminalDeliveryFromComposedOwner() {
        var state by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        var homePersisted = 0
        var workspacePersisted = 0
        val consumed = mutableListOf<String>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                    val home = rememberEntitySaveCoordinator(
                        state = state,
                        consume = { requestId ->
                            consumed += requestId
                            state = PersistenceRequestState.Idle
                        },
                        requestNamespace = "home-habit-quick",
                        onPersisted = { homePersisted++ },
                    )
                    rememberEntitySaveCoordinator(
                        state = state,
                        consume = { requestId ->
                            consumed += requestId
                            state = PersistenceRequestState.Idle
                        },
                        requestNamespace = "habit-workspace",
                        onPersisted = { workspacePersisted++ },
                    )
                    WhipButton(
                        onClick = { home.begin()?.let { state = PersistenceRequestState.Running(it) } },
                    ) { Text("Owned home save") }
                }
            }
        }

        compose.onNodeWithText("Owned home save").performClick()
        val requestId = (state as PersistenceRequestState.Running).requestId
        compose.runOnIdle {
            state = PersistenceRequestState.Finished(
                requestId,
                WhipResult.Success(EntitySaveReceipt(3, null)),
            )
        }

        compose.waitUntil(2_000) { homePersisted == 1 }
        compose.runOnIdle {
            assertEquals(0, workspacePersisted)
            assertEquals(listOf(requestId), consumed)
        }
    }

    @Test
    fun taskFailureKeepsDraftAndScopeWhileSuccessReconcilesExactlyOnce() {
        var saveState by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        var selectedScope by mutableStateOf<AreaScope>(AreaScope.All)
        var submittedDraft: TaskDraft? = null
        var saveRequests = 0
        val areas = listOf(area("work", "Work"), area("health", "Health"))
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    settingsState = SettingsUiState(
                        settings = AppSettings(setupCompleted = true),
                        areas = areas,
                        taxonomyLoaded = true,
                    ),
                    areaScope = selectedScope,
                    onSelectAreaScope = { selectedScope = it },
                    taskEditorSaveState = saveState,
                    onTaskEditorSaveResultConsumed = { requestId ->
                        if ((saveState as? PersistenceRequestState.Finished)?.requestId == requestId) {
                            saveState = PersistenceRequestState.Idle
                        }
                    },
                    onSaveTask = { _, _, _ -> error("Production request path expected") },
                    onSaveTaskRequest = { _, _, draft, _, requestId ->
                        saveRequests++
                        submittedDraft = draft
                        saveState = PersistenceRequestState.Running(requestId)
                        true
                    },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, track, or workout").performClick()
        compose.onNodeWithText("New Task").performClick()
        compose.onNodeWithTag("task-editor-title").performTextInput("Keep this draft")
        compose.onNodeWithContentDescription("Area selection: Choose Area").performScrollTo().performClick()
        compose.onNodeWithText("Health").performClick()
        compose.runOnIdle { selectedScope = AreaScope.One("work") }
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithTag("persistence-saving-overlay")
            .assertIsDisplayed()
            .assertContentDescriptionContains("Editing is temporarily unavailable", substring = true)
        compose.onAllNodesWithTag("task-editor-title").assertCountEquals(0)
        compose.runOnIdle {
            assertEquals(AreaScope.One("work"), selectedScope)
            assertEquals("health", submittedDraft?.areaId)
            assertEquals(1, saveRequests)
        }
        pressBack()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()

        val failedRequest = (saveState as PersistenceRequestState.Running).requestId
        saveState = PersistenceRequestState.Finished(
            failedRequest,
            WhipResult.Failure("Database unavailable"),
        )

        compose.onNodeWithTag("task-persistence-save-problem").assertIsDisplayed()
        compose.onAllNodesWithTag("task-editor-title").assertCountEquals(1)
        compose.onAllNodesWithText("Keep this draft").assertCountEquals(1)
        compose.onNodeWithText("Save").assertIsEnabled()
        compose.runOnIdle { assertEquals(AreaScope.One("work"), selectedScope) }

        compose.onNodeWithText("Save").performClick()
        val successfulRequest = (saveState as PersistenceRequestState.Running).requestId
        saveState = PersistenceRequestState.Finished(
            successfulRequest,
            WhipResult.Success(EntitySaveReceipt(72, "health")),
        )

        compose.waitUntil(2_000) { selectedScope == AreaScope.One("health") }
        compose.onAllNodesWithTag("task-editor-title").assertCountEquals(0)
        compose.runOnIdle {
            assertEquals(AreaScope.One("health"), selectedScope)
            assertEquals(2, saveRequests)
        }
    }

    @Test
    fun habitSavingBlocksEditingAndDeepFailureReturnsToTheRetainedDraft() {
        var saving by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitEditorDialog(
                    habit = null,
                    initialChecklist = emptyList(),
                    today = LocalDate.of(2026, 8, 31),
                    onDismiss = {},
                    onSave = {},
                    saving = saving,
                    persistenceError = error,
                )
            }
        }

        compose.onNodeWithTag("habit-editor-name").performTextInput("Retained Habit")
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Additional Details"))
        compose.runOnIdle { saving = true }
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.onAllNodesWithTag("habit-editor-name").assertCountEquals(0)
        compose.onNodeWithTag("persistence-saving-overlay").performKeyInput {
            keyDown(Key.Tab)
            keyUp(Key.Tab)
            keyDown(Key.A)
            keyUp(Key.A)
        }

        compose.runOnIdle {
            saving = false
            error = "Habit repository unavailable"
        }
        compose.onNodeWithTag("habit-persistence-save-problem").assertIsDisplayed()
        compose.onNodeWithTag("habit-editor-name").assertIsDisplayed()
        compose.onNodeWithText("Retained Habit").assertIsDisplayed()
    }

    @Test
    fun basicHabitCreationSummarizesRemindersAndDisclosesAdvancedScheduleOptions() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitEditorDialog(
                    habit = null,
                    initialChecklist = emptyList(),
                    today = LocalDate.of(2026, 9, 2),
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithTag("habit-editor-fields")
            .performScrollToNode(hasText("Reminders & Schedule Options"))
        compose.onNodeWithText("Off — no reminders configured").assertIsDisplayed()
        compose.onAllNodesWithText("Default Reminders").assertCountEquals(0)
        compose.onAllNodesWithText("End Condition").assertCountEquals(0)
        compose.onAllNodesWithText("First Day of Week").assertCountEquals(0)

        compose.onNodeWithTag("habit-schedule-options").assertIsDisplayed().performClick()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Default Reminders"))
        compose.onNodeWithText("Default Reminders").assertIsDisplayed()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("End Condition"))
        compose.onNodeWithText("End Condition").assertIsDisplayed()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("First Day of Week"))
        compose.onNodeWithText("First Day of Week").assertIsDisplayed()
    }

    @Test
    fun checklistModeRemovesNumericQuickButtonsAndSavesCanonicalValues() {
        var savedDraft: HabitDraft? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitEditorDialog(
                    habit = null,
                    initialDraft = HabitDraft(
                        name = "Pack",
                        trackingMode = HabitTrackingMode.Count,
                        quickIncrement = 5.0,
                        quickActions = listOf(5.0, 10.0, 15.0),
                        startDate = LocalDate.of(2026, 9, 3),
                        checklistItems = listOf(HabitChecklistItemDraft("Keys", 0)),
                    ),
                    initialChecklist = emptyList(),
                    today = LocalDate.of(2026, 9, 3),
                    onDismiss = {},
                    onSave = { savedDraft = it },
                )
            }
        }

        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Quick Buttons"))
        compose.onNodeWithText("Quick Buttons").assertIsDisplayed()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Checklist"))
        compose.onNodeWithText("Checklist").performClick()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Additional Details"))
        compose.onAllNodesWithText("Quick Buttons").assertCountEquals(0)
        compose.onAllNodesWithText("Build a Range").assertCountEquals(0)
        compose.onAllNodesWithText("Quick increment").assertCountEquals(0)

        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertEquals(1.0, requireNotNull(savedDraft).quickIncrement, 0.0)
            assertTrue(requireNotNull(savedDraft).quickActions.isEmpty())
        }
    }

    @Test
    fun atMostHabitShowsAndSavesItsMaximumWhileIgnoringInactiveScheduleValues() {
        var savedDraft: HabitDraft? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitEditorDialog(
                    habit = null,
                    initialDraft = HabitDraft(
                        name = "Caffeine",
                        trackingMode = HabitTrackingMode.Count,
                        comparison = TargetComparison.AtMost,
                        targetMin = null,
                        targetMax = 3.0,
                        scheduleType = HabitScheduleType.Daily,
                        scheduleInterval = 0,
                        startDate = LocalDate.of(2026, 9, 3),
                    ),
                    initialChecklist = emptyList(),
                    today = LocalDate.of(2026, 9, 3),
                    onDismiss = {},
                    onSave = { savedDraft = it },
                )
            }
        }

        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Maximum per day"))
        compose.onNodeWithText("Maximum per day").assertIsDisplayed()
        compose.onAllNodesWithText("Target per day").assertCountEquals(0)
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            val saved = requireNotNull(savedDraft)
            assertNull(saved.targetMin)
            assertEquals(3.0, saved.targetMax ?: -1.0, 0.0)
            assertEquals(1, saved.scheduleInterval)
        }
    }

    @Test
    fun configuredHabitAutomaticallyRevealsItsReminderAndScheduleSettings() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitEditorDialog(
                    habit = null,
                    initialDraft = HabitDraft(
                        name = "Configured Habit",
                        startDate = LocalDate.of(2026, 9, 2),
                        reminderMinutes = listOf(8 * 60, 17 * 60 + 30),
                        weekdayReminderMinutes = mapOf(DayOfWeek.WEDNESDAY to listOf(12 * 60)),
                        endType = HabitEndType.OnDate,
                        endDate = LocalDate.of(2026, 12, 31),
                        weekStart = DayOfWeek.SUNDAY,
                    ),
                    initialChecklist = emptyList(),
                    today = LocalDate.of(2026, 9, 2),
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithTag("habit-editor-fields")
            .performScrollToNode(hasText("Weekday overrides: Wed", substring = true))
        compose.onNodeWithText("Default:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Weekday overrides: Wed", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Default Reminders"))
        compose.onNodeWithText("Default Reminders").assertIsDisplayed()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Ends ", substring = true))
        compose.onNodeWithText("Ends ", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("habit-editor-fields")
            .performScrollToNode(hasText("Week starts Sunday (different from your app default)"))
        compose.onNodeWithText("Week starts Sunday (different from your app default)").assertIsDisplayed()
    }

    @Test
    fun habitScheduleDisclosureSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                HabitEditorDialog(
                    habit = null,
                    initialChecklist = emptyList(),
                    today = LocalDate.of(2026, 9, 2),
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithTag("habit-editor-fields")
            .performScrollToNode(hasText("Reminders & Schedule Options"))
        compose.onNodeWithTag("habit-schedule-options").performClick()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Default Reminders"))
        compose.onNodeWithText("Default Reminders").assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Default Reminders"))
        compose.onNodeWithText("Default Reminders").assertIsDisplayed()
    }

    @Test
    fun invalidHiddenEndConditionReopensScheduleOptionsOnSave() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitEditorDialog(
                    habit = null,
                    initialDraft = HabitDraft(
                        name = "Needs an end date",
                        startDate = LocalDate.of(2026, 9, 2),
                    ),
                    initialChecklist = emptyList(),
                    today = LocalDate.of(2026, 9, 2),
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithTag("habit-editor-fields")
            .performScrollToNode(hasText("Reminders & Schedule Options"))
        compose.onNodeWithTag("habit-schedule-options").performClick()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("End Condition"))
        compose.onNodeWithText("Never").performClick()
        compose.onNodeWithText("On Date").performClick()
        compose.onNodeWithTag("habit-editor-fields")
            .performScrollToNode(hasText("Reminders & Schedule Options"))
        compose.onNodeWithTag("habit-schedule-options").performClick()
        compose.onAllNodesWithText("Choose End Date").assertCountEquals(0)

        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("habit-editor-fields")
            .performScrollToNode(hasText("Choose an end date on or after the start date", substring = true))
        compose.onNodeWithText("Choose an end date on or after the start date", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("habit-editor-fields").performScrollToNode(hasText("Choose End Date"))
        compose.onNodeWithText("Choose End Date").assertIsDisplayed()
    }

    @Test
    fun verifiedSaveToAnArchivedAreaFallsBackToAllAreas() {
        var saveState by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        var selectedScope by mutableStateOf<AreaScope>(AreaScope.One("health"))
        var areas by mutableStateOf(listOf(area("work", "Work"), area("health", "Health")))
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    settingsState = SettingsUiState(
                        settings = AppSettings(setupCompleted = true),
                        areas = areas,
                        taxonomyLoaded = true,
                    ),
                    areaScope = selectedScope,
                    onSelectAreaScope = { selectedScope = it },
                    taskEditorSaveState = saveState,
                    onTaskEditorSaveResultConsumed = { saveState = PersistenceRequestState.Idle },
                    onSaveTask = { _, _, _ -> error("Production request path expected") },
                    onSaveTaskRequest = { _, _, _, _, requestId ->
                        saveState = PersistenceRequestState.Running(requestId)
                        true
                    },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, track, or workout").performClick()
        compose.onNodeWithText("New Task").performClick()
        compose.onNodeWithTag("task-editor-title").performTextInput("Archived Area Save")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            areas = listOf(area("work", "Work"), area("health", "Health").copy(archived = true))
            val requestId = (saveState as PersistenceRequestState.Running).requestId
            saveState = PersistenceRequestState.Finished(
                requestId,
                WhipResult.Success(EntitySaveReceipt(91, "health", areaVerified = true)),
            )
        }

        compose.waitUntil(2_000) { selectedScope == AreaScope.All }
        compose.onAllNodesWithTag("task-editor-title").assertCountEquals(0)
    }

    @Test
    fun rejectedSaveAndNewRetainsIntentThenAcceptedSuccessStartsAFreshDraft() {
        var saveState by mutableStateOf<PersistenceRequestState<EntitySaveReceipt>>(PersistenceRequestState.Idle)
        var saveRequests = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    settingsState = SettingsUiState(
                        settings = AppSettings(setupCompleted = true, powerMode = true),
                        taxonomyLoaded = true,
                    ),
                    taskEditorSaveState = saveState,
                    onTaskEditorSaveResultConsumed = { saveState = PersistenceRequestState.Idle },
                    onSaveTask = { _, _, _ -> error("Production request path expected") },
                    onSaveTaskRequest = { _, _, _, _, requestId ->
                        saveRequests++
                        if (saveRequests == 1) {
                            false
                        } else {
                            saveState = PersistenceRequestState.Running(requestId)
                            true
                        }
                    },
                    onComplete = {},
                    onSkip = {},
                    onReschedule = { _, _ -> },
                    onArchive = {},
                    onReopen = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Add task, habit, goal, track, or workout").performClick()
        compose.onNodeWithText("New Task").performClick()
        compose.onNodeWithTag("task-editor-title").performTextInput("Chain this Task")
        compose.onNodeWithText("Save & New").performClick()

        compose.onNodeWithTag("task-persistence-save-problem").assertIsDisplayed()
        compose.onNodeWithText("Chain this Task").assertIsDisplayed()
        compose.onNodeWithText("Save & New").performClick()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.runOnIdle {
            val requestId = (saveState as PersistenceRequestState.Running).requestId
            saveState = PersistenceRequestState.Finished(
                requestId,
                WhipResult.Success(EntitySaveReceipt(92, null)),
            )
        }

        compose.waitUntil(2_000) {
            compose.onAllNodesWithTag("task-editor-title").fetchSemanticsNodes(atLeastOneRootRequired = false)
                .singleOrNull()?.config?.let { config ->
                    config.contains(SemanticsProperties.EditableText) &&
                        config[SemanticsProperties.EditableText].text.isEmpty()
                } == true
        }
        compose.onNodeWithText("Save & New").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, saveRequests) }
    }

    @Test
    fun goalSavingBlocksEditingAndDeepFailureReturnsToTheRetainedDraft() {
        var saving by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                GoalEditorDialog(
                    projection = null,
                    today = LocalDate.of(2026, 8, 31),
                    activeZoneId = ZoneId.of("America/Toronto"),
                    nowMillis = 1_788_132_600_000,
                    customUnits = emptyList(),
                    onDismiss = {},
                    onSave = {},
                    saving = saving,
                    persistenceError = error,
                )
            }
        }

        compose.onNodeWithTag("goal-editor-name").performTextInput("Retained Goal")
        compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Additional Details"))
        compose.runOnIdle { saving = true }
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.onAllNodesWithTag("goal-editor-name").assertCountEquals(0)

        compose.runOnIdle {
            saving = false
            error = "Goal repository unavailable"
        }
        compose.onNodeWithTag("goal-persistence-save-problem").assertIsDisplayed()
        compose.onNodeWithTag("goal-editor-name").assertIsDisplayed()
        compose.onNodeWithText("Retained Goal").assertIsDisplayed()
    }

    private fun area(id: String, name: String) = Area(
        id = id,
        name = name,
        colorArgb = 0xFF1565C0,
        position = 0,
        archived = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
