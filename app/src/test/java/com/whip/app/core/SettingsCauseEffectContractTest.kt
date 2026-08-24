package com.whip.app.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCauseEffectContractTest {
    @Test
    fun everyPersistedSettingHasNamedPersistenceAndCauseEffectEvidence() {
        val root = sequenceOf(File("."), File(".."))
            .first { File(it, "app/src/main/java/com/whip/app/core/AppSettings.kt").isFile }
        val source = File(root, "app/src/main/java/com/whip/app/core/AppSettings.kt").readText()
        val settingsBlock = source.substringAfter("data class AppSettings(").substringBefore("\n)")
        val declared = Regex("val\\s+([A-Za-z0-9_]+):").findAll(settingsBlock).map { it.groupValues[1] }.toSet()
        val rows = File(root, "docs/quality/settings-cause-effect.tsv").readLines().drop(1).filter(String::isNotBlank)
        val documented = rows.associate { row ->
            val parts = row.split('\t')
            require(parts.size == 3) { "Cause/effect row must have three tab-separated columns: $row" }
            parts[0] to parts.drop(1)
        }

        assertEquals("Update settings cause/effect coverage when AppSettings changes", declared, documented.keys)
        val testSources = sequenceOf(
            File(root, "app/src/test"),
            File(root, "app/src/androidTest"),
        ).flatMap { directory ->
            directory.walkTopDown().filter { it.isFile && it.extension == "kt" }
        }.map(File::readText).toList()
        documented.forEach { (setting, evidence) ->
            assertTrue("$setting lacks persistence evidence", evidence[0].endsWith("Test"))
            assertTrue("$setting lacks behavior evidence", evidence[1].endsWith("Test"))
            evidence.forEach { className ->
                assertTrue(
                    "$setting names missing evidence class $className",
                    testSources.any { source -> Regex("class\\s+$className\\b").containsMatchIn(source) },
                )
            }
        }
    }
}
