package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogBodyArchitectureTest {
    private val uiRoot = sequenceOf(
        File("src/main/java/com/whip/app/ui"),
        File("app/src/main/java/com/whip/app/ui"),
    ).firstOrNull(File::isDirectory) ?: error("Unable to locate UI source root")

    @Test
    fun ordinaryDialogBodiesUseTheSharedWidthAndSpacingContract() {
        val components = File(uiRoot, "ProductivityEditorComponents.kt").readText()
        assertTrue(components.contains("internal fun WhipDialogBody("))
        assertTrue(components.contains("modifier = modifier.fillMaxWidth()"))
        assertTrue(components.contains("Arrangement.spacedBy(WhipSpacing.sibling)"))

        listOf(
            "PermanentDeleteDialog.kt",
            "AreaManagementDialog.kt",
            "TagManagementDialog.kt",
            "SettingsScreens.kt",
        ).forEach { fileName ->
            assertTrue(
                "$fileName must use the shared body for its ordinary dialog content",
                File(uiRoot, fileName).readText().contains("WhipDialogBody"),
            )
        }
    }
}
