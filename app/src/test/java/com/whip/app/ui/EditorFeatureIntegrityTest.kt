package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorFeatureIntegrityTest {
    private val uiRoot: File = sequenceOf(
        File("src/main/java/com/whip/app/ui"),
        File("app/src/main/java/com/whip/app/ui"),
    ).firstOrNull(File::isDirectory) ?: error("Unable to locate UI source root")
    private val docsRoot: File = sequenceOf(
        File("docs"),
        File("../docs"),
    ).firstOrNull(File::isDirectory) ?: error("Unable to locate documentation root")

    @Test
    fun colorControlsExistOnlyForAFeatureThatRendersItsColor() {
        val callers = uiRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "WhipColorPicker.kt" }
            .filter { it.readText().contains("WhipColorField(") }
            .map(File::getName)
            .toList()

        assertEquals(listOf("AreaPicker.kt"), callers)
        val renderedAreaColor = File(uiRoot, "AreaPicker.kt").readText()
        assertTrue(renderedAreaColor.contains("area?.colorArgb"))
    }

    @Test
    fun customUnitGuidanceNamesTheRealOwnerAndEditorsCreateUnitsInContext() {
        val settings = File(uiRoot, "SettingsScreens.kt").readText()
        val habit = File(uiRoot, "HabitScreens.kt").readText()
        val goal = File(uiRoot, "GoalScreens.kt").readText()
        val track = File(uiRoot, "TrackScreens.kt").readText()
        val combined = listOf(settings, habit, goal, track).joinToString("\n")

        assertTrue(settings.contains("Planning & Units"))
        assertTrue(settings.contains("Habit Defaults"))
        assertTrue(settings.contains("Habit measurements, Goal values, and number fields in Tracks"))
        assertTrue(habit.contains("UnitSelectionField("))
        assertTrue(goal.contains("UnitSelectionField("))
        assertTrue(track.contains("UnitSelectionField("))
        assertFalse(combined.contains("Settings > Custom Units"))
    }

    @Test
    fun instructionalSettingsPathsMatchTheCurrentInformationArchitecture() {
        val settings = File(uiRoot, "SettingsScreens.kt").readText()
        val firstRun = File(uiRoot, "FirstRunSetupDialog.kt").readText()
        val gym = File(uiRoot, "GymScreens.kt").readText()
        val rationale = File(uiRoot.parentFile, "health/HealthPermissionsRationaleActivity.kt").readText()
        val reminder = File(uiRoot.parentFile, "reminders/ReminderNotifications.kt").readText()
        val strings = File(
            uiRoot.parentFile.parentFile.parentFile.parentFile.parentFile,
            "res/values/strings.xml",
        ).readText()
        val userGuide = File(docsRoot, "user-guide.md").readText()
        val privacy = File(docsRoot, "privacy.md").readText()
        val combined = listOf(settings, firstRun, gym, rationale, reminder, strings, userGuide, privacy).joinToString("\n")

        listOf("Appearance & Home", "Planning & Units", "Organization", "Reminders", "Data & Privacy", "About Whip")
            .forEach { label -> assertTrue("Missing current Settings category: $label", settings.contains(label)) }
        assertTrue(firstRun.contains("Settings → Reminders"))
        assertTrue(firstRun.contains("Settings → Appearance & Home"))
        assertTrue(gym.contains("Settings → Planning & Units → Gym Defaults"))
        assertTrue(strings.contains("Settings → Data &amp; Privacy → Health &amp; Privacy"))
        assertTrue(reminder.contains("Settings → Reminders"))
        assertTrue(userGuide.contains("Settings → Appearance & Home → Home Overview"))
        assertTrue(userGuide.contains("Settings → Planning & Units → Gym Defaults"))
        assertTrue(userGuide.contains("Data & Privacy → Backup & Export"))
        assertTrue(privacy.contains("Settings → Data & Privacy → Reset Whip and Delete All Data"))
        listOf("Home Overview", "Gym Defaults", "Backup & Export", "Health & Privacy")
            .forEach { heading -> assertTrue("Missing Settings heading: $heading", settings.contains(heading)) }
        assertFalse(combined.contains("Reminders & Integrations"))
        assertFalse(combined.contains("About & Diagnostics"))
        assertFalse(combined.contains("Delete all local data"))
        assertFalse(settings.contains("selected && settings.healthConnectEnabled"))
    }

    @Test
    fun habitEditorOnlyOffersRulesWithDownstreamBehavior() {
        val habit = File(uiRoot, "HabitScreens.kt").readText()

        assertFalse(habit.contains("HabitIntent"))
        assertFalse(habit.contains("MissingDataPolicyEditor"))
        assertFalse(habit.contains("Earliest check-in"))
        assertFalse(habit.contains("Latest check-in"))
        assertTrue(habit.contains("TargetComparison.entries"))
        assertTrue(habit.contains("HabitTrackingMode.Count"))
        assertTrue(habit.contains("TargetComparison.AtMost"))
        assertTrue(habit.contains("trackingMode = HabitTrackingMode.CheckOff"))
    }

    @Test
    fun goalEditorDerivesEntryMeaningAvoidsRedundantLoggingAndExplainsRequiredFields() {
        val goal = File(uiRoot, "GoalScreens.kt").readText()

        assertFalse(goal.contains("GoalEntryMode"))
        assertFalse(goal.contains("Entry meaning"))
        assertFalse(goal.contains("Progress follows milestones"))
        assertFalse(goal.contains("GoalPaceType.Milestone"))
        assertFalse(goal.contains("colorArgb"))
        assertTrue(goal.contains("type.compatibleAggregations()"))
        assertTrue(goal.contains("if (deadline != null && type != GoalType.OpenEndedTrend)"))
        assertTrue(goal.contains("Enter the amount to add. Whip adds each entry"))
        assertFalse(goal.contains("Log Goal Value"))
        assertFalse(goal.contains("No measurable active goals"))
        assertTrue(goal.contains("required = type != GoalType.OpenEndedTrend"))
        assertTrue(goal.contains("currentDraft.validationErrors"))
        assertTrue(goal.contains("testTag = \"goal-save-problem\""))
    }

    @Test
    fun tracksExposeTheCompleteTypedFeatureWithoutLegacyCapsOrCosmeticColor() {
        val trackUi = File(uiRoot, "TrackScreens.kt").readText()
        val trackViewModel = File(uiRoot, "TrackViewModel.kt").readText()
        val trackDomain = File(uiRoot.parentFile, "domain/TrackModels.kt").readText()

        assertFalse(trackUi.contains("colorArgb"))
        assertFalse(trackDomain.contains("fields.take(7)"))
        assertFalse(trackDomain.contains("fields.size <= 7"))
        listOf(
            "Import Entries From CSV",
            "Entry Identity",
            "Single Choice",
            "Scale Preset",
        ).forEach { copy -> assertTrue("Missing Track UX: $copy", trackUi.contains(copy)) }
        assertTrue(trackUi.contains("TRACK_ENTRY_PAGE_SIZE = 100"))
        assertTrue(trackUi.contains("Possible Existing"))
        assertTrue(trackUi.contains("Replace With"))
        val appUi = File(uiRoot, "WhipApp.kt").readText()
        assertFalse(appUi.contains("Add Another"))
        assertFalse(appUi.contains("onCancelTrackOperation"))
        assertFalse(trackViewModel.contains("runningJob?.cancel()"))
        assertTrue(trackViewModel.contains("operationMutex.withLock"))
        assertTrue(trackViewModel.contains("acknowledgement?.await()"))
    }
}
