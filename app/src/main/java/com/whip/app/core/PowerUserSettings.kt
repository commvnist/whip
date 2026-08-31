package com.whip.app.core

import com.whip.app.domain.AreaScope
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.normalizeCustomIdentityEmojis
import java.nio.charset.StandardCharsets
import java.util.Base64

data class SavedTaskFilter(
    val name: String,
    val priorities: Set<TaskPriority> = emptySet(),
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
    /** Smart, Manual, Scheduled Date, Deadline, Completion Date, Archived Date, Priority, Title. */
    val sortMode: String = "Smart",
    /** Applies to explicit sortable fields; Smart and Manual retain their authored order. */
    val sortDescending: Boolean = false,
    /** None, Date, Area, Priority. */
    val groupMode: String = "None",
    val areaId: String? = null,
)

/** Keeps saved navigation values within the routes supported by the current workspace. */
internal fun SavedTaskFilter.normalizedNavigation(): SavedTaskFilter {
    val knownDestinations = setOf("Inbox", "Today", "Upcoming", "Completed", "Archived")
    val safeDestination = when {
        destination.isBlank() -> ""
        destination == "Anytime" -> "Inbox"
        destination in knownDestinations -> destination
        else -> "Today"
    }
    val requestedView = planningView.takeIf { it in setOf("List", "Agenda", "Calendar") } ?: "List"
    return copy(
        destination = safeDestination,
        planningView = requestedView.takeIf { safeDestination == "Upcoming" } ?: "List",
    )
}

internal fun AppSettings.withoutAreaReferences(areaId: String): AppSettings = copy(
    activeAreaScope = activeAreaScope.takeUnless { it == AreaScope.One(areaId).storageKey }
        ?: AreaScope.All.storageKey,
    chosenOpeningAreaScope = chosenOpeningAreaScope.takeUnless { it == AreaScope.One(areaId).storageKey }
        ?: AreaScope.All.storageKey,
    savedTaskFilters = savedTaskFilters.map { filter ->
        if (filter.areaId == areaId) {
            filter.copy(areaId = null)
        } else {
            filter
        }
    },
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
        filter.sortDescending.toString(),
    ).joinToString("|")
}

internal fun String?.decodeTaskFilters(): List<SavedTaskFilter> = this.orEmpty().lineSequence()
    .mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size !in 16..17) return@mapNotNull null
        SavedTaskFilter(
            name = parts[0].decoded().takeIf(String::isNotBlank) ?: return@mapNotNull null,
            priorities = parts[1].split(',').mapNotNullTo(linkedSetOf()) {
                runCatching { TaskPriority.valueOf(it) }.getOrNull()
            },
            pinnedOnly = parts[2].toBooleanStrictOrNull() ?: false,
            tags = parts[3].split(',').map(String::decoded).filterTo(linkedSetOf(), String::isNotBlank),
            requireAllTags = parts[4].toBooleanStrictOrNull() ?: true,
            dateMode = parts[5].takeIf { it in setOf("Any", "Today", "Overdue", "Next7Days", "NoDate") } ?: "Any",
            deadlineOnly = parts[6].toBooleanStrictOrNull() ?: false,
            inboxOnly = parts[7].toBooleanStrictOrNull() ?: false,
            efforts = parts[8].split(',').mapNotNullTo(linkedSetOf()) {
                runCatching { TaskEffort.valueOf(it) }.getOrNull()
            },
            maximumDurationMinutes = parts[9].toIntOrNull()?.takeIf { it > 0 },
            textQuery = parts[10].decoded(),
            destination = parts[11],
            planningView = parts[12].takeIf { it in setOf("List", "Agenda", "Calendar") } ?: "List",
            sortMode = parts[13].takeIf {
                it in setOf("Smart", "Manual", "Scheduled Date", "Deadline", "Completion Date", "Archived Date", "Title", "Priority")
            } ?: "Smart",
            groupMode = parts[14].takeIf {
                it in setOf("None", "Scheduled Date", "Completion Date", "Archived Date", "Area", "Priority")
            } ?: "None",
            areaId = parts[15].decoded().takeIf(String::isNotBlank),
            sortDescending = parts.getOrNull(16)?.toBooleanStrictOrNull() ?: false,
        ).normalizedNavigation()
    }.distinctBy { it.name.lowercase() }.toList()

internal fun List<CustomIdentityEmoji>.encodeCustomIdentityEmojis(): String =
    normalizeCustomIdentityEmojis(this).joinToString("\n") { choice ->
        "${choice.emoji.encoded()}|${choice.name.encoded()}"
    }

internal fun String?.decodeCustomIdentityEmojis(): List<CustomIdentityEmoji> =
    normalizeCustomIdentityEmojis(
        this.orEmpty().lineSequence().mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size != 2) return@mapNotNull null
            CustomIdentityEmoji(
                emoji = parts[0].decoded(),
                name = parts[1].decoded(),
            )
        }.toList(),
    )

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

internal fun List<TrackedGymRecord>.encodeTrackedGymRecords(): String =
    normalizeTrackedGymRecords(this).joinToString("\n") { selection ->
        listOf(
            selection.exerciseUuid.encoded(),
            selection.type.name,
            selection.secondaryValue?.toString().orEmpty(),
            selection.machineProfileUuid.orEmpty().encoded(),
            selection.position.toString(),
        ).joinToString("|")
    }

internal fun String?.decodeTrackedGymRecords(): List<TrackedGymRecord> = normalizeTrackedGymRecords(
    this.orEmpty().lineSequence().mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size != 5) return@mapNotNull null
        TrackedGymRecord(
            exerciseUuid = parts[0].decoded().takeIf(String::isNotBlank) ?: return@mapNotNull null,
            type = runCatching { PersonalRecordType.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null,
            secondaryValue = parts[2].takeIf(String::isNotBlank)?.toDoubleOrNull() ?: if (parts[2].isBlank()) null else return@mapNotNull null,
            machineProfileUuid = parts[3].decoded().takeIf(String::isNotBlank),
            position = parts[4].toIntOrNull() ?: return@mapNotNull null,
        )
    }.toList(),
)

private fun String.encoded(): String = Base64.getUrlEncoder().withoutPadding()
    .encodeToString(toByteArray(StandardCharsets.UTF_8))

private fun String.decoded(): String = runCatching {
    String(Base64.getUrlDecoder().decode(this), StandardCharsets.UTF_8)
}.getOrDefault("")
