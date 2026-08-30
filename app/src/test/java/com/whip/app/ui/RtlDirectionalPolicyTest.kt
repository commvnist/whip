package com.whip.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RtlDirectionalPolicyTest {
    private val uiRoot = sequenceOf(
        File("src/main/java/com/whip/app/ui"),
        File("app/src/main/java/com/whip/app/ui"),
    ).firstOrNull(File::isDirectory) ?: error("Unable to locate UI sources")

    @Test
    fun sharedForwardIndicatorsUseAnAutoMirroredVector() {
        assertTrue(Icons.AutoMirrored.Outlined.NavigateNext.autoMirror)

        val expectedAutoMirroredUses = mapOf(
            "ItemControlPatterns.kt" to 2,
            "WhipPagePatterns.kt" to 1,
            "TaskComponents.kt" to 1,
            "WhipApp.kt" to 6,
        )
        expectedAutoMirroredUses.forEach { (name, expectedUses) ->
            val source = File(uiRoot, name).readText()
            assertEquals(
                "$name must keep every forward navigation indicator auto-mirrored",
                expectedUses,
                Regex("Icons\\.AutoMirrored\\.Outlined\\.NavigateNext").findAll(source).count(),
            )
            assertFalse(source.contains("Icons.Outlined.ChevronRight"))
            assertFalse(source.contains("material.icons.outlined.ChevronRight"))
        }
    }
}
