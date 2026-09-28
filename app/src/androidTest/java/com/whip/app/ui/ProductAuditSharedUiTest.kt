package com.whip.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.assertDialogFontScale
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.WhipResult
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.Area
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.theme.WhipTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductAuditSharedUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test
    fun emptyCustomDimensionCanCreateAndSelectItsFirstUnit() {
        var units by mutableStateOf(BuiltInUnits.all)
        var selected by mutableStateOf("missing")
        var dimension by mutableStateOf(UnitDimension.Custom)
        var createdUnit: UnitDefinition? = null
        val state = MutableStateFlow<PersistenceRequestState<CustomUnitMutationReceipt>>(PersistenceRequestState.Idle)
        val create = CreateCustomUnitAction(state, { state.value = PersistenceRequestState.Idle }) { request, id, name, symbol, kind, factor ->
            createdUnit = UnitDefinition(id, name, symbol, kind, factor, custom = true)
            state.value = PersistenceRequestState.Finished(request, WhipResult.Success(CustomUnitMutationReceipt(id)))
            true
        }
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                UnitSelectionField(units, selected, dimension, { selected = it }, create)
            }
        }
        compose.onNodeWithText("Create Custom Unit…").assertIsEnabled().performClick()
        compose.onNodeWithTag("custom-unit-name").performTextReplacement("Practice Block")
        compose.onNodeWithTag("custom-unit-symbol").performScrollTo().performTextReplacement("block")
        compose.onNodeWithTag("custom-unit-factor").performScrollTo().performTextReplacement("1")
        compose.onNodeWithTag("custom-unit-confirm").performClick()
        compose.onNodeWithText("The unit was saved. Waiting for the updated unit list…").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals("missing", selected)
            units = units + requireNotNull(createdUnit)
        }
        compose.onNodeWithText("Practice Block (block)").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(UnitDimension.Custom, units.single { it.id == selected }.dimension)
            dimension = UnitDimension.Mass
            selected = "missing"
        }
        // Display must never pretend the first option is already the saved selection.
        compose.onNodeWithText("Choose Unit").assertIsDisplayed().performClick()
        compose.onNodeWithText("kilograms (kg)").performClick()
        compose.runOnIdle { assertEquals("kilogram", selected) }
    }

    @Test
    fun interruptedInlineAreaCreationRetainsDraftAndRejectsDisposedCallback() {
        val callbacks = mutableListOf<(Result<String>) -> Unit>()
        var selections = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                CreateAreaDialog(
                    existingAreas = emptyList(),
                    onDismiss = {},
                    onCreate = { name, _, callback ->
                        assertEquals("Shared Focus", name)
                        callbacks += callback
                    },
                    onSelected = { _, _ -> selections++ },
                )
            }
        }
        compose.onNodeWithText("Area name").performTextReplacement("Shared Focus")
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Area name").assertTextContains("Shared Focus")
        compose.onNodeWithText("Area creation was interrupted.", substring = true).performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            callbacks.single()(Result.success("old-result"))
            assertEquals(0, selections)
        }
        compose.onNodeWithText("Create").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(2, callbacks.size)
            callbacks.last()(Result.success("current-result"))
            assertEquals(1, selections)
        }
    }

    @Test
    fun savingColorBlocksFurtherEditingAndKeepsDraftOnFailure() {
        var saving by mutableStateOf(false)
        var failure by mutableStateOf<String?>(null)
        var dismissed = false
        val submitted = mutableListOf<Long?>()
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipColorPickerDialog(
                    title = "Area Color", initialColor = null,
                    onDismiss = { dismissed = true },
                    onConfirm = { submitted += it; saving = true },
                    saving = saving, error = failure,
                )
            }
        }
        compose.onNodeWithTag("color-preset-rose").performClick()
        compose.onNodeWithTag("color-picker-apply").performClick()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.onAllNodesWithTag("color-picker-apply").assertCountEquals(0)
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.runOnIdle {
            assertFalse(dismissed)
            assertEquals(1, submitted.size)
            saving = false
            failure = "Try applying the color again."
        }
        compose.onNodeWithTag("color-picker-preview-name").assertTextContains("Rose")
        compose.onNodeWithText("Try applying the color again.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("color-picker-apply").performClick()
        compose.runOnIdle { assertEquals(listOf(submitted.first(), submitted.first()), submitted) }
    }

    @Test
    @AndroidFontScale
    fun areaCreationLongFailureCanScrollWhileActionsStayReachable() {
        val error = "The Area could not be saved because its existing assignments are being updated. " +
            "Your changes are still here. Wait for the other change to finish and try again."
        var moving by mutableStateOf(false)
        var moved: String? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                if (moving) MoveAreaItemsDialog(
                    modifier = Modifier.width(320.dp), sourceId = "source",
                    sourceName = "Personal wellbeing and long term projects",
                    usage = AreaUsageCounts(tasks = 2),
                    targets = (1..12).map { Area("target-$it", "Destination $it", null, it, false, 1, 1) },
                    onDismiss = {}, onMove = { moved = it },
                ) else CreateAreaDialog(
                    modifier = Modifier.width(320.dp), existingAreas = emptyList(),
                    onDismiss = { moving = true }, onCreate = { _, _, _ -> }, controlledError = error,
                )
            }
        }
        compose.assertDialogFontScale()
        compose.onNodeWithText(error).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Create").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        val colorLayouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Default", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(colorLayouts) }
        assertTrue(colorLayouts.isNotEmpty())
        assertFalse(colorLayouts.single().didOverflowWidth)
        captureVisualCatalogSurface("product-audit.shared.area-error")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("move-area-choice-list").performScrollToNode(hasText("Destination 12"))
        compose.onNodeWithText("Destination 12").assertIsDisplayed().performClick()
        compose.onNodeWithText("Move 2 Items").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("target-12", moved) }
        captureVisualCatalogSurface("product-audit.shared.area-move")
    }

    @Test
    @AndroidFontScale
    fun clockModesKeepAuthoredTimeAndDuplicateGuidanceAfterRecreation() {
        var saved: Int? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                ClockPickerDialog(
                    title = "Reminder Time", initialMinutes = 8 * 60,
                    occupiedMinutes = listOf(8 * 60), onDismiss = {}, onSet = { saved = it },
                )
            }
        }
        compose.assertDialogFontScale()
        compose.onNodeWithTag("clock-picker-keyboard").assertIsDisplayed()
        compose.onNodeWithText("Add").assertIsNotEnabled()
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("9")
        fields[1].performTextReplacement("35")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.onNodeWithText("Use Clock").performScrollTo().performClick()
        compose.onNodeWithTag("clock-picker-dial").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Use Keyboard").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("clock-picker-keyboard").assertIsDisplayed()
        captureVisualCatalogSurface("product-audit.shared.clock-keyboard")
        compose.onNodeWithText("Add").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(9 * 60 + 35, saved) }
    }
}
