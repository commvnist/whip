package com.whip.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceRoleArchitectureTest {
    private val uiRoot = sequenceOf(
        File("src/main/java/com/whip/app/ui"),
        File("app/src/main/java/com/whip/app/ui"),
    ).firstOrNull(File::isDirectory) ?: error("Unable to locate UI source root")

    @Test
    fun ordinaryInformationAndCollectionRowsUseTheirSemanticSurfaceRoles() {
        val app = File(uiRoot, "WhipApp.kt").readText()
        val review = File(uiRoot, "ReviewDialog.kt").readText()
        val tracks = File(uiRoot, "TrackScreens.kt").readText()
        val settings = File(uiRoot, "SettingsScreens.kt").readText()
        val routines = File(uiRoot, "RoutineBuilder.kt").readText()

        val homeIntroduction = app.substringBefore("home-support-introduction").takeLast(500) +
            app.substringAfter("home-support-introduction").take(1_500)
        assertTrue(homeIntroduction.contains("WhipGroupedInformationCard"))
        assertTrue(app.contains("WhipNoticeCard(\n                    title = taskName"))
        assertTrue(Regex("WhipCollectionCard\\(\\s*onClick = \\{ onOpenPlanningHabit").findAll(app).count() == 2)

        assertTrue(review.substringAfter("productivityAreaLabel?.let").contains("WhipGroupedInformationCard"))
        assertTrue(review.substringBefore("30-Day Correlations").takeLast(500).contains("WhipGroupedInformationCard"))

        listOf("track-activity-filters", "Possible Existing Entry", "track_csv_validation_preview")
            .forEach { marker ->
                val neighborhood = tracks.substringBefore(marker).takeLast(700) + tracks.substringAfter(marker).take(700)
                assertTrue("$marker must use the grouped information surface", neighborhood.contains("WhipGroupedInformationCard"))
            }
        assertTrue(tracks.substringBefore("Recently Active Tracks").takeLast(700).contains("InsightCard("))
        assertTrue(tracks.substringAfter("Recently Active Tracks").take(1_400).contains("WhipCollectionCard("))

        assertTrue(settings.substringAfter("state.customUnits.forEach").take(1_000).contains("WhipCollectionCard"))
        assertTrue(settings.substringAfter("SettingsHeading(\"Custom Emojis\")").take(1_000).contains("WhipGroupedInformationCard"))
        assertTrue(Regex("WhipCollectionCard\\(\\s*onClick = \\{ onChoose").findAll(routines).count() == 2)
    }

    @Test
    fun reorderChartCalendarAndSelectionSurfacesRemainExplicitExceptions() {
        val review = File(uiRoot, "ReviewDialog.kt").readText()
        val app = File(uiRoot, "WhipApp.kt").readText()
        val tracks = File(uiRoot, "TrackScreens.kt").readText()
        val gym = File(uiRoot, "GymScreens.kt").readText()

        assertTrue(review.substringAfter("private fun ReviewSignalCard").contains("Card("))
        assertTrue(app.substringAfter("private fun TaskMonthPlanner").contains("Card("))
        listOf(app, tracks, gym).forEach { source -> assertTrue(source.contains("whipReorderItem(")) }
        assertTrue(app.contains(".selectable("))
    }
}
