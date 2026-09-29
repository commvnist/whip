package com.whip.app.ui

import androidx.compose.runtime.saveable.listSaver
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.distanceFromMetres
import com.whip.app.domain.distanceToMetres
import com.whip.app.domain.equipmentScopeKey
import com.whip.app.domain.loadInterpretationMultiplier
import com.whip.app.domain.massFromKilograms
import com.whip.app.domain.massToKilograms

internal val QuickSetDraftSaver = listSaver<WorkoutSetDraft?, String>(
    save = { draft -> draft?.let { listOf(
        it.weight?.toString().orEmpty(), it.weightUnitId, it.reps?.toString().orEmpty(),
        it.distance?.toString().orEmpty(), it.distanceUnitId, it.durationSeconds?.toString().orEmpty(),
        it.bodyweightKg?.toString().orEmpty(), it.planned.toString(), it.completed.toString(),
        it.classification.name, it.note, it.rpe?.toString().orEmpty(), it.rir?.toString().orEmpty(),
        it.tempo, it.restSeconds?.toString().orEmpty(), it.machineLoadValue?.toString().orEmpty(),
        it.unilateral.toString(), it.workSection.name, it.optionalWorkKind.name,
    ) } ?: emptyList() },
    restore = { v -> if (v.isEmpty()) null else WorkoutSetDraft(
        weight = v[0].toDoubleOrNull(), weightUnitId = v[1], reps = v[2].toIntOrNull(),
        distance = v[3].toDoubleOrNull(), distanceUnitId = v[4], durationSeconds = v[5].toLongOrNull(),
        bodyweightKg = v[6].toDoubleOrNull(), planned = v[7].toBoolean(), completed = v[8].toBoolean(),
        classification = WorkoutSetClassification.valueOf(v[9]), note = v[10],
        rpe = v[11].toDoubleOrNull(), rir = v[12].toDoubleOrNull(), tempo = v[13],
        restSeconds = v[14].toIntOrNull(), machineLoadValue = v[15].toDoubleOrNull(),
        unilateral = v[16].toBoolean(), workSection = RoutineWorkSection.valueOf(v[17]),
        optionalWorkKind = RoutineOptionalWorkKind.valueOf(v[18]),
    ) },
)

/** Raw entries are reusable only when both placements give them the same meaning. */
internal fun compatibleQuickSetHistory(current: WorkoutExercise, previous: WorkoutExercise): Boolean =
    current.exerciseId == previous.exerciseId &&
        current.equipmentScopeKey == previous.equipmentScopeKey &&
        current.trackingTypeSnapshot == previous.trackingTypeSnapshot &&
        current.loadInterpretationSnapshot == previous.loadInterpretationSnapshot &&
        current.bodyweightLoadPolicySnapshot == previous.bodyweightLoadPolicySnapshot &&
        current.effectiveBodyweightPercentSnapshot == previous.effectiveBodyweightPercentSnapshot &&
        current.machineLoadTypeSnapshot == previous.machineLoadTypeSnapshot &&
        current.baseLoadKgSnapshot == previous.baseLoadKgSnapshot &&
        current.machineStackModeSnapshot == previous.machineStackModeSnapshot &&
        current.machinePulleyRatioSnapshot == previous.machinePulleyRatioSnapshot &&
        current.machineAddOnPlateKgSnapshot == previous.machineAddOnPlateKgSnapshot &&
        current.machineMassMappingKgSnapshot == previous.machineMassMappingKgSnapshot

/** Convert the entered quantity, not total effective resistance, into the receiving fields. */
internal fun convertedQuickSetSuggestion(
    set: WorkoutSet,
    source: WorkoutExercise,
    weightUnitId: String,
    distanceUnitId: String,
): WorkoutSet? {
    if (!set.completed || set.deletedAtMillis != null) return null
    val sourceWeightUnit = set.enteredWeightUnitId
        ?: source.machineUnitIdSnapshot.takeIf(String::isNotBlank)
        ?: source.exerciseWeightUnitSnapshot
    val rawKg = if (source.machineLoadTypeSnapshot == MachineLoadType.Level) null else {
        set.enteredWeight?.let { massToKilograms(it, sourceWeightUnit) }
            ?: set.canonicalWeightKg?.let { total ->
                (total - (source.baseLoadKgSnapshot ?: 0.0) - (source.machineAddOnPlateKgSnapshot ?: 0.0)) /
                    loadInterpretationMultiplier(source.loadInterpretationSnapshot, source.machineStackModeSnapshot,
                        source.machinePulleyRatioSnapshot, set.unilateral)
            }
    }
    val metres = set.enteredDistance?.let { value ->
        set.enteredDistanceUnitId?.let { distanceToMetres(value, it) }
    } ?: set.canonicalDistanceMetres
    val weight = rawKg?.let { massFromKilograms(it, weightUnitId) }
    return set.copy(
        enteredWeight = weight,
        enteredWeightUnitId = weightUnitId,
        enteredDistance = metres?.let { distanceFromMetres(it, distanceUnitId) },
        enteredDistanceUnitId = distanceUnitId,
        machineLoadValue = if (source.machineLoadTypeSnapshot == MachineLoadType.Mass) weight else set.machineLoadValue,
    )
}
