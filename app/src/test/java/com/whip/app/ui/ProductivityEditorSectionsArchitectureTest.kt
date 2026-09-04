package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductivityEditorSectionsArchitectureTest {
    private val uiRoot = sequenceOf(
        File("src/main/java/com/whip/app/ui"),
        File("app/src/main/java/com/whip/app/ui"),
    ).firstOrNull(File::isDirectory) ?: error("Unable to locate UI source root")

    @Test
    fun productivityEditorsShareOneIdentityAndOrganizationSectionContract() {
        val sections = File(uiRoot, "ProductivityEditorSections.kt").readText()
        assertTrue(sections.contains("internal fun ProductivityIdentitySection("))
        assertTrue(sections.contains("internal fun ProductivityOrganizationSection("))
        assertTrue(sections.contains("identityFields()\n        emojiPicker()"))
        assertTrue(sections.contains("areaPicker()\n        extras()"))

        listOf(
            "TaskEditorDialog.kt" to "task-editor-title",
            "HabitScreens.kt" to "habit-editor-name",
            "GoalScreens.kt" to "goal-editor-name",
            "TrackScreens.kt" to "track-editor-name",
        ).forEach { (fileName, requiredFieldTag) ->
            val editor = File(uiRoot, fileName).readText()
            assertTrue("$fileName must adopt the shared identity section", editor.contains("ProductivityIdentitySection("))
            assertTrue("$fileName must adopt the shared organization section", editor.contains("ProductivityOrganizationSection("))
            assertTrue("$fileName lost its required identity field", editor.contains(requiredFieldTag))
            assertTrue("$fileName lost its emoji picker", editor.contains("WhipEmojiPicker("))
            assertTrue("$fileName lost its Area picker", editor.contains("AreaPicker("))
        }
    }

    @Test
    fun editorSpecificSemanticsAndSingleUnitHierarchyRemainExplicit() {
        val task = File(uiRoot, "TaskEditorDialog.kt").readText()
        val habit = File(uiRoot, "HabitScreens.kt").readText()
        val goal = File(uiRoot, "GoalScreens.kt").readText()

        assertTrue(task.contains("smart-task-editor-preview"))
        assertTrue(task.contains("smart-task-capture-apply"))
        assertTrue(task.contains("Choose an Area before saving."))
        assertTrue(habit.contains("Choose an Area for this Habit"))
        assertTrue(habit.contains("EditorSectionHeader(\"Tracking\""))
        assertTrue(habit.contains("UnitSelectionField("))
        assertTrue(goal.contains("Choose an Area for this Goal"))
        assertTrue(goal.contains("EditorSectionHeader(\"Measurement\")"))
        assertTrue(goal.contains("UnitSelectionField("))
        assertFalse(goal.contains("Text(\"Unit\", fontWeight = FontWeight.Bold)"))
        assertFalse(habit.contains("Text(\"Unit\", fontWeight = FontWeight.Bold)"))
    }
}
