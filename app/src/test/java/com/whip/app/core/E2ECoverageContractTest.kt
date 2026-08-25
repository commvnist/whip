package com.whip.app.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E2ECoverageContractTest {
    @Test
    fun everyFirstClassCapabilityHasTraceableCauseEffectEvidence() {
        val root = sequenceOf(File("."), File(".."))
            .first { File(it, "docs/quality/e2e-coverage.tsv").isFile }
        val matrix = File(root, "docs/quality/e2e-coverage.tsv").readLines().filter(String::isNotBlank)
        assertEquals(
            "capability\thappy_path\talternate_failure\tpersistence_recreation\taccessibility_adaptive",
            matrix.first(),
        )

        val rows = matrix.drop(1).map { line ->
            line.split('\t').also { columns ->
                assertEquals("Each E2E row needs all five evidence columns: $line", 5, columns.size)
                assertTrue("Blank E2E evidence column: $line", columns.all(String::isNotBlank))
            }
        }
        val capabilities = rows.map { it.first() }.toSet()
        assertEquals("Duplicate capability IDs are not allowed", rows.size, capabilities.size)
        assertEquals(REQUIRED_CAPABILITIES, capabilities)

        val testFiles = listOf(
            File(root, "app/src/test"),
            File(root, "app/src/androidTest"),
        ).flatMap { tree -> tree.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
        val sourceByClass = testFiles.associateBy({ it.nameWithoutExtension }, File::readText)
        val androidClasses = File(root, "app/src/androidTest").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.nameWithoutExtension }
            .toSet()

        rows.forEach { columns ->
            evidenceRefs(columns[1]).forEach { reference ->
                if (!reference.startsWith("manual:")) {
                    assertTrue("Happy-path evidence must be an Android E2E/integration test: $reference", reference.substringBefore('#') in androidClasses)
                }
            }
            columns.drop(1).flatMap(::evidenceRefs).forEach { reference ->
                if (reference.startsWith("manual:")) return@forEach
                val className = reference.substringBefore('#')
                val methodName = reference.substringAfter('#')
                val source = sourceByClass[className]
                assertTrue("Missing evidence class $className", source != null)
                assertTrue(
                    "Missing evidence method $reference",
                    Regex("fun\\s+${Regex.escape(methodName)}\\s*\\(").containsMatchIn(requireNotNull(source)),
                )
            }
        }
    }

    private fun evidenceRefs(cell: String): List<String> = cell.split(',').map(String::trim).filter(String::isNotBlank)

    private companion object {
        val REQUIRED_CAPABILITIES = setOf(
            "app-shell-home-navigation",
            "tasks-create-edit-complete",
            "tasks-schedule-recurrence-history",
            "tasks-bulk-planning-filters",
            "habits-checkoff-count-history",
            "habits-skip-streak-reminders",
            "habits-schedules-checklists-health",
            "goals-create-record-insights",
            "goals-elapsed-milestones-consistency",
            "tracks-definition-entry-crud",
            "tracks-typed-fields-sorting-csv",
            "tracks-goal-capture-followup-automation",
            "gym-workout-set-rest-history",
            "gym-exercises-machines-records",
            "gym-routines-prescriptions",
            "areas-scope-taxonomy-moves",
            "search-exact-routing-review",
            "settings-cause-effect",
            "identity-emojis-units-colors",
            "automations-cross-domain",
            "notifications-actions-deeplinks",
            "backup-restore-encryption-portable-folder",
            "deletion-cascade-recovery",
            "database-migration-forward-compatibility",
            "fold-tablet-compact-layout",
            "accessibility-locale-large-text-rtl",
            "widget-area-scope",
            "health-connect-reconciliation",
        )
    }
}
