package com.whip.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.WhipAdaptiveLayout
import com.whip.app.ui.WhipFoldInfo
import com.whip.app.ui.WhipFoldOrientation
import com.whip.app.ui.WhipScreen
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
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity, current.fontScale),
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
}
