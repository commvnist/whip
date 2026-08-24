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

    @Test
    fun scrollingDestinationNavigationDoesNotDrawEdgeFadesOrShadows() {
        val sourceRoot = sequenceOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory)
            ?: error("Unable to locate app source root")
        val controls = File(sourceRoot, "com/whip/app/ui/ItemControlPatterns.kt").readText()
        val destinationBar = controls.substringAfter("internal fun <T> DestinationTabBar(")
            .substringBefore("internal fun <T> SegmentedChoiceBar(")
        val forbiddenTreatments = listOf(
            "Brush.",
            "canScrollBackward",
            "canScrollForward",
            ".shadow(",
        ).filter(destinationBar::contains)

        assertTrue(
            "Scrollable destination navigation draws edge treatments: $forbiddenTreatments",
            forbiddenTreatments.isEmpty(),
        )
    }

    @Test
    fun workflowDestinationsStayTogetherAndInsightsAlwaysEndsTheVisibleNavigation() {
        val sourceRoot = sequenceOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory)
            ?: error("Unable to locate app source root")
        val habit = File(sourceRoot, "com/whip/app/ui/HabitScreens.kt").readText()
        val goal = File(sourceRoot, "com/whip/app/ui/GoalScreens.kt").readText()
        val track = File(sourceRoot, "com/whip/app/ui/TrackScreens.kt").readText()

        assertTrue(habit.contains("enum class HabitDestination { Today, All, Connections, Archived, Insights }"))
        assertTrue(habit.contains("listOf(HabitDestination.Today, HabitDestination.All, HabitDestination.Insights)"))
        assertTrue(goal.contains("enum class GoalDestination { Active, Completed, Archived, Insights }"))
        assertTrue(goal.contains("listOf(GoalDestination.Active, GoalDestination.Completed, GoalDestination.Insights)"))
        assertTrue(track.contains("Entries(\"Entries\"),\n    Automations(\"Automations\"),\n    Options(\"Options\"),\n    Insights(\"Insights\")"))
        assertTrue(track.contains("listOf(TrackDetailDestination.Entries, TrackDetailDestination.Automations, TrackDetailDestination.Insights)"))
        assertTrue(goal.contains("GoalDestination.Completed) \"Done\""))
        assertTrue(track.contains("TrackDetailDestination.Automations) \"Auto\""))
    }
}
