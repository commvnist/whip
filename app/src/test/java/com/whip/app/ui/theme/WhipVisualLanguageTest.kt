package com.whip.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhipVisualLanguageTest {
    @Test
    fun brandInkAndWarmWhiteHaveStrongTextContrast() {
        val lighter = WhipWarmWhite.luminance() + 0.05f
        val darker = WhipInk.luminance() + 0.05f

        assertEquals(Color(0xFF090909), WhipInk)
        assertEquals(Color(0xFFF5F3EA), WhipWarmWhite)
        assertTrue("Brand text contrast must meet WCAG AA", lighter / darker >= 4.5f)
        assertTrue("The fixed action color must remain tied to Whip ink", WhipPurple == WhipInk)
        assertTrue("Semantic accents must not collapse into the action color", WhipGreen != WhipPurple)
        assertTrue("Semantic accents must remain distinguishable", WhipAmber != WhipGreen)
    }

    @Test
    fun semanticRolesRemainDistinctAndMapToTheirMaterialMeaning() {
        val source = lightColorScheme(
            primary = Color.Red,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDDDD),
            onPrimaryContainer = Color(0xFF330000),
            secondary = Color.Green,
            tertiary = Color.Blue,
        )

        val semantic = source.toWhipSemanticColors()

        assertEquals(source.primary, semantic.action)
        assertEquals(source.primaryContainer, semantic.selection)
        assertEquals(source.secondary, semantic.success)
        assertEquals(source.tertiary, semantic.warning)
        assertEquals(source.error, semantic.destructive)
        assertEquals(source.onSurfaceVariant, semantic.metadata)
        assertTrue(semantic.action != semantic.success)
        assertTrue(semantic.action != semantic.warning)
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
    fun destinationNavigationKeepsEveryPeerDirectAndStable() {
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
            "stringResource(R.string.action_more)",
            "Icons.Outlined.ArrowDropDown",
        ).filter(destinationBar::contains)

        assertTrue(
            "Destination navigation reintroduced ambiguous or decorative overflow: $forbiddenTreatments",
            forbiddenTreatments.isEmpty(),
        )
        assertTrue(destinationBar.contains("destinations.forEach"))
        assertFalse(destinationBar.contains("WhipOverflowMenu("))
        assertFalse(destinationBar.contains("primaryDestinations.filter"))
        assertFalse(destinationBar.contains("pagesExpanded"))
        assertTrue(destinationBar.contains("horizontalScroll"))
        assertTrue(destinationBar.contains("bringIntoViewRequester"))
        assertTrue(destinationBar.contains("TextOverflow.Clip"))
    }

    @Test
    fun workspaceDefinitionsRetainAllPeerDestinationsAsDirectNavigation() {
        val sourceRoot = sequenceOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory)
            ?: error("Unable to locate app source root")
        val habit = File(sourceRoot, "com/whip/app/ui/HabitScreens.kt").readText()
        val goal = File(sourceRoot, "com/whip/app/ui/GoalScreens.kt").readText()
        val track = File(sourceRoot, "com/whip/app/ui/TrackScreens.kt").readText()
        val taskPolicy = File(sourceRoot, "com/whip/app/ui/TaskWorkspacePolicy.kt").readText()
        val inspector = File(sourceRoot, "com/whip/app/ui/EntityInspector.kt").readText()

        assertTrue(habit.contains("enum class HabitDestination"))
        assertTrue(habit.contains("destinations = HabitDestination.entries"))
        assertTrue(goal.contains("enum class GoalDestination { Active, Completed, Archived, Insights }"))
        assertTrue(goal.contains("destinations = GoalDestination.entries"))
        assertTrue(track.contains("Entries(\"Entries\"),\n    Options(\"Options\"),\n    Insights(\"Insights\")"))
        assertTrue(track.contains("destinations = TrackDetailDestination.entries"))
        assertTrue(goal.contains("GoalDestination.Completed -> \"History\""))
        assertTrue(track.contains("compactLabel = TrackDetailDestination::label"))
        assertTrue(taskPolicy.contains("TaskWorkspaceDestination.History"))
        assertTrue(taskPolicy.contains("allTaskWorkspaceDestinations = primaryTaskWorkspaceDestinations"))
        assertTrue(inspector.contains("destinations = sections"))
        assertFalse(inspector.contains("sections.filter { it.placement"))
    }
}
