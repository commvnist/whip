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

    @Test
    fun persistentNavigationRailDoesNotMoveWithTheKeyboardInset() {
        val app = File(sourceRoot, "com/whip/app/ui/WhipApp.kt").readText()
        val rail = app.substringAfter("private fun WhipNavigationRail(")
            .substringBefore("private fun WhipBrandMark(")

        assertTrue(
            "The persistent rail must remember its largest available height so the IME cannot recenter it",
            rail.contains("stableRailHeight") && rail.contains("stableTopOffset"),
        )
        assertFalse(
            "The persistent rail must not apply an IME-derived inset",
            rail.contains("imePadding()") || rail.contains("windowInsetsPadding(WindowInsets.safeDrawing)"),
        )
    }

    @Test
    fun entityDetailsUseOneAdaptiveInspectorContract() {
        val inspector = File(sourceRoot, "com/whip/app/ui/EntityInspector.kt").readText()
        val tasks = File(sourceRoot, "com/whip/app/ui/TaskComponents.kt").readText()
        val habits = File(sourceRoot, "com/whip/app/ui/HabitScreens.kt").readText()
        val goals = File(sourceRoot, "com/whip/app/ui/GoalScreens.kt").readText()
        val tracks = File(sourceRoot, "com/whip/app/ui/TrackScreens.kt").readText()
        val gym = File(sourceRoot, "com/whip/app/ui/GymScreens.kt").readText()

        listOf(
            "entity-inspector-header",
            "entity-inspector-status",
            "entity-inspector-section-selector",
            "entity-inspector-primary-",
            "entity-inspector-danger-zone",
            "LocalWhipDialogPlacement.current",
            "absoluteOffset(x = placement.offsetX)",
        ).forEach { contract ->
            assertTrue("Entity inspector is missing $contract", inspector.contains(contract))
        }
        assertTrue(
            "Entity inspector content must fill a stable center lane instead of resizing the dialog per section",
            inspector.contains("val inspectorHeight = minOf(maxHeight * 0.94f, 720.dp)") &&
                inspector.contains(".height(inspectorHeight)") &&
                inspector.contains(".weight(1f)"),
        )
        assertTrue(
            "Entity inspector sections must use Whip's adaptive destination contract",
            inspector.contains("DestinationTabBar("),
        )
        assertFalse(
            "Entity inspector controls must not expose compatibility labels through zero-size text",
            inspector.contains("Text(editLabel, modifier = Modifier.size(0.dp))") ||
                inspector.contains("Text(section.legacyLabel, modifier = Modifier.size(0.dp))") ||
                inspector.contains("Text(\"Close\", modifier = Modifier.size(0.dp))"),
        )
        listOf(tasks, habits, goals, tracks, gym).forEach { source ->
            assertTrue("A first-class entity detail surface bypasses EntityInspector", source.contains("EntityInspector("))
        }
    }

    @Test
    fun workspaceCreationHasOneStableTopBarHostAndNoFab() {
        val app = File(sourceRoot, "com/whip/app/ui/WhipApp.kt").readText()
        val pagePatterns = File(sourceRoot, "com/whip/app/ui/WhipPagePatterns.kt").readText()

        assertTrue(app.contains("workspace-add-action"))
        assertFalse(app.contains("FloatingActionButton"))
        assertFalse(app.contains("floatingActionButton ="))
        assertTrue(app.contains("adaptiveLayout == WhipAdaptiveLayout.Compact ||"))
        assertTrue(pagePatterns.contains("bottom = WhipSpacing.screenExpanded"))
        val createShortcut = app.substringAfter("Key.N -> {").substringBefore("Key.H ->")
        assertTrue("Ctrl+N must invoke the same contextual resolver as the visible Add control", createShortcut.contains("triggerAdd()"))
        assertFalse("Ctrl+N must not maintain a second Gym creation route", createShortcut.contains("gymAddExpanded = true"))
        assertFalse("Ctrl+N must not navigate away from Settings", createShortcut.contains("appDestination = AppDestination.Home"))

        val gymLibraryAddMenu = app.substringAfter("DropdownMenu(expanded = gymAddExpanded")
            .substringBefore("supportsPaneExpansion")
        assertTrue(
            gymLibraryAddMenu.contains(
                "label = if (gymState.activeSession == null) \"Start Workout\" else \"Add to Workout\"",
            ),
        )
        listOf("Routine", "Exercise", "Machine", "Category").forEach { item ->
            assertTrue("Gym Library Add is missing $item", gymLibraryAddMenu.contains("label = \"New $item\""))
        }
        assertTrue(app.contains("GymDestination.Library -> \"Add workout, routine, exercise, machine, or category\""))
        assertTrue("Focused collection modes must suppress all shell creation", app.contains("taskSelectionMode = focusedCollectionMode"))

        val quickCapture = app.substringAfter("fun submitQuickCapture()")
            .substringBefore("fun finishSelection()")
        assertTrue(quickCapture.contains("areaScope.requiresExplicitCreationArea(availableAreas)"))
        assertTrue(quickCapture.contains("onAddDetails(submittedQuickCapture)"))
        assertFalse(quickCapture.contains("availableAreas.firstOrNull()?.id"))
    }

    @Test
    fun routineSuccessFeedbackIsQuietByDefaultAcrossEveryWorkspace() {
        val core = File(sourceRoot, "com/whip/app/core/AppRuntime.kt").readText()
        assertTrue(core.contains("feedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline"))

        listOf("TaskViewModel.kt", "HabitViewModel.kt", "GoalViewModel.kt", "GymViewModel.kt", "TrackViewModel.kt")
            .forEach { fileName ->
                val source = File(sourceRoot, "com/whip/app/ui/$fileName").readText()
                assertTrue(
                    "$fileName must make routine success quiet unless a call explicitly opts into a snackbar",
                    source.contains("successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Inline"),
                )
                assertFalse(
                    "$fileName must not make snackbars the default success behavior",
                    source.contains("successFeedbackPresentation: OperationFeedbackPresentation = OperationFeedbackPresentation.Snackbar"),
                )
            }
    }

    @Test
    fun primaryWorkspaceSwitchingPreservesEachWorkspaceContext() {
        val app = File(sourceRoot, "com/whip/app/ui/WhipApp.kt").readText()
        val switcher = app.substringAfter("fun selectPrimaryDestination(destination: AppDestination)")
            .substringBefore("val collectionStatusNowMillis")

        listOf(
            "taskDestination =",
            "habitDestinationState.value =",
            "goalDestinationState.value =",
            "trackWorkspaceDestinationState.value =",
            "trackDetailDestinationState.value =",
            "selectedTrackState.value =",
            "gymDestination =",
        ).forEach { reset ->
            assertFalse("Primary navigation must not reset its workspace with $reset", switcher.contains(reset))
        }
        assertTrue(switcher.contains("appDestination = destination"))
        assertTrue(
            "Disposed workspace subtrees must retain filters, scroll-adjacent UI state, and view choices",
            app.contains("rememberSaveableStateHolder()") &&
                app.contains("SaveableStateProvider(appDestination.name)"),
        )
    }

    @Test
    fun statusColorAndTypographyAreExplicitDesignSystemContracts() {
        val inspector = File(sourceRoot, "com/whip/app/ui/EntityInspector.kt").readText()
        val type = File(sourceRoot, "com/whip/app/ui/theme/Type.kt").readText()

        assertTrue(inspector.contains("enum class WhipStatusTone"))
        assertTrue(inspector.contains("statusTone: WhipStatusTone"))
        assertFalse("Status color must not be inferred from translated copy", inspector.contains("status.lowercase()"))
        listOf("titleSmall =", "bodySmall =", "labelMedium =", "labelSmall =").forEach { role ->
            assertTrue("Typography is missing an explicit $role role", type.contains(role))
        }

        val patterns = File(sourceRoot, "com/whip/app/ui/WhipPagePatterns.kt").readText()
        listOf("WhipCollectionCard", "WhipMetricTile", "WhipNoticeCard", "WhipSettingsSectionCard")
            .forEach { primitive ->
                assertTrue("The design system is missing its canonical $primitive role", patterns.contains("fun $primitive("))
            }
        assertTrue("Notices must express semantic tone instead of local color guesses", patterns.contains("enum class WhipNoticeTone"))
    }
}
