package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiDesignArchitectureTest {
    private val sourceRoot = sequenceOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory)
        ?: error("Unable to locate app source root")

    private fun uiFiles() = File(sourceRoot, "com/whip/app/ui")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }

    @Test
    fun transientSurfacesCannotBypassPaneAwareDialogPlacement() {
        val rawDialog = Regex("(^|\\s)AlertDialog\\(")
        val violations = uiFiles().flatMap { file ->
            file.readLines().asSequence().mapIndexedNotNull { index, line ->
                if (rawDialog.containsMatchIn(line)) "${file.name}:${index + 1}" else null
            }
        }.toList()

        assertTrue("Raw AlertDialogs can cross a Fold hinge: $violations", violations.isEmpty())
    }

    @Test
    fun interactiveUiDoesNotReintroduceSub48DpTargets() {
        val forbidden = listOf("size(44.dp)", "height(44.dp)", "heightIn(min = 44.dp)")
        val violations = uiFiles().flatMap { file ->
            file.readLines().asSequence().mapIndexedNotNull { index, line ->
                forbidden.firstOrNull(line::contains)?.let { "${file.name}:${index + 1} uses $it" }
            }
        }.toList()

        assertTrue("Touch targets below 48 dp: $violations", violations.isEmpty())
    }

    @Test
    fun editorHierarchyAndLocalizedChromeRemainFirstClassPatterns() {
        val task = File(sourceRoot, "com/whip/app/ui/TaskEditorDialog.kt").readText()
        val habit = File(sourceRoot, "com/whip/app/ui/HabitScreens.kt").readText()
        val goal = File(sourceRoot, "com/whip/app/ui/GoalScreens.kt").readText()
        val app = File(sourceRoot, "com/whip/app/ui/WhipApp.kt").readText()

        listOf("Basics", "Schedule", "Organization", "Planning").forEach { section ->
            assertTrue("Task editor is missing $section hierarchy", task.contains("EditorSectionHeader(\"$section\""))
        }
        listOf("Basics", "Tracking", "Schedule", "Organization").forEach { section ->
            assertTrue("Habit editor is missing $section hierarchy", habit.contains("EditorSectionHeader(\"$section\""))
        }
        listOf("Basics", "Target", "Schedule", "Organization").forEach { section ->
            assertTrue("Goal editor is missing $section hierarchy", goal.contains("EditorSectionHeader(\"$section\""))
        }
        assertTrue(app.contains("stringResource(R.string.nav_home)"))
        assertTrue(app.contains("pluralStringResource(R.plurals.entry_count"))
    }

    @Test
    fun themeDoesNotCollapseSuccessWarningAndActionIntoOneAccent() {
        val theme = File(sourceRoot, "com/whip/app/ui/theme/Theme.kt").readText()
        assertFalse(theme.contains("withUnifiedHighlights"))
        assertTrue(theme.contains("success = secondary"))
        assertTrue(theme.contains("warning = tertiary"))
        assertTrue(theme.contains("destructive = error"))
    }

    @Test
    fun everyFirstClassWorkspaceUsesTheSharedHeaderAndDestinationContract() {
        val app = File(sourceRoot, "com/whip/app/ui/WhipApp.kt").readText()
        val tasks = app
        val habits = File(sourceRoot, "com/whip/app/ui/HabitScreens.kt").readText()
        val goals = File(sourceRoot, "com/whip/app/ui/GoalScreens.kt").readText()
        val tracks = File(sourceRoot, "com/whip/app/ui/TrackScreens.kt").readText()
        val gym = File(sourceRoot, "com/whip/app/ui/GymScreens.kt").readText()

        assertTrue(app.contains("workspace-top-app-bar"))
        assertTrue(app.contains("workspace-header-identity"))
        assertTrue(tasks.contains("task-workspace-navigation"))
        assertTrue(habits.contains("habit-workspace-navigation"))
        assertTrue(goals.contains("goal-workspace-navigation"))
        assertTrue(tracks.contains("track-workspace-navigation"))
        assertTrue(gym.contains("gym-workspace-navigation"))
        assertTrue(tracks.contains("TrackWorkspaceDestination.Activity"))
        assertTrue(tracks.contains("TrackWorkspaceDestination.Insights"))
        assertTrue(tracks.contains("track-detail-navigation"))
        assertTrue("Gym must remain global instead of receiving a fake Area scope", app.contains("appDestination in setOf(AppDestination.Home, AppDestination.Tasks, AppDestination.Habits, AppDestination.Goals, AppDestination.Tracks)"))
    }
}
