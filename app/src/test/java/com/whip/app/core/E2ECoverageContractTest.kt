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
                assertEvidenceReference(reference, sourceByClass)
            }
        }
    }

    @Test
    fun refinedCauseEffectMatrixNamesExecutableTestsAndHonestEvidenceTiers() {
        val root = sequenceOf(File("."), File(".."))
            .first { File(it, "docs/quality/QA_CAUSE_EFFECT_MATRIX_2026-08-27.tsv").isFile }
        val matrix = File(root, "docs/quality/QA_CAUSE_EFFECT_MATRIX_2026-08-27.tsv")
            .readLines()
            .filter(String::isNotBlank)
        assertEquals(
            "capability\tinput_or_trigger\tuser_visible_output\tpersisted_or_dependent_consequence\t" +
                "recovery_or_failure_contract\tstrongest_evidence_tier\texecutable_evidence\t" +
                "adaptive_accessibility_evidence\tremaining_gap\tdisposition",
            matrix.first(),
        )

        val rows = matrix.drop(1).map { line ->
            line.split('\t').also { columns ->
                assertEquals("Each cause/effect row needs all ten columns: $line", 10, columns.size)
                assertTrue("Blank cause/effect column: $line", columns.all(String::isNotBlank))
            }
        }
        assertEquals(REQUIRED_REFINED_CAPABILITIES, rows.map { it[0] }.toSet())
        assertEquals("Duplicate refined capability IDs are not allowed", rows.size, rows.map { it[0] }.toSet().size)

        val testFiles = listOf(File(root, "app/src/test"), File(root, "app/src/androidTest"))
            .flatMap { tree -> tree.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
        val sourceByClass = testFiles.associateBy({ it.nameWithoutExtension }, File::readText)
        val androidClasses = File(root, "app/src/androidTest").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.nameWithoutExtension }
            .toSet()

        rows.forEach { columns ->
            val tier = columns[5]
            assertTrue("Unknown evidence tier '$tier' for ${columns[0]}", tier in EVIDENCE_TIERS)
            val executable = evidenceRefs(columns[6])
            val adaptive = evidenceRefs(columns[7])
            assertTrue("${columns[0]} has no executable or explicit manual evidence", executable.isNotEmpty())
            assertTrue("${columns[0]} has no adaptive/accessibility or explicit manual evidence", adaptive.isNotEmpty())
            (executable + adaptive).forEach { assertEvidenceReference(it, sourceByClass) }

            val executableClasses = executable.filterNot { it.startsWith("manual:") }.map { it.substringBefore('#') }
            when (tier) {
                "real-app E2E" -> assertTrue(
                    "${columns[0]} claims real-app E2E without an E2E test",
                    executableClasses.any { it.endsWith("E2ETest") },
                )
                "component journey", "repository integration" -> assertTrue(
                    "${columns[0]} claims Android evidence without an Android test",
                    executableClasses.any(androidClasses::contains),
                )
                "domain/unit" -> assertTrue(
                    "${columns[0]} claims domain/unit evidence without a JVM test",
                    executableClasses.any { it !in androidClasses },
                )
                "manual/platform-owned" -> assertTrue(
                    "${columns[0]} is manual/platform-owned but has no explicit manual marker",
                    (executable + adaptive).any { it.startsWith("manual:") },
                )
            }
        }
    }

    private fun assertEvidenceReference(reference: String, sourceByClass: Map<String, String>) {
        if (reference.startsWith("manual:")) {
            assertTrue(
                "Manual evidence markers must use stable kebab-case IDs: $reference",
                Regex("manual:[a-z0-9]+(?:-[a-z0-9]+)*").matches(reference),
            )
            return
        }
        assertTrue("Evidence must use exact Class#method syntax: $reference", EXACT_EVIDENCE.matches(reference))
        val className = reference.substringBefore('#')
        val methodName = reference.substringAfter('#')
        val source = sourceByClass[className]
        assertTrue("Missing evidence class $className", source != null)
        assertTrue(
            "Evidence method is missing or is not annotated @Test: $reference",
            Regex(
                "@Test(?:\\s*\\([^)]*\\))?(?:\\s*@[A-Za-z0-9_.]+(?:\\([^)]*\\))?)*" +
                    "\\s*fun\\s+${Regex.escape(methodName)}\\s*\\(",
            ).containsMatchIn(requireNotNull(source)),
        )
    }

    private fun evidenceRefs(cell: String): List<String> =
        cell.split(Regex("[;,]")).map(String::trim).filter(String::isNotBlank)

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
            "gym-workout-set-rest-history",
            "gym-exercises-machines-records",
            "gym-routines-prescriptions",
            "areas-scope-taxonomy-moves",
            "search-exact-routing-review",
            "settings-cause-effect",
            "identity-emojis-units-colors",
            "notifications-actions-deeplinks",
            "backup-restore-encryption-portable-folder",
            "deletion-cascade-recovery",
            "database-migration-forward-compatibility",
            "fold-tablet-compact-layout",
            "accessibility-locale-large-text-rtl",
            "widget-area-scope",
            "health-connect-reconciliation",
        )

        val REQUIRED_REFINED_CAPABILITIES = setOf(
            "setup-and-returning-launch",
            "primary-navigation",
            "home-dashboard",
            "global-add",
            "unified-search",
            "task-create-edit",
            "task-completion-history",
            "task-planning-bulk",
            "task-custom-order",
            "habit-create-checkin",
            "habit-skip-history-health",
            "habit-goal-track-order",
            "goal-create-measure",
            "tracks-definition-entry",
            "tracks-import-export-history",
            "gym-active-workout",
            "gym-set-rest-timer",
            "gym-exercise-machine-category",
            "gym-routines-prescriptions",
            "gym-history",
            "gym-progress-records",
            "gym-tools",
            "areas-taxonomy",
            "settings-cause-effect",
            "backup-restore-reset",
            "notifications-deep-links",
            "widgets-share-shortcuts",
            "database-forward-migration",
            "loading-error-retry",
            "adaptive-ime-accessibility",
            "performance-stability",
        )
        val EVIDENCE_TIERS = setOf(
            "real-app E2E",
            "component journey",
            "repository integration",
            "domain/unit",
            "manual/platform-owned",
        )
        val EXACT_EVIDENCE = Regex("[A-Za-z_][A-Za-z0-9_]*#[A-Za-z_][A-Za-z0-9_]*")
    }
}
