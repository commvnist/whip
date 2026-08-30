package com.whip.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.PaneAwareAlertDialog
import com.whip.app.ui.WhipChoiceList
import com.whip.app.ui.WhipAdaptiveLayout
import com.whip.app.ui.WhipFoldInfo
import com.whip.app.ui.WhipFoldOrientation
import com.whip.app.ui.WhipSingleChoiceRow
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.WhipTextButton
import com.whip.app.ui.theme.WhipTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Screenshot-backed structural matrix; physical review owns device-specific golden approval. */
@RunWith(AndroidJUnit4::class)
class VisualAcceptanceMatrixTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun primaryAdaptiveThemeFontAndRtlMatrixHasAnOpaqueConformingSurface() {
        val config = mutableStateOf(MatrixCase("initial", WhipAdaptiveLayout.Compact, false, 1f, LayoutDirection.Ltr))
        val baseDensity = compose.density.density
        compose.setContent {
            val current = config.value
            val caseDensity = if (current.layout == WhipAdaptiveLayout.Compact) baseDensity else 1f
            CompositionLocalProvider(
                LocalDensity provides Density(caseDensity, current.fontScale),
                LocalLayoutDirection provides current.direction,
            ) {
                WhipTheme(darkTheme = current.dark, dynamicColor = current.dynamicColor) {
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        adaptiveLayout = current.layout,
                        foldInfo = current.foldInfo,
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }

        matrixCases().forEach { next ->
            compose.runOnIdle { config.value = next }
            compose.waitForIdle()
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            check(pixels.width > 100 && pixels.height > 100) { "${next.name} produced an empty screenshot" }
            val samples = listOf(
                pixels[2, 2],
                pixels[pixels.width - 3, 2],
                pixels[2, pixels.height - 3],
                pixels[pixels.width - 3, pixels.height - 3],
            )
            check(samples.all { it.alpha > 0.99f }) { "${next.name} exposed a transparent window edge: $samples" }
            if (next.dark) {
                check(samples.none { it.luminance() > 0.85f }) { "${next.name} exposed a white edge in dark theme: $samples" }
            }

            val rootBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
            val topBarBounds = compose.onNodeWithTag("workspace-top-app-bar")
                .fetchSemanticsNode().boundsInRoot
            val identityBounds = compose.onNodeWithTag("workspace-header-identity")
                .fetchSemanticsNode().boundsInRoot
            check(rootBounds.fullyContains(topBarBounds)) {
                "${next.name} clipped its workspace top bar: root=$rootBounds topBar=$topBarBounds"
            }
            check(topBarBounds.fullyContains(identityBounds)) {
                "${next.name} clipped its workspace identity: topBar=$topBarBounds identity=$identityBounds"
            }
            val caseDensity = if (next.layout == WhipAdaptiveLayout.Compact) baseDensity else 1f
            check(identityBounds.width > 0f && identityBounds.height >= 48f * caseDensity) {
                "${next.name} produced an unreachable workspace identity: $identityBounds"
            }

            val navigationTag = when (next.layout) {
                WhipAdaptiveLayout.Compact -> "adaptive-bottom-navigation"
                WhipAdaptiveLayout.TabletopFold -> "adaptive-tabletop-navigation"
                else -> "adaptive-navigation-rail"
            }
            val navigationBounds = compose.onNodeWithTag(navigationTag)
                .fetchSemanticsNode().boundsInRoot
            check(rootBounds.fullyContains(navigationBounds)) {
                "${next.name} clipped primary navigation: root=$rootBounds navigation=$navigationBounds"
            }
            check(navigationBounds.width > 0f && navigationBounds.height > 0f) {
                "${next.name} produced zero-size primary navigation: $navigationBounds"
            }

            when (next.layout) {
                WhipAdaptiveLayout.Compact -> check(topBarBounds.bottom <= navigationBounds.top) {
                    "${next.name} overlapped top and bottom chrome: topBar=$topBarBounds navigation=$navigationBounds"
                }
                WhipAdaptiveLayout.TabletopFold -> check(navigationBounds.bottom <= topBarBounds.top) {
                    "${next.name} overlapped tabletop navigation and content: topBar=$topBarBounds navigation=$navigationBounds"
                }
                else -> if (next.direction == LayoutDirection.Ltr) {
                    check(navigationBounds.right <= topBarBounds.left) {
                        "${next.name} overlapped leading rail and workspace chrome: topBar=$topBarBounds navigation=$navigationBounds"
                    }
                } else {
                    check(navigationBounds.left >= topBarBounds.right) {
                        "${next.name} overlapped RTL leading rail and workspace chrome: topBar=$topBarBounds navigation=$navigationBounds"
                    }
                }
            }
        }
    }

    @Test
    fun longChoiceAndNestedDialogStatesKeepTheirFinalItemsAndActionsReachable() {
        val nestedOpen = mutableStateOf(false)
        val twoX = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides twoX) {
                WhipTheme(dynamicColor = false) {
                    PaneAwareAlertDialog(
                        modifier = Modifier.width(300.dp).height(460.dp).testTag("matrix-choice-dialog"),
                        onDismissRequest = {},
                        title = { Text("Choose an option") },
                        text = {
                            WhipChoiceList(Modifier.testTag("matrix-choice-list")) {
                                items(31, key = { it }) { index ->
                                    WhipSingleChoiceRow(
                                        label = "Option ${index + 1}",
                                        selected = index == 0,
                                        onSelect = {},
                                        modifier = Modifier.testTag("matrix-choice-${index + 1}"),
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            WhipTextButton(
                                onClick = { nestedOpen.value = true },
                                modifier = Modifier.testTag("matrix-choice-continue"),
                            ) { Text("Continue") }
                        },
                        dismissButton = {
                            WhipTextButton(onClick = {}) { Text("Cancel") }
                        },
                    )
                    if (nestedOpen.value) {
                        PaneAwareAlertDialog(
                            modifier = Modifier.width(300.dp).testTag("matrix-nested-dialog"),
                            onDismissRequest = { nestedOpen.value = false },
                            title = { Text("Confirm selection") },
                            text = { Text("This second gate must remain readable above the original dialog.") },
                            confirmButton = {
                                WhipTextButton(
                                    onClick = {},
                                    modifier = Modifier.testTag("matrix-nested-confirm"),
                                ) { Text("Confirm") }
                            },
                            dismissButton = {
                                WhipTextButton(
                                    onClick = { nestedOpen.value = false },
                                    modifier = Modifier.testTag("matrix-nested-cancel"),
                                ) { Text("Keep Editing") }
                            },
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("matrix-choice-list").performScrollToIndex(30)
        compose.onNodeWithTag("matrix-choice-31").assertIsDisplayed()
        val parent = compose.onNodeWithTag("matrix-choice-dialog").fetchSemanticsNode().boundsInRoot
        val finalChoice = compose.onNodeWithTag("matrix-choice-31").fetchSemanticsNode().boundsInRoot
        val continueAction = compose.onNodeWithTag("matrix-choice-continue").fetchSemanticsNode().boundsInRoot
        check(parent.fullyContains(finalChoice)) { "Long choice list clipped its final item: $finalChoice inside $parent" }
        check(parent.fullyContains(continueAction)) { "Long choice list displaced its action: $continueAction inside $parent" }

        compose.onNodeWithTag("matrix-choice-continue").performClick()
        compose.onNodeWithTag("matrix-nested-dialog").assertIsDisplayed()
        val nested = compose.onNodeWithTag("matrix-nested-dialog").fetchSemanticsNode().boundsInRoot
        val confirm = compose.onNodeWithTag("matrix-nested-confirm").fetchSemanticsNode().boundsInRoot
        val cancel = compose.onNodeWithTag("matrix-nested-cancel").fetchSemanticsNode().boundsInRoot
        check(nested.fullyContains(confirm)) { "Nested confirmation clipped its primary action: $confirm inside $nested" }
        check(nested.fullyContains(cancel)) { "Nested confirmation clipped its safe action: $cancel inside $nested" }
        check(confirm.height >= 48f * twoX.density && cancel.height >= 48f * twoX.density) {
            "Nested confirmation actions fell below the 48 dp touch target: confirm=$confirm cancel=$cancel"
        }
    }

    private data class MatrixCase(
        val name: String,
        val layout: WhipAdaptiveLayout,
        val dark: Boolean,
        val fontScale: Float,
        val direction: LayoutDirection,
        val foldInfo: WhipFoldInfo? = null,
        val dynamicColor: Boolean = false,
    )

    private fun matrixCases() = listOf(
        MatrixCase("compact-light-100", WhipAdaptiveLayout.Compact, false, 1f, LayoutDirection.Ltr),
        MatrixCase("compact-dark-200-rtl", WhipAdaptiveLayout.Compact, true, 2f, LayoutDirection.Rtl),
        MatrixCase("compact-light-320", WhipAdaptiveLayout.Compact, false, 3.2f, LayoutDirection.Ltr),
        MatrixCase("rail-light-150", WhipAdaptiveLayout.ExpandedDashboard, false, 1.5f, LayoutDirection.Ltr),
        MatrixCase("rail-dark-200-rtl", WhipAdaptiveLayout.ExpandedDashboard, true, 2f, LayoutDirection.Rtl),
        MatrixCase("rail-dynamic-light-130", WhipAdaptiveLayout.ExpandedDashboard, false, 1.3f, LayoutDirection.Ltr, dynamicColor = true),
        MatrixCase(
            "book-dark-130",
            WhipAdaptiveLayout.BookFold,
            true,
            1.3f,
            LayoutDirection.Ltr,
            WhipFoldInfo(WhipFoldOrientation.Vertical, 700, 0, 740, 1_800, separating = true, halfOpened = true),
        ),
        MatrixCase(
            "book-light-320",
            WhipAdaptiveLayout.BookFold,
            false,
            3.2f,
            LayoutDirection.Ltr,
            WhipFoldInfo(WhipFoldOrientation.Vertical, 700, 0, 740, 1_800, separating = true, halfOpened = true),
        ),
        MatrixCase(
            "tabletop-light-200-rtl",
            WhipAdaptiveLayout.TabletopFold,
            false,
            2f,
            LayoutDirection.Rtl,
            WhipFoldInfo(WhipFoldOrientation.Horizontal, 0, 700, 1_800, 740, separating = true, halfOpened = true),
        ),
    )

    private fun Rect.fullyContains(other: Rect): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom
}
