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
        val combined = listOf(settings, habit, goal).joinToString("\n")

        assertTrue(settings.contains("Planning & Units"))
        assertTrue(settings.contains("Habit Defaults"))
        assertTrue(settings.contains("Create reusable units for Habits and Goals"))
        assertTrue(habit.contains("UnitSelectionField("))
        assertTrue(goal.contains("UnitSelectionField("))
        assertFalse(combined.contains("Settings > Custom Units"))
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
    fun goalEditorDerivesEntryMeaningAndOnlyShowsRealPaceChoices() {
        val goal = File(uiRoot, "GoalScreens.kt").readText()

        assertFalse(goal.contains("GoalEntryMode"))
        assertFalse(goal.contains("Entry meaning"))
        assertFalse(goal.contains("Progress follows milestones"))
        assertFalse(goal.contains("GoalPaceType.Milestone"))
        assertFalse(goal.contains("colorArgb"))
        assertTrue(goal.contains("type.compatibleAggregations()"))
        assertTrue(goal.contains("if (deadline != null && type != GoalType.OpenEndedTrend)"))
        assertTrue(goal.contains("Enter the amount to add. Whip adds each entry"))
        assertTrue(goal.contains("Log Goal Value"))
        assertTrue(goal.contains("No measurable active goals"))
    }
}
