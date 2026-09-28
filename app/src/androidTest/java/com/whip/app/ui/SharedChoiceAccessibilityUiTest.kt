package com.whip.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class SharedChoiceAccessibilityUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test
    fun choiceMenuExposesSelectionAndClosesWhenUnavailable() {
        var enabled by mutableStateOf(true)
        var selected by mutableStateOf("Daily")
        val unit = UnitDefinition("custom", "Training distance", "steps", UnitDimension.Distance, 1.0, custom = true)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column {
                SelectionField("Period", listOf("Daily", "Weekly"), selected, { it }, { selected = it }, enabled = enabled)
                UnitSelectionField(units = listOf(unit), selectedUnitId = unit.id, dimension = unit.dimension,
                    onSelect = {}, onCreateUnit = UnavailableCreateCustomUnitAction, enabled = enabled)
                }
            }
        }
        compose.onNodeWithContentDescription("Period: Daily").performClick()
        compose.onNodeWithContentDescription("Period option: Daily").assertIsSelected()
        compose.onNodeWithContentDescription("Period option: Weekly").assertIsNotSelected()
        compose.onAllNodesWithContentDescription("Selected").assertCountEquals(0)
        compose.runOnIdle { enabled = false }
        compose.onNodeWithContentDescription("Period: Daily").assertIsNotEnabled()
        compose.onAllNodesWithContentDescription("Period option: Weekly").assertCountEquals(0)
        compose.runOnIdle { enabled = true }
        compose.onAllNodesWithContentDescription("Period option: Weekly").assertCountEquals(0)
        compose.onNodeWithContentDescription("Period: Daily").performClick()
        compose.onNodeWithContentDescription("Period option: Weekly").performClick()
        compose.runOnIdle { assertEquals("Weekly", selected) }
        compose.onNodeWithContentDescription("Unit: ${unitDefinitionDisplayLabel(unit)}").performClick()
        compose.onNode(hasText(unitDefinitionDisplayLabel(unit)) and isSelected()).assertIsDisplayed()
        compose.runOnIdle { enabled = false }
        compose.onNodeWithContentDescription("Unit: ${unitDefinitionDisplayLabel(unit)}").assertIsNotEnabled()
        compose.onAllNodes(hasText(unitDefinitionDisplayLabel(unit)) and isSelected()).assertCountEquals(0)
    }

    @Test
    @AndroidFontScale(2f)
    fun longValuesWrapAndNarrowViewChoicesExposeSelection() {
        val value = "Every scheduled working day of the month"
        val unit = UnitDefinition("custom", "Long training distance unit", "steps", UnitDimension.Distance, 1.0, custom = true)
        var view by mutableStateOf("Calendar")
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column(Modifier.width(300.dp).verticalScroll(rememberScrollState())) {
                    SelectionField("Schedule", listOf(value), value, { it }, {})
                    UnitSelectionField(
                        units = listOf(unit), selectedUnitId = unit.id, dimension = unit.dimension,
                        onSelect = {}, onCreateUnit = UnavailableCreateCustomUnitAction,
                    )
                    SegmentedChoiceBar(view, listOf("Calendar", "Agenda", "List"), { view = it }, { it })
                }
            }
        }
        fun assertCompleteWrappedText(text: String) {
            compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    assertTrue(action(results))
                    val layout = results.single()
                    assertTrue(layout.lineCount > 1)
                    assertFalse(layout.hasVisualOverflow)
                    repeat(layout.lineCount) { assertFalse(layout.isLineEllipsized(it)) }
                }
        }
        assertCompleteWrappedText(value)
        assertCompleteWrappedText(unitDefinitionDisplayLabel(unit))
        captureVisualCatalogSurface("ux-upgrades.shared.choices-large")
        compose.onNodeWithContentDescription("Choose view. Selected Calendar").performScrollTo().performClick()
        compose.onNode(hasText("Calendar") and isSelected()).assertIsDisplayed()
        compose.onNodeWithText("Agenda").assertIsNotSelected().performClick()
        compose.runOnIdle { assertEquals("Agenda", view) }
        compose.onAllNodesWithContentDescription("Selected").assertCountEquals(0)
    }
}
