package com.whip.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.HealthDataType
import com.whip.app.core.HomeSection
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.SavedReviewFilter
import com.whip.app.core.PlatePreset
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.core.SettingsRepository
import com.whip.app.domain.AvoidMissingPolicy
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(
    val envelopeVersion: Int,
    val databaseVersion: Int,
    val exportedAt: Instant,
    val tableCounts: Map<String, Int>,
    val totalRecords: Int,
    val duplicateStableIds: Int,
    val checksumValid: Boolean,
    val settingsIncluded: Boolean,
    val restoreCompatible: Boolean = databaseVersion in 1..DATABASE_VERSION,
    val compatibilityMessage: String? = null,
)

data class BackupMergeSummary(
    val importedRecords: Int,
    val skippedExistingRecords: Int,
    val settingsKept: Boolean = true,
)

interface BackupRepository {
    suspend fun exportBackup(): String
    suspend fun previewBackup(json: String): BackupPreview
    suspend fun restoreBackup(json: String)
    suspend fun mergeBackup(json: String): BackupMergeSummary
    suspend fun exportTasksCsv(): String
    suspend fun exportHabitsCsv(): String
    suspend fun exportGoalsCsv(): String
    suspend fun exportGymCsv(): String
    suspend fun deleteAllData()
}

class RoomBackupRepository(
    private val database: WhipDatabase,
    private val settingsRepository: SettingsRepository? = null,
) : BackupRepository {
    override suspend fun exportBackup(): String = database.withTransaction {
        val db = database.openHelper.readableDatabase
        val tables = JSONObject()
        EXPORT_TABLES.forEach { table ->
            val rows = JSONArray()
            db.query("SELECT * FROM ${safeIdentifier(table)}").use { cursor ->
                while (cursor.moveToNext()) rows.put(cursor.toJsonRow())
            }
            tables.put(table, rows)
        }
        val settings = settingsRepository?.current()?.toJson()
        val payload = checksumPayload(tables, settings, ENVELOPE_VERSION)
        JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("envelopeVersion", ENVELOPE_VERSION)
            .put("databaseVersion", DATABASE_VERSION)
            .put("exportedAt", Instant.now().toString())
            .put("checksumSha256", sha256(payload))
            .put("tables", tables)
            .apply { settings?.let { put("settings", it) } }
            .toString(2)
    }

    override suspend fun previewBackup(json: String): BackupPreview {
        val root = parseAndValidate(json, requireCurrentDatabaseVersion = false)
        val tables = root.getJSONObject("tables")
        val counts = EXPORT_TABLES.associateWith { table -> tables.optJSONArray(table)?.length() ?: 0 }
        var duplicates = 0
        val db = database.openHelper.readableDatabase
        EXPORT_TABLES.forEach { table ->
            val array = tables.optJSONArray(table) ?: return@forEach
            for (index in 0 until array.length()) {
                val row = array.getJSONObject(index)
                val key = when {
                    row.has("uuid") && !row.isNull("uuid") -> "uuid" to row.getString("uuid")
                    row.has("id") && row.opt("id") is String -> "id" to row.getString("id")
                    else -> null
                } ?: continue
                db.query("SELECT COUNT(*) FROM ${safeIdentifier(table)} WHERE ${safeIdentifier(key.first)} = ?", arrayOf(key.second)).use { cursor ->
                    if (cursor.moveToFirst() && cursor.getInt(0) > 0) duplicates++
                }
            }
        }
        val settings = root.optJSONObject("settings")
        val envelopeVersion = root.getInt("envelopeVersion")
        val payload = checksumPayload(tables, settings, envelopeVersion)
        val databaseVersion = root.getInt("databaseVersion")
        return BackupPreview(
            envelopeVersion = envelopeVersion,
            databaseVersion = databaseVersion,
            exportedAt = Instant.parse(root.getString("exportedAt")),
            tableCounts = counts,
            totalRecords = counts.values.sum(),
            duplicateStableIds = duplicates,
            checksumValid = root.getString("checksumSha256") == sha256(payload),
            settingsIncluded = settings != null,
            restoreCompatible = databaseVersion in 1..DATABASE_VERSION,
            compatibilityMessage = if (databaseVersion > DATABASE_VERSION) {
                "Created by a newer Whip data format (version $databaseVersion). Update Whip before restoring."
            } else if (databaseVersion < 1) {
                "This backup's data format is not supported."
            } else null,
        )
    }

    override suspend fun restoreBackup(json: String) {
        val root = parseAndValidate(json, requireCurrentDatabaseVersion = true)
        val tables = root.getJSONObject("tables")
        val settings = root.optJSONObject("settings")
        require(
            root.getString("checksumSha256") == sha256(
                checksumPayload(tables, settings, root.getInt("envelopeVersion")),
            ),
        ) { "Backup checksum does not match" }
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            val sourceVersion = root.getInt("databaseVersion")
            EXPORT_TABLES.asReversed().forEach { table -> db.execSQL("DELETE FROM ${safeIdentifier(table)}") }
            EXPORT_TABLES.forEach { table ->
                val rows = tables.optJSONArray(table) ?: JSONArray()
                for (index in 0 until rows.length()) {
                    val values = rows.getJSONObject(index).toContentValues()
                        .withLegacyDefaults(table, sourceVersion)
                    val result = db.insert(safeIdentifier(table), SQLiteDatabase.CONFLICT_ABORT, values)
                    require(result != -1L) { "Could not restore $table row ${index + 1}" }
                }
            }
            if (sourceVersion < 22) normalizeLegacyMachineScopes(db)
            if (sourceVersion < 27) backfillLegacyAreas(db)
        }
        settings?.let { restored -> settingsRepository?.update { restored.toAppSettings() } }
    }

    override suspend fun mergeBackup(json: String): BackupMergeSummary {
        val root = parseAndValidate(json, requireCurrentDatabaseVersion = true)
        val tables = root.getJSONObject("tables")
        val settings = root.optJSONObject("settings")
        require(
            root.getString("checksumSha256") == sha256(
                checksumPayload(tables, settings, root.getInt("envelopeVersion")),
            ),
        ) { "Backup checksum does not match" }
        val sourceVersion = root.getInt("databaseVersion")
        // Pre-v26 tasks and steps only had device-local integer IDs. Namespace
        // those identities by backup content so one legacy file is idempotent,
        // while task 1 from two devices cannot collide.
        val legacyTaskIdentityNamespace = if (sourceVersion < 26) {
            sha256(tables.toString()).take(24)
        } else {
            null
        }
        return database.withTransaction {
            val db = database.openHelper.writableDatabase
            val idMaps = mutableMapOf<String, MutableMap<Long, Long>>()
            val areaIdMap = mutableMapOf<String, String>()
            var imported = 0
            var skipped = 0
            EXPORT_TABLES.forEach { table ->
                val rows = tables.optJSONArray(table) ?: JSONArray()
                val metadata = mergeTableMetadata(db, table)
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    if (table == "areas") {
                        val sourceAreaId = row.optString("id")
                        val name = row.optString("name").trim()
                        val key = row.optString("nameKey").ifBlank { name.lowercase() }
                        val existingAreaId = db.query(
                            "SELECT id FROM areas WHERE id = ? OR nameKey = ? LIMIT 1",
                            arrayOf(sourceAreaId, key),
                        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                        if (existingAreaId != null) {
                            areaIdMap[sourceAreaId] = existingAreaId
                            skipped++
                            continue
                        }
                    }
                    val sourceNumericId = row.optLongOrNull("id")
                    val legacyStableUuid = when {
                        legacyTaskIdentityNamespace == null || sourceNumericId == null -> null
                        table == "tasks" -> "legacy-import-$legacyTaskIdentityNamespace-task-$sourceNumericId"
                        table == "task_steps" -> "legacy-import-$legacyTaskIdentityNamespace-task-step-$sourceNumericId"
                        else -> null
                    }
                    val stable = legacyStableUuid?.let { "uuid" to it } ?: row.stableMergeKey(metadata)
                    val existing = stable?.let { (column, value) ->
                        db.query(
                            "SELECT ${if (metadata.autoNumericId) "id" else safeIdentifier(column)} FROM ${safeIdentifier(table)} WHERE ${safeIdentifier(column)} = ? LIMIT 1",
                            arrayOf(value),
                        ).use { cursor ->
                            if (!cursor.moveToFirst()) null
                            else ExistingMergeRow(if (metadata.autoNumericId) cursor.getLong(0) else null)
                        }
                    }
                    if (existing != null) {
                        existing.numericId?.let { target -> sourceNumericId?.let { source -> idMaps.getOrPut(table, ::mutableMapOf)[source] = target } }
                        skipped++
                        continue
                    }
                    val values = row.toContentValues().withLegacyDefaults(table, sourceVersion).apply {
                        legacyStableUuid?.let { put("uuid", it) }
                    }
                    if (table in setOf("tasks", "habits", "goals")) {
                        values.getAsString("areaId")?.let { sourceId -> values.put("areaId", areaIdMap[sourceId] ?: sourceId) }
                    }
                    remapForeignKeys(values, metadata, idMaps)
                    remapPolymorphicReferences(table, values, idMaps)
                    if (metadata.autoNumericId) values.remove("id")
                    val result = db.insert(safeIdentifier(table), SQLiteDatabase.CONFLICT_IGNORE, values)
                    if (result == -1L) {
                        skipped++
                    } else {
                        imported++
                        if (table == "areas") row.optString("id").takeIf(String::isNotBlank)?.let { areaIdMap[it] = it }
                        if (metadata.autoNumericId && sourceNumericId != null) {
                            idMaps.getOrPut(table, ::mutableMapOf)[sourceNumericId] = result
                        }
                    }
                }
            }
            if (sourceVersion < 22) normalizeLegacyMachineScopes(db)
            if (sourceVersion < 27) backfillLegacyAreas(db)
            BackupMergeSummary(imported, skipped)
        }
    }

    override suspend fun exportTasksCsv(): String = queryCsv(
        "SELECT t.id, t.title, t.notes, t.scheduleKind, t.dateEpochDay, t.deadlineEpochDay, t.priority, COALESCE(a.name, '') AS area, t.tagsCsv, t.recurrenceUnit, t.recurrenceInterval, t.recurrenceAnchor, t.reminderOffsetsMinutesCsv, t.completedAtMillis, t.archived FROM tasks t LEFT JOIN areas a ON a.id = t.areaId ORDER BY t.id",
    )

    override suspend fun exportHabitsCsv(): String = queryCsv(
        "SELECT h.uuid AS habitUuid, h.name AS habit, COALESCE(a.name, '') AS area, h.trackingMode, l.uuid AS logUuid, l.localEpochDay, l.value, l.enteredUnitId, l.status, l.note FROM habits h LEFT JOIN areas a ON a.id = h.areaId LEFT JOIN habit_logs l ON l.habitId = h.id ORDER BY h.id, l.timestampMillis",
    )

    override suspend fun exportGoalsCsv(): String = queryCsv(
        "SELECT g.uuid AS goalUuid, g.name AS goal, COALESCE(a.name, '') AS area, g.type, g.unitId, e.id AS entryId, e.localEpochDay, e.enteredValue, e.enteredUnitId, e.status, e.sourceType, e.note FROM goals g LEFT JOIN areas a ON a.id = g.areaId LEFT JOIN metric_entries e ON e.metricId = g.metricId ORDER BY g.id, e.timestampMillis",
    )

    override suspend fun exportGymCsv(): String = queryCsv(
        "SELECT s.uuid AS workoutUuid, s.localEpochDay, s.name AS workout, e.uuid AS exerciseUuid, e.name AS exercise, e.trackingType, e.archived AS exerciseArchived, we.machineProfileUuidSnapshot AS machineScopeUuid, we.machineNameSnapshot AS machine, we.machineConfigurationGroupSnapshot AS machineConfigurationFamily, we.machineConfigurationVersionSnapshot AS machineConfigurationVersion, we.machineConfigurationSnapshot AS machineConfiguration, we.machineLoadTypeSnapshot AS machineLoadType, ws.uuid AS setUuid, ws.position, ws.classification, ws.machineLoadValue, ws.enteredWeight, ws.enteredWeightUnitId, ws.repetitions, ws.enteredDistance, ws.enteredDistanceUnitId, ws.durationSeconds, ws.rpe, ws.rir, ws.tempo, ws.note FROM exercises e LEFT JOIN workout_exercises we ON we.exerciseId = e.id LEFT JOIN workout_sessions s ON s.id = we.sessionId LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id AND ws.deletedAtMillis IS NULL ORDER BY e.position, s.startedAtMillis, we.position, ws.position",
    )

    override suspend fun deleteAllData() {
        database.clearAllTables()
        settingsRepository?.update { AppSettings() }
    }

    private suspend fun queryCsv(sql: String): String = database.withTransaction {
        buildString {
            database.openHelper.readableDatabase.query(sql).use { cursor ->
                appendLine(cursor.columnNames.joinToString(",", transform = ::csvCell))
                while (cursor.moveToNext()) {
                    appendLine(cursor.columnNames.indices.joinToString(",") { index ->
                        val value = if (cursor.isNull(index)) "" else when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP)
                            else -> cursor.getString(index)
                        }
                        csvCell(value)
                    })
                }
            }
        }
    }

    private fun parseAndValidate(json: String, requireCurrentDatabaseVersion: Boolean): JSONObject {
        val root = runCatching { JSONObject(json) }.getOrElse { error("This is not valid JSON") }
        require(root.optString("format") == BACKUP_FORMAT) { "This is not a Whip backup" }
        require(root.optInt("envelopeVersion") in 1..ENVELOPE_VERSION) { "Unsupported backup envelope version" }
        val dbVersion = root.optInt("databaseVersion")
        if (requireCurrentDatabaseVersion) require(dbVersion in 1..DATABASE_VERSION) {
            "This backup uses unsupported database version $dbVersion; this build supports versions 1–$DATABASE_VERSION"
        }
        require(root.has("tables")) { "Backup has no table data" }
        return root
    }
}

internal fun ContentValues.withLegacyDefaults(table: String, sourceVersion: Int): ContentValues = apply {
    fun default(column: String, value: Any?) {
        if (containsKey(column)) return
        when (value) {
            null -> putNull(column)
            is String -> put(column, value)
            is Int -> put(column, value)
            is Long -> put(column, value)
            is Double -> put(column, value)
            is Float -> put(column, value)
            else -> error("Unsupported legacy default for $table.$column")
        }
    }

    if (table == "tasks" && sourceVersion < 2) {
        default("updatedAtMillis", getAsLong("createdAtMillis") ?: 0L)
        default("showSubtaskProgress", 0)
        default("progressDisplay", "Percent")
        default("autoCompleteFromSteps", 1)
        default("repeatStepPolicy", "Reset")
    }
    if (table == "habits" && sourceVersion < 10) default("weekdayReminderMinutesCsv", "")
    if (sourceVersion < 11) when (table) {
        "tasks" -> default("pinned", 0)
        "gym_routines" -> default("pinned", 0)
        "workout_sessions" -> default("sourceRoutineId", null)
        "habits", "goals" -> {
            default("area", "")
            default("tagsCsv", "")
        }
    }
    if (table == "goals" && sourceVersion < 12) {
        default("aggregationPeriod", "All")
        default("rollingDays", null)
        default("consistencyPeriod", "Week")
        default("consistencyRequiredPeriods", null)
    }
    if (sourceVersion < 13 && table in setOf("task_steps", "task_step_snapshots")) {
        default("notes", "")
        // Weight existed in the historical schema, but tolerate hand-edited or
        // partially exported legacy rows that omitted it. Whip no longer exposes
        // weighted subtasks, so every restored value is normalized to equal weight.
        default("weight", 1.0)
        put("weight", 1.0)
    }
    if (table == "tasks" && sourceVersion < 14) {
        default("priority", "None")
        default("area", "")
        default("tagsCsv", "")
        default("deadlineEpochDay", null)
        default("recurrenceAnchor", "Schedule")
        default("reminderOffsetsMinutesCsv", if (getAsInteger("reminderEnabled") == 1) "0" else "")
        default("locationReminderEnabled", 0)
        default("locationName", "")
        default("locationLatitude", null)
        default("locationLongitude", null)
        default("locationRadiusMeters", 150f)
        default("locationTrigger", "Arrive")
    }
    if (sourceVersion < 15) when (table) {
        "workout_exercises" -> {
            default("machineId", null)
            default("machineNameSnapshot", "")
            default("machineLoadTypeSnapshot", "")
            default("machineUnitIdSnapshot", "")
            default("machineLevelLabelSnapshot", "")
        }
        "workout_sets", "routine_sets" -> default("machineLoadValue", null)
        "routine_exercises", "personal_records" -> default("machineId", null)
    }
    if (sourceVersion < 16) when (table) {
        "exercises" -> default("loadInterpretation", "Total")
        "gym_machines" -> {
            default("loadInterpretation", "Total")
            default("baseLoadKg", null)
        }
        "workout_exercises" -> {
            default("loadInterpretationSnapshot", "Total")
            default("baseLoadKgSnapshot", null)
        }
    }
    if (table == "tasks" && sourceVersion < 17) default("missedOccurrencePolicy", "KeepLatest")
    if (table == "workout_exercises" && sourceVersion < 18) {
        default("trackingTypeSnapshot", "WeightReps")
        default("bodyweightLoadPolicySnapshot", "ExternalWeightOnly")
        default("effectiveBodyweightPercentSnapshot", 100.0)
        default("oneRepMaxFormulaSnapshot", "Epley")
        default("includeInVolumeSnapshot", 1)
        default("includeInPersonalRecordsSnapshot", 1)
    }
    if (sourceVersion < 19) when (table) {
        "gym_machines" -> {
            default("configurationGroupId", getAsString("uuid") ?: "")
            default("configurationVersion", 1)
            default("seatPosition", "")
            default("backPosition", "")
            default("attachment", "")
            default("pulleyRatio", 1.0)
            default("stackMode", "Single")
            default("addOnPlateKg", null)
            default("stackLabelsCsv", "")
            default("massMappingCsv", "")
            default("compatibleForComparison", 0)
        }
        "workout_exercises" -> {
            default("exerciseWeightUnitSnapshot", "kilogram")
            default("loadMultiplierSnapshot", if (getAsString("loadInterpretationSnapshot") in setOf("PerHand", "PerSide")) 2.0 else 1.0)
            default("machineConfigurationGroupSnapshot", "")
            default("machineConfigurationVersionSnapshot", 1)
            default("machineConfigurationSnapshot", "")
            default("machinePulleyRatioSnapshot", 1.0)
            default("machineStackModeSnapshot", "Single")
            default("machineAddOnPlateKgSnapshot", null)
            default("machineMassMappingCsvSnapshot", "")
        }
        "workout_sets" -> {
            default("unilateral", 0)
            val prescribed = getAsInteger("planned") == 1
            default("prescribedCanonicalWeightKg", getAsDouble("canonicalWeightKg").takeIf { prescribed })
            default("prescribedEnteredWeight", getAsDouble("enteredWeight").takeIf { prescribed })
            default("prescribedWeightUnitId", getAsString("enteredWeightUnitId").takeIf { prescribed })
            default("prescribedRepetitions", getAsInteger("repetitions").takeIf { prescribed })
            default("prescribedRpe", getAsDouble("rpe").takeIf { prescribed })
            default("prescribedRir", getAsDouble("rir").takeIf { prescribed })
            default("prescribedDurationSeconds", getAsLong("durationSeconds").takeIf { prescribed })
            default("prescribedMachineLoadValue", getAsDouble("machineLoadValue").takeIf { prescribed })
            default("prescribedRepetitionsMax", null)
        }
        "routine_sets" -> {
            default("unilateral", 0)
            default("repetitionsMax", null)
        }
    }
    if (table == "tasks" && sourceVersion < 20) {
        default("inbox", 0)
        default("durationMinutes", null)
        default("effort", "Moderate")
    }
    if (table == "habits" && sourceVersion < 21) default("sourceMetricId", null)
    if (sourceVersion < 22) when (table) {
        "workout_exercises" -> default("machineProfileUuidSnapshot", null)
        "routine_exercises" -> {
            default("equipmentBindingState", if (getAsLong("machineId") == null) "None" else "Resolved")
            default("machineProfileUuidSnapshot", null)
            default("machineNameSnapshot", "")
            default("machineLoadTypeSnapshot", "")
            default("machineUnitIdSnapshot", "")
            default("machineLevelLabelSnapshot", "")
            default("machineLoadInterpretationSnapshot", "Total")
            default("machineConfigurationGroupSnapshot", "")
            default("machineConfigurationVersionSnapshot", 1)
            default("machineConfigurationSnapshot", "")
        }
        "personal_records" -> default("machineProfileUuidSnapshot", null)
    }
    if (sourceVersion < 25) when (table) {
        "routine_exercises" -> {
            default("trainingMaxPercent", 90.0)
            default("progressionPercentagesCsv", "")
            default("alternativeExerciseIdsCsv", "")
        }
        "routine_sets" -> {
            default("loadPrescriptionType", "Absolute")
            default("loadPercentage", null)
        }
        "workout_exercises" -> default("alternativeExerciseIdsCsvSnapshot", "")
        "workout_sets" -> default("prescriptionSourceLabel", "")
    }
    if (sourceVersion < 26) when (table) {
        "tasks" -> {
            default("uuid", "legacy-task-${getAsLong("id") ?: 0L}")
            default("manualPosition", (getAsLong("id") ?: 0L).toInt())
        }
        "task_steps" -> default("uuid", "legacy-task-step-${getAsLong("id") ?: 0L}")
    }
    if (sourceVersion < 27) when (table) {
        "areas" -> default("nameKey", getAsString("name").orEmpty().trim().lowercase())
        "tasks", "habits", "goals" -> default("areaId", null)
    }
}

private fun backfillLegacyAreas(db: androidx.sqlite.db.SupportSQLiteDatabase) {
    listOf("tasks", "habits", "goals").forEach { table ->
        db.execSQL(
            "INSERT OR IGNORE INTO areas " +
                "(id, name, nameKey, colorArgb, position, archived, createdAtMillis, updatedAtMillis) " +
                "SELECT 'legacy-area-' || lower(hex(randomblob(16))), trim(area), lower(trim(area)), " +
                "NULL, 100000, 0, 0, 0 FROM $table WHERE trim(area) <> '' GROUP BY lower(trim(area))",
        )
        db.execSQL(
            "UPDATE $table SET areaId = " +
                "(SELECT id FROM areas WHERE nameKey = lower(trim($table.area)) LIMIT 1) " +
                "WHERE areaId IS NULL AND trim(area) <> ''",
        )
        db.execSQL(
            "UPDATE $table SET area = COALESCE((SELECT name FROM areas WHERE id = $table.areaId), '')",
        )
    }
}

private fun normalizeLegacyMachineScopes(db: androidx.sqlite.db.SupportSQLiteDatabase) {
    db.execSQL(
        """
        UPDATE workout_exercises SET machineProfileUuidSnapshot = CASE
            WHEN machineId IS NULL THEN NULL
            ELSE COALESCE((SELECT uuid FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId),
                'legacy-machine-id:' || machineId)
        END
        """.trimIndent(),
    )
    db.execSQL(
        """
        UPDATE routine_exercises SET
            equipmentBindingState = CASE
                WHEN machineId IS NULL THEN 'None'
                WHEN EXISTS(SELECT 1 FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId) THEN 'Resolved'
                ELSE 'NeedsEquipment'
            END,
            machineProfileUuidSnapshot = COALESCE((SELECT uuid FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId),
                CASE WHEN machineId IS NULL THEN NULL ELSE 'legacy-machine-id:' || machineId END),
            machineNameSnapshot = COALESCE((SELECT CASE WHEN location = '' THEN name ELSE name || ' · ' || location END FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), machineNameSnapshot),
            machineLoadTypeSnapshot = COALESCE((SELECT loadType FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), machineLoadTypeSnapshot),
            machineUnitIdSnapshot = COALESCE((SELECT unitId FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), machineUnitIdSnapshot),
            machineLevelLabelSnapshot = COALESCE((SELECT levelLabel FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), machineLevelLabelSnapshot),
            machineLoadInterpretationSnapshot = COALESCE((SELECT loadInterpretation FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), machineLoadInterpretationSnapshot),
            machineConfigurationGroupSnapshot = COALESCE((SELECT configurationGroupId FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), machineConfigurationGroupSnapshot),
            machineConfigurationVersionSnapshot = COALESCE((SELECT configurationVersion FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), machineConfigurationVersionSnapshot)
        """.trimIndent(),
    )
    db.execSQL(
        """
        UPDATE personal_records SET machineProfileUuidSnapshot = COALESCE(
            (SELECT uuid FROM gym_machines WHERE gym_machines.id = personal_records.machineId),
            (SELECT we.machineProfileUuidSnapshot FROM workout_sets ws
                JOIN workout_exercises we ON we.id = ws.workoutExerciseId
                WHERE ws.id = personal_records.sourceSetId),
            CASE WHEN machineId IS NULL THEN NULL ELSE 'legacy-machine-id:' || machineId END
        )
        """.trimIndent(),
    )
    listOf("workout_exercises", "routine_exercises", "personal_records").forEach { table ->
        db.execSQL(
            "UPDATE $table SET machineId = NULL WHERE machineId IS NOT NULL " +
                "AND NOT EXISTS(SELECT 1 FROM gym_machines WHERE gym_machines.id = $table.machineId)",
        )
    }
}

private data class MergeForeignKey(
    val childColumn: String,
    val parentTable: String,
)

private data class MergeTableMetadata(
    val columns: Set<String>,
    val autoNumericId: Boolean,
    val foreignKeys: List<MergeForeignKey>,
)

private data class ExistingMergeRow(val numericId: Long?)

private fun mergeTableMetadata(
    db: androidx.sqlite.db.SupportSQLiteDatabase,
    table: String,
): MergeTableMetadata {
    val columns = linkedSetOf<String>()
    var autoNumericId = false
    db.query("PRAGMA table_info(${safeIdentifier(table)})").use { cursor ->
        val nameIndex = cursor.getColumnIndexOrThrow("name")
        val typeIndex = cursor.getColumnIndexOrThrow("type")
        val pkIndex = cursor.getColumnIndexOrThrow("pk")
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameIndex)
            columns += name
            if (name == "id" && cursor.getInt(pkIndex) > 0 && cursor.getString(typeIndex).contains("INT", true)) {
                autoNumericId = true
            }
        }
    }
    val foreignKeys = buildList {
        db.query("PRAGMA foreign_key_list(${safeIdentifier(table)})").use { cursor ->
            val parentIndex = cursor.getColumnIndexOrThrow("table")
            val fromIndex = cursor.getColumnIndexOrThrow("from")
            val toIndex = cursor.getColumnIndexOrThrow("to")
            while (cursor.moveToNext()) {
                if (cursor.getString(toIndex) == "id") {
                    add(MergeForeignKey(cursor.getString(fromIndex), cursor.getString(parentIndex)))
                }
            }
        }
    }
    return MergeTableMetadata(columns, autoNumericId, foreignKeys)
}

private fun JSONObject.stableMergeKey(metadata: MergeTableMetadata): Pair<String, String>? = when {
    "uuid" in metadata.columns && has("uuid") && !isNull("uuid") && optString("uuid").isNotBlank() ->
        "uuid" to optString("uuid")
    !metadata.autoNumericId && "id" in metadata.columns && has("id") && opt("id") is String && optString("id").isNotBlank() ->
        "id" to optString("id")
    else -> null
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (!has(key) || isNull(key) || opt(key) !is Number) null else optLong(key)

private fun remapForeignKeys(
    values: ContentValues,
    metadata: MergeTableMetadata,
    idMaps: Map<String, Map<Long, Long>>,
) {
    metadata.foreignKeys.forEach { key ->
        val old = values.getAsLong(key.childColumn) ?: return@forEach
        idMaps[key.parentTable]?.get(old)?.let { values.put(key.childColumn, it) }
    }
}

private fun remapPolymorphicReferences(
    table: String,
    values: ContentValues,
    idMaps: Map<String, Map<Long, Long>>,
) {
    fun remap(column: String, parent: String) {
        val old = values.getAsLong(column) ?: return
        idMaps[parent]?.get(old)?.let { values.put(column, it) }
    }
    fun sourceParent(type: String?): String? = when (type) {
        "Task", "Subtask" -> "tasks"
        "Habit" -> "habits"
        "Workout" -> "workout_sessions"
        "Exercise" -> "exercises"
        else -> null
    }
    fun remapCsv(column: String, parent: String) {
        val original = values.getAsString(column) ?: return
        values.put(
            column,
            original.split(',').mapNotNull(String::trim).mapNotNull(String::toLongOrNull)
                .map { idMaps[parent]?.get(it) ?: it }.distinct().joinToString(","),
        )
    }
    when (table) {
        "task_step_snapshots" -> remap("stepId", "task_steps")
        "workout_sessions" -> remap("sourceRoutineId", "gym_routines")
        "workout_exercises" -> {
            remap("machineId", "gym_machines")
            remapCsv("alternativeExerciseIdsCsvSnapshot", "exercises")
        }
        "routine_exercises" -> {
            remap("machineId", "gym_machines")
            remapCsv("alternativeExerciseIdsCsv", "exercises")
        }
        "personal_records" -> remap("machineId", "gym_machines")
        "graph_presets" -> remapCsv("exerciseIdsCsv", "exercises")
        "link_rules" -> {
            sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
            if (values.getAsString("sourceType") == "Subtask") remap("sourceItemId", "task_steps")
        }
        "contributions" -> sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
        "trigger_rules" -> {
            sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
            when (values.getAsString("targetType")) {
                "Habit" -> remap("targetEntityId", "habits")
                "Task" -> remap("targetEntityId", "tasks")
            }
        }
    }
}

private fun Cursor.toJsonRow(): JSONObject = JSONObject().also { row ->
    columnNames.indices.forEach { index ->
        val value: Any = if (isNull(index)) JSONObject.NULL else when (getType(index)) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
            Cursor.FIELD_TYPE_BLOB -> JSONObject().put("blobBase64", Base64.encodeToString(getBlob(index), Base64.NO_WRAP))
            else -> getString(index)
        }
        row.put(columnNames[index], value)
    }
}

private fun JSONObject.toContentValues(): ContentValues = ContentValues().also { values ->
    keys().forEach { key ->
        val value = get(key)
        when (value) {
            JSONObject.NULL -> values.putNull(key)
            is Int -> values.put(key, value)
            is Long -> values.put(key, value)
            is Double -> values.put(key, value)
            is Boolean -> values.put(key, value)
            is String -> values.put(key, value)
            is JSONObject -> if (value.has("blobBase64")) values.put(key, Base64.decode(value.getString("blobBase64"), Base64.DEFAULT)) else error("Unsupported object in $key")
            else -> error("Unsupported backup value in $key")
        }
    }
}

private fun safeIdentifier(value: String): String {
    require(value.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) { "Unsafe database identifier" }
    return value
}

private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

private const val BACKUP_FORMAT = "whip-backup"
private const val ENVELOPE_VERSION = 2
private const val DATABASE_VERSION = 27

private val EXPORT_TABLES = listOf(
    "areas",
    "tasks", "task_occurrences", "task_steps", "task_step_states", "task_step_snapshots",
    "unit_definitions", "metric_definitions", "metric_entries", "tags", "entity_tag_links",
    "exercises", "exercise_categories", "exercise_category_joins", "gym_machines",
    "gym_routines", "routine_days", "routine_exercises", "routine_sets",
    "workout_sessions", "workout_groups", "workout_exercises", "workout_sets",
    "personal_records", "graph_presets", "habits", "habit_checklist_items", "habit_logs",
    "habit_checklist_states", "habit_pauses", "goals", "goal_milestones", "goal_completion_snapshots",
    "link_rules", "contributions", "trigger_rules", "trigger_occurrences",
)

private fun checksumPayload(tables: JSONObject, settings: JSONObject?, envelopeVersion: Int): String =
    if (envelopeVersion == 1) tables.toString() else tables.toString() + "\n" + settings?.toString().orEmpty()

private fun AppSettings.toJson(): JSONObject = JSONObject()
    .put("setupCompleted", setupCompleted)
    .put("powerMode", powerMode)
    .put("lowPressureMode", lowPressureMode)
    .put("backupPrivacyChoice", backupPrivacyChoice)
    .put("backupPrivacyChoiceHandled", backupPrivacyChoiceHandled)
    .put("notificationPermissionRequested", notificationPermissionRequested)
    .put("activeAreaScope", activeAreaScope)
    .put("hardSetClassifications", JSONArray(hardSetClassifications.toList()))
    .put("categoryAllocationMode", categoryAllocationMode)
    .put("adjustE1rmForEffort", adjustE1rmForEffort)
    .put("includeAssistedInPersonalRecords", includeAssistedInPersonalRecords)
    .put("themeMode", themeMode.name)
    .put("dynamicColor", dynamicColor)
    .put("firstDayOfWeek", firstDayOfWeek.name)
    .put("timeZoneId", timeZoneId ?: JSONObject.NULL)
    .put("dayCutoffMinutes", dayCutoffMinutes)
    .put("massUnitId", massUnitId)
    .put("distanceUnitId", distanceUnitId)
    .put("volumeUnitId", volumeUnitId)
    .put("gymWeightUnitId", gymWeightUnitId)
    .put("numberPrecision", numberPrecision)
    .put("oneRepMaxFormula", oneRepMaxFormula)
    .put("oneRepMaxRepCutoff", oneRepMaxRepCutoff)
    .put("defaultRestSeconds", defaultRestSeconds)
    .put("timerSound", timerSound)
    .put("timerVibration", timerVibration)
    .put("keepScreenAwake", keepScreenAwake)
    .put("restTimerAutoStart", restTimerAutoStart)
    .put("showGymRpe", showGymRpe)
    .put("showGymRir", showGymRir)
    .put("showGymTempo", showGymTempo)
    .put("includeWarmupsInGymStats", includeWarmupsInGymStats)
    .putNullable("quietStartMinutes", quietStartMinutes)
    .putNullable("quietEndMinutes", quietEndMinutes)
    .put("homeSections", JSONArray(homeSections.map(HomeSection::name)))
    .put("hiddenHomeSections", JSONArray(hiddenHomeSections.map(HomeSection::name)))
    .put("collapsedHomeSections", JSONArray(collapsedHomeSections.map(HomeSection::name)))
    .put("healthConnectEnabled", healthConnectEnabled)
    .put("healthDataTypes", JSONArray(healthDataTypes.map(HealthDataType::name)))
    .put("healthSyncDays", healthSyncDays)
    .put("reviewPeriod", reviewPeriod.name)
    .put("defaultTaskStepPolicy", defaultTaskStepPolicy.name)
    .put("showAllUpcomingTaskOccurrences", showAllUpcomingTaskOccurrences)
    .put("showHabitsInTaskPlanning", showHabitsInTaskPlanning)
    .put("defaultHabitWeekStart", defaultHabitWeekStart.name)
    .put("defaultAvoidMissingPolicy", defaultAvoidMissingPolicy.name)
    .put("naturalLanguageTaskCapture", naturalLanguageTaskCapture)
    .put("savedTaskFilters", JSONArray(savedTaskFilters.map { filter ->
        JSONObject()
            .put("name", filter.name)
            .put("priorities", JSONArray(filter.priorities.map(TaskPriority::name)))
            .put("area", filter.area)
            .put("areaId", filter.areaId ?: JSONObject.NULL)
            .put("tag", filter.tag)
            .put("pinnedOnly", filter.pinnedOnly)
            .put("tags", JSONArray(filter.tags.toList()))
            .put("requireAllTags", filter.requireAllTags)
            .put("dateMode", filter.dateMode)
            .put("deadlineOnly", filter.deadlineOnly)
            .put("inboxOnly", filter.inboxOnly)
            .put("efforts", JSONArray(filter.efforts.map(TaskEffort::name)))
            .putNullable("maximumDurationMinutes", filter.maximumDurationMinutes)
            .put("textQuery", filter.textQuery)
            .put("destination", filter.destination)
            .put("planningView", filter.planningView)
            .put("sortMode", filter.sortMode)
            .put("groupMode", filter.groupMode)
    }))
    .put("homeTaskFilterName", homeTaskFilterName ?: JSONObject.NULL)
    .put("savedReviewFilters", JSONArray(savedReviewFilters.map { filter ->
        JSONObject().put("name", filter.name)
            .put("sections", JSONArray(filter.sections.map(HomeSection::name)))
    }))
    .put("selectedReviewFilterName", selectedReviewFilterName ?: JSONObject.NULL)
    .put("reviewSections", JSONArray(reviewSections.map(HomeSection::name)))
    .put("gymCompactSetRows", gymCompactSetRows)
    .put("platePresets", JSONArray(platePresets.map { preset ->
        JSONObject().put("name", preset.name).put("unitId", preset.unitId)
            .put("barWeight", preset.barWeight).put("plates", JSONArray(preset.plates))
    }))
    .put("repPrescriptionSchemes", JSONArray(repPrescriptionSchemes.map { scheme ->
        JSONObject()
            .put("id", scheme.id)
            .put("name", scheme.name)
            .put("setCount", scheme.setCount)
            .put("repetitionsMin", scheme.repetitionsMin)
            .put("repetitionsMax", scheme.repetitionsMax)
            .put("classification", scheme.classification.name)
            .putNullable("restSeconds", scheme.restSeconds)
    }))
    .put("locationRemindersEnabled", locationRemindersEnabled)
    .putNullableLong("focusTimerDeadlineMillis", focusTimerDeadlineMillis)
    .putNullableLong("focusTimerTaskId", focusTimerTaskId)

private fun JSONObject.toAppSettings(): AppSettings = AppSettings(
    setupCompleted = optBoolean("setupCompleted", true),
    powerMode = optBoolean("powerMode", true),
    lowPressureMode = optBoolean("lowPressureMode", false),
    backupPrivacyChoice = optString("backupPrivacyChoice", "Later"),
    backupPrivacyChoiceHandled = optBoolean("backupPrivacyChoiceHandled", true),
    notificationPermissionRequested = optBoolean("notificationPermissionRequested", false),
    activeAreaScope = optString("activeAreaScope", "all"),
    hardSetClassifications = optJSONArray("hardSetClassifications")?.let { array ->
        (0 until array.length()).mapNotNullTo(mutableSetOf()) { index -> array.optString(index).takeIf(String::isNotBlank) }
    }?.ifEmpty { setOf("Working", "BackOff", "Drop", "Amrap", "Failure") }
        ?: setOf("Working", "BackOff", "Drop", "Amrap", "Failure"),
    categoryAllocationMode = optString("categoryAllocationMode", "Fractional"),
    adjustE1rmForEffort = optBoolean("adjustE1rmForEffort", false),
    includeAssistedInPersonalRecords = optBoolean("includeAssistedInPersonalRecords", false),
    themeMode = enumValue("themeMode", AppThemeMode.System),
    dynamicColor = optBoolean("dynamicColor", true),
    firstDayOfWeek = enumValue("firstDayOfWeek", DayOfWeek.MONDAY),
    timeZoneId = optString("timeZoneId").takeUnless { !has("timeZoneId") || isNull("timeZoneId") || runCatching { java.time.ZoneId.of(it) }.isFailure },
    dayCutoffMinutes = optInt("dayCutoffMinutes", 0).coerceIn(0, 1439),
    massUnitId = optString("massUnitId", "kilogram"),
    distanceUnitId = optString("distanceUnitId", "kilometre"),
    volumeUnitId = optString("volumeUnitId", "litre"),
    gymWeightUnitId = optString("gymWeightUnitId", "kilogram"),
    numberPrecision = optInt("numberPrecision", 1).coerceIn(0, 6),
    oneRepMaxFormula = optString("oneRepMaxFormula", "Epley"),
    oneRepMaxRepCutoff = optInt("oneRepMaxRepCutoff", 10).coerceIn(1, 36),
    defaultRestSeconds = optInt("defaultRestSeconds", 120).coerceAtLeast(0),
    timerSound = optBoolean("timerSound", true),
    timerVibration = optBoolean("timerVibration", true),
    keepScreenAwake = optBoolean("keepScreenAwake", false),
    restTimerAutoStart = optBoolean("restTimerAutoStart", true),
    showGymRpe = optBoolean("showGymRpe", true),
    showGymRir = optBoolean("showGymRir", true),
    showGymTempo = optBoolean("showGymTempo", true),
    includeWarmupsInGymStats = optBoolean("includeWarmupsInGymStats", false),
    quietStartMinutes = nullableInt("quietStartMinutes"),
    quietEndMinutes = nullableInt("quietEndMinutes"),
    homeSections = enumList("homeSections", HomeSection.entries).takeIf { it.toSet() == HomeSection.entries.toSet() }
        ?: HomeSection.entries,
    hiddenHomeSections = enumSet("hiddenHomeSections", HomeSection.entries),
    collapsedHomeSections = enumSet("collapsedHomeSections", HomeSection.entries),
    healthConnectEnabled = optBoolean("healthConnectEnabled", false),
    healthDataTypes = if (has("healthDataTypes")) enumSet("healthDataTypes", HealthDataType.entries) else HealthDataType.entries.toSet(),
    healthSyncDays = optInt("healthSyncDays", 30).coerceIn(1, 365),
    reviewPeriod = enumValue("reviewPeriod", ReviewPeriod.Weekly),
    defaultTaskStepPolicy = enumValue("defaultTaskStepPolicy", RepeatStepPolicy.Reset),
    showAllUpcomingTaskOccurrences = optBoolean("showAllUpcomingTaskOccurrences", false),
    showHabitsInTaskPlanning = optBoolean("showHabitsInTaskPlanning", false),
    defaultHabitWeekStart = enumValue("defaultHabitWeekStart", DayOfWeek.MONDAY),
    defaultAvoidMissingPolicy = enumValue("defaultAvoidMissingPolicy", AvoidMissingPolicy.Unknown),
    naturalLanguageTaskCapture = optBoolean("naturalLanguageTaskCapture", false),
    savedTaskFilters = optJSONArray("savedTaskFilters").objects().mapNotNull { value ->
        value.optString("name").takeIf(String::isNotBlank)?.let { name ->
            SavedTaskFilter(
                name = name,
                priorities = value.optJSONArray("priorities").enumNames<TaskPriority>(),
                area = value.optString("area"),
                areaId = value.nullableString("areaId"),
                tag = value.optString("tag"),
                pinnedOnly = value.optBoolean("pinnedOnly", false),
                tags = value.optJSONArray("tags")?.let { array ->
                    (0 until array.length()).mapNotNullTo(linkedSetOf()) { index -> array.optString(index).takeIf(String::isNotBlank) }
                }.orEmpty(),
                requireAllTags = value.optBoolean("requireAllTags", true),
                dateMode = value.optString("dateMode", "Any"),
                deadlineOnly = value.optBoolean("deadlineOnly", false),
                inboxOnly = value.optBoolean("inboxOnly", false),
                efforts = value.optJSONArray("efforts").enumNames<TaskEffort>(),
                maximumDurationMinutes = value.nullableInt("maximumDurationMinutes"),
                textQuery = value.optString("textQuery"),
                destination = value.optString("destination"),
                planningView = value.optString("planningView", "List"),
                sortMode = value.optString("sortMode", "Smart"),
                groupMode = value.optString("groupMode", "None"),
            )
        }
    },
    homeTaskFilterName = nullableString("homeTaskFilterName"),
    savedReviewFilters = optJSONArray("savedReviewFilters").objects().mapNotNull { value ->
        value.optString("name").takeIf(String::isNotBlank)?.let { name ->
            SavedReviewFilter(name, value.optJSONArray("sections").enumNames<HomeSection>().ifEmpty { HomeSection.entries.toSet() })
        }
    },
    selectedReviewFilterName = nullableString("selectedReviewFilterName"),
    reviewSections = optJSONArray("reviewSections").enumNames<HomeSection>().ifEmpty { HomeSection.entries.toSet() },
    gymCompactSetRows = optBoolean("gymCompactSetRows", false),
    platePresets = optJSONArray("platePresets").objects().mapNotNull { value ->
        val name = value.optString("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
        val plates = value.optJSONArray("plates").doubles().filter { it > 0 }
        PlatePreset(name, value.optString("unitId", "kilogram"), value.optDouble("barWeight", 20.0), plates)
            .takeIf { plates.isNotEmpty() }
    },
    repPrescriptionSchemes = optJSONArray("repPrescriptionSchemes").objects().mapNotNull { value ->
        RepPrescriptionScheme(
            id = value.optString("id"),
            name = value.optString("name"),
            setCount = value.optInt("setCount"),
            repetitionsMin = value.optInt("repetitionsMin"),
            repetitionsMax = value.optInt("repetitionsMax"),
            classification = runCatching {
                com.whip.app.domain.WorkoutSetClassification.valueOf(value.optString("classification"))
            }.getOrNull() ?: return@mapNotNull null,
            restSeconds = value.nullableInt("restSeconds"),
        ).takeIf(RepPrescriptionScheme::isValid)
    }.distinctBy(RepPrescriptionScheme::id),
    locationRemindersEnabled = optBoolean("locationRemindersEnabled", true),
    focusTimerDeadlineMillis = nullableLong("focusTimerDeadlineMillis"),
    focusTimerTaskId = nullableLong("focusTimerTaskId"),
)

private fun JSONObject.putNullable(key: String, value: Int?): JSONObject = put(key, value ?: JSONObject.NULL)
private fun JSONObject.putNullableLong(key: String, value: Long?): JSONObject = put(key, value ?: JSONObject.NULL)
private fun JSONObject.nullableInt(key: String): Int? = if (!has(key) || isNull(key)) null else optInt(key)
private fun JSONObject.nullableLong(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
private fun JSONObject.nullableString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)
private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else
    (0 until length()).mapNotNull { optJSONObject(it) }
private fun JSONArray?.doubles(): List<Double> = if (this == null) emptyList() else
    (0 until length()).mapNotNull { index -> optDouble(index).takeUnless(Double::isNaN) }
private inline fun <reified T : Enum<T>> JSONArray?.enumNames(): Set<T> = if (this == null) emptySet() else
    (0 until length()).mapNotNullTo(linkedSetOf()) { index -> runCatching { enumValueOf<T>(optString(index)) }.getOrNull() }
private inline fun <reified T : Enum<T>> JSONObject.enumValue(key: String, default: T): T =
    runCatching { enumValueOf<T>(optString(key)) }.getOrDefault(default)
private fun <T : Enum<T>> JSONObject.enumList(key: String, values: List<T>): List<T> {
    val byName = values.associateBy { it.name }
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).mapNotNull { byName[array.optString(it)] }
}
private fun <T : Enum<T>> JSONObject.enumSet(key: String, values: List<T>): Set<T> = enumList(key, values).toSet()
