package com.whip.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import java.io.File
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

        val callers = listOf(
            "ItemControlPatterns.kt",
            "WhipPagePatterns.kt",
            "TaskComponents.kt",
            "WhipApp.kt",
        )
        callers.forEach { name ->
            val source = File(uiRoot, name).readText()
            assertTrue(
                "$name must keep every forward navigation indicator auto-mirrored",
                source.contains("Icons.AutoMirrored.Outlined.NavigateNext"),
            )
            assertFalse(source.contains("Icons.Outlined.NavigateNext"))
            assertFalse(source.contains("material.icons.outlined.NavigateNext"))
            assertFalse(source.contains("Icons.Outlined.ChevronRight"))
            assertFalse(source.contains("material.icons.outlined.ChevronRight"))
        }
    }
}
