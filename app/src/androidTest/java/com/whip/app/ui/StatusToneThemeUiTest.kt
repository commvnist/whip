package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusToneThemeUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyTypedStatusToneRemainsVisuallyDistinctWithIdenticalCopyAcrossThemes() {
        val theme = mutableStateOf(ThemeCase(dark = false, dynamic = false))
        compose.setContent {
            WhipTheme(darkTheme = theme.value.dark, dynamicColor = theme.value.dynamic) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WhipStatusTone.entries.forEach { tone ->
                        WhipStatusBadge(
                            label = "Shared status",
                            tone = tone,
                            modifier = Modifier.testTag("status-tone-${tone.name}"),
                        )
                    }
                }
            }
        }

        listOf(
            ThemeCase(dark = false, dynamic = false),
            ThemeCase(dark = true, dynamic = false),
            ThemeCase(dark = false, dynamic = true),
        ).forEach { case ->
            compose.runOnIdle { theme.value = case }
            val dominantColors = WhipStatusTone.entries.map { tone ->
                val node = compose.onNodeWithTag("status-tone-${tone.name}").assertIsDisplayed()
                val pixels = node.captureToImage().toPixelMap()
                buildList {
                    for (x in 0 until pixels.width) {
                        for (y in 0 until pixels.height) add(pixels[x, y].toArgb())
                    }
                }.groupingBy { it }.eachCount().maxBy { it.value }.key
            }
            assertTrue(
                "Typed status tones must not collapse into copy-derived styling in $case: $dominantColors",
                dominantColors.distinct().size >= 4,
            )
        }
    }

    private data class ThemeCase(val dark: Boolean, val dynamic: Boolean)
}
