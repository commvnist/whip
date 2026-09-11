package com.whip.app.data

import androidx.room.withTransaction
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.MeasurementEntryStatus

/** Retires old source links without deleting the activity they previously projected. */
internal suspend fun WhipDatabase.retireLegacyHealthHabitSources() = withTransaction {
    val habits = habitDao()
    val measurements = measurementDao()
    val linked = habits.getAllHabits().filter { it.sourceMeasurementId?.startsWith("health-connect-") == true }
    if (linked.isEmpty()) return@withTransaction
    val units = measurements.getAllUnits().associateBy { it.id }
    val existingLogs = habits.getAllLogs().associateBy { it.uuid }
    linked.forEach { habit ->
        measurements.getEntriesForMeasurement(requireNotNull(habit.sourceMeasurementId)).forEach entry@{ entry ->
            if (entry.status !in setOf(MeasurementEntryStatus.Recorded.name, MeasurementEntryStatus.Failed.name)) {
                return@entry
            }
            val value = when {
                entry.enteredValue != null && entry.enteredUnitId == habit.unitId -> entry.enteredValue
                entry.canonicalValue != null -> {
                    val builtIn = BuiltInUnits.get(habit.unitId)
                    val custom = units[habit.unitId]
                    when {
                        builtIn != null -> builtIn.fromCanonical(entry.canonicalValue)
                        custom != null -> entry.canonicalValue / custom.toCanonicalFactor - custom.toCanonicalOffset
                        else -> entry.canonicalValue
                    }
                }
                else -> null
            }
            val log = HabitLogEntity(
                uuid = "measurement:${habit.id}:${entry.id}",
                habitId = habit.id,
                value = value,
                canonicalValue = entry.canonicalValue,
                enteredUnitId = habit.unitId,
                status = entry.status,
                timestampMillis = entry.timestampMillis,
                localEpochDay = entry.localEpochDay,
                zoneId = entry.zoneId,
                offsetSeconds = entry.offsetSeconds,
                note = entry.note,
                sourceType = entry.sourceType,
                sourceId = entry.sourceId,
                measurementEntryId = entry.id,
                createdAtMillis = entry.createdAtMillis,
                updatedAtMillis = entry.updatedAtMillis,
            )
            val existing = existingLogs[log.uuid]
            if (existing == null) habits.insertLog(log)
            else check(existing.copy(id = 0) == log) { "Saved Habit history conflicts with its legacy measurement" }
        }
        habits.updateHabit(habit.copy(sourceMeasurementId = null))
    }
}
