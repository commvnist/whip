package com.whip.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.domain.TrackDefinitionBoundary
import com.whip.app.domain.TrackDefinitionConflictKind
import com.whip.app.domain.TrackDefinitionRemovalReview
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldRemovalImpact
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.LocalWhipDialogPlacement
import com.whip.app.ui.UnavailableCreateCustomUnitAction
import com.whip.app.ui.TrackDefinitionReviewUiState
import com.whip.app.ui.TrackEditor
import com.whip.app.ui.TrackEntryUnavailableRoute
import com.whip.app.ui.WhipDialogPlacement
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDefinitionMutationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun freshAddedNumberFieldUsesCurrentUnitAndPrecisionPreferences() {
        var saved: TrackDraft? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEditor(
                    liveInitial = null,
                    targetTrackId = null,
                    openingDraft = null,
                    routeOpeningBoundary = null,
                    targetName = null,
                    areas = emptyList(),
                    customUnits = emptyList(),
                    settings = AppSettings(massUnitId = "pound", numberPrecision = 3),
                    defaultAreaId = null,
                    saving = false,
                    modifier = Modifier,
                    onDismiss = {},
                    onCreateArea = { _, _, _ -> },
                    onCreateCustomUnit = UnavailableCreateCustomUnitAction,
                    onRetryPreparation = {},
                    onReview = { _, _, _ -> },
                    onSave = { draft, _, _ -> saved = draft },
                )
            }
        }

        compose.onNodeWithTag("track-editor-name").performTextInput("Measurements")
        compose.onNodeWithText("Add Field").performClick()
        compose.onNodeWithTag("track-field-name").performTextInput("Weight")
        compose.onNodeWithContentDescription("Field Type: Short Text").performClick()
        compose.onNodeWithContentDescription("Field Type option: Number").performClick()
        compose.onNodeWithContentDescription("Measurement Type: Count").performClick()
        compose.onNodeWithContentDescription("Measurement Type option: Mass").performClick()
        compose.onNodeWithContentDescription("Unit: pounds (lb)").assertIsDisplayed()
        compose.onNodeWithContentDescription("Decimal Places: 3").assertIsDisplayed()
        compose.onNodeWithText("Save Field").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            val field = requireNotNull(saved).fields.single { it.name == "Weight" }
            assertEquals(TrackFieldType.Number, field.type)
            assertEquals(UnitDimension.Mass, field.dimension)
            assertEquals("pound", field.unitId)
            assertEquals(3, field.precision)
        }
    }

    @Test
    fun openingNumberFieldRetainsItsAuthoredUnitAndPrecision() {
        val authored = TrackFieldDraft(
            name = "Weight",
            type = TrackFieldType.Number,
            dimension = UnitDimension.Mass,
            unitId = "gram",
            precision = 4,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEditor(
                    liveInitial = null,
                    targetTrackId = null,
                    openingDraft = TrackDraft("Measurements", fields = listOf(authored)),
                    routeOpeningBoundary = null,
                    targetName = null,
                    areas = emptyList(),
                    customUnits = emptyList(),
                    settings = AppSettings(massUnitId = "pound", numberPrecision = 1),
                    defaultAreaId = null,
                    saving = false,
                    modifier = Modifier,
                    onDismiss = {},
                    onCreateArea = { _, _, _ -> },
                    onCreateCustomUnit = UnavailableCreateCustomUnitAction,
                    onRetryPreparation = {},
                    onReview = { _, _, _ -> },
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onNodeWithContentDescription("Edit Field Weight").performClick()
        compose.onNodeWithContentDescription("Unit: grams (g)").assertIsDisplayed()
        compose.onNodeWithContentDescription("Decimal Places: 4").assertIsDisplayed()
    }

    @Test
    fun exactRemovalReviewKeepsLongImpactAndActionsReachableAtTwoHundredPercentText() {
        val draft = TrackDraft(
            name = "Health log",
            fields = listOf(
                TrackFieldDraft(
                    name = "Date",
                    type = TrackFieldType.Date,
                    required = true,
                    primary = true,
                    uuid = "field-date",
                    id = 1,
                ),
            ),
        )
        val boundary = TrackDefinitionBoundary(9, "track-9", 1, "definition")
        val review = TrackDefinitionRemovalReview(
            trackId = 9,
            definitionRevisionToken = "definition",
            removalRevisionToken = "removal",
            removedFields = (1..8).map { index ->
                TrackFieldRemovalImpact(
                    fieldId = 100L + index,
                    fieldUuid = "removed-$index",
                    fieldName = "Historical field $index",
                    savedValueCount = index * 3,
                    childChoiceCount = index,
                    legacyLinkSourceCount = index,
                    legacyLinkConditionCount = 1,
                    legacyTriggerConditionCount = index,
                    legacyTriggerMappingCount = 1,
                )
            },
            removedChoices = emptyList(),
        )
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(compose.density.density, fontScale = 2f),
                LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
            ) {
                WhipTheme(dynamicColor = false) {
                    TrackEditor(
                        liveInitial = null,
                        targetTrackId = 9,
                        openingDraft = draft,
                        routeOpeningBoundary = boundary,
                        targetName = "Health log",
                        areas = emptyList(),
                        customUnits = emptyList(),
                        defaultAreaId = null,
                        saving = false,
                        definitionReviewState = TrackDefinitionReviewUiState(
                            sessionId = 4,
                            trackId = 9,
                            boundary = boundary,
                            review = review,
                            reviewedDraft = draft,
                        ),
                        modifier = Modifier.width(320.dp),
                        sessionId = 4,
                        onDismiss = {},
                        onCreateArea = { _, _, _ -> },
                        onCreateCustomUnit = UnavailableCreateCustomUnitAction,
                        onRetryPreparation = {},
                        onReview = { _, _, _ -> },
                        onSave = { _, _, _ -> },
                    )
                }
            }
        }

        compose.onNodeWithTag("track-definition-removal-review").assertIsDisplayed()
        val bounds = compose.onNodeWithTag("track-definition-removal-review").getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left <= 321.dp)
        compose.onNodeWithTag("track-definition-removal-impact-list")
            .performScrollToNode(hasText("Historical field 8"))
        compose.onNodeWithText("Historical field 8").assertIsDisplayed()
        compose.onNodeWithText("9 legacy Trigger references", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Apply Reviewed Changes").assertIsDisplayed()
        compose.onNodeWithText("Keep Editing").assertIsDisplayed()
    }

    @Test
    fun savingTrackConsumesBackAndExposesOneAccessibleBlockingSurface() {
        var dismissed = 0
        var saving by mutableStateOf(true)
        val draft = TrackDraft(
            name = "Protected draft",
            fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true)),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEditor(
                    liveInitial = null,
                    targetTrackId = null,
                    openingDraft = draft,
                    routeOpeningBoundary = null,
                    targetName = null,
                    areas = emptyList(),
                    customUnits = emptyList(),
                    defaultAreaId = null,
                    saving = saving,
                    modifier = Modifier,
                    onDismiss = { dismissed++ },
                    onCreateArea = { _, _, _ -> },
                    onCreateCustomUnit = UnavailableCreateCustomUnitAction,
                    onRetryPreparation = {},
                    onReview = { _, _, _ -> },
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onNodeWithContentDescription("Saving Track", substring = true).assertIsDisplayed()
        pressBack()
        compose.runOnIdle { assertEquals(0, dismissed) }
        compose.runOnIdle { saving = false }
        compose.onNodeWithTag("track-editor-name").performTextInput(" changed")
        // The first Back closes the software keyboard; the second reaches the editor.
        pressBack()
        compose.runOnIdle { assertEquals(0, dismissed) }
        pressBack()
        compose.runOnIdle { assertEquals(0, dismissed) }
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
    }

    @Test
    fun missingEditTargetIsNotReinterpretedAsCreateButRecoveryFailureRemainsRetryable() {
        var copiedDraft: TrackDraft? = null
        var persistenceError by mutableStateOf<String?>(null)
        var reviewState by mutableStateOf(
            TrackDefinitionReviewUiState(
                sessionId = 21,
                trackId = 9,
                targetMissing = true,
                conflictKind = TrackDefinitionConflictKind.TargetMissing,
                errorMessage = "This Track is no longer available. Your draft is still here.",
            ),
        )
        val draft = TrackDraft(
            name = "Original Track",
            fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true)),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEditor(
                    liveInitial = null,
                    targetTrackId = 9,
                    openingDraft = draft,
                    routeOpeningBoundary = null,
                    targetName = "Original Track",
                    areas = emptyList(),
                    customUnits = emptyList(),
                    defaultAreaId = null,
                    saving = false,
                    persistenceError = persistenceError,
                    definitionReviewState = reviewState,
                    modifier = Modifier,
                    sessionId = 21,
                    onDismiss = {},
                    onCreateArea = { _, _, _ -> },
                    onCreateCustomUnit = UnavailableCreateCustomUnitAction,
                    onRetryPreparation = {},
                    onReview = { _, _, _ -> },
                    onSave = { _, _, _ -> },
                    onSaveCopy = { copiedDraft = it },
                )
            }
        }

        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithTag("track-definition-conflict").assertIsDisplayed()
        compose.onNodeWithText("Save Draft as New Track").performClick()
        compose.runOnIdle {
            assertEquals(draft.name, copiedDraft?.name)
            assertEquals(draft.fields.map { it.name }, copiedDraft?.fields?.map { it.name })
        }
        compose.runOnIdle { persistenceError = "The draft copy could not be saved." }
        compose.onNodeWithTag("track-persistence-save-problem")
            .assertContentDescriptionContains("draft copy could not be saved", substring = true)

        compose.runOnIdle {
            persistenceError = null
            reviewState = reviewState.copy(
                targetMissing = false,
                conflictKind = null,
                errorMessage = "Whip data is unavailable while recovery is in progress",
            )
        }
        compose.onNodeWithText("Save").assertIsEnabled()
        compose.onNodeWithTag("track-persistence-save-problem")
            .assertContentDescriptionContains("recovery is in progress", substring = true)
    }

    @Test
    fun unavailableEntryRouteExplainsNoMutationAndAlwaysProvidesAnExit() {
        var dismissed = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEntryUnavailableRoute(
                    title = "Entry Unavailable",
                    message = "This Entry is no longer available. It was not reinterpreted as a new Entry.",
                    modifier = Modifier,
                    onDismiss = { dismissed++ },
                )
            }
        }

        compose.onNodeWithTag("track-entry-unavailable").assertIsDisplayed()
        compose.onNodeWithText("This Entry is no longer available", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Close").assertIsDisplayed()
        pressBack()
        compose.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test
    fun asynchronousDefinitionConflictScrollsToAndAnnouncesItsRecoveryAction() {
        val boundary = TrackDefinitionBoundary(9, "track-9", 1, "definition")
        val draft = TrackDraft(
            name = "Long Track",
            fields = (1..12).map { index ->
                TrackFieldDraft(
                    name = "Field $index",
                    type = TrackFieldType.ShortText,
                    primary = index == 1,
                    uuid = "field-$index",
                    id = index.toLong(),
                )
            },
        )
        var reviewState by mutableStateOf(
            TrackDefinitionReviewUiState(
                sessionId = 31,
                trackId = 9,
                boundary = boundary,
            ),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackEditor(
                    liveInitial = null,
                    targetTrackId = 9,
                    openingDraft = draft,
                    routeOpeningBoundary = boundary,
                    targetName = "Long Track",
                    areas = emptyList(),
                    customUnits = emptyList(),
                    defaultAreaId = null,
                    saving = false,
                    definitionReviewState = reviewState,
                    modifier = Modifier,
                    sessionId = 31,
                    onDismiss = {},
                    onCreateArea = { _, _, _ -> },
                    onCreateCustomUnit = UnavailableCreateCustomUnitAction,
                    onRetryPreparation = {},
                    onReview = { _, _, _ -> },
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("track-editor-list").performScrollToNode(hasText("Organization"))
        compose.onNodeWithText("Organization").assertIsDisplayed()
        compose.runOnIdle {
            reviewState = reviewState.copy(
                conflictKind = TrackDefinitionConflictKind.DefinitionChanged,
                errorMessage = "This Track definition changed while this editor was open.",
            )
        }

        compose.onNodeWithTag("track-definition-conflict")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
        compose.onNodeWithText("Save Draft as New Track").assertIsDisplayed()
    }
}
