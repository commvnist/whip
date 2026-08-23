package com.whip.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhipVisualLanguageTest {
    @Test
    fun equivalentHighlightsUseOneAccentFamily() {
        val source = lightColorScheme(
            primary = Color.Red,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDDDD),
            onPrimaryContainer = Color(0xFF330000),
            secondary = Color.Green,
            tertiary = Color.Blue,
        )

        val unified = source.withUnifiedHighlights()

        assertEquals(unified.primary, unified.secondary)
        assertEquals(unified.primary, unified.tertiary)
        assertEquals(unified.primaryContainer, unified.secondaryContainer)
        assertEquals(unified.primaryContainer, unified.tertiaryContainer)
        assertEquals(unified.onPrimaryContainer, unified.onSecondaryContainer)
        assertEquals(unified.onPrimaryContainer, unified.onTertiaryContainer)
        assertEquals(source.outlineVariant, unified.outlineVariant)
        assertEquals(source.error, unified.error)
    }

    @Test
    fun componentShapesUseRestrainedRectangularCorners() {
        assertEquals(RoundedCornerShape(4.dp), WhipShapes.extraSmall)
        assertEquals(RoundedCornerShape(6.dp), WhipShapes.small)
        assertEquals(RoundedCornerShape(8.dp), WhipShapes.medium)
        assertEquals(RoundedCornerShape(10.dp), WhipShapes.large)
        assertEquals(RoundedCornerShape(12.dp), WhipShapes.extraLarge)
    }

    @Test
    fun screensCannotBypassSharedRectangularActionControls() {
        val sourceRoot = sequenceOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory)
            ?: error("Unable to locate app source root")
        val forbidden = listOf(
            "androidx.compose.material3.Button",
            "androidx.compose.material3.OutlinedButton",
            "androidx.compose.material3.TextButton",
            "androidx.compose.material3.FilterChip",
            "androidx.compose.material3.NavigationBarItem",
            "androidx.compose.material3.NavigationRailItem",
        )
        val violations = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "WhipControls.kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    forbidden.firstOrNull { component ->
                        line.trim() == "import $component" || line.trim().startsWith("import $component as ")
                    }
                        ?.let { "${file.relativeTo(sourceRoot)}:${index + 1} imports $it" }
                }
            }
            .toList()

        assertTrue("Screens bypass shared controls:\n${violations.joinToString("\n")}", violations.isEmpty())
    }
}
