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

        val taskBulkEditor = app.substringAfter("private fun TaskBulkEditDialog(")
            .substringBefore("private fun ScheduledTask.matches(")
        assertTrue(
            "Task bulk editing must use the same trailing toggle rows as the rest of Whip",
            Regex("WhipSettingsRow\\(").findAll(taskBulkEditor).count() == 2,
        )
        assertFalse(
            "Task bulk editing must not reintroduce leading one-off checkboxes",
            taskBulkEditor.contains("Checkbox("),
        )
        val taskSelectionActions = app.substringAfter(".testTag(\"task-selection-actions\")")
            .substringBefore("if (reordering) {")
        assertTrue(
            "Low-frequency Task selection commands must share one overflow menu",
            taskSelectionActions.contains("WhipOverflowMenu(") &&
                taskSelectionActions.contains("label = \"Archive\"") &&
                taskSelectionActions.contains("label = \"Delete Permanently\"") &&
                taskSelectionActions.contains("role = WhipMenuItemRole.Destructive"),
        )
        assertFalse(
            "Task selection must not expose permanent deletion as a peer button",
            taskSelectionActions.contains("WhipOutlinedButton(\n                                enabled = selectedItems.isNotEmpty(),\n                                onClick = {\n                                    val ids"),
        )
        val taskPageActions = app.substringAfter("supportingText = taskDestinationSupportingText")
            .substringBefore("if (selectionMode) {")
        assertTrue(
            "Task page actions must use the same icon-action anchor as Habits, Goals, and Tracks",
            taskPageActions.contains("label = \"More task list actions\"") &&
                Regex("WhipPageIconAction\\(").findAll(taskPageActions).count() == 2,
        )
    }

    @Test
    fun fullScreenEditorsUseOneAdaptiveChromeAndPrimaryActionHierarchy() {
        val patterns = File(sourceRoot, "com/whip/app/ui/WhipPagePatterns.kt").readText()
        val productivity = File(sourceRoot, "com/whip/app/ui/ProductivityEditorComponents.kt").readText()
        val tasks = File(sourceRoot, "com/whip/app/ui/TaskEditorDialog.kt").readText()
        val tracks = File(sourceRoot, "com/whip/app/ui/TrackScreens.kt").readText()
        val routines = File(sourceRoot, "com/whip/app/ui/RoutineBuilder.kt").readText()
        val gym = File(sourceRoot, "com/whip/app/ui/GymScreens.kt").readText()

        listOf(productivity, tasks, tracks, routines).forEach { source ->
            assertTrue("A primary editor bypasses WhipEditorHeader", source.contains("WhipEditorHeader("))
        }
        assertTrue(patterns.contains("maxWidth < 360.dp * fontScale"))
        assertTrue(patterns.contains("heightIn(min = 48.dp)"))
        assertTrue("Track editors must not preserve a separate TopAppBar design", !tracks.contains("TopAppBar("))
        assertTrue("Routine outline must have one unambiguous exit", routines.contains("WhipTrailingCloseAction("))
        assertTrue("Nested routine pages must navigate back to the outline", routines.contains("WhipBackAction(label = \"Back to routine outline\""))
        assertTrue("Primary Gym editors must use the filled save hierarchy", gym.contains("confirmButton = {\n            WhipButton("))
        assertTrue("Primary Gym editors must use the same icon exit contract", gym.contains("WhipTrailingCloseAction("))
    }

    @Test
    fun userFacingChoicesUseExplicitLabelsAndSharedSectionHierarchy() {
        val settingsModel = File(sourceRoot, "com/whip/app/core/AppSettings.kt").readText()
        val taskModel = File(sourceRoot, "com/whip/app/domain/TaskModels.kt").readText()
        val measurementModel = File(sourceRoot, "com/whip/app/domain/MeasurementModels.kt").readText()
        val gymModel = File(sourceRoot, "com/whip/app/domain/GymModels.kt").readText()
        val habits = File(sourceRoot, "com/whip/app/ui/HabitScreens.kt").readText()
        val firstRun = File(sourceRoot, "com/whip/app/ui/FirstRunSetupDialog.kt").readText()
        val review = File(sourceRoot, "com/whip/app/ui/ReviewDialog.kt").readText()
        val settings = File(sourceRoot, "com/whip/app/ui/SettingsScreens.kt").readText()
        val tasks = File(sourceRoot, "com/whip/app/ui/TaskComponents.kt").readText()
        val gym = File(sourceRoot, "com/whip/app/ui/GymScreens.kt").readText()

        listOf(
            "enum class AppThemeMode(val label: String)",
            "enum class HomeSection(val label: String)",
            "enum class ReviewSection(val label: String)",
            "enum class HealthDataType(val label: String)",
            "enum class ReviewPeriod(val label: String)",
        ).forEach { contract -> assertTrue("Settings choices are missing $contract", settingsModel.contains(contract)) }
        assertTrue(taskModel.contains("enum class TaskPriority(val label: String)"))
        assertTrue(measurementModel.contains("enum class UnitDimension(val label: String)"))
        assertTrue(gymModel.contains("enum class WorkoutSessionState(val label: String)"))
        assertTrue(gymModel.contains("enum class WorkoutGroupType(val label: String)"))
        assertTrue(habits.contains("enum class HabitDestination(val label: String)"))
        assertFalse(firstRun.contains("Text(section.name)"))
        assertFalse(review.contains("Text(value.name)"))
        assertFalse(review.contains("Text(section.name)"))
        assertFalse(settings.contains("Show ${'$'}{section.name} on Home"))
        assertFalse(settings.contains("${'$'}{type.name} · sync paused"))
        assertFalse(tasks.contains("it.name.take(3).lowercase()"))
        assertFalse(habits.contains("label = { it.name }"))
        assertFalse(habits.contains("day.name.take(2)"))
        assertTrue("Settings subsections must use the shared editor hierarchy", settings.contains("EditorSectionHeader(text)"))
        assertTrue("Gym navigation must use explicit UI labels", gym.contains("label = GymDestination::label"))
        listOf("Resistance", "Exercises (Optional)", "Explore a Trend", "1RM and Percentage Calculator", "Plate Calculator")
            .forEach { heading ->
                assertTrue("Gym heading '$heading' is missing", gym.contains("\"$heading\""))
                assertFalse("Gym heading '$heading' bypasses EditorSectionHeader", gym.contains("Text(\"$heading\""))
            }
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
        assertTrue(tracks.contains("TrackWorkspaceDestination.Archived"))
        assertTrue(tracks.contains("track-detail-navigation"))
        assertTrue("Gym must remain global instead of receiving a fake Area scope", app.contains("appDestination in setOf(AppDestination.Home, AppDestination.Tasks, AppDestination.Habits, AppDestination.Goals, AppDestination.Tracks)"))
    }

    @Test
    fun sharedEmptyStateAndCollectionSpacingOwnWorkspaceVerticalRhythm() {
        val app = File(sourceRoot, "com/whip/app/ui/WhipApp.kt").readText()
        val habits = File(sourceRoot, "com/whip/app/ui/HabitScreens.kt").readText()
        val goals = File(sourceRoot, "com/whip/app/ui/GoalScreens.kt").readText()
        val tracks = File(sourceRoot, "com/whip/app/ui/TrackScreens.kt").readText()
        val gym = File(sourceRoot, "com/whip/app/ui/GymScreens.kt").readText()
        val patterns = File(sourceRoot, "com/whip/app/ui/WhipPagePatterns.kt").readText()

        val trackLines = tracks.lines()
        val locallyPaddedEmptyStates = trackLines.mapIndexedNotNull { index, line ->
            if (
                line.contains("padding(vertical =") &&
                trackLines.subList((index - 12).coerceAtLeast(0), index + 1).any { it.contains("WhipEmptyState(") }
            ) index + 1 else null
        }
        assertTrue(
            "Track empty states must not stack local vertical padding on WhipEmptyState's shared rhythm: $locallyPaddedEmptyStates",
            locallyPaddedEmptyStates.isEmpty(),
        )
        listOf(
            app to "if (appSettings.compactItemLayout) WhipSpacing.micro else WhipSpacing.compact",
            habits to "if (compact) WhipSpacing.micro else WhipSpacing.compact",
            goals to "if (compactItemLayout) WhipSpacing.micro else WhipSpacing.compact",
            tracks to "if (userCompact) WhipSpacing.micro else WhipSpacing.compact",
        ).forEach { (source, sharedSpacing) ->
            assertTrue(
                "Every first-class collection must use the shared compact and standard item gaps",
                source.contains(sharedSpacing),
            )
        }
        assertTrue(app.contains("private fun SupportPaneEmptyMessage("))
        assertTrue(app.contains("private fun SupportPaneDescription("))
        assertTrue(app.contains("text = stringResource(R.string.support_tracks_empty)"))
        assertTrue(app.contains("text = stringResource(R.string.support_gym_empty)"))
        assertFalse(app.contains("WhipEmptyState(\n                stringResource(R.string.support_tracks_empty"))
        assertTrue("Page headers must reserve a stable two-line supporting-text rhythm", patterns.contains("minLines = 2"))
        assertFalse(
            "Gym pages must not bypass the shared collection gap",
            Regex(
                "contentPadding\\s*=\\s*WhipPageContentPadding,\\s*" +
                    "verticalArrangement\\s*=\\s*Arrangement\\.spacedBy\\((10|12)\\.dp\\)",
            ).containsMatchIn(gym),
        )
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
        assertTrue(
            "A one-section inspector must not render a redundant selector",
            inspector.contains("if (sections.size > 1)"),
        )
        assertFalse(
            "Entity inspector controls must not expose compatibility labels through zero-size text",
            inspector.contains("Text(editLabel, modifier = Modifier.size(0.dp))") ||
                inspector.contains("Text(section.connectedLabel, modifier = Modifier.size(0.dp))") ||
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
                "label = if (gymState.activeSession == null) \"Start Workout\" else \"Add Exercise to This Workout\"",
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

        val gymScreens = File(sourceRoot, "com/whip/app/ui/GymScreens.kt").readText()
        val picker = File(sourceRoot, "com/whip/app/ui/GymExercisePicker.kt").readText()
        assertTrue("Exercise pickers must share the bounded exercise-specific body", picker.contains("fun GymExercisePickerBody("))
        assertTrue(
            "Single-select and Machine linked exercises must both consume the shared picker",
            Regex("GymExercisePickerBody\\(").findAll(gymScreens).count() == 2,
        )
        assertTrue("The rest execution lane must use the canonical collection card", gymScreens.contains("WhipCollectionCard(\n                modifier = Modifier.testTag(\"workout-execution-lane\")"))
        assertTrue("Collection cards must lock their canonical shape", patterns.contains("val shape = MaterialTheme.shapes.medium"))
    }
}
