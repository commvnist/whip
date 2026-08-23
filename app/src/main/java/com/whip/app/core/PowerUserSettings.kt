package com.whip.app.core

import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.WorkoutSetClassification
import java.nio.charset.StandardCharsets
import java.util.Base64

data class SavedTaskFilter(
    val name: String,
    val priorities: Set<TaskPriority> = emptySet(),
    val area: String = "",
    val tag: String = "",
    val pinnedOnly: Boolean = false,
    val tags: Set<String> = emptySet(),
    val requireAllTags: Boolean = true,
    /** Any, Today, Overdue, Next7Days, NoDate. */
    val dateMode: String = "Any",
    val deadlineOnly: Boolean = false,
    val inboxOnly: Boolean = false,
    val efforts: Set<TaskEffort> = emptySet(),
    val maximumDurationMinutes: Int? = null,
    val textQuery: String = "",
    val destination: String = "",
    val planningView: String = "List",
    /** Smart, Title, Date, Priority. */
    val sortMode: String = "Smart",
    /** None, Date, Area, Priority. */
    val groupMode: String = "None",
    /** Canonical Area identity. `area` is retained to decode pre-v27 filters. */
    val areaId: String? = null,
)

data class SavedReviewFilter(
    val name: String,
    val sections: Set<HomeSection> = HomeSection.entries.toSet(),
)

data class PlatePreset(
    val name: String,
    val unitId: String,
    val barWeight: Double,
    val plates: List<Double>,
    val plateQuantities: Map<Double, Int> = emptyMap(),
    val collarWeight: Double = 0.0,
    val perSideLoading: Boolean = true,
)

/** A user-owned shortcut for filling a routine's set prescriptions. */
data class RepPrescriptionScheme(
    val id: String,
    val name: String = "",
    val setCount: Int,
    val repetitionsMin: Int,
    val repetitionsMax: Int = repetitionsMin,
    val classification: WorkoutSetClassification = WorkoutSetClassification.Working,
    val restSeconds: Int? = null,
) {
    val prescriptionLabel: String
        get() = "$setCount × ${if (repetitionsMin == repetitionsMax) repetitionsMin else "$repetitionsMin–$repetitionsMax"}"

    val displayLabel: String
        get() = name.trim().takeIf(String::isNotEmpty)?.let { "$it · $prescriptionLabel" } ?: prescriptionLabel

    fun isValid(): Boolean = id.isNotBlank() && setCount in 1..100 &&
        repetitionsMin in 1..1000 && repetitionsMax in repetitionsMin..1000 &&
        (restSeconds == null || restSeconds in 0..86_400)
}

internal fun List<SavedTaskFilter>.encodeTaskFilters(): String = joinToString("\n") { filter ->
    listOf(
        filter.name.encoded(),
        filter.priorities.joinToString(",", transform = TaskPriority::name),
        filter.area.encoded(),
        filter.tag.encoded(),
        filter.pinnedOnly.toString(),
        filter.tags.sortedBy(String::lowercase).joinToString(",") { it.encoded() },
        filter.requireAllTags.toString(),
        filter.dateMode,
        filter.deadlineOnly.toString(),
        filter.inboxOnly.toString(),
        filter.efforts.joinToString(",", transform = TaskEffort::name),
        filter.maximumDurationMinutes?.toString().orEmpty(),
        filter.textQuery.encoded(),
        filter.destination,
        filter.planningView,
        filter.sortMode,
        filter.groupMode,
        filter.areaId.orEmpty().encoded(),
    ).joinToString("|")
}

internal fun String?.decodeTaskFilters(): List<SavedTaskFilter> = this.orEmpty().lineSequence()
    .mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size !in setOf(5, 12, 16, 17, 18)) return@mapNotNull null
        SavedTaskFilter(
            name = parts[0].decoded().takeIf(String::isNotBlank) ?: return@mapNotNull null,
            priorities = parts[1].split(',').mapNotNullTo(linkedSetOf()) {
                runCatching { TaskPriority.valueOf(it) }.getOrNull()
            },
            area = parts[2].decoded(),
            tag = parts[3].decoded(),
            pinnedOnly = parts[4].toBooleanStrictOrNull() ?: false,
            tags = parts.getOrNull(5).orEmpty().split(',').map(String::decoded).filterTo(linkedSetOf(), String::isNotBlank),
            requireAllTags = parts.getOrNull(6)?.toBooleanStrictOrNull() ?: true,
            dateMode = parts.getOrNull(7)?.takeIf { it in setOf("Any", "Today", "Overdue", "Next7Days", "NoDate") } ?: "Any",
            deadlineOnly = parts.getOrNull(8)?.toBooleanStrictOrNull() ?: false,
            inboxOnly = parts.getOrNull(9)?.toBooleanStrictOrNull() ?: false,
            efforts = parts.getOrNull(10).orEmpty().split(',').mapNotNullTo(linkedSetOf()) {
                runCatching { TaskEffort.valueOf(it) }.getOrNull()
            },
            maximumDurationMinutes = parts.getOrNull(11)?.toIntOrNull()?.takeIf { it > 0 },
            textQuery = parts.getOrNull(12)?.decoded().orEmpty(),
            destination = parts.getOrNull(13).orEmpty(),
            planningView = parts.getOrNull(14)?.takeIf { it in setOf("List", "Agenda", "Calendar") } ?: "List",
            sortMode = parts.getOrNull(15)?.takeIf { it in setOf("Smart", "Manual", "Title", "Date", "Priority") } ?: "Smart",
            groupMode = parts.getOrNull(16)?.takeIf { it in setOf("None", "Date", "Area", "Priority") } ?: "None",
            areaId = parts.getOrNull(17)?.decoded()?.takeIf(String::isNotBlank),
        )
    }.distinctBy { it.name.lowercase() }.toList()

internal fun List<SavedReviewFilter>.encodeReviewFilters(): String = joinToString("\n") { filter ->
    "${filter.name.encoded()}|${filter.sections.joinToString(",", transform = HomeSection::name)}"
}

internal fun String?.decodeReviewFilters(): List<SavedReviewFilter> = this.orEmpty().lineSequence()
    .mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size != 2) return@mapNotNull null
        SavedReviewFilter(
            name = parts[0].decoded().takeIf(String::isNotBlank) ?: return@mapNotNull null,
            sections = parts[1].split(',').mapNotNullTo(linkedSetOf()) {
                runCatching { HomeSection.valueOf(it) }.getOrNull()
            }.ifEmpty { HomeSection.entries.toSet() },
        )
    }.distinctBy { it.name.lowercase() }.toList()

internal fun List<PlatePreset>.encodePlatePresets(): String = joinToString("\n") { preset ->
    listOf(
        preset.name.encoded(),
        preset.unitId,
        preset.barWeight.toString(),
        preset.plates.joinToString(","),
        preset.plateQuantities.entries.sortedByDescending { it.key }.joinToString(",") { "${it.key}:${it.value}" },
        preset.collarWeight.toString(),
        preset.perSideLoading.toString(),
    ).joinToString("|")
}

internal fun String?.decodePlatePresets(): List<PlatePreset> = this.orEmpty().lineSequence()
    .mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size !in setOf(4, 7)) return@mapNotNull null
        PlatePreset(
            name = parts[0].decoded().takeIf(String::isNotBlank) ?: return@mapNotNull null,
            unitId = normalizeMassUnit(parts[1]),
            barWeight = parts[2].toDoubleOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null,
            plates = parts[3].split(',').mapNotNull(String::toDoubleOrNull)
                .filter { it > 0 }.distinct().sortedDescending(),
            plateQuantities = parts.getOrNull(4).orEmpty().split(',').mapNotNull { item ->
                val pair = item.split(':', limit = 2)
                val plate = pair.getOrNull(0)?.toDoubleOrNull() ?: return@mapNotNull null
                val quantity = pair.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
                plate to quantity
            }.toMap(),
            collarWeight = parts.getOrNull(5)?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            perSideLoading = parts.getOrNull(6)?.toBooleanStrictOrNull() ?: true,
        )
    }.filter { it.plates.isNotEmpty() }.distinctBy { it.name.lowercase() }.toList()

internal fun List<RepPrescriptionScheme>.encodeRepPrescriptionSchemes(): String =
    filter(RepPrescriptionScheme::isValid).joinToString("\n") { scheme ->
        listOf(
            scheme.id.encoded(),
            scheme.name.encoded(),
            scheme.setCount.toString(),
            scheme.repetitionsMin.toString(),
            scheme.repetitionsMax.toString(),
            scheme.classification.name,
            scheme.restSeconds?.toString().orEmpty(),
        ).joinToString("|")
    }

internal fun String?.decodeRepPrescriptionSchemes(): List<RepPrescriptionScheme> = this.orEmpty().lineSequence()
    .mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size != 7) return@mapNotNull null
        RepPrescriptionScheme(
            id = parts[0].decoded(),
            name = parts[1].decoded(),
            setCount = parts[2].toIntOrNull() ?: return@mapNotNull null,
            repetitionsMin = parts[3].toIntOrNull() ?: return@mapNotNull null,
            repetitionsMax = parts[4].toIntOrNull() ?: return@mapNotNull null,
            classification = runCatching { WorkoutSetClassification.valueOf(parts[5]) }.getOrNull()
                ?: return@mapNotNull null,
            restSeconds = if (parts[6].isBlank()) null else parts[6].toIntOrNull() ?: return@mapNotNull null,
        ).takeIf(RepPrescriptionScheme::isValid)
    }.distinctBy(RepPrescriptionScheme::id).toList()

private fun String.encoded(): String = Base64.getUrlEncoder().withoutPadding()
    .encodeToString(toByteArray(StandardCharsets.UTF_8))

private fun String.decoded(): String = runCatching {
    String(Base64.getUrlDecoder().decode(this), StandardCharsets.UTF_8)
}.getOrDefault("")
