package com.whip.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

        compose.onNodeWithContentDescription("Area selection: No area").performClick()
        compose.onNodeWithContentDescription("Area Work").assertIsDisplayed().performClick()
        assertEquals("work" to "Work", selected.get())

        compose.onNodeWithContentDescription("Area selection: No area").performClick()
        compose.onNodeWithText("Create area…").performClick()
        compose.onNodeWithText("Area name").performTextInput("Personal")
        compose.onNodeWithText("Create").performClick()
        assertEquals("created-personal" to "Personal", selected.get())
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
    fun zeroAreaScopeOffersSetupInsteadOfDuplicateAllAndNoAreaFilters() {
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

        compose.onNodeWithText("Set up areas").performClick()
        compose.onNodeWithText("Create area…").assertIsDisplayed().performClick()
        compose.onNodeWithText("Area name").performTextInput("Client Delta")
        compose.onNodeWithText("Create").performClick()

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
                    unassignedUsage = AreaUsageCounts(goals = 4),
                    onSelect = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Area scope: Work").performClick()
        compose.onNodeWithText("✓  Work · 3 items").assertIsDisplayed()
        compose.onNodeWithText("No area · 4 items", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Edit areas…").assertIsDisplayed()
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

        compose.onNodeWithContentDescription("Area selection: No area").performClick()
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
                    onDismiss = { choice.set("cancel") },
                    onKeepItems = { choice.set("keep") },
                    onDeleteItems = { choice.set("delete") },
                )
            }
        }

        compose.onNodeWithText("Delete Client Delta permanently?").assertIsDisplayed()
        compose.onNodeWithText("Moving them keeps the items and their history.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Deleting the items cannot be undone.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Move items to No area").assertIsDisplayed().performClick()
        assertEquals("keep", choice.get())
        compose.onNodeWithText("Delete area and 4 items").assertIsDisplayed().performClick()
        assertEquals("delete", choice.get())
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
