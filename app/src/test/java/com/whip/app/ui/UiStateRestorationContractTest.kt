package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateRestorationContractTest {
    private val sourceRoot = sequenceOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory)
        ?: error("Unable to locate app source root")

    @Test
    fun nestedEditorsAndSelectionKeepDraftStateAcrossRecreation() {
        val tracks = File(sourceRoot, "com/whip/app/ui/TrackScreens.kt").readText()
        val gym = File(sourceRoot, "com/whip/app/ui/GymScreens.kt").readText()

        assertTrue(
            "Track multi-selection IDs must survive activity recreation",
            tracks.contains("var selectedIds by rememberSaveable { mutableStateOf<Set<Long>>(emptySet()) }"),
        )
        assertTrue(
            "Track choice drafts must survive activity recreation",
            tracks.contains("var choices by rememberSaveable(initial.uuid, initial.id)"),
        )
        assertTrue(
            "Rest-timer preset drafts must survive activity recreation",
            gym.contains("var presetDraft by rememberSaveable(presetSeconds)"),
        )
    }
}
