package com.whip.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.whip.app.WhipApplication
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope
import com.whip.app.ui.theme.WhipTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AreaFeatureUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pickerSelectsExistingAreaAndCreatesInline() {
        val selected = AtomicReference<Pair<String?, String>>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                AreaPicker(
                    areas = listOf(area("work", "Work"), area("health", "Health")),
                    selectedAreaId = null,
                    selectedAreaName = "",
                    onSelect = { id, name -> selected.set(id to name) },
                    onCreateArea = { name, _, onCreated -> onCreated(Result.success("created-${name.lowercase()}")) },
                )
            }
        }

        compose.onNodeWithContentDescription("Area selection: Work").performClick()
        compose.onNodeWithContentDescription("Area Work").assertIsDisplayed().performClick()
        assertEquals("work" to "Work", selected.get())

        compose.onNodeWithContentDescription("Area selection: Work").performClick()
        compose.onNodeWithText("Create Area…").performClick()
        compose.onNodeWithText("Area name").performTextInput("Personal")
        compose.onNodeWithText("Create").performClick()
        assertEquals("created-personal" to "Personal", selected.get())
    }

    @Test
    fun pickerDoesNotClaimARequiredDefaultWasInheritedFromTheCurrentView() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                AreaPicker(
                    areas = listOf(area("main", "Main")),
                    selectedAreaId = "main",
                    selectedAreaName = "Main",
                    onSelect = { _, _ -> },
                    onCreateArea = { _, _, _ -> },
                    inheritedFromScope = false,
                )
            }
        }

        compose.onAllNodesWithText("Defaulted from the current area view.").assertCountEquals(0)
    }

    @Test
    fun pickerExplainsWhenItsAreaWasActuallyInheritedFromScope() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                AreaPicker(
                    areas = listOf(area("main", "Main")),
                    selectedAreaId = "main",
                    selectedAreaName = "Main",
                    onSelect = { _, _ -> },
                    onCreateArea = { _, _, _ -> },
                    inheritedFromScope = true,
                )
            }
        }

        compose.onNodeWithText("Defaulted from the current area view.").assertIsDisplayed()
    }

    @Test
    fun badgeUsesTextAndChangesTheGlobalScope() {
        val selected = AtomicReference<String>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAreaUiContext provides AreaUiContext(listOf(area("work", "Work"))) { selected.set(it) },
                ) {
                    AreaBadge("work", "Work")
                }
            }
        }

        compose.onNodeWithContentDescription("Area Work. Show only this area.").performClick()
        assertEquals("work", selected.get())
    }

    @Test
    fun emergencyZeroAreaStateCreatesMainWithoutShowingAPlaceholder() {
        val selected = AtomicReference<AreaScope>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                AreaScopeMenu(
                    scope = AreaScope.All,
                    areas = emptyList(),
                    onSelect = selected::set,
                    onCreateArea = { _, _, result -> result(Result.success("client-delta")) },
                )
            }
        }

        compose.onNodeWithText("Main").performClick()
        compose.onNodeWithText("Create Main Area").assertIsDisplayed().performClick()

        assertEquals(AreaScope.One("client-delta"), selected.get())
        compose.onAllNodesWithText("Create Personal").assertCountEquals(0)
        compose.onAllNodesWithText("Create Work").assertCountEquals(0)
        compose.onAllNodesWithText("Create Health").assertCountEquals(0)
    }

    @Test
    fun scopeMenuShowsSelectionAndItemCountsWithSentenceCaseLabels() {
        val work = area("work", "Work")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                AreaScopeMenu(
                    scope = AreaScope.One("work"),
                    areas = listOf(work),
                    usage = mapOf("work" to AreaUsageCounts(tasks = 2, habits = 1)),
                    onSelect = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Area scope: Work").performClick()
        compose.onNodeWithText("Work · 3 items").assertIsDisplayed()
        compose.onAllNodesWithText("No area", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Manage Areas").assertIsDisplayed()
    }

    @Test
    fun areaWorkspaceKeepsExitTrailingAndBackLeadingAtTwoHundredPercentText() {
        val dismissed = AtomicReference(false)
        val application = ApplicationProvider.getApplicationContext<WhipApplication>()
        val viewModel = SettingsViewModel(application)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    AreaManagementDialog(
                        state = SettingsUiState(areas = listOf(area("right-hand", "Right Hand"))),
                        viewModel = viewModel,
                        onDismiss = { dismissed.set(true) },
                    )
                }
            }
        }

        val rootTitle = compose.onNodeWithTag("area-destination-title").fetchSemanticsNode().boundsInRoot
        val rootClose = compose.onNodeWithTag("area-close-action").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        check(rootClose.center.x > rootTitle.center.x) {
            "Areas exit must stay on the trailing side: title=$rootTitle close=$rootClose"
        }

        compose.onNodeWithContentDescription("Open area details for Right Hand").performClick()
        val detailTitle = compose.onNodeWithTag("area-destination-title").fetchSemanticsNode().boundsInRoot
        val back = compose.onNodeWithTag("area-back-action").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithTag("area-close-action").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        check(back.center.x < detailTitle.center.x && close.center.x > detailTitle.center.x) {
            "Area details must keep Back leading and Exit trailing: back=$back title=$detailTitle close=$close"
        }

        compose.onNodeWithContentDescription("Close Areas").performClick()
        compose.runOnIdle { assertEquals(true, dismissed.get()) }
    }

    @Test
    fun largeAreaPickerUsesSearchAndCanSelectTheLastArea() {
        val selected = AtomicReference<Pair<String?, String>>()
        val areas = (1..50).map { area("area-$it", "Area $it") }
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                AreaPicker(areas, null, "", { id, name -> selected.set(id to name) }, { _, _, _ -> })
            }
        }

        compose.onNodeWithContentDescription("Area selection: Area 1").performClick()
        compose.onNodeWithText("Find area").performTextInput("Area 50")
        compose.onNodeWithContentDescription("Area Area 50").performClick()
        assertEquals("area-50" to "Area 50", selected.get())
    }

    @Test
    fun permanentDeleteExplainsBothChoicesAndRequiresAnExplicitChoice() {
        val choice = AtomicReference<String>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                PermanentAreaDeleteDialog(
                    area = area("client-delta", "Client Delta"),
                    usage = AreaUsageCounts(tasks = 2, habits = 1, goals = 1),
                    replacementAreas = listOf(area("personal", "Personal")),
                    onDismiss = { choice.set("cancel") },
                    onMoveItems = { choice.set("move-$it") },
                    onDeleteItems = { choice.set("delete") },
                )
            }
        }

        compose.onNodeWithText("Delete Client Delta Permanently?").assertIsDisplayed()
        compose.onNodeWithText("Moving them keeps the items and their history.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Deleting the items cannot be undone.", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Move items to Personal").performClick()
        compose.onNodeWithText("Move Items and Delete Area").assertIsDisplayed().performClick()
        assertEquals("move-personal", choice.get())
        compose.onNodeWithText("Delete Area and 4 Items").assertIsDisplayed().performClick()
        assertEquals("delete", choice.get())
    }

    @Test
    fun allItemsCanBeMovedBetweenAreasInOneAction() {
        val target = AtomicReference<String?>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                MoveAreaItemsDialog(
                    sourceId = "main",
                    sourceName = "Main",
                    usage = AreaUsageCounts(tasks = 2, habits = 1, goals = 1),
                    targets = listOf(area("personal", "Personal")),
                    onDismiss = {},
                    onMove = target::set,
                )
            }
        }

        compose.onNodeWithText("Move Everything from Main").assertIsDisplayed()
        compose.onNodeWithContentDescription("Move to Personal").performClick()
        compose.onNodeWithText("Move 4 Items").performClick()
        assertEquals("personal", target.get())
    }

    @Test
    fun deletingTheOnlyAreaPromptsForAReplacementFirst() {
        val choice = AtomicReference<String>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                LastAreaRequiredDialog(
                    modifier = androidx.compose.ui.Modifier,
                    area = area("main", "Main"),
                    action = "delete",
                    onDismiss = { choice.set("cancel") },
                    onCreateArea = { choice.set("create") },
                )
            }
        }

        compose.onNodeWithText("Create Another Area First").assertIsDisplayed()
        compose.onNodeWithText("Main is your only active Area.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Create Area").performClick()
        assertEquals("create", choice.get())
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
