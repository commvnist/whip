package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditUiStringResourcePolicyTest {
    private val projectRoot = sequenceOf(File("."), File(".."))
        .map(File::getCanonicalFile)
        .firstOrNull { File(it, "app/src/main/java/com/whip/app/ui").isDirectory }
        ?: error("Unable to locate project sources")

    @Test
    fun auditIntroducedUiCopyUsesAndroidResources() {
        val requiredResources = mapOf(
            "AreaManagementDialog.kt" to listOf(
                "area_move_to_accessibility",
                "area_move_items_to_accessibility",
            ),
            "GymScreens.kt" to listOf(
                "gym_workout_saving",
                "gym_workout_save_failed",
                "gym_workout_discard_subject",
                "gym_workout_group_type",
                "gym_workout_group_type_accessibility",
                "gym_workout_group_include_accessibility",
                "gym_workout_group_discard_subject",
                "gym_workout_group_save_failed",
            ),
            "SettingsScreens.kt" to listOf(
                "settings_reset_explanation",
                "settings_reset_entry_title",
                "state_destructive_action",
                "settings_backup_replace_title",
                "settings_backup_replace_intro",
                "settings_backup_replace_impact_records",
                "settings_backup_replace_impact_preferences",
                "settings_backup_replace_impact_recovery",
                "settings_reset_confirm_title",
                "settings_reset_confirm_intro",
                "settings_reset_impact_records",
                "settings_reset_impact_preferences",
                "settings_reset_impact_backup_link",
                "settings_reset_confirm_action",
                "settings_backup_last_verified",
                "settings_backup_last_verified_file",
                "settings_backup_automatic_retention",
                "settings_health_sync_paused_empty",
                "settings_health_sync_paused_saved",
                "settings_health_last_sync",
                "settings_health_imported_entries",
            ),
            "WhipApp.kt" to listOf(
                "home_support_clear_title",
                "home_support_clear_message",
                "home_clear_review_message",
                "home_clear_review_action",
                "home_clear_existing_message",
                "home_resume_title",
                "home_resume_message",
                "home_resume_inbox",
                "home_resume_upcoming",
                "home_resume_habits",
                "home_resume_goals",
                "home_resume_tracks",
                "home_resume_gym",
                "home_resume_gym_support",
                "home_resume_inbox_count",
                "home_resume_upcoming_count",
                "home_resume_habit_count",
                "home_resume_goal_count",
                "home_resume_track_count",
                "support_domain_loading_title",
                "support_domain_loading_message",
                "support_tracks_description",
                "support_tracks_empty",
                "support_gym_empty",
                "support_track_overview_title",
                "task_bulk_completion_review_title",
                "task_bulk_completion_review_message",
                "task_bulk_unfinished_subtasks",
                "task_bulk_selected_tasks",
                "action_complete_anyway",
                "action_keep_working",
                "home_support_today_title",
                "home_support_introduction_title",
                "home_support_introduction_message",
                "home_support_introduction_nudge",
                "home_support_privacy_note",
                "home_support_habits_remaining",
                "home_support_pinned_tracks",
                "home_support_workout",
                "state_in_progress",
                "state_ready",
            ),
            "TrackScreens.kt" to listOf(
                "track_csv_default_export_name",
                "track_csv_exporting_title",
                "track_csv_export_failed_title",
                "track_csv_export_progress",
                "track_csv_export_failed_fallback",
                "track_csv_choose_another_destination",
                "track_csv_import_reading",
                "track_csv_import_previewing",
                "track_csv_choose_another_file",
                "track_csv_more_issues",
                "track_csv_validation_preview",
                "track_csv_validation_summary",
                "track_csv_issue_row",
                "track_csv_fix_invalid_rows",
                "track_csv_importing",
                "track_csv_import_entries",
                "track_csv_import_title",
                "track_csv_mapping_description",
                "track_csv_entry_date",
                "track_csv_use_today_date",
                "track_csv_primary_field",
                "track_csv_do_not_import",
                "track_csv_unit_field",
                "track_csv_use_current_unit",
            ),
        )
        requiredResources.forEach { (fileName, resourceNames) ->
            val source = uiSource(fileName)
            resourceNames.forEach { resourceName ->
                assertTrue("$fileName must use $resourceName", source.contains("R.string.$resourceName") || source.contains("R.plurals.$resourceName"))
            }
        }

        val forbiddenCopy = mapOf(
            "AreaManagementDialog.kt" to listOf(
                "accessibilityLabel = \"Move to ",
                "accessibilityLabel = \"Move items to ",
            ),
            "GymScreens.kt" to listOf(
                "subject = \"workout\"",
                "subject = \"workout exercise group\"",
                "accessibilityLabel = \"Group type ",
                "accessibilityLabel = \"Include ",
                "Text(\"Group type\"",
            ),
            "SettingsScreens.kt" to listOf(
                "stateDescription = \"Destructive action\"",
                "\"Reset clears Whip's internal data",
                "paneTitle = \"Confirm Backup Replacement\"",
                "Text(\"Replace Everything With This Backup?\")",
                "\"Everything currently in Whip—including",
                "\"Replacing…\"",
                "Text(\"Reset Whip and Delete All Data?\")",
                "Text(\"Reset and Delete Everything\")",
                "title = \"Reset Whip and Delete All Data\"",
                "Last verified: \$",
                "Last sync: \$",
                "Previously imported records remain in Whip until",
            ),
            "WhipApp.kt" to listOf(
                "title = \"Your Day Is Clear\"",
                "WhipEmptyState(\"No Tracks Yet\"",
                "SupportPaneTitle(\"Track Overview\")",
                "title = \"${'$'}domain loading\"",
                "\"Complete Selected Tasks With Unfinished Subtasks?\"",
                "unfinished subtask remains across",
                "SupportPaneTitle(\"Today\")",
                "\"Your Day, Brought Together\"",
                "\"Private by default. Your data stays on this device",
                "AdaptiveSummaryCard(\"Tasks Today\"",
                "AdaptiveSummaryCard(\"Habits Remaining\"",
            ),
            "TrackScreens.kt" to listOf(
                "\"Exporting Track CSV\"",
                "\"Could Not Export CSV\"",
                "\"Formatting and writing the export.",
                "\"Could not export this Track.\"",
                "\"Choose Another Destination\"",
                "\"Reading and checking the selected file…\"",
                "\"Rebuilding the validation preview…\"",
                "\"Choose Another File\"",
                "more issues · fix the source file and preview again",
                "Text(\"Validation Preview\"",
                "Text(\"Row ${'$'}{issue.rowNumber}",
                "Text(if (saving) \"Importing…\"",
                "Text(\"Import ${'$'}{projection.track.name} Entries\")",
                "SelectionField(\"Entry Date\"",
                "it ?: \"Use Today's Date\"",
                "it ?: \"Do Not Import\"",
            ),
        )
        forbiddenCopy.forEach { (fileName, literals) ->
            val source = uiSource(fileName)
            literals.forEach { literal ->
                assertFalse("$fileName reintroduced audit UI copy: $literal", source.contains(literal))
            }
        }
    }

    private fun uiSource(fileName: String): String =
        File(projectRoot, "app/src/main/java/com/whip/app/ui/$fileName").readText()
}
