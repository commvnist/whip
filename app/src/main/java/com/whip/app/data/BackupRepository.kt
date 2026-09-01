package com.whip.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.withTransaction
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.AreaOpeningMode
import com.whip.app.core.HealthDataType
import com.whip.app.core.HomeSection
import com.whip.app.core.ReviewSection
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.SavedTaskFilter
import com.whip.app.core.normalizedNavigation
import com.whip.app.core.PlatePreset
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.core.TrackedGymRecord
import com.whip.app.core.SettingsRepository
import com.whip.app.core.SystemWhipClock
import com.whip.app.core.UuidWhipIdGenerator
import com.whip.app.core.DEFAULT_REST_TIMER_PRESET_SECONDS
import com.whip.app.core.normalizeRestTimerPresets
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.DEFAULT_GOAL_EMOJI
import com.whip.app.domain.DEFAULT_HABIT_EMOJI
import com.whip.app.domain.DEFAULT_TASK_EMOJI
import com.whip.app.domain.DEFAULT_TRACK_EMOJI
import com.whip.app.domain.normalizedIdentityEmoji
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
    val restoreCompatible: Boolean = databaseVersion in OLDEST_COMPATIBLE_DATABASE_VERSION..BACKUP_DATABASE_VERSION,
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
    suspend fun exportTracksCsv(): String
    suspend fun deleteAllData()
}

class RoomBackupRepository(
    private val database: WhipDatabase,
    private val settingsRepository: SettingsRepository? = null,
    private val areaRepository: AreaRepository? = null,
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
        retireAutomationBackupRows(tables)
        val settings = settingsRepository?.current()?.toJson()
        val payload = checksumPayload(tables, settings)
        JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("envelopeVersion", ENVELOPE_VERSION)
            .put("databaseVersion", BACKUP_DATABASE_VERSION)
            .put("exportedAt", Instant.now().toString())
            .put("checksumSha256", sha256(payload))
            .put("tables", tables)
            .apply { settings?.let { put("settings", it) } }
            .toString(2)
    }

    override suspend fun previewBackup(json: String): BackupPreview {
        val root = parseAndValidate(json)
        val tables = root.getJSONObject("tables")
        val settings = root.optJSONObject("settings")
        val checksumValid = root.getString("checksumSha256") == sha256(checksumPayload(tables, settings))
        if (checksumValid) upgradeBackupTables(root.getInt("databaseVersion"), tables)
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
        val envelopeVersion = root.getInt("envelopeVersion")
        val databaseVersion = root.getInt("databaseVersion")
        return BackupPreview(
            envelopeVersion = envelopeVersion,
            databaseVersion = databaseVersion,
            exportedAt = Instant.parse(root.getString("exportedAt")),
            tableCounts = counts,
            totalRecords = counts.values.sum(),
            duplicateStableIds = duplicates,
            checksumValid = checksumValid,
            settingsIncluded = settings != null,
            restoreCompatible = true,
            compatibilityMessage = if (databaseVersion < BACKUP_DATABASE_VERSION) {
                "This backup will be upgraded to the current Whip backup format during restore."
            } else {
                null
            },
        )
    }

    override suspend fun restoreBackup(json: String) {
        val root = parseAndValidate(json)
        val tables = root.getJSONObject("tables")
        val settings = root.optJSONObject("settings")
        require(
            root.getString("checksumSha256") == sha256(
                checksumPayload(tables, settings),
            ),
        ) { "Backup checksum does not match" }
        upgradeBackupTables(root.getInt("databaseVersion"), tables)
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            EXPORT_TABLES.asReversed().forEach { table -> db.execSQL("DELETE FROM ${safeIdentifier(table)}") }
            EXPORT_TABLES.forEach { table ->
                val rows = tables.getJSONArray(table)
                for (index in 0 until rows.length()) {
                    val values = rows.getJSONObject(index).toContentValues()
                    values.applyBackupCompatibilityDefaults(table)
                    values.retireAutomation(table)
                    values.normalizeIdentityEmoji(table)
                    val result = db.insert(safeIdentifier(table), SQLiteDatabase.CONFLICT_ABORT, values)
                    require(result != -1L) { "Could not restore $table row ${index + 1}" }
                }
            }
        }
        settings?.let { restored -> settingsRepository?.update { restored.toAppSettings() } }
        areaRepository?.ensureDefaultArea()
        RoomTrackRepository(database, SystemWhipClock, UuidWhipIdGenerator).rebuildSearchIndex()
    }

    override suspend fun mergeBackup(json: String): BackupMergeSummary {
        val root = parseAndValidate(json)
        val tables = root.getJSONObject("tables")
        val settings = root.optJSONObject("settings")
        require(
            root.getString("checksumSha256") == sha256(
                checksumPayload(tables, settings),
            ),
        ) { "Backup checksum does not match" }
        upgradeBackupTables(root.getInt("databaseVersion"), tables)
        val summary = database.withTransaction {
            val db = database.openHelper.writableDatabase
            val idMaps = mutableMapOf<String, MutableMap<Long, Long>>()
            val areaIdMap = mutableMapOf<String, String>()
            val pendingEntryOccurrenceLinks = mutableListOf<Pair<Long, Long>>()
            var imported = 0
            var skipped = 0
            EXPORT_TABLES.forEach { table ->
                val rows = tables.getJSONArray(table)
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
                    val stable = row.stableMergeKey(metadata)
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
                    val values = row.toContentValues()
                    values.applyBackupCompatibilityDefaults(table)
                    values.retireAutomation(table)
                    values.normalizeIdentityEmoji(table)
                    val sourceOccurrenceId = if (table == "track_entries") {
                        values.getAsLong("sourceOccurrenceId")?.also { values.putNull("sourceOccurrenceId") }
                    } else {
                        null
                    }
                    if (table in setOf("tasks", "habits", "goals", "tracks")) {
                        values.getAsString("areaId")?.let { sourceId -> values.put("areaId", areaIdMap[sourceId] ?: sourceId) }
                    }
                    remapForeignKeys(values, metadata, idMaps)
                    remapPolymorphicReferences(table, values, idMaps)
                    if (metadata.autoNumericId) values.remove("id")
                    val result = db.insert(safeIdentifier(table), SQLiteDatabase.CONFLICT_IGNORE, values)
                    if (result == -1L) {
                        if (metadata.autoNumericId && sourceNumericId != null) {
                            existingNaturalChildId(db, table, values)?.let { targetId ->
                                idMaps.getOrPut(table, ::mutableMapOf)[sourceNumericId] = targetId
                            }
                        }
                        skipped++
                    } else {
                        imported++
                        if (table == "areas") row.optString("id").takeIf(String::isNotBlank)?.let { areaIdMap[it] = it }
                        if (metadata.autoNumericId && sourceNumericId != null) {
                            idMaps.getOrPut(table, ::mutableMapOf)[sourceNumericId] = result
                        }
                        if (table == "track_entries" && sourceOccurrenceId != null) {
                            pendingEntryOccurrenceLinks += result to sourceOccurrenceId
                        }
                    }
                }
            }
            val sourceOccurrences = tables.getJSONArray("trigger_occurrences")
            pendingEntryOccurrenceLinks.forEach { (entryId, sourceOccurrenceId) ->
                val targetOccurrenceId = idMaps["trigger_occurrences"]?.get(sourceOccurrenceId)
                    ?: sourceOccurrences.findObjectByNumericId(sourceOccurrenceId)?.let { source ->
                        val sourceRuleId = source.optLongOrNull("triggerRuleId") ?: return@let null
                        val targetRuleId = idMaps["trigger_rules"]?.get(sourceRuleId) ?: sourceRuleId
                        db.query(
                            "SELECT id FROM trigger_occurrences WHERE triggerRuleId = ? AND sourceEventId = ? LIMIT 1",
                            arrayOf(targetRuleId.toString(), source.optString("sourceEventId")),
                        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
                    }
                targetOccurrenceId?.let { occurrenceId ->
                    db.execSQL(
                        "UPDATE track_entries SET sourceOccurrenceId = ? WHERE id = ?",
                        arrayOf(occurrenceId, entryId),
                    )
                }
            }
            BackupMergeSummary(imported, skipped)
        }
        areaRepository?.ensureDefaultArea()
        RoomTrackRepository(database, SystemWhipClock, UuidWhipIdGenerator).rebuildSearchIndex()
        return summary
    }

    override suspend fun exportTasksCsv(): String = queryCsv(
        "SELECT t.id, t.title, t.notes, t.scheduleKind, t.dateEpochDay, t.deadlineEpochDay, t.priority, COALESCE(a.name, '') AS area, t.tagsCsv, t.recurrenceUnit, t.recurrenceInterval, t.recurrenceAnchor, t.reminderOffsetsMinutesCsv, t.completedAtMillis, t.archived FROM tasks t LEFT JOIN areas a ON a.id = t.areaId ORDER BY t.id",
    )

    override suspend fun exportHabitsCsv(): String = queryCsv(
        "SELECT h.uuid AS habitUuid, h.name AS habit, COALESCE(a.name, '') AS area, h.trackingMode, l.uuid AS eventUuid, l.localEpochDay, l.value, l.enteredUnitId, l.status, l.note FROM habits h LEFT JOIN areas a ON a.id = h.areaId LEFT JOIN habit_logs l ON l.habitId = h.id UNION ALL SELECT h.uuid, h.name, COALESCE(a.name, ''), h.trackingMode, s.uuid, s.localEpochDay, NULL, NULL, 'Skipped', '' FROM habits h JOIN habit_skips s ON s.habitId = h.id LEFT JOIN areas a ON a.id = h.areaId ORDER BY habitUuid, localEpochDay",
    )

    override suspend fun exportGoalsCsv(): String = queryCsv(
        "SELECT g.uuid AS goalUuid, g.name AS goal, COALESCE(a.name, '') AS area, g.type, g.unitId, g.elapsedStartMillis, g.elapsedDisplayUnit, e.id AS entryId, e.localEpochDay, e.enteredValue, e.enteredUnitId, e.status, e.sourceType, e.note FROM goals g LEFT JOIN areas a ON a.id = g.areaId LEFT JOIN metric_entries e ON e.metricId = g.metricId ORDER BY g.id, e.timestampMillis",
    )

    override suspend fun exportGymCsv(): String = queryCsv(
        "SELECT s.uuid AS workoutUuid, s.localEpochDay, s.name AS workout, e.uuid AS exerciseUuid, e.name AS exercise, e.trackingType, e.archived AS exerciseArchived, we.machineProfileUuidSnapshot AS machineScopeUuid, we.machineNameSnapshot AS machine, we.machineConfigurationGroupSnapshot AS machineConfigurationFamily, we.machineConfigurationVersionSnapshot AS machineConfigurationVersion, we.machineConfigurationSnapshot AS machineConfiguration, we.machineLoadTypeSnapshot AS machineLoadType, ws.uuid AS setUuid, ws.position, ws.classification, ws.machineLoadValue, ws.enteredWeight, ws.enteredWeightUnitId, ws.repetitions, ws.enteredDistance, ws.enteredDistanceUnitId, ws.durationSeconds, ws.rpe, ws.rir, ws.tempo, ws.note FROM exercises e LEFT JOIN workout_exercises we ON we.exerciseId = e.id LEFT JOIN workout_sessions s ON s.id = we.sessionId LEFT JOIN workout_sets ws ON ws.workoutExerciseId = we.id AND ws.deletedAtMillis IS NULL ORDER BY e.position, s.startedAtMillis, we.position, ws.position",
    )

    override suspend fun exportTracksCsv(): String = queryCsv(
        "SELECT t.uuid AS trackUuid, t.name AS track, a.name AS area, e.uuid AS entryUuid, e.entryEpochDay, f.uuid AS fieldUuid, f.name AS field, f.type, v.textValue, v.enteredNumber, v.enteredUnitId, v.canonicalNumber, v.dateEpochDay, v.booleanValue, o.label AS choiceValue, v.scaleValue FROM tracks t JOIN areas a ON a.id = t.areaId LEFT JOIN track_entries e ON e.trackId = t.id LEFT JOIN track_values v ON v.entryId = e.id LEFT JOIN track_fields f ON f.id = v.fieldId LEFT JOIN track_choice_options o ON o.id = v.choiceOptionId ORDER BY t.position, e.entryEpochDay, e.createdAtMillis, f.position",
    )

    override suspend fun deleteAllData() {
        database.clearAllTables()
        areaRepository?.ensureDefaultArea()
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

    private fun parseAndValidate(json: String): JSONObject {
        val root = runCatching { JSONObject(json) }.getOrElse { error("This is not valid JSON") }
        require(root.optString("format") == BACKUP_FORMAT) { "This is not a Whip backup" }
        require(root.optInt("envelopeVersion") == ENVELOPE_VERSION) {
            "This backup uses unsupported envelope version ${root.optInt("envelopeVersion")}; this build requires version $ENVELOPE_VERSION"
        }
        val dbVersion = root.optInt("databaseVersion")
        require(dbVersion in OLDEST_COMPATIBLE_DATABASE_VERSION..BACKUP_DATABASE_VERSION) {
            "This backup uses unsupported data version $dbVersion; this build supports versions " +
                "$OLDEST_COMPATIBLE_DATABASE_VERSION through $BACKUP_DATABASE_VERSION"
        }
        val tables = root.optJSONObject("tables") ?: error("Backup has no table data")
        val tableNames = tables.keys().asSequence().toSet()
        val expected = when (dbVersion) {
            BACKUP_DATABASE_VERSION -> EXPORT_TABLES.toSet()
            15, 14 -> VERSION_FIFTEEN_EXPORT_TABLES.toSet()
            13 -> VERSION_THIRTEEN_EXPORT_TABLES.toSet()
            9 -> VERSION_THIRTEEN_EXPORT_TABLES.toSet()
            8 -> VERSION_EIGHT_EXPORT_TABLES.toSet()
            else -> LEGACY_EXPORT_TABLES.toSet()
        }
        val tableSetIsCompatible = tableNames == expected ||
            (
                dbVersion < BACKUP_DATABASE_VERSION &&
                    tableNames.containsAll(expected) &&
                    EXPORT_TABLES.toSet().containsAll(tableNames)
            )
        require(tableSetIsCompatible) {
            "Backup table set does not match this build"
        }
        require(tableNames.all { tables.optJSONArray(it) != null }) { "Backup contains invalid table data" }
        tableNames.forEach { table ->
            val rows = tables.getJSONArray(table)
            val seen = mutableSetOf<String>()
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val stable = when {
                    row.has("uuid") && !row.isNull("uuid") -> row.optString("uuid").takeIf(String::isNotBlank)
                    row.opt("id") is String -> row.optString("id").takeIf(String::isNotBlank)
                    else -> null
                } ?: continue
                require(seen.add(stable)) { "Backup contains duplicate stable identity '$stable' in $table" }
            }
        }
        return root
    }

    /** Upgrade a checksum-verified older envelope in memory before restore or merge. */
    private fun upgradeBackupTables(databaseVersion: Int, tables: JSONObject) {
        if (databaseVersion < 8 && !tables.has("habit_skips")) {
            val logs = tables.getJSONArray("habit_logs")
            val retainedLogs = JSONArray()
            val skips = JSONArray()
            val obsoleteMetricEntryIds = mutableSetOf<String>()
            for (index in 0 until logs.length()) {
                val row = logs.getJSONObject(index)
                when (row.optString("status")) {
                    "Skipped", "Excused" -> {
                        val logUuid = row.optString("uuid")
                        skips.put(
                            JSONObject()
                                .put("uuid", "habit-skip-$logUuid")
                                .put("habitId", row.getLong("habitId"))
                                .put("localEpochDay", row.getLong("localEpochDay"))
                                .put("skippedAtMillis", row.optLong("timestampMillis"))
                                .put("createdAtMillis", row.optLong("createdAtMillis"))
                                .put("updatedAtMillis", row.optLong("updatedAtMillis")),
                        )
                        row.optString("metricEntryId").takeIf(String::isNotBlank)?.let(obsoleteMetricEntryIds::add)
                    }
                    "Missing" -> row.optString("metricEntryId").takeIf(String::isNotBlank)?.let(obsoleteMetricEntryIds::add)
                    else -> retainedLogs.put(row)
                }
            }
            val deduplicatedSkips = JSONArray()
            val seenOccurrences = mutableSetOf<Pair<Long, Long>>()
            for (index in 0 until skips.length()) {
                val row = skips.getJSONObject(index)
                if (seenOccurrences.add(row.getLong("habitId") to row.getLong("localEpochDay"))) deduplicatedSkips.put(row)
            }
            val entries = tables.getJSONArray("metric_entries")
            val retainedEntries = JSONArray()
            for (index in 0 until entries.length()) {
                entries.getJSONObject(index).takeUnless { it.optString("id") in obsoleteMetricEntryIds }?.let(retainedEntries::put)
            }
            tables.put("habit_logs", retainedLogs)
            tables.put("metric_entries", retainedEntries)
            tables.put("habit_skips", deduplicatedSkips)
        }
        if (databaseVersion < 9 && !tables.has("gym_machine_exercise_joins")) {
            val joins = JSONArray()
            val machines = tables.getJSONArray("gym_machines")
            for (index in 0 until machines.length()) {
                val machine = machines.getJSONObject(index)
                if (machine.has("exerciseId") && !machine.isNull("exerciseId")) {
                    joins.put(
                        JSONObject()
                            .put("machineId", machine.getLong("id"))
                            .put("exerciseId", machine.getLong("exerciseId")),
                    )
                }
            }
            tables.put("gym_machine_exercise_joins", joins)
        }
        if (databaseVersion < 13) upgradeTypedGymProgramming(tables)
        if (databaseVersion < 14 && !tables.has("training_max_decisions")) {
            tables.put("training_max_decisions", JSONArray())
        }
        if (databaseVersion < 16) {
            if (!tables.has("goal_completion_snapshots")) tables.put("goal_completion_snapshots", JSONArray())
            if (!tables.has("goal_elapsed_reset_events")) tables.put("goal_elapsed_reset_events", JSONArray())
            val goalUuids = mutableMapOf<Long, String>()
            tables.optJSONArray("goals")?.forEachObject { goal ->
                goal.optLongOrNull("id")?.let { id ->
                    goal.optString("uuid").takeIf(String::isNotBlank)?.let { goalUuids[id] = it }
                }
            }
            tables.optJSONArray("goal_completion_snapshots")?.forEachObject { row ->
                if (!row.has("uuid") || row.optString("uuid").isBlank()) {
                    val goalId = row.optLong("goalId")
                    val goalIdentity = goalUuids[goalId] ?: "missing-$goalId"
                    row.put("uuid", "legacy-goal-closure:$goalIdentity:${row.optLong("id")}")
                }
            }
            tables.optJSONArray("goals")?.forEachObject { row ->
                if (!row.has("archived")) {
                    val legacyArchived = row.optString("status") == "Archived"
                    row.put("archived", if (legacyArchived) 1 else 0)
                    if (legacyArchived) row.put("status", "Active")
                }
            }
        }
        retireAutomationBackupRows(tables)
    }

    /** Mirrors the Room 32 -> 33 semantic backfill for checksum-valid portable backups. */
    private fun upgradeTypedGymProgramming(tables: JSONObject) {
        fun inferredWorkSection(row: JSONObject): String = when {
            row.optString("note").startsWith("Main Work ·") -> "Main"
            row.optString("note").startsWith("Supplemental ·") -> "Supplemental"
            else -> "Unspecified"
        }

        val routineSets = tables.optJSONArray("routine_sets") ?: JSONArray()
        val routineSetsByExercise = mutableMapOf<Long, MutableList<JSONObject>>()
        for (index in 0 until routineSets.length()) {
            val row = routineSets.getJSONObject(index)
            if (!row.has("workSection")) row.put("workSection", inferredWorkSection(row))
            if (!row.has("optionalWorkKind")) row.put("optionalWorkKind", "None")
            if (!row.has("mainWorkScheme")) row.put("mainWorkScheme", "")
            if (!row.has("supplementalScheme")) row.put("supplementalScheme", "")
            routineSetsByExercise.getOrPut(row.optLong("routineExerciseId"), ::mutableListOf) += row
        }

        val routineExercises = tables.optJSONArray("routine_exercises") ?: JSONArray()
        for (index in 0 until routineExercises.length()) {
            val row = routineExercises.getJSONObject(index)
            val sets = routineSetsByExercise[row.optLong("id")].orEmpty()
            val main = sets.filter { it.optString("workSection") == "Main" }
            val supplemental = sets.filter { it.optString("workSection") == "Supplemental" }
            if (!row.has("mainWorkScheme")) {
                row.put(
                    "mainWorkScheme",
                    when {
                        main.any { it.optString("classification") == "Amrap" } -> "ClassicPrSet"
                        main.isNotEmpty() && main.all { it.optInt("repetitions") == 5 } -> "FivesPro"
                        main.isNotEmpty() -> "ClassicMinimumReps"
                        else -> "Unspecified"
                    },
                )
            }
            if (!row.has("supplementalScheme")) {
                row.put(
                    "supplementalScheme",
                    when {
                        supplemental.any { it.optInt("repetitions") == 10 } -> "BoringButBig"
                        supplemental.isNotEmpty() -> "FirstSetLast"
                        else -> "None"
                    },
                )
            }
            if (!row.has("assistanceRole")) {
                row.put("assistanceRole", if (main.isEmpty()) "Unspecified" else "MainLift")
            }
            if (!row.has("placementKind")) {
                row.put(
                    "placementKind",
                    when {
                        main.isNotEmpty() || row.optString("assistanceRole") == "MainLift" -> "MainLift"
                        sets.any { it.optString("workSection") == "Assistance" } ||
                            row.optString("assistanceRole") in setOf("Push", "Pull", "SingleLegCore", "Other") -> "Assistance"
                        else -> "General"
                    },
                )
            }
            if (!row.has("assistanceCategory")) {
                row.put(
                    "assistanceCategory",
                    row.optString("assistanceRole").takeIf { it in setOf("Push", "Pull", "SingleLegCore", "Other") }
                        ?: if (row.optString("placementKind") == "Assistance") "Other" else "Unspecified",
                )
            }
            if (!row.has("jokerSetsEnabled")) row.put("jokerSetsEnabled", false)
        }

        val workoutSets = tables.optJSONArray("workout_sets") ?: JSONArray()
        val workoutSetsByExercise = mutableMapOf<Long, MutableList<JSONObject>>()
        for (index in 0 until workoutSets.length()) {
            val row = workoutSets.getJSONObject(index)
            if (!row.has("workSectionSnapshot")) row.put("workSectionSnapshot", inferredWorkSection(row))
            if (!row.has("optionalWorkKindSnapshot")) row.put("optionalWorkKindSnapshot", "None")
            workoutSetsByExercise.getOrPut(row.optLong("workoutExerciseId"), ::mutableListOf) += row
        }

        val workoutExercises = tables.optJSONArray("workout_exercises") ?: JSONArray()
        for (index in 0 until workoutExercises.length()) {
            val row = workoutExercises.getJSONObject(index)
            val sets = workoutSetsByExercise[row.optLong("id")].orEmpty()
            val main = sets.filter { it.optString("workSectionSnapshot") == "Main" }
            val supplemental = sets.filter { it.optString("workSectionSnapshot") == "Supplemental" }
            if (!row.has("mainWorkSchemeSnapshot")) {
                row.put(
                    "mainWorkSchemeSnapshot",
                    when {
                        main.any { it.optString("classification") == "Amrap" } -> "ClassicPrSet"
                        main.isNotEmpty() && main.all { it.optInt("repetitions") == 5 } -> "FivesPro"
                        main.isNotEmpty() -> "ClassicMinimumReps"
                        else -> "Unspecified"
                    },
                )
            }
            if (!row.has("supplementalSchemeSnapshot")) {
                row.put(
                    "supplementalSchemeSnapshot",
                    when {
                        supplemental.any { it.optInt("repetitions") == 10 } -> "BoringButBig"
                        supplemental.isNotEmpty() -> "FirstSetLast"
                        else -> "None"
                    },
                )
            }
            if (!row.has("assistanceRoleSnapshot")) {
                row.put("assistanceRoleSnapshot", if (main.isEmpty()) "Unspecified" else "MainLift")
            }
            if (!row.has("placementKindSnapshot")) {
                row.put(
                    "placementKindSnapshot",
                    when {
                        main.isNotEmpty() || row.optString("assistanceRoleSnapshot") == "MainLift" -> "MainLift"
                        sets.any { it.optString("workSectionSnapshot") == "Assistance" } ||
                            row.optString("assistanceRoleSnapshot") in setOf("Push", "Pull", "SingleLegCore", "Other") -> "Assistance"
                        else -> "General"
                    },
                )
            }
            if (!row.has("assistanceCategorySnapshot")) {
                row.put(
                    "assistanceCategorySnapshot",
                    row.optString("assistanceRoleSnapshot")
                        .takeIf { it in setOf("Push", "Pull", "SingleLegCore", "Other") }
                        ?: if (row.optString("placementKindSnapshot") == "Assistance") "Other" else "Unspecified",
                )
            }
            if (!row.has("jokerSetsEnabledSnapshot")) row.put("jokerSetsEnabledSnapshot", false)
        }

        val routines = tables.optJSONArray("gym_routines") ?: JSONArray()
        for (index in 0 until routines.length()) {
            val row = routines.getJSONObject(index)
            if (!row.has("trainingMaxIncreaseEligible")) row.put("trainingMaxIncreaseEligible", true)
            if (!row.has("programPhaseRolesCsv")) {
                val labels = row.optString("programPhaseLabelsCsv")
                    .split(',').map(String::trim).filter(String::isNotBlank)
                row.put(
                    "programPhaseRolesCsv",
                    labels.joinToString(",") { label ->
                        when {
                            label.contains("deload", ignoreCase = true) -> "Deload"
                            label.contains("tm test", ignoreCase = true) -> "TrainingMaxTest"
                            label.contains("pr test", ignoreCase = true) -> "PersonalRecordTest"
                            label.contains("leader", ignoreCase = true) -> "Leader"
                            label.contains("anchor", ignoreCase = true) -> "Anchor"
                            else -> "Standard"
                        }
                    },
                )
            }
            if (!row.has("trainingMaxAdvanceAfterPhaseIndicesCsv")) {
                val finalPhase = row.optInt("programPhaseCount", 1) - 1
                row.put(
                    "trainingMaxAdvanceAfterPhaseIndicesCsv",
                    finalPhase.toString().takeIf {
                        row.optString("programKind", "Static") != "Static" && finalPhase >= 0
                    }.orEmpty(),
                )
            }
            if (!row.has("programTemplateKey")) {
                row.put(
                    "programTemplateKey",
                    if (row.optString("programKind", "Static") in setOf(
                            "FiveThreeOne", "FiveThreeOneClassic", "FiveSPro", "BoringButBig", "FirstSetLast",
                        )
                    ) "LegacyFiveThreeOne" else "None",
                )
            }
            if (!row.has("programTemplateRevision")) {
                row.put("programTemplateRevision", if (row.optString("programTemplateKey") == "None") 0 else 1)
            }
        }

        val sessions = tables.optJSONArray("workout_sessions") ?: JSONArray()
        for (index in 0 until sessions.length()) {
            val row = sessions.getJSONObject(index)
            if (!row.has("sourceRoutinePhaseLabel")) row.put("sourceRoutinePhaseLabel", "")
            if (!row.has("sourceRoutinePhaseRole")) row.put("sourceRoutinePhaseRole", "Standard")
        }
    }
}

private fun ContentValues.applyBackupCompatibilityDefaults(table: String) {
    if (table == "track_fields" && !containsKey("scaleStep")) put("scaleStep", 1.0)
    if (table == "tasks" && !containsKey("icon")) put("icon", DEFAULT_TASK_EMOJI)
    if (table == "goals" && !containsKey("archived")) {
        val legacyArchived = getAsString("status") == "Archived"
        put("archived", legacyArchived)
        if (legacyArchived) put("status", "Active")
    }
    if (table == "gym_machines" && !containsKey("levelDirection")) put("levelDirection", "HigherNumberMoreResistance")
    if (table == "gym_routines") {
        if (!containsKey("programKind")) put("programKind", "Static")
        if (!containsKey("programPhaseCount")) put("programPhaseCount", 1)
        if (!containsKey("programPhaseLabelsCsv")) put("programPhaseLabelsCsv", "")
        if (!containsKey("currentProgramPhaseIndex")) put("currentProgramPhaseIndex", 0)
        if (!containsKey("currentProgramCycle")) put("currentProgramCycle", 1)
        if (!containsKey("nextProgramDayPosition")) put("nextProgramDayPosition", 0)
        if (!containsKey("trainingMaxIncreaseEligible")) put("trainingMaxIncreaseEligible", true)
        if (!containsKey("programPhaseRolesCsv")) put("programPhaseRolesCsv", "")
        if (!containsKey("trainingMaxAdvanceAfterPhaseIndicesCsv")) {
            val finalPhase = (getAsInteger("programPhaseCount") ?: 1) - 1
            put(
                "trainingMaxAdvanceAfterPhaseIndicesCsv",
                finalPhase.toString().takeIf {
                    getAsString("programKind") != "Static" && finalPhase >= 0
                }.orEmpty(),
            )
        }
        if (!containsKey("programTemplateKey")) {
            put(
                "programTemplateKey",
                if (getAsString("programKind") in setOf(
                        "FiveThreeOne", "FiveThreeOneClassic", "FiveSPro", "BoringButBig", "FirstSetLast",
                    )
                ) "LegacyFiveThreeOne" else "None",
            )
        }
        if (!containsKey("programTemplateRevision")) {
            put("programTemplateRevision", if (getAsString("programTemplateKey") == "None") 0 else 1)
        }
        if (!containsKey("progressionMode")) put("progressionMode", "Standard")
        if (!containsKey("allowNonStandardHigherSuggestions")) put("allowNonStandardHigherSuggestions", false)
    }
    if (table == "routine_days" && !containsKey("progressionIndex")) put("progressionIndex", 0)
    if (table == "routine_exercises") {
        if (!containsKey("trainingMaxUnitId")) put("trainingMaxUnitId", "kilogram")
        if (!containsKey("trainingMaxSource")) put("trainingMaxSource", "EstimatedOneRepMaxPercent")
        if (!containsKey("mainWorkScheme")) put("mainWorkScheme", "Unspecified")
        if (!containsKey("supplementalScheme")) put("supplementalScheme", "None")
        if (!containsKey("assistanceRole")) put("assistanceRole", "Unspecified")
        if (!containsKey("placementKind")) {
            put(
                "placementKind",
                when (getAsString("assistanceRole")) {
                    "MainLift" -> "MainLift"
                    "Push", "Pull", "SingleLegCore", "Other" -> "Assistance"
                    else -> "General"
                },
            )
        }
        if (!containsKey("assistanceCategory")) {
            put(
                "assistanceCategory",
                getAsString("assistanceRole").takeIf { it in setOf("Push", "Pull", "SingleLegCore", "Other") }
                    ?: if (getAsString("placementKind") == "Assistance") "Other" else "Unspecified",
            )
        }
        if (!containsKey("jokerSetsEnabled")) put("jokerSetsEnabled", false)
        if (!containsKey("trainingMaxBasisKind")) {
            put(
                "trainingMaxBasisKind",
                if (containsKey("trainingMaxValue") && getAsDouble("trainingMaxValue") != null) {
                    "ExplicitTrainingMax"
                } else {
                    "Unspecified"
                },
            )
        }
        if (!containsKey("trainingMaxBasisValue") && containsKey("trainingMaxValue")) {
            put("trainingMaxBasisValue", getAsDouble("trainingMaxValue"))
        }
        if (!containsKey("trainingMaxBasisUnitId")) {
            put("trainingMaxBasisUnitId", getAsString("trainingMaxUnitId").orEmpty())
        }
        if (!containsKey("trainingMaxIncreaseEligible")) put("trainingMaxIncreaseEligible", true)
    }
    if (table == "routine_sets") {
        if (!containsKey("workSection")) put("workSection", "Unspecified")
        if (!containsKey("optionalWorkKind")) put("optionalWorkKind", "None")
        if (!containsKey("mainWorkScheme")) put("mainWorkScheme", "")
        if (!containsKey("supplementalScheme")) put("supplementalScheme", "")
    }
    if (table == "workout_sessions") {
        if (!containsKey("sourceRoutineProgramKind")) put("sourceRoutineProgramKind", "Static")
        if (!containsKey("programProgressAdvanced")) put("programProgressAdvanced", false)
        if (!containsKey("requiredMainWorkInvalidated")) put("requiredMainWorkInvalidated", false)
        if (!containsKey("invalidatedMainExerciseIdsCsv")) put("invalidatedMainExerciseIdsCsv", "")
        if (!containsKey("sourceRoutinePhaseLabel")) put("sourceRoutinePhaseLabel", "")
        if (!containsKey("sourceRoutinePhaseRole")) put("sourceRoutinePhaseRole", "Standard")
    }
    if (table == "workout_exercises") {
        if (!containsKey("trainingMaxUnitIdSnapshot")) put("trainingMaxUnitIdSnapshot", "")
        if (!containsKey("trainingMaxSourceSnapshot")) {
            put("trainingMaxSourceSnapshot", "EstimatedOneRepMaxPercent")
        }
        if (!containsKey("mainWorkSchemeSnapshot")) put("mainWorkSchemeSnapshot", "Unspecified")
        if (!containsKey("supplementalSchemeSnapshot")) put("supplementalSchemeSnapshot", "None")
        if (!containsKey("assistanceRoleSnapshot")) put("assistanceRoleSnapshot", "Unspecified")
        if (!containsKey("placementKindSnapshot")) {
            put(
                "placementKindSnapshot",
                when (getAsString("assistanceRoleSnapshot")) {
                    "MainLift" -> "MainLift"
                    "Push", "Pull", "SingleLegCore", "Other" -> "Assistance"
                    else -> "General"
                },
            )
        }
        if (!containsKey("assistanceCategorySnapshot")) {
            put(
                "assistanceCategorySnapshot",
                getAsString("assistanceRoleSnapshot")
                    .takeIf { it in setOf("Push", "Pull", "SingleLegCore", "Other") }
                    ?: if (getAsString("placementKindSnapshot") == "Assistance") "Other" else "Unspecified",
            )
        }
        if (!containsKey("jokerSetsEnabledSnapshot")) put("jokerSetsEnabledSnapshot", false)
    }
    if (table == "workout_sets") {
        if (!containsKey("workSectionSnapshot")) put("workSectionSnapshot", "Unspecified")
        if (!containsKey("optionalWorkKindSnapshot")) put("optionalWorkKindSnapshot", "None")
        if (!containsKey("prescribedClassificationSnapshot")) {
            put("prescribedClassificationSnapshot", getAsString("classification") ?: "Working")
        }
    }
}

private fun ContentValues.retireAutomation(table: String) {
    when (table) {
        "link_rules" -> put("enabled", false)
        "trigger_rules" -> {
            put("enabled", false)
            put("notificationEnabled", false)
        }
        "trigger_occurrences" -> if (getAsLong("fulfilledEntryId") == null && getAsLong("dismissedAtMillis") == null) {
            put("dismissedAtMillis", getAsLong("deliveredAtMillis") ?: getAsLong("availableAtMillis") ?: 0L)
            putNull("remindAtMillis")
        }
    }
}

private fun retireAutomationBackupRows(tables: JSONObject) {
    tables.optJSONArray("link_rules")?.forEachObject { row -> row.put("enabled", 0) }
    tables.optJSONArray("trigger_rules")?.forEachObject { row ->
        row.put("enabled", 0)
        row.put("notificationEnabled", 0)
    }
    tables.optJSONArray("trigger_occurrences")?.forEachObject { row ->
        if (row.optLongOrNull("fulfilledEntryId") == null && row.optLongOrNull("dismissedAtMillis") == null) {
            row.put(
                "dismissedAtMillis",
                row.optLongOrNull("deliveredAtMillis") ?: row.optLongOrNull("availableAtMillis") ?: 0L,
            )
            row.put("remindAtMillis", JSONObject.NULL)
        }
    }
}

private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (index in 0 until length()) block(getJSONObject(index))
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

private fun JSONArray.findObjectByNumericId(id: Long): JSONObject? {
    for (index in 0 until length()) {
        val candidate = optJSONObject(index) ?: continue
        if (candidate.optLongOrNull("id") == id) return candidate
    }
    return null
}

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

private fun existingNaturalChildId(
    db: androidx.sqlite.db.SupportSQLiteDatabase,
    table: String,
    values: ContentValues,
): Long? {
    val (parentColumn, positionColumn) = when (table) {
        "link_rule_conditions" -> "linkRuleId" to "position"
        "trigger_rule_conditions" -> "triggerRuleId" to "position"
        "trigger_field_mappings" -> "triggerRuleId" to "targetFieldId"
        else -> return null
    }
    val parent = values.getAsLong(parentColumn) ?: return null
    val position = values.getAsLong(positionColumn) ?: return null
    return db.query(
        "SELECT id FROM ${safeIdentifier(table)} WHERE ${safeIdentifier(parentColumn)} = ? AND ${safeIdentifier(positionColumn)} = ? LIMIT 1",
        arrayOf(parent.toString(), position.toString()),
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }
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
        "Track" -> "tracks"
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
        "workout_sessions" -> {
            remap("sourceRoutineId", "gym_routines")
            remap("sourceRoutineDayId", "routine_days")
        }
        "workout_exercises" -> {
            remap("machineId", "gym_machines")
            remapCsv("alternativeExerciseIdsCsvSnapshot", "exercises")
        }
        "routine_exercises" -> {
            remap("machineId", "gym_machines")
            remapCsv("alternativeExerciseIdsCsv", "exercises")
        }
        "personal_records" -> remap("machineId", "gym_machines")
        "gym_machine_exercise_joins" -> {
            remap("machineId", "gym_machines")
            remap("exerciseId", "exercises")
        }
        "graph_presets" -> remapCsv("exerciseIdsCsv", "exercises")
        "link_rules" -> {
            sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
            if (values.getAsString("sourceType") == "Subtask") remap("sourceItemId", "task_steps")
        }
        "contributions" -> sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
        "trigger_rules" -> {
            sourceParent(values.getAsString("sourceType"))?.let { remap("sourceEntityId", it) }
            if (values.getAsString("sourceType") == "Subtask") remap("sourceItemId", "task_steps")
            when (values.getAsString("targetType")) {
                "Habit" -> remap("targetEntityId", "habits")
                "Task" -> remap("targetEntityId", "tasks")
                "Track" -> remap("targetEntityId", "tracks")
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
private const val OLDEST_COMPATIBLE_DATABASE_VERSION = 5
private const val BACKUP_DATABASE_VERSION = 16

private fun ContentValues.normalizeIdentityEmoji(table: String) {
    val defaultEmoji = when (table) {
        "tasks" -> DEFAULT_TASK_EMOJI
        "habits" -> DEFAULT_HABIT_EMOJI
        "goals" -> DEFAULT_GOAL_EMOJI
        "tracks" -> DEFAULT_TRACK_EMOJI
        else -> return
    }
    put("icon", getAsString("icon").orEmpty().normalizedIdentityEmoji(defaultEmoji))
}

private val EXPORT_TABLES = listOf(
    "areas",
    "tasks", "task_occurrences", "task_steps", "task_step_states", "task_step_snapshots",
    "unit_definitions", "metric_definitions", "metric_entries", "tags",
    "exercises", "exercise_categories", "exercise_category_joins", "gym_machines", "gym_machine_exercise_joins",
    "gym_routines", "routine_days", "routine_exercises", "routine_sets", "training_max_decisions",
    "workout_sessions", "workout_groups", "workout_exercises", "workout_sets",
    "personal_records", "graph_presets", "habits", "habit_checklist_items", "habit_logs",
    "habit_checklist_states", "habit_pauses", "habit_skips", "goals", "goal_milestones",
    "goal_completion_snapshots", "goal_elapsed_reset_events",
    "tracks", "track_fields", "track_choice_options", "track_entries", "track_values",
    "link_rules", "link_rule_conditions", "link_condition_choices", "contributions", "trigger_rules", "trigger_rule_conditions", "trigger_condition_choices", "trigger_field_mappings", "trigger_occurrences",
)

private val VERSION_FIFTEEN_EXPORT_TABLES = EXPORT_TABLES - setOf("goal_completion_snapshots", "goal_elapsed_reset_events")
private val VERSION_THIRTEEN_EXPORT_TABLES = VERSION_FIFTEEN_EXPORT_TABLES - "training_max_decisions"
private val VERSION_EIGHT_EXPORT_TABLES = VERSION_THIRTEEN_EXPORT_TABLES - "gym_machine_exercise_joins"
private val LEGACY_EXPORT_TABLES = VERSION_EIGHT_EXPORT_TABLES - "habit_skips"

private fun checksumPayload(tables: JSONObject, settings: JSONObject?): String =
    tables.toString() + "\n" + settings?.toString().orEmpty()

private fun AppSettings.toJson(): JSONObject = JSONObject()
    .put("setupCompleted", setupCompleted)
    .put("powerMode", powerMode)
    .put("lowPressureMode", lowPressureMode)
    .put("notificationPermissionRequested", notificationPermissionRequested)
    .put("activeAreaScope", activeAreaScope)
    .put("areaOpeningMode", areaOpeningMode.name)
    .put("chosenOpeningAreaScope", chosenOpeningAreaScope)
    .put("hardSetClassifications", JSONArray(hardSetClassifications.toList()))
    .put("categoryAllocationMode", categoryAllocationMode)
    .put("adjustE1rmForEffort", adjustE1rmForEffort)
    .put("includeAssistedInPersonalRecords", includeAssistedInPersonalRecords)
    .put("themeMode", themeMode.name)
    .put("dynamicColor", dynamicColor)
    .put("compactItemLayout", compactItemLayout)
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
    .put("restTimerPresetSeconds", JSONArray(restTimerPresetSeconds))
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
    .put("activeTaskSortMode", activeTaskSortMode)
    .put("defaultHabitWeekStart", defaultHabitWeekStart.name)
    .put("naturalLanguageTaskCapture", naturalLanguageTaskCapture)
    .put("customIdentityEmojis", JSONArray(customIdentityEmojis.map { choice ->
        JSONObject().put("emoji", choice.emoji).put("name", choice.name)
    }))
    .put("savedTaskFilters", JSONArray(savedTaskFilters.map { filter ->
        JSONObject()
            .put("name", filter.name)
            .put("priorities", JSONArray(filter.priorities.map(TaskPriority::name)))
            .put("areaId", filter.areaId ?: JSONObject.NULL)
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
            .put("sortDescending", filter.sortDescending)
            .put("groupMode", filter.groupMode)
    }))
    .put("homeTaskFilterName", homeTaskFilterName ?: JSONObject.NULL)
    .put("reviewSections", JSONArray(reviewSections.map(ReviewSection::name)))
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
    .put("trackedGymRecords", JSONArray(trackedGymRecords.map { selection ->
        JSONObject()
            .put("exerciseUuid", selection.exerciseUuid)
            .put("type", selection.type.name)
            .put("secondaryValue", selection.secondaryValue ?: JSONObject.NULL)
            .put("machineProfileUuid", selection.machineProfileUuid ?: JSONObject.NULL)
            .put("position", selection.position)
    }))
    .putNullableLong("focusTimerDeadlineMillis", focusTimerDeadlineMillis)
    .putNullableLong("focusTimerTaskId", focusTimerTaskId)

private fun JSONObject.toAppSettings(): AppSettings = AppSettings(
    setupCompleted = optBoolean("setupCompleted", true),
    powerMode = optBoolean("powerMode", true),
    lowPressureMode = optBoolean("lowPressureMode", false),
    notificationPermissionRequested = optBoolean("notificationPermissionRequested", false),
    activeAreaScope = optString("activeAreaScope", "all"),
    areaOpeningMode = enumValue("areaOpeningMode", AreaOpeningMode.LastUsed),
    chosenOpeningAreaScope = optString("chosenOpeningAreaScope", "all"),
    hardSetClassifications = optJSONArray("hardSetClassifications")?.let { array ->
        (0 until array.length()).mapNotNullTo(mutableSetOf()) { index -> array.optString(index).takeIf(String::isNotBlank) }
    }?.ifEmpty { setOf("Working", "BackOff", "Drop", "Amrap", "Failure") }
        ?: setOf("Working", "BackOff", "Drop", "Amrap", "Failure"),
    categoryAllocationMode = optString("categoryAllocationMode", "Fractional"),
    adjustE1rmForEffort = optBoolean("adjustE1rmForEffort", false),
    includeAssistedInPersonalRecords = optBoolean("includeAssistedInPersonalRecords", false),
    themeMode = enumValue("themeMode", AppThemeMode.System),
    dynamicColor = optBoolean("dynamicColor", true),
    compactItemLayout = optBoolean("compactItemLayout", false),
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
    restTimerPresetSeconds = normalizeRestTimerPresets(
        optJSONArray("restTimerPresetSeconds")?.let { array ->
            (0 until array.length()).map { index -> array.optInt(index, -1) }
        } ?: DEFAULT_REST_TIMER_PRESET_SECONDS,
    ),
    timerSound = optBoolean("timerSound", true),
    timerVibration = optBoolean("timerVibration", true),
    keepScreenAwake = optBoolean("keepScreenAwake", false),
    restTimerAutoStart = optBoolean("restTimerAutoStart", true),
    showGymRpe = optBoolean("showGymRpe", false),
    showGymRir = optBoolean("showGymRir", false),
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
    activeTaskSortMode = optString("activeTaskSortMode", "Smart"),
    defaultHabitWeekStart = enumValue("defaultHabitWeekStart", DayOfWeek.MONDAY),
    naturalLanguageTaskCapture = optBoolean("naturalLanguageTaskCapture", true),
    customIdentityEmojis = optJSONArray("customIdentityEmojis").objects().mapNotNull { value ->
        val emoji = value.optString("emoji").takeIf(String::isNotBlank) ?: return@mapNotNull null
        val name = value.optString("name").takeIf(String::isNotBlank) ?: return@mapNotNull null
        CustomIdentityEmoji(emoji = emoji, name = name)
    },
    savedTaskFilters = optJSONArray("savedTaskFilters").objects().mapNotNull { value ->
        value.optString("name").takeIf(String::isNotBlank)?.let { name ->
            SavedTaskFilter(
                name = name,
                priorities = value.optJSONArray("priorities").enumNames<TaskPriority>(),
                areaId = value.nullableString("areaId"),
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
                sortDescending = value.optBoolean("sortDescending", false),
                groupMode = value.optString("groupMode", "None"),
            ).normalizedNavigation()
        }
    },
    homeTaskFilterName = nullableString("homeTaskFilterName"),
    reviewSections = optJSONArray("reviewSections").enumNames<ReviewSection>().ifEmpty { ReviewSection.entries.toSet() },
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
    trackedGymRecords = optJSONArray("trackedGymRecords").objects().mapNotNull { value ->
        TrackedGymRecord(
            exerciseUuid = value.optString("exerciseUuid").takeIf(String::isNotBlank) ?: return@mapNotNull null,
            type = runCatching { PersonalRecordType.valueOf(value.optString("type")) }.getOrNull()
                ?: return@mapNotNull null,
            secondaryValue = value.optDouble("secondaryValue").takeUnless(Double::isNaN),
            machineProfileUuid = value.nullableString("machineProfileUuid"),
            position = value.optInt("position"),
        )
    },
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
