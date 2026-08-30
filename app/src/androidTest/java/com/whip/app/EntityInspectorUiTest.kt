package com.whip.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.ScheduledSubtask
import com.whip.app.domain.ScheduledTask
import com.whip.app.domain.TaskStep
import com.whip.app.domain.WhipTask
import com.whip.app.ui.EntityInspector
import com.whip.app.ui.EntityInspectorPrimaryAction
import com.whip.app.ui.EntityInspectorSection
import com.whip.app.ui.EntityInspectorSectionPlacement
import com.whip.app.ui.TaskActionsDialog
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntityInspectorUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun taskInspectorKeepsIdentityNavigationAndPrimaryActionStable() {
        val completed = AtomicInteger()
        val item = ScheduledTask(
            task = WhipTask(
                id = 901,
                title = "Prepare launch notes",
                notes = "Keep the outcome clear.",
                scheduleKind = ScheduleKind.Once,
                date = LocalDate.of(2026, 8, 26),
                recurrence = null,
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                area = "Work",
                icon = "📝",
            ),
            originalDate = LocalDate.of(2026, 8, 26),
            scheduledDate = LocalDate.of(2026, 8, 26),
        )

        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TaskActionsDialog(
                    item = item,
                    onDismiss = {},
                    onComplete = { completed.incrementAndGet() },
                    onEdit = {},
                    onReschedule = {},
                    onSkip = {},
                    onArchive = {},
                    onDeletePermanently = {},
                    onPin = {},
                    onDuplicate = {},
                    onStartFocus = {},
                    onToggleSubtask = { _, _ -> },
                    onPromoteSubtask = {},
                    onReopenOccurrence = {},
                    onResetOccurrence = {},
                )
            }
        }

        listOf(
            "task-actions-surface",
            "entity-inspector",
            "entity-inspector-header",
            "entity-inspector-title",
            "entity-inspector-status",
            "entity-inspector-close",
            "entity-inspector-edit",
            "entity-inspector-section-selector",
            "task-detail-section-Overview",
            "entity-inspector-primary-complete",
        ).forEach { tag -> compose.onNodeWithTag(tag).assertIsDisplayed() }

        compose.onNodeWithContentDescription("Close Task details").assertHasClickAction()
        compose.onNodeWithContentDescription("Edit Task").assertHasClickAction()
        compose.onNodeWithText("Overview").assertIsSelected()
        compose.onAllNodesWithText("Schedule", useUnmergedTree = true).assertCountEquals(0)

        val inspectorBefore = compose.onNodeWithTag("entity-inspector").getUnclippedBoundsInRoot()
        val headerBefore = compose.onNodeWithTag("entity-inspector-header").getUnclippedBoundsInRoot()
        val primaryBefore = compose.onNodeWithTag("entity-inspector-primary-complete").getUnclippedBoundsInRoot()
        assertTrue(
            compose.onNodeWithTag("entity-inspector-close").getUnclippedBoundsInRoot().left >
                compose.onNodeWithTag("entity-inspector-edit").getUnclippedBoundsInRoot().left,
        )
        compose.onNodeWithTag("task-detail-section-Schedule").assertHasClickAction().performClick()
        compose.onNodeWithText("Activity").assertIsSelected()
        compose.onNodeWithTag("entity-inspector-content-activity").assertIsDisplayed()
        assertEquals(inspectorBefore, compose.onNodeWithTag("entity-inspector").getUnclippedBoundsInRoot())
        assertEquals(headerBefore, compose.onNodeWithTag("entity-inspector-header").getUnclippedBoundsInRoot())
        assertEquals(primaryBefore, compose.onNodeWithTag("entity-inspector-primary-complete").getUnclippedBoundsInRoot())
        compose.onNodeWithTag("entity-inspector-primary-complete").performClick()
        compose.runOnIdle { assertEquals(1, completed.get()) }
    }

    @Test
    fun taskSubtasksKeepCompletionTrailingSpacedAndResponsive() {
        val toggleCount = AtomicInteger()
        val item = ScheduledTask(
            task = WhipTask(
                id = 902,
                title = "Prepare launch notes",
                notes = "",
                scheduleKind = ScheduleKind.Once,
                date = LocalDate.of(2026, 8, 26),
                recurrence = null,
                timeMinutes = null,
                reminderEnabled = false,
                archived = false,
                completedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                area = "Work",
                icon = "📝",
            ),
            originalDate = LocalDate.of(2026, 8, 26),
            scheduledDate = LocalDate.of(2026, 8, 26),
            subtasks = listOf(
                ScheduledSubtask(
                    step = TaskStep(
                        id = 77,
                        taskId = 902,
                        title = "Confirm every launch dependency before publishing",
                        notes = "",
                        position = 0,
                        createdAtMillis = 1,
                        updatedAtMillis = 1,
                    ),
                    completed = true,
                    completedAtMillis = 1,
                    title = "Confirm every launch dependency before publishing",
                ),
            ),
        )

        compose.setContent {
            val largeText = Density(compose.density.density, fontScale = 2f)
            CompositionLocalProvider(LocalDensity provides largeText) {
                WhipTheme(dynamicColor = false) {
                    TaskActionsDialog(
                        item = item,
                        onDismiss = {},
                        onComplete = {},
                        onEdit = {},
                        onReschedule = {},
                        onSkip = {},
                        onArchive = {},
                        onDeletePermanently = {},
                        onPin = {},
                        onDuplicate = {},
                        onStartFocus = {},
                        onToggleSubtask = { _, _ -> toggleCount.incrementAndGet() },
                        onPromoteSubtask = {},
                        onReopenOccurrence = {},
                        onResetOccurrence = {},
                        modifier = Modifier.width(340.dp),
                    )
                }
            }
        }

        val text = compose.onNodeWithTag("task-subtask-text-77", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val check = compose.onNodeWithTag("task-subtask-check-77", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        val convert = compose.onNodeWithTag("task-subtask-convert-77", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Subtask completion must trail its text", check.left >= text.right + 8.dp)
        assertTrue("Subtask completion target must remain 48 dp", check.bottom - check.top >= 48.dp)
        assertTrue("Large text must place conversion below the completion line", convert.top >= check.bottom)
        compose.onNodeWithTag("task-subtask-check-77", useUnmergedTree = true).assertIsOn().performClick()
        compose.runOnIdle { assertEquals(1, toggleCount.get()) }
    }

    @Test
    fun inspectorKeepsDirectSectionsStableAndUsesContextualOverflowForOptions() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                var selected by remember { mutableStateOf("today") }
                EntityInspector(
                    entityType = "Habit",
                    title = "Read",
                    emoji = "📚",
                    context = "Daily",
                    status = "Active",
                    sections = listOf(
                        EntityInspectorSection("today", "Today"),
                        EntityInspectorSection("history", "History"),
                        EntityInspectorSection("automation", "Automation"),
                        EntityInspectorSection(
                            "options",
                            "Options",
                            placement = EntityInspectorSectionPlacement.Overflow,
                        ),
                    ),
                    selectedSectionId = selected,
                    onSelectSection = { selected = it },
                    onDismiss = {},
                    onEdit = {},
                    editLabel = "Edit Habit",
                    modifier = Modifier.width(320.dp),
                ) {
                    Text("Selected $selected")
                }
            }
        }

        compose.onNodeWithText("Today").assertIsSelected()
        compose.onNodeWithText("Automation").assertIsDisplayed()
        compose.onAllNodesWithText("Options").assertCountEquals(0)
        compose.onNodeWithContentDescription("More Habit options").assertIsDisplayed().performClick()
        compose.onNodeWithText("Options").assertIsDisplayed().performClick()
        compose.onAllNodesWithTag("entity-inspector-section-options").assertCountEquals(0)
        compose.onNodeWithTag("entity-inspector-section-today").assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-section-history").assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-section-automation").assertIsDisplayed()
        compose.onNodeWithText("Selected options").assertIsDisplayed()
        compose.onNodeWithContentDescription("More Habit options").performClick()
        compose.onNodeWithText("Options").assertIsDisplayed()
        compose.onNodeWithText("Options").assertIsSelected()
    }

    @Test
    fun compactLargeTextInspectorKeepsOverviewDirectAndOptionsInOverflow() {
        val largeText = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides largeText) {
                WhipTheme(dynamicColor = false) {
                    EntityInspector(
                        entityType = "Exercise",
                        title = "Barbell row",
                        emoji = "🏋️",
                        context = "Weight and reps",
                        status = "Available",
                        sections = listOf(
                            EntityInspectorSection("overview", "Overview"),
                            EntityInspectorSection(
                                "options",
                                "Options",
                                placement = EntityInspectorSectionPlacement.Overflow,
                            ),
                        ),
                        selectedSectionId = "overview",
                        onSelectSection = {},
                        onDismiss = {},
                        onEdit = {},
                        modifier = Modifier.width(280.dp),
                    ) { Text("Exercise overview") }
                }
            }
        }

        compose.onNodeWithTag("entity-inspector-section-overview").assertIsDisplayed()
        compose.onAllNodesWithTag("entity-inspector-section-options").assertCountEquals(0)
        compose.onNodeWithContentDescription("More Exercise options").assertIsDisplayed().performClick()
        compose.onNodeWithText("Options").assertIsDisplayed()
    }

    @Test
    fun fixedInspectorFrameKeepsLongCenterContentScrollable() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                var selected by remember { mutableStateOf("overview") }
                EntityInspector(
                    entityType = "Goal",
                    title = "Long goal",
                    emoji = "🎯",
                    context = "Main",
                    status = "Active",
                    sections = listOf(
                        EntityInspectorSection("overview", "Overview"),
                        EntityInspectorSection("options", "Options"),
                    ),
                    selectedSectionId = selected,
                    onSelectSection = { selected = it },
                    onDismiss = {},
                    onEdit = {},
                    editLabel = "Edit Goal",
                    modifier = Modifier.width(320.dp),
                    primaryAction = EntityInspectorPrimaryAction("log", "Log Progress", {}),
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        repeat(30) { index -> Text("$selected evidence $index") }
                    }
                }
            }
        }

        compose.onNodeWithText("overview evidence 29").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-section-options").performClick()
        compose.onNodeWithText("options evidence 0").assertIsDisplayed()
        compose.onNodeWithText("options evidence 29").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-section-overview").performClick()
        compose.onNodeWithText("overview evidence 29").assertIsDisplayed()
        compose.onNodeWithTag("entity-inspector-primary-log").assertIsDisplayed()
    }
}
