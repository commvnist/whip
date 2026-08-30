package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedSearchStringResourcePolicyTest {
    private val sourceRoot = sequenceOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory)
        ?: error("Unable to locate app source root")
    private val searchSource = File(sourceRoot, "com/whip/app/ui/UnifiedSearchDialog.kt").readText()

    @Test
    fun composableSearchSurfaceUsesResourcesForFixedDisplayText() {
        val composableSource = searchSource
            .substringAfter("@Composable\ninternal fun UnifiedSearchDialog(")
            .substringBefore("\ninternal fun WhipSearchResult.isVisibleInAreaScope")
        val internalLiterals = setOf(
            "",
            " ",
            " · ",
            "\\\\s+",
            "area:",
            ",",
            "|",
            "active",
            "archived",
            "completed",
            "inbox",
            "deadline overdue",
            "past scheduled date",
            "paused",
            "discarded",
            "filters",
            "data-status",
            "start",
            "searching",
            "incomplete-empty",
            "empty",
            "show-more",
        )
        val stringLiteral = Regex("\"(?:\\\\.|[^\"\\\\])*\"")
        val interpolation = Regex("\\$\\{[^}]*}|\\$[A-Za-z_][A-Za-z0-9_]*")
        val violations = composableSource.lineSequence().mapIndexedNotNull { index, line ->
            stringLiteral.findAll(line).mapNotNull { match ->
                val literal = match.value.removeSurrounding("\"")
                val fixedText = interpolation.replace(literal, "")
                val allowed = literal in internalLiterals ||
                    literal.startsWith("unified-search-") ||
                    literal.startsWith("search-") ||
                    fixedText.none(Char::isLetter)
                if (allowed) null else "UnifiedSearchDialog.kt:${index + 1}: ${match.value}"
            }.toList().takeIf(List<String>::isNotEmpty)?.joinToString()
        }.toList()

        assertTrue(
            "Fixed search display text must use Android string resources: $violations",
            violations.isEmpty(),
        )
    }

    @Test
    fun pureSearchLabelsRemainStableOutsideCompose() {
        val pureSearchModel = searchSource.substringBefore("@Composable\ninternal fun UnifiedSearchDialog(")

        listOf("Track Entry", "Tasks", "Habits", "Goals", "Tracks", "Gym").forEach { label ->
            assertTrue("Missing stable pure search label: $label", pureSearchModel.contains("\"$label\""))
        }
    }
}
