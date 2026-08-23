package com.whip.app.core

import com.whip.app.domain.AreaScope
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.WorkoutSetClassification
import org.junit.Assert.assertEquals
import org.junit.Test

class PowerUserSettingsTest {
    @Test
    fun savedFiltersAndPlatePresetsRoundTripSpecialCharacters() {
        val filters = listOf(
            SavedTaskFilter(
                "Work | urgent",
                setOf(TaskPriority.High, TaskPriority.Urgent),
                "Client/A",
                "next,up",
                true,
                tags = setOf("next,up", "@office"),
                requireAllTags = false,
                dateMode = "Next7Days",
                deadlineOnly = true,
                inboxOnly = false,
                efforts = setOf(TaskEffort.Deep),
                maximumDurationMinutes = 90,
                textQuery = "launch checklist",
                destination = "Upcoming",
                planningView = "Calendar",
                sortMode = "Priority",
                groupMode = "Area",
                areaId = "area-client-a",
            ),
        )
        assertEquals(filters, filters.encodeTaskFilters().decodeTaskFilters())

        val reviews = listOf(SavedReviewFilter("Training + habits", setOf(HomeSection.Gym, HomeSection.Habits)))
        assertEquals(reviews, reviews.encodeReviewFilters().decodeReviewFilters())

        val plates = listOf(PlatePreset("Garage | lb", "pound", 45.0, listOf(45.0, 25.0, 10.0)))
        assertEquals(plates, plates.encodePlatePresets().decodePlatePresets())

        val schemes = listOf(
            RepPrescriptionScheme(
                id = "heavy|scheme",
                name = "Top set · δ",
                setCount = 4,
                repetitionsMin = 6,
                repetitionsMax = 8,
                classification = WorkoutSetClassification.BackOff,
                restSeconds = 150,
            ),
            RepPrescriptionScheme("simple", setCount = 3, repetitionsMin = 5),
        )
        assertEquals(schemes, schemes.encodeRepPrescriptionSchemes().decodeRepPrescriptionSchemes())
    }

    @Test
    fun invalidRepPrescriptionSchemesAreNeverPersistedOrDecoded() {
        val invalid = listOf(
            RepPrescriptionScheme("", setCount = 3, repetitionsMin = 5),
            RepPrescriptionScheme("zero", setCount = 0, repetitionsMin = 5),
            RepPrescriptionScheme("backwards", setCount = 3, repetitionsMin = 10, repetitionsMax = 8),
        )
        assertEquals("", invalid.encodeRepPrescriptionSchemes())
        assertEquals(emptyList<RepPrescriptionScheme>(), "aWQ|bmFtZQ|3|8|10|Working|not-a-number".decodeRepPrescriptionSchemes())
    }

    @Test
    fun deletingAnAreaClearsEverySavedReferenceWithoutRemovingFilters() {
        val settings = AppSettings(
            activeAreaScope = AreaScope.One("client-delta").storageKey,
            savedTaskFilters = listOf(
                SavedTaskFilter("By ID", area = "Client Delta", areaId = "client-delta"),
                SavedTaskFilter("Legacy", area = "client delta"),
                SavedTaskFilter("Other", area = "Other", areaId = "other"),
            ),
        )

        val result = settings.withoutAreaReferences("client-delta", "Client Delta")

        assertEquals(AreaScope.All.storageKey, result.activeAreaScope)
        assertEquals(SavedTaskFilter("By ID"), result.savedTaskFilters[0])
        assertEquals(SavedTaskFilter("Legacy"), result.savedTaskFilters[1])
        assertEquals(settings.savedTaskFilters[2], result.savedTaskFilters[2])
    }
}
