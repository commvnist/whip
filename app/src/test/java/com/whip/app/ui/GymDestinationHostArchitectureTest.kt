package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymDestinationHostArchitectureTest {
    private val uiRoot = sequenceOf(
        File("src/main/java/com/whip/app/ui"),
        File("app/src/main/java/com/whip/app/ui"),
    ).firstOrNull(File::isDirectory) ?: error("Unable to locate UI source root")

    @Test
    fun gymRouteChromeIsSharedWithoutMovingCoordinatorOwnership() {
        val host = File(uiRoot, "GymDestinationHost.kt").readText()
        val screens = File(uiRoot, "GymScreens.kt").readText()
        val area = screens.substringAfter("fun GymAreaContent(").substringBefore("private fun GymLibraryLanding(")

        assertTrue(host.contains("internal fun GymDestinationHost("))
        assertTrue(host.contains("DestinationTabBar("))
        assertTrue(host.contains("destination in libraryGymDestinations"))
        assertTrue(host.contains("WhipBackAction("))
        assertTrue(host.contains("WhipSpacing.screenCompact"))
        assertTrue(area.contains("GymDestinationHost("))
        assertTrue(area.contains("when (destination)"))
        assertTrue(area.contains("rememberPersistenceRequestCoordinator("))
        assertTrue(area.contains("rememberGymCatalogMutationCoordinator("))
        assertFalse(host.contains("rememberPersistenceRequestCoordinator("))
        assertFalse(host.contains("GymViewModel"))
    }
}
